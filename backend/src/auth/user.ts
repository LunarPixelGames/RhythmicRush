import type { AuthenticatedUser, Env } from "../types";
import { requireFeature } from "../shared/features";

export async function ensureUser(env: Env, user: AuthenticatedUser): Promise<void> {
  const tombstone = await env.DB.prepare("SELECT deleted_at FROM account_tombstones WHERE uid = ?")
    .bind(user.uid)
    .first<{ deleted_at: number }>();
  if (tombstone) {
    throw new UserStateError(
      "This cloud account was deleted. Sign out locally before creating another account.",
    );
  }

  const existing = await env.DB.prepare("SELECT status FROM users WHERE uid = ?")
    .bind(user.uid)
    .first<{ status: string }>();
  if (existing?.status === "deleted") {
    throw new UserStateError(
      "This cloud account is pending deletion. Use the recovery email to restore it.",
    );
  }
  if (existing?.status === "suspended") {
    throw new UserStateError("This cloud account is suspended.");
  }
  if (!existing) {
    requireFeature(
      env,
      "newAccounts",
      "New account creation is temporarily disabled.",
    );
  }

  const now = Date.now();
  await env.DB.prepare(
    `INSERT INTO users (
       uid, email_verified, providers_json, status, created_at, updated_at
     ) VALUES (?, ?, ?, 'active', ?, ?)
     ON CONFLICT(uid) DO UPDATE SET
       email_verified = excluded.email_verified,
       providers_json = excluded.providers_json,
       updated_at = excluded.updated_at`,
  )
    .bind(
      user.uid,
      user.emailVerified ? 1 : 0,
      JSON.stringify(user.providers),
      now,
      now,
    )
    .run();

  await env.DB.prepare(
    `INSERT INTO saves (uid, created_at, updated_at)
     VALUES (?, ?, ?)
     ON CONFLICT(uid) DO NOTHING`,
  )
    .bind(user.uid, now, now)
    .run();
}

export class UserStateError extends Error {}
