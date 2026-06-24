import type { AuthenticatedUser, Env } from "../types";
import { fetchProgress } from "./fetch";
import { enforceRateLimit } from "../shared/rateLimit";
import { isRecord, ValidationError } from "../shared/validation";

interface UploadLevel {
  levelId: string;
  bestPercent: number;
  totalAttempts: number;
  completed: boolean;
}

interface UploadBody {
  schemaVersion: number;
  contentVersion: number;
  deviceId: string;
  lastKnownRevision: number;
  legacyCoinFloor?: number;
  levels: UploadLevel[];
}

interface CatalogLevel {
  level_id: string;
  coin_reward: number;
  point_reward: number;
}

export interface UploadResult {
  progress: Awaited<ReturnType<typeof fetchProgress>>;
  levelsImproved: number;
  attemptsAdded: number;
  rewardsReconciled: boolean;
}

export async function uploadProgress(
  env: Env,
  user: AuthenticatedUser,
  bodyValue: unknown,
  idempotencyKey: string,
): Promise<UploadResult> {
  const body = validateUpload(bodyValue);
  const requestHash = await hashRequest(body);

  const replay = await env.DB.prepare(
    `SELECT request_hash, response_json
     FROM operations WHERE uid = ? AND operation_key = ?`,
  )
    .bind(user.uid, idempotencyKey)
    .first<{ request_hash: string; response_json: string }>();
  if (replay) {
    if (replay.request_hash !== requestHash) {
      throw new ProgressConflictError(
        "That operation ID was already used for different progress data.",
      );
    }
    return JSON.parse(replay.response_json) as UploadResult;
  }

  await enforceRateLimit(env, user.uid, "progress-upload", 30, 60 * 60);

  const catalogRows = await env.DB.prepare(
    `SELECT level_id, coin_reward, point_reward
     FROM level_catalog WHERE active = 1 AND content_version <= ?`,
  )
    .bind(body.contentVersion)
    .all<CatalogLevel>();
  const catalog = new Map(catalogRows.results.map((level) => [level.level_id, level]));

  const unknown = body.levels.find((level) => !catalog.has(level.levelId));
  if (unknown) {
    throw new UpdateRequiredError(`Unknown level ID ${unknown.levelId}. Update the game.`);
  }

  const currentSave = await env.DB.prepare(
    `SELECT revision FROM saves WHERE uid = ?`,
  )
    .bind(user.uid)
    .first<{ revision: number }>();
  if (!currentSave) throw new Error("The user save was not initialized.");
  if (body.lastKnownRevision > currentSave.revision) {
    throw new ProgressConflictError("The client revision is newer than the cloud revision.");
  }

  const existingLevels = await env.DB.prepare(
    `SELECT level_id, best_percent, completed
     FROM level_progress WHERE uid = ?`,
  )
    .bind(user.uid)
    .all<{ level_id: string; best_percent: number; completed: number }>();
  const levelState = new Map(existingLevels.results.map((level) => [level.level_id, level]));

  let levelsImproved = 0;
  let attemptsAdded = 0;
  let newCompletions = 0;
  const statements: D1PreparedStatement[] = [];
  const now = Date.now();
  const initialMigration = currentSave.revision === 0 && body.legacyCoinFloor !== undefined;

  for (const uploaded of body.levels) {
    const existingDevice = await env.DB.prepare(
      `SELECT accepted_total FROM device_attempts
       WHERE uid = ? AND device_id = ? AND level_id = ?`,
    )
      .bind(user.uid, body.deviceId, uploaded.levelId)
      .first<{ accepted_total: number }>();
    const acceptedBefore = existingDevice?.accepted_total ?? 0;
    if (uploaded.totalAttempts < acceptedBefore) {
      throw new ProgressConflictError(
        `Attempt total decreased for level ${uploaded.levelId}.`,
      );
    }
    attemptsAdded += uploaded.totalAttempts - acceptedBefore;

    statements.push(
      env.DB.prepare(
        `INSERT INTO device_attempts
           (uid, device_id, level_id, accepted_total, updated_at)
         VALUES (?, ?, ?, ?, ?)
         ON CONFLICT(uid, device_id, level_id) DO UPDATE SET
           accepted_total = MAX(device_attempts.accepted_total, excluded.accepted_total),
           updated_at = excluded.updated_at`,
      ).bind(user.uid, body.deviceId, uploaded.levelId, uploaded.totalAttempts, now),
    );

    const existing = levelState.get(uploaded.levelId);
    if (uploaded.bestPercent > (existing?.best_percent ?? 0)) levelsImproved += 1;
    const completed = uploaded.completed || uploaded.bestPercent >= 100;
    if (completed && existing?.completed !== 1) newCompletions += 1;

    statements.push(
      env.DB.prepare(
        `INSERT INTO level_progress
           (uid, level_id, best_percent, completed, completion_reward_granted,
            completed_at, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT(uid, level_id) DO UPDATE SET
           best_percent = MAX(level_progress.best_percent, excluded.best_percent),
           completed = MAX(level_progress.completed, excluded.completed),
           completion_reward_granted =
             MAX(level_progress.completion_reward_granted, excluded.completion_reward_granted),
           completed_at = CASE
             WHEN level_progress.completed_at IS NULL AND excluded.completed = 1
               THEN excluded.completed_at
             ELSE level_progress.completed_at
           END,
           updated_at = excluded.updated_at`,
      ).bind(
        user.uid,
        uploaded.levelId,
        uploaded.bestPercent,
        completed ? 1 : 0,
        completed ? 1 : 0,
        completed ? now : null,
        now,
      ),
    );

    if (completed) {
      const rewards = catalog.get(uploaded.levelId)!;
      statements.push(
        env.DB.prepare(
          `INSERT OR IGNORE INTO completion_rewards
             (uid, level_id, coin_reward, point_reward, granted_at)
           VALUES (?, ?, ?, ?, ?)`,
        ).bind(
          user.uid,
          uploaded.levelId,
          initialMigration ? 0 : rewards.coin_reward,
          rewards.point_reward,
          now,
        ),
      );
    }
  }

  statements.push(
    env.DB.prepare(
      `UPDATE saves SET
         schema_version = ?,
         content_version = ?,
         revision = revision + 1,
         legacy_coin_floor = MAX(legacy_coin_floor, ?),
         earned_coins = COALESCE(
           (SELECT SUM(coin_reward) FROM completion_rewards WHERE uid = ?), 0
         ),
         points = COALESCE(
           (SELECT SUM(point_reward) FROM completion_rewards WHERE uid = ?), 0
         ),
         completed_levels = (
           SELECT COUNT(*) FROM level_progress WHERE uid = ? AND completed = 1
         ),
         updated_at = ?
       WHERE uid = ?`,
    ).bind(
      body.schemaVersion,
      body.contentVersion,
      initialMigration ? (body.legacyCoinFloor ?? 0) : 0,
      user.uid,
      user.uid,
      user.uid,
      now,
      user.uid,
    ),
  );

  await env.DB.batch(statements);
  const progress = await fetchProgress(env, user.uid);
  const account = await env.DB.prepare(
    `SELECT username, status, leaderboard_banned
     FROM users WHERE uid = ?`,
  )
    .bind(user.uid)
    .first<{
      username: string | null;
      status: string;
      leaderboard_banned: number;
    }>();

  if (account?.username) {
    const eligible =
      account.status === "active" &&
      account.leaderboard_banned !== 1 &&
      (!user.email || user.emailVerified);
    await env.DB.prepare(
      `INSERT INTO leaderboard_entries
         (uid, username, points, completed_levels, score_achieved_at, eligible, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(uid) DO UPDATE SET
         username = excluded.username,
         points = excluded.points,
         completed_levels = excluded.completed_levels,
         score_achieved_at = CASE
           WHEN excluded.points <> leaderboard_entries.points
             THEN excluded.score_achieved_at
           ELSE leaderboard_entries.score_achieved_at
         END,
         eligible = excluded.eligible,
         updated_at = excluded.updated_at`,
    )
      .bind(
        user.uid,
        account.username,
        progress.points,
        progress.completedLevels,
        now,
        eligible ? 1 : 0,
        now,
      )
      .run();

    await env.DB.prepare(
      `UPDATE leaderboard_cache
       SET generated_at = 0, lease_until = NULL
       WHERE cache_key = 'global-points-v1'`,
    ).run();
  }

  const result: UploadResult = {
    progress,
    levelsImproved,
    attemptsAdded,
    rewardsReconciled: newCompletions > 0,
  };
  await env.DB.prepare(
    `INSERT INTO operations
       (uid, operation_key, operation_type, request_hash, status_code,
        response_json, created_at, expires_at)
     VALUES (?, ?, 'progress-upload', ?, 200, ?, ?, ?)`,
  )
    .bind(
      user.uid,
      idempotencyKey,
      requestHash,
      JSON.stringify(result),
      now,
      now + 7 * 24 * 60 * 60 * 1000,
    )
    .run();

  return result;
}

