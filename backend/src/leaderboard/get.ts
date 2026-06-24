import type { Env } from "../types";
import { enforceRateLimit } from "../shared/rateLimit";

const CACHE_KEY = "global-points-v1";
const CACHE_TTL_MS = 5 * 60 * 1000;
const CACHE_LEASE_MS = 15 * 1000;

interface RankedEntry {
  uid: string;
  username: string;
  points: number;
  completedLevels: number;
  scoreAchievedAt: number;
}

interface CachedLeaderboard {
  entries: RankedEntry[];
}

export interface LeaderboardSnapshot {
  entries: Array<{
    rank: number;
    username: string;
    points: number;
    completedLevels: number;
    currentPlayer: boolean;
  }>;
  currentPlayer: {
    rank: number;
    username: string;
    points: number;
    completedLevels: number;
  } | null;
  currentPlayerStatus: "ranked" | "unranked" | "ineligible" | "banned";
  generatedAt: number;
  nextRefreshAt: number;
}

export async function getLeaderboard(
  env: Env,
  uid: string,
  forceRefresh: boolean,
): Promise<LeaderboardSnapshot> {
  const now = Date.now();
  if (forceRefresh) {
    await enforceRateLimit(env, uid, "leaderboard-refresh", 1, 5 * 60);
  }

  const cached = await readCache(env);
  const cacheIsFresh =
    cached !== null &&
    cached.generatedAt > 0 &&
    now - cached.generatedAt < CACHE_TTL_MS;

  let source = cached;
  if (!cacheIsFresh) {
    source = await regenerateWithLease(env, cached, now);
  }
  if (!source || source.generatedAt <= 0) {
    source = {
      payload: { entries: await queryTopEntries(env) },
      generatedAt: now,
    };
  }

  const ranked = source.payload.entries.map((entry, index) => ({
    rank: index + 1,
    username: entry.username,
    points: entry.points,
    completedLevels: entry.completedLevels,
    currentPlayer: entry.uid === uid,
  }));

  const ownTopIndex = source.payload.entries.findIndex((entry) => entry.uid === uid);
  let currentPlayer: LeaderboardSnapshot["currentPlayer"] =
    ownTopIndex >= 0
      ? {
          rank: ownTopIndex + 1,
          username: source.payload.entries[ownTopIndex]!.username,
          points: source.payload.entries[ownTopIndex]!.points,
          completedLevels: source.payload.entries[ownTopIndex]!.completedLevels,
        }
      : null;

  const account = await env.DB.prepare(
    `SELECT username, status, leaderboard_banned
     FROM users WHERE uid = ?`,
  )
    .bind(uid)
    .first<{
      username: string | null;
      status: string;
      leaderboard_banned: number;
    }>();

  let currentPlayerStatus: LeaderboardSnapshot["currentPlayerStatus"] = "unranked";
  if (account?.leaderboard_banned === 1) {
    currentPlayerStatus = "banned";
  } else if (account?.status !== "active" || !account?.username) {
    currentPlayerStatus = "ineligible";
  } else {
    const own = await env.DB.prepare(
      `SELECT username, points, completed_levels, score_achieved_at, eligible
       FROM leaderboard_entries WHERE uid = ?`,
    )
      .bind(uid)
      .first<{
        username: string;
        points: number;
        completed_levels: number;
        score_achieved_at: number;
        eligible: number;
      }>();

    if (own?.eligible === 1) {
      currentPlayerStatus = "ranked";
      if (!currentPlayer) {
        const ahead = await env.DB.prepare(
          `SELECT COUNT(*) AS count
           FROM leaderboard_entries
           WHERE eligible = 1 AND (
             points > ? OR
             (points = ? AND completed_levels > ?) OR
             (points = ? AND completed_levels = ? AND score_achieved_at < ?) OR
             (points = ? AND completed_levels = ? AND score_achieved_at = ? AND uid < ?)
           )`,
        )
          .bind(
            own.points,
            own.points,
            own.completed_levels,
            own.points,
            own.completed_levels,
            own.score_achieved_at,
            own.points,
            own.completed_levels,
            own.score_achieved_at,
            uid,
          )
          .first<{ count: number }>();

        currentPlayer = {
          rank: Number(ahead?.count ?? 0) + 1,
          username: own.username,
          points: own.points,
          completedLevels: own.completed_levels,
        };
      }
    } else if (own) {
      currentPlayerStatus = "ineligible";
    }
  }

  return {
    entries: ranked,
    currentPlayer,
    currentPlayerStatus,
    generatedAt: source.generatedAt,
    nextRefreshAt: now + CACHE_TTL_MS,
  };
}

async function queryTopEntries(env: Env): Promise<RankedEntry[]> {
  const rows = await env.DB.prepare(
    `SELECT uid, username, points, completed_levels, score_achieved_at
     FROM leaderboard_entries
     WHERE eligible = 1
     ORDER BY points DESC, completed_levels DESC, score_achieved_at ASC, uid ASC
     LIMIT 100`,
  ).all<{
    uid: string;
    username: string;
    points: number;
    completed_levels: number;
    score_achieved_at: number;
  }>();

  return rows.results.map((entry) => ({
    uid: entry.uid,
    username: entry.username,
    points: entry.points,
    completedLevels: entry.completed_levels,
    scoreAchievedAt: entry.score_achieved_at,
  }));
}

async function readCache(
  env: Env,
): Promise<{ payload: CachedLeaderboard; generatedAt: number } | null> {
  const row = await env.DB.prepare(
    `SELECT payload_json, generated_at
     FROM leaderboard_cache WHERE cache_key = ?`,
  )
    .bind(CACHE_KEY)
    .first<{ payload_json: string; generated_at: number }>();
  if (!row) return null;

  try {
    const parsed = JSON.parse(row.payload_json) as CachedLeaderboard;
    if (!Array.isArray(parsed.entries)) return null;
    return { payload: parsed, generatedAt: row.generated_at };
  } catch {
    return null;
  }
}

async function regenerateWithLease(
  env: Env,
  stale: { payload: CachedLeaderboard; generatedAt: number } | null,
  now: number,
): Promise<{ payload: CachedLeaderboard; generatedAt: number } | null> {
  await env.DB.prepare(
    `INSERT OR IGNORE INTO leaderboard_cache
       (cache_key, payload_json, generated_at, lease_until)
     VALUES (?, '{"entries":[]}', 0, NULL)`,
  )
    .bind(CACHE_KEY)
    .run();

  const lease = await env.DB.prepare(
    `UPDATE leaderboard_cache
     SET lease_until = ?
     WHERE cache_key = ?
       AND (lease_until IS NULL OR lease_until < ?)`,
  )
    .bind(now + CACHE_LEASE_MS, CACHE_KEY, now)
    .run();

  if ((lease.meta.changes ?? 0) < 1) return stale;

  try {
    const payload: CachedLeaderboard = { entries: await queryTopEntries(env) };
    await env.DB.prepare(
      `UPDATE leaderboard_cache
       SET payload_json = ?, generated_at = ?, lease_until = NULL
       WHERE cache_key = ?`,
    )
      .bind(JSON.stringify(payload), now, CACHE_KEY)
      .run();
    return { payload, generatedAt: now };
  } catch (error) {
    await env.DB.prepare(
      `UPDATE leaderboard_cache SET lease_until = NULL WHERE cache_key = ?`,
    )
      .bind(CACHE_KEY)
      .run();
    throw error;
  }
}
