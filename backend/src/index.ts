import {
  deleteCloudAccount,
  DeleteAccountError,
  RecentAuthenticationRequiredError,
} from "./accounts/delete";
import { getProfile } from "./accounts/profile";
import { authenticate, FirebaseAuthError } from "./auth/firebase";
import { ensureUser, UserStateError } from "./auth/user";
import { getLeaderboard } from "./leaderboard/get";
import { fetchProgress } from "./progress/fetch";
import {
  ProgressConflictError,
  UpdateRequiredError,
  uploadProgress,
} from "./progress/upload";
import {
  corsPreflight,
  errorResponse,
  jsonResponse,
  readJson,
  requestId,
  RequestBodyError,
} from "./shared/http";
import { FeatureDisabledError, requireFeature } from "./shared/features";
import { RateLimitError } from "./shared/rateLimit";
import { ValidationError } from "./shared/validation";
import type { AuthenticatedUser, Env } from "./types";
import { reserveUsername, UsernameError } from "./usernames/reserve";

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const id = requestId(request);

    try {
      if (request.method === "OPTIONS") return corsPreflight(env);

      const url = new URL(request.url);
      if (url.pathname === "/health") {
        requireMethod(request, "GET");
        return jsonResponse(env, id, 200, {
          service: "rhythmic-rush-api",
          environment: env.ENVIRONMENT,
          status: "ok",
        });
      }

      if (!url.pathname.startsWith("/v1/")) {
        return errorResponse(env, id, 404, "NOT_FOUND", "Endpoint not found.");
      }

      const user = await authenticate(request, env);
      await ensureUser(env, user);

      if (url.pathname === "/v1/profile") {
        requireMethod(request, "GET");
        return jsonResponse(env, id, 200, await getProfile(env, user));
      }

      if (url.pathname === "/v1/usernames/reserve") {
        requireMethod(request, "POST");
        requireIdempotencyKey(request);
        const body = await readJson<unknown>(request);
        return jsonResponse(env, id, 200, await reserveUsername(env, user, body));
      }

      if (url.pathname === "/v1/progress/fetch") {
        requireMethod(request, "POST");
        return jsonResponse(env, id, 200, await fetchProgress(env, user.uid));
      }

      if (url.pathname === "/v1/progress/upload") {
        requireMethod(request, "POST");
        requireFeature(env, "progressUploads", "Cloud save uploads are temporarily disabled.");
        const operationKey = requireIdempotencyKey(request);
        const body = await readJson<unknown>(request);
        return jsonResponse(
          env,
          id,
          200,
          await uploadProgress(env, user, body, operationKey),
        );
      }

      if (url.pathname === "/v1/leaderboard") {
        requireMethod(request, "GET");
        const forceRefresh = url.searchParams.get("refresh") === "true";
        if (forceRefresh) {
          requireFeature(
            env,
            "leaderboardRefresh",
            "Manual leaderboard refresh is temporarily disabled.",
          );
        }
        return jsonResponse(env, id, 200, await getLeaderboard(env, user.uid, forceRefresh));
      }

      if (url.pathname === "/v1/accounts/merge") {
        requireMethod(request, "POST");
        return featureNotReady(env, id, "Account merge");
      }

      if (url.pathname === "/v1/account") {
        requireMethod(request, "DELETE");
        requireFeature(env, "accountDeletion", "Account deletion is not available yet.");
        const body = await readJson<unknown>(request);
        return jsonResponse(env, id, 200, await deleteCloudAccount(env, user, body));
      }

      return errorResponse(env, id, 404, "NOT_FOUND", "Endpoint not found.");
    } catch (error) {
      return handleError(env, id, error);
    }
  },
};

function requireMethod(request: Request, expected: string): void {
  if (request.method !== expected) {
    throw new MethodError(`This endpoint requires ${expected}.`);
  }
}

function requireIdempotencyKey(request: Request): string {
  const value = request.headers.get("X-Idempotency-Key")?.trim();
  if (!value || !/^[A-Za-z0-9._:-]{16,128}$/.test(value)) {
    throw new ValidationError(
      "X-Idempotency-Key must contain 16-128 safe identifier characters.",
    );
  }
  return value;
}

function featureNotReady(env: Env, id: string, name: string): Response {
  return errorResponse(
    env,
    id,
    503,
    "FEATURE_NOT_READY",
    `${name} is disabled until its validation phase is complete.`,
  );
}

function handleError(env: Env, id: string, error: unknown): Response {
  if (error instanceof FirebaseAuthError) {
    const status = error.code === "AUTH_REQUIRED" ? 401 : 403;
    return errorResponse(env, id, status, error.code, error.message);
  }
  if (error instanceof MethodError) {
    return errorResponse(env, id, 405, "METHOD_NOT_ALLOWED", error.message);
  }
  if (error instanceof FeatureDisabledError) {
    return errorResponse(env, id, 503, "FEATURE_NOT_READY", error.message);
  }
  if (
    error instanceof ValidationError ||
    error instanceof RequestBodyError ||
    error instanceof DeleteAccountError
  ) {
    return errorResponse(env, id, 400, "BAD_REQUEST", error.message);
  }
  if (error instanceof RecentAuthenticationRequiredError) {
    return errorResponse(env, id, 403, "AUTH_INVALID", error.message);
  }
  if (error instanceof UserStateError) {
    return errorResponse(env, id, 410, "ACCOUNT_DELETED", error.message);
  }
  if (error instanceof UsernameError) {
    const status = error.code === "EMAIL_VERIFICATION_REQUIRED" ? 403 : 409;
    return errorResponse(env, id, status, error.code, error.message);
  }
  if (error instanceof RateLimitError) {
    return errorResponse(
      env,
      id,
      429,
      "RATE_LIMITED",
      error.message,
      error.retryAfterSeconds,
    );
  }
  if (error instanceof ProgressConflictError) {
    return errorResponse(env, id, 409, "CONFLICT", error.message);
  }
  if (error instanceof UpdateRequiredError) {
    return errorResponse(env, id, 409, "UPDATE_REQUIRED", error.message);
  }

  console.error("Unhandled request error", {
    requestId: id,
    name: error instanceof Error ? error.name : "UnknownError",
    message: error instanceof Error ? error.message : String(error),
  });
  return errorResponse(env, id, 500, "SERVER_ERROR", "The server could not complete the request.");
}

class MethodError extends Error {}
