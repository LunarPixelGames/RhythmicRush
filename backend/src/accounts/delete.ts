import type { AuthenticatedUser, Env } from "../types";
import { isRecord } from "../shared/validation";

export interface DeleteResult {
  deleted: true;
}

export async function deleteCloudAccount(
  env: Env,
  user: AuthenticatedUser,
  body: unknown,
): Promise<DeleteResult> {
  if (!isRecord(body) || body.confirm !== true) {
    throw new DeleteAccountError("Set confirm to true to delete cloud account data.");
  }

  requireRecentAuthentication(user);

  const now = Date.now();
  await env.DB.batch([
    env.DB.prepare("DELETE FROM usernames WHERE uid = ?").bind(user.uid),
    env.DB.prepare("DELETE FROM users WHERE uid = ?").bind(user.uid),
    env.DB.prepare("DELETE FROM rate_limits WHERE subject = ?").bind(user.uid),
    env.DB.prepare(
      "DELETE FROM merge_tickets WHERE canonical_uid = ? OR secondary_uid = ?",
    ).bind(user.uid, user.uid),
    env.DB.prepare("DELETE FROM deletion_requests WHERE uid = ?").bind(user.uid),
    env.DB.prepare(
      `UPDATE leaderboard_cache
       SET generated_at = 0, lease_until = NULL
       WHERE cache_key = 'global-points-v1'`,
    ),
  ]);

  console.log("Deleted cloud account data", { uid: user.uid, deletedAt: now });
  return { deleted: true };
}

function requireRecentAuthentication(user: AuthenticatedUser): void {
  const nowSeconds = Math.floor(Date.now() / 1000);
  const authenticationTime = user.claims.auth_time;
  if (
    authenticationTime === undefined ||
    !Number.isFinite(authenticationTime) ||
    nowSeconds - authenticationTime > 5 * 60
  ) {
    throw new RecentAuthenticationRequiredError(
      "Sign in again before deleting cloud account data.",
    );
  }
}

export class DeleteAccountError extends Error {}
export class RecentAuthenticationRequiredError extends Error {}
