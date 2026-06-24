import type { Env } from "../types";

export type FeatureFlag =
  | "newAccounts"
  | "progressUploads"
  | "leaderboardRefresh"
  | "accountDeletion";

export function isFeatureEnabled(env: Env, feature: FeatureFlag): boolean {
  switch (feature) {
    case "newAccounts":
      return readBoolean(env.ENABLE_NEW_ACCOUNTS, true);
    case "progressUploads":
      return readBoolean(env.ENABLE_PROGRESS_UPLOADS, true);
    case "leaderboardRefresh":
      return readBoolean(env.ENABLE_LEADERBOARD_REFRESH, true);
    case "accountDeletion":
      return readBoolean(env.ENABLE_ACCOUNT_DELETION, false);
  }
}

export function requireFeature(env: Env, feature: FeatureFlag, message: string): void {
  if (!isFeatureEnabled(env, feature)) throw new FeatureDisabledError(message);
}

function readBoolean(value: string | undefined, defaultValue: boolean): boolean {
  if (value === undefined || value.trim() === "") return defaultValue;
  switch (value.trim().toLowerCase()) {
    case "1":
    case "true":
    case "yes":
    case "on":
    case "enabled":
      return true;
    case "0":
    case "false":
    case "no":
    case "off":
    case "disabled":
      return false;
    default:
      return defaultValue;
  }
}

export class FeatureDisabledError extends Error {}
