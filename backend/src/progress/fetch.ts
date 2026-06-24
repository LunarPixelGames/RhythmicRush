import type { Env } from "../types";

export interface CloudProgress {
  schemaVersion: number;
  contentVersion: number;
  revision: number;
  coins: number;
  points: number;
  completedLevels: number;
  levels: Array<{
    levelId: string;
    bestPercent: number;
    completed: boolean;
    attempts: number;
  }>;
  updatedAt: number;
}

export async function fetchProgress(env: Env, uid: string): Promise<CloudProgress> {
  const save = await env.DB.prepare(
    `SELECT schema_version, content_version, revision, legacy_coin_floor,
            earned_coins, points, completed_levels, updated_at
     FROM saves WHERE uid = ?`,
  )
    .bind(uid)
    .first<{
      schema_version: number;
      content_version: number;
      revision: number;
      legacy_coin_floor: number;
      earned_coins: number;
      points: number;
      completed_levels: number;
      updated_at: number;
    }>();

  if (!save) throw new Error("The user save was not initialized.");

  const levels = await env.DB.prepare(
    `SELECT lp.level_id, lp.best_percent, lp.completed,
            COALESCE(SUM(da.accepted_total), 0) AS attempts
     FROM level_progress lp
     LEFT JOIN device_attempts da
       ON da.uid = lp.uid AND da.level_id = lp.level_id
     WHERE lp.uid = ?
     GROUP BY lp.level_id, lp.best_percent, lp.completed
     ORDER BY lp.level_id`,
  )
    .bind(uid)
    .all<{
      level_id: string;
      best_percent: number;
      completed: number;
      attempts: number;
    }>();

  return {
    schemaVersion: save.schema_version,
    contentVersion: save.content_version,
    revision: save.revision,
    coins: Math.max(save.legacy_coin_floor, 0) + save.earned_coins,
    points: save.points,
    completedLevels: save.completed_levels,
    levels: levels.results.map((level) => ({
      levelId: level.level_id,
      bestPercent: level.best_percent,
      completed: level.completed === 1,
      attempts: Number(level.attempts),
    })),
    updatedAt: save.updated_at,
  };
}