function validateUpload(value: unknown): UploadBody {
  if (!isRecord(value)) throw new ValidationError("The request body must be an object.");
  const schemaVersion = integer(value.schemaVersion, "schemaVersion", 1, 10);
  const contentVersion = integer(value.contentVersion, "contentVersion", 1, 1);
  if (typeof value.deviceId !== "string" ||
      !/^[0-9a-fA-F-]{36}$/.test(value.deviceId)) {
    throw new ValidationError("deviceId must be a UUID.");
  }
  const lastKnownRevision = integer(
    value.lastKnownRevision,
    "lastKnownRevision",
    0,
    Number.MAX_SAFE_INTEGER,
  );
  const legacyCoinFloor = value.legacyCoinFloor === undefined
    ? undefined
    : integer(value.legacyCoinFloor, "legacyCoinFloor", 0, 10_000_000);
  if (!Array.isArray(value.levels) || value.levels.length > 200) {
    throw new ValidationError("levels must be an array with at most 200 entries.");
  }
  const seen = new Set<string>();
  const levels = value.levels.map((entry, index): UploadLevel => {
    if (!isRecord(entry)) throw new ValidationError(`levels[${index}] must be an object.`);
    if (typeof entry.levelId !== "string" || !/^[0-9]{1,8}$/.test(entry.levelId)) {
      throw new ValidationError(`levels[${index}].levelId is invalid.`);
    }
    if (seen.has(entry.levelId)) throw new ValidationError("Duplicate level ID.");
    seen.add(entry.levelId);
    return {
      levelId: entry.levelId,
      bestPercent: integer(entry.bestPercent, "bestPercent", 0, 100),
      totalAttempts: integer(entry.totalAttempts, "totalAttempts", 0, 100_000_000),
      completed: entry.completed === true,
    };
  });
  return {
    schemaVersion,
    contentVersion,
    deviceId: value.deviceId,
    lastKnownRevision,
    ...(legacyCoinFloor === undefined ? {} : { legacyCoinFloor }),
    levels,
  };
}

function integer(
  value: unknown,
  name: string,
  minimum: number,
  maximum: number,
): number {
  if (typeof value !== "number" || !Number.isSafeInteger(value) ||
      value < minimum || value > maximum) {
    throw new ValidationError(`${name} is outside the supported range.`);
  }
  return value;
}

async function hashRequest(value: UploadBody): Promise<string> {
  const bytes = new TextEncoder().encode(JSON.stringify(value));
  const hash = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(hash))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

export class ProgressConflictError extends Error {}
export class UpdateRequiredError extends Error {}
