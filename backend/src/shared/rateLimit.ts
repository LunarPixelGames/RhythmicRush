import type { Env } from "../types";

export async function enforceRateLimit(
  env: Env,
  subject: string,
  action: string,
  maximum: number,
  windowSeconds: number,
): Promise<void> {
  const now = Math.floor(Date.now() / 1000);
  const windowStart = now - windowSeconds;
  const result = await env.DB.prepare(
    `INSERT INTO rate_limits (subject, action, window_started_at, count)
     VALUES (?, ?, ?, 1)
     ON CONFLICT(subject, action) DO UPDATE SET
       window_started_at = CASE
         WHEN rate_limits.window_started_at <= ? THEN excluded.window_started_at
         ELSE rate_limits.window_started_at
       END,
       count = CASE
         WHEN rate_limits.window_started_at <= ? THEN 1
         ELSE rate_limits.count + 1
       END
     RETURNING window_started_at, count`,
  )
    .bind(subject, action, now, windowStart, windowStart)
    .first<{ window_started_at: number; count: number }>();

  if (!result) throw new Error("Rate-limit state could not be read.");
  if (result.count > maximum) {
    const retryAfterSeconds = Math.max(1, result.window_started_at + windowSeconds - now);
    throw new RateLimitError(retryAfterSeconds);
  }
}

export class RateLimitError extends Error {
  constructor(readonly retryAfterSeconds: number) {
    super("Too many requests.");
  }
}
