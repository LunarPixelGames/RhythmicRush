import type { AuthenticatedUser, Env } from "../types";

export interface AccountProfile {
  uid: string;
  username: string | null;
  email: string | null;
  emailVerified: boolean;
  providers: string[];
  status: string;
  createdAt: number;
  updatedAt: number;
}

export async function getProfile(
  env: Env,
  authenticatedUser: AuthenticatedUser,
): Promise<AccountProfile> {
  const row = await env.DB.prepare(
    `SELECT username, email_verified, providers_json, status, created_at, updated_at
     FROM users WHERE uid = ?`,
  )
    .bind(authenticatedUser.uid)
    .first<{
      username: string | null;
      email_verified: number;
      providers_json: string;
      status: string;
      created_at: number;
      updated_at: number;
    }>();

  if (!row) throw new Error("The authenticated profile was not initialized.");

  return {
    uid: authenticatedUser.uid,
    username: row.username,
    email: authenticatedUser.email ?? null,
    emailVerified: row.email_verified === 1,
    providers: parseProviders(row.providers_json),
    status: row.status,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

function parseProviders(value: string): string[] {
  try {
    const parsed = JSON.parse(value) as unknown;
    return Array.isArray(parsed)
      ? parsed.filter((provider): provider is string => typeof provider === "string")
      : [];
  } catch {
    return [];
  }
}
