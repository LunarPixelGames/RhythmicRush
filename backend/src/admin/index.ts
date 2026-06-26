import type { Env, AuthenticatedUser } from "../types";

export async function requireAdmin(user: AuthenticatedUser): Promise<void> {
  const claims: any = user.claims as any;
  if (!claims || !(claims.admin === true || claims.rhythmic_admin === true)) {
    throw new AdminError("ADMIN_REQUIRED", "Admin privileges required.");
  }
}

export async function getDevLeaderboard(env: Env, limit = 100) {
  const rows = await env.DB.prepare(
    `SELECT l.uid, l.username, l.points, l.completed_levels, l.score_achieved_at, l.eligible,
            u.status, u.leaderboard_banned, u.username AS account_username
     FROM leaderboard_entries l
     LEFT JOIN users u ON u.uid = l.uid
     ORDER BY l.points DESC, l.completed_levels DESC, l.score_achieved_at ASC
     LIMIT ?`,
  )
    .bind(limit)
    .all();
  return rows.results;
}

export async function getUserDetails(env: Env, uid: string) {
  const account = await env.DB.prepare(`SELECT uid, username, email_verified, status, created_at, updated_at, leaderboard_banned, leaderboard_ban_reason FROM users WHERE uid = ?`).bind(uid).first();
  if (!account) return null;

  const save = await env.DB.prepare(`SELECT * FROM saves WHERE uid = ?`).bind(uid).first();
  const levels = await env.DB.prepare(`SELECT level_id, best_percent, completed, completed_at, updated_at FROM level_progress WHERE uid = ?`).bind(uid).all();
  const attempts = await env.DB.prepare(`SELECT device_id, accepted_total, updated_at FROM device_attempts WHERE uid = ?`).bind(uid).all();

  return {
    account,
    save: save ?? null,
    level_progress: levels.results ?? [],
    device_attempts: attempts.results ?? [],
  };
}

export async function banUser(env: Env, uid: string, ban: boolean, reason?: string) {
  const now = Date.now();
  if (ban) {
    await env.DB.prepare(`UPDATE users SET leaderboard_banned = 1, leaderboard_ban_reason = ?, updated_at = ? WHERE uid = ?`).bind(reason ?? null, now, uid).run();
  } else {
    await env.DB.prepare(`UPDATE users SET leaderboard_banned = 0, leaderboard_ban_reason = NULL, updated_at = ? WHERE uid = ?`).bind(now, uid).run();
  }
  // Invalidate leaderboard cache so moderation takes effect quickly
  await env.DB.prepare(`UPDATE leaderboard_cache SET generated_at = 0, lease_until = NULL WHERE cache_key = 'global-points-v1'`).run();
  return { ok: true };
}

export async function renameUser(env: Env, uid: string, newDisplayName: string, force = false) {
  const normalized = newDisplayName.trim().toLowerCase();
  if (!normalized) throw new AdminError("BAD_REQUEST", "Username cannot be empty.");

  // check existing reservation
  const existing = await env.DB.prepare(`SELECT uid FROM usernames WHERE normalized = ?`).bind(normalized).first();
  if (existing && existing.uid !== uid) {
    if (!force) throw new AdminError("CONFLICT", "Username already reserved by another account.");
    // force takeover
    await env.DB.prepare(`DELETE FROM usernames WHERE normalized = ?`).bind(normalized).run();
  }

  const now = Date.now();
  // upsert usernames table
  await env.DB.prepare(`INSERT OR REPLACE INTO usernames (normalized, display_name, uid, reserved_at, released_at) VALUES (?, ?, ?, ?, NULL)`).bind(normalized, newDisplayName, uid, now).run();
  // update users and leaderboard_entries
  await env.DB.prepare(`UPDATE users SET username = ?, updated_at = ? WHERE uid = ?`).bind(newDisplayName, now, uid).run();
  await env.DB.prepare(`UPDATE leaderboard_entries SET username = ? WHERE uid = ?`).bind(newDisplayName, uid).run();
  // invalidate caches
  await env.DB.prepare(`UPDATE leaderboard_cache SET generated_at = 0, lease_until = NULL WHERE cache_key = 'global-points-v1'`).run();

  return { ok: true };
}

export async function adminDeleteUser(env: Env, targetUid: string) {
  // delete user data similarly to deleteCloudAccount but without auth checks
  const now = Date.now();
  await env.DB.batch([
    env.DB.prepare("DELETE FROM usernames WHERE uid = ?").bind(targetUid),
    env.DB.prepare("DELETE FROM users WHERE uid = ?").bind(targetUid),
    env.DB.prepare("DELETE FROM rate_limits WHERE subject = ?").bind(targetUid),
    env.DB.prepare(
      "DELETE FROM merge_tickets WHERE canonical_uid = ? OR secondary_uid = ?",
    ).bind(targetUid, targetUid),
    env.DB.prepare("DELETE FROM deletion_requests WHERE uid = ?").bind(targetUid),
    env.DB.prepare(
      `UPDATE leaderboard_cache
        SET generated_at = 0, lease_until = NULL
        WHERE cache_key = 'global-points-v1'`,
    ),
  ]);
  console.log("Admin deleted cloud account data", { uid: targetUid, deletedAt: now });
  return { deleted: true };
}

export class AdminError extends Error {
  constructor(readonly code: string, message: string) {
    super(message);
  }
}

