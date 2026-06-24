import { describe, expect, it } from "vitest";
import { isFeatureEnabled, requireFeature, FeatureDisabledError } from "../src/shared/features";
import type { Env } from "../src/types";

function env(overrides: Partial<Env> = {}): Env {
  return {
    DB: {} as D1Database,
    ENVIRONMENT: "test",
    FIREBASE_PROJECT_ID: "rhythmic-rush-test",
    FIREBASE_PROJECT_NUMBER: "123",
    APP_CHECK_MODE: "off",
    ALLOWED_ORIGIN: "*",
    ...overrides,
  };
}

describe("feature flags", () => {
  it("keeps normal dev features enabled by default", () => {
    const value = env();
    expect(isFeatureEnabled(value, "newAccounts")).toBe(true);
    expect(isFeatureEnabled(value, "progressUploads")).toBe(true);
    expect(isFeatureEnabled(value, "leaderboardRefresh")).toBe(true);
  });

  it("keeps account deletion disabled until recovery email is configured", () => {
    expect(isFeatureEnabled(env(), "accountDeletion")).toBe(false);
    expect(() => requireFeature(env(), "accountDeletion", "disabled")).toThrow(
      FeatureDisabledError,
    );
  });

  it("honors explicit rollback switches", () => {
    const value = env({
      ENABLE_NEW_ACCOUNTS: "false",
      ENABLE_PROGRESS_UPLOADS: "off",
      ENABLE_LEADERBOARD_REFRESH: "0",
      ENABLE_ACCOUNT_DELETION: "true",
    });
    expect(isFeatureEnabled(value, "newAccounts")).toBe(false);
    expect(isFeatureEnabled(value, "progressUploads")).toBe(false);
    expect(isFeatureEnabled(value, "leaderboardRefresh")).toBe(false);
    expect(isFeatureEnabled(value, "accountDeletion")).toBe(true);
  });
});
