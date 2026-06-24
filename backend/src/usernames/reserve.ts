import type { AuthenticatedUser, Env } from "../types";
import { enforceRateLimit } from "../shared/rateLimit";
import { isRecord, requireString, ValidationError } from "../shared/validation";

const USERNAME_PATTERN = /^[A-Za-z0-9_]+$/;
const RESERVED_NAMES = new Set([
  "admin",
  "administrator",
  "firebase",
  "google",
  "moderator",
  "rhythmicrush",
  "support",
  "system",
]);

export interface ReservedUsername {
  username: string;
  normalized: string;
}

export async function reserveUsername(
  env: Env,
  user: AuthenticatedUser,
  body: unknown,
): Promise<ReservedUsername> {
  if (user.email && !user.emailVerified) {
    throw new UsernameError(
      "EMAIL_VERIFICATION_REQUIRED",
      "Verify your email before choosing a public username.",
    );
  }
  if (!isRecord(body)) throw new ValidationError("The request body must be an object.");

  const username = requireString(body.username, "username", 3, 20);
  if (!USERNAME_PATTERN.test(username)) {
    throw new ValidationError("Username may contain only letters, digits, and underscores.");
  }
  const normalized = username.toLowerCase();
  if (RESERVED_NAMES.has(normalized)) {
    throw new ValidationError("That username is reserved.");
  }

  await enforceRateLimit(env, user.uid, "username-reserve", 10, 60 * 60);
  await env.DB.prepare(
    `DELETE FROM usernames
     WHERE released_at IS NOT NULL AND released_at <= ?`,
  )
    .bind(Date.now() - 30 * 24 * 60 * 60 * 1000)
    .run();

  const current = await env.DB.prepare("SELECT username FROM users WHERE uid = ?")
    .bind(user.uid)
    .first<{ username: string | null }>();
  if (current?.username) {
    if (current.username.toLowerCase() === normalized) {
      return { username: current.username, normalized };
    }
    throw new UsernameError(
      "CONFLICT",
      "Username changes are unavailable until rename cooldown support is implemented.",
    );
  }

  const now = Date.now();
  try {
    await env.DB.batch([
      env.DB.prepare(
        `INSERT INTO usernames (normalized, display_name, uid, reserved_at)
         VALUES (?, ?, ?, ?)`,
      ).bind(normalized, username, user.uid, now),
      env.DB.prepare("UPDATE users SET username = ?, updated_at = ? WHERE uid = ?").bind(
        username,
        now,
        user.uid,
      ),
    ]);
  } catch (error) {
    if (String(error).toLowerCase().includes("unique")) {
      throw new UsernameError("CONFLICT", "That username is already taken.");
    }
    throw error;
  }

  return { username, normalized };
}

export class UsernameError extends Error {
  constructor(
    readonly code: "CONFLICT" | "EMAIL_VERIFICATION_REQUIRED",
    message: string,
  ) {
    super(message);
  }
}
