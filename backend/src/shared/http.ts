import type { ApiResponse, Env, ErrorCode } from "../types";

const MAX_JSON_BYTES = 64 * 1024;

export function requestId(request: Request): string {
  const supplied = request.headers.get("X-Request-ID")?.trim();
  if (supplied && /^[A-Za-z0-9._-]{8,100}$/.test(supplied)) return supplied;
  return crypto.randomUUID();
}

export function jsonResponse<T>(
  env: Env,
  requestIdValue: string,
  status: number,
  data: T,
  extraHeaders: HeadersInit = {},
): Response {
  const body: ApiResponse<T> = {
    ok: status >= 200 && status < 300,
    requestId: requestIdValue,
    serverTime: Date.now(),
    data,
  };

  return response(env, status, body, extraHeaders);
}

export function errorResponse(
  env: Env,
  requestIdValue: string,
  status: number,
  code: ErrorCode,
  message: string,
  retryAfterSeconds?: number,
): Response {
  const body: ApiResponse<never> = {
    ok: false,
    requestId: requestIdValue,
    serverTime: Date.now(),
    error: {
      code,
      message,
      ...(retryAfterSeconds === undefined ? {} : { retryAfterSeconds }),
    },
  };

  const headers: HeadersInit =
    retryAfterSeconds === undefined ? {} : { "Retry-After": String(retryAfterSeconds) };
  return response(env, status, body, headers);
}

function response(
  env: Env,
  status: number,
  body: ApiResponse<unknown>,
  extraHeaders: HeadersInit,
): Response {
  const headers = new Headers(extraHeaders);
  headers.set("Content-Type", "application/json; charset=utf-8");
  headers.set("Cache-Control", "no-store");
  headers.set("X-Content-Type-Options", "nosniff");
  headers.set("Referrer-Policy", "no-referrer");
  headers.set("Access-Control-Allow-Origin", env.ALLOWED_ORIGIN || "*");
  headers.set("Vary", "Origin");

  return new Response(JSON.stringify(body), { status, headers });
}

export async function readJson<T>(request: Request): Promise<T> {
  const contentType = request.headers.get("Content-Type") ?? "";
  if (!contentType.toLowerCase().includes("application/json")) {
    throw new RequestBodyError("Content-Type must be application/json.");
  }

  const declaredSize = Number(request.headers.get("Content-Length") ?? "0");
  if (Number.isFinite(declaredSize) && declaredSize > MAX_JSON_BYTES) {
    throw new RequestBodyError("Request body is too large.");
  }

  const text = await request.text();
  if (new TextEncoder().encode(text).byteLength > MAX_JSON_BYTES) {
    throw new RequestBodyError("Request body is too large.");
  }

  try {
    return JSON.parse(text) as T;
  } catch {
    throw new RequestBodyError("Request body is not valid JSON.");
  }
}

export function corsPreflight(env: Env): Response {
  return new Response(null, {
    status: 204,
    headers: {
      "Access-Control-Allow-Origin": env.ALLOWED_ORIGIN || "*",
      "Access-Control-Allow-Headers":
        "Authorization, Content-Type, X-Firebase-AppCheck, X-Request-ID, X-Idempotency-Key",
      "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS",
      "Access-Control-Max-Age": "86400",
      Vary: "Origin",
    },
  });
}

export class RequestBodyError extends Error {}
