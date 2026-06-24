import type { AuthenticatedUser, Env, FirebaseClaims } from "../types";

const FIREBASE_JWKS_URL =
  "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
const CLOCK_SKEW_SECONDS = 60;

interface JwtHeader {
  alg?: string;
  kid?: string;
  typ?: string;
}

interface FirebaseJsonWebKey extends JsonWebKey {
  kid?: string;
}

interface JsonWebKeySet {
  keys: FirebaseJsonWebKey[];
}

let cachedKeys = new Map<string, JsonWebKey>();
let cachedKeysExpireAt = 0;

export async function authenticate(request: Request, env: Env): Promise<AuthenticatedUser> {
  const authorization = request.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) {
    throw new FirebaseAuthError("AUTH_REQUIRED", "A Firebase ID token is required.");
  }

  const token = authorization.slice("Bearer ".length).trim();
  const claims = await verifyFirebaseIdToken(token, env.FIREBASE_PROJECT_ID);

  if (env.APP_CHECK_MODE === "enforce") {
    throw new FirebaseAuthError(
      "APP_CHECK_REQUIRED",
      "App Check enforcement is not enabled until token verification is implemented.",
    );
  }

  return {
    uid: claims.sub,
    ...(claims.email ? { email: claims.email } : {}),
    emailVerified: claims.email_verified === true,
    providers: providerNames(claims),
    claims,
  };
}

export async function verifyFirebaseIdToken(
  token: string,
  projectId: string,
  nowSeconds = Math.floor(Date.now() / 1000),
): Promise<FirebaseClaims> {
  const parts = token.split(".");
  if (parts.length !== 3) {
    throw new FirebaseAuthError("AUTH_INVALID", "The Firebase ID token is malformed.");
  }

  const encodedHeader = parts[0];
  const encodedPayload = parts[1];
  const encodedSignature = parts[2];
  if (!encodedHeader || !encodedPayload || !encodedSignature) {
    throw new FirebaseAuthError("AUTH_INVALID", "The Firebase ID token is malformed.");
  }

  const header = decodeJson<JwtHeader>(encodedHeader);
  const claims = decodeJson<FirebaseClaims>(encodedPayload);

  if (header.alg !== "RS256" || !header.kid) {
    throw new FirebaseAuthError("AUTH_INVALID", "The Firebase ID token algorithm is invalid.");
  }

  const key = await firebasePublicKey(header.kid);
  const cryptoKey = await crypto.subtle.importKey(
    "jwk",
    key,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["verify"],
  );

  const verified = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    base64UrlBytes(encodedSignature),
    new TextEncoder().encode(`${encodedHeader}.${encodedPayload}`),
  );
  if (!verified) {
    throw new FirebaseAuthError("AUTH_INVALID", "The Firebase ID token signature is invalid.");
  }

  validateClaims(claims, projectId, nowSeconds);
  return claims;
}

async function firebasePublicKey(kid: string): Promise<JsonWebKey> {
  if (Date.now() >= cachedKeysExpireAt || !cachedKeys.has(kid)) {
    const response = await fetch(FIREBASE_JWKS_URL, {
      headers: { Accept: "application/json" },
      cf: { cacheTtl: 3600, cacheEverything: true },
    });
    if (!response.ok) {
      throw new FirebaseAuthError(
        "AUTH_INVALID",
        "Firebase token verification keys are temporarily unavailable.",
      );
    }

    const body = (await response.json()) as JsonWebKeySet;
    cachedKeys = new Map(
      body.keys
        .filter((candidate) => typeof candidate.kid === "string")
        .map((candidate) => [candidate.kid as string, candidate]),
    );
    cachedKeysExpireAt = Date.now() + cacheLifetimeMs(response.headers.get("Cache-Control"));
  }

  const key = cachedKeys.get(kid);
  if (!key) {
    throw new FirebaseAuthError("AUTH_INVALID", "The Firebase ID token key is unknown.");
  }
  return key;
}

function validateClaims(claims: FirebaseClaims, projectId: string, nowSeconds: number): void {
  if (claims.aud !== projectId) {
    throw new FirebaseAuthError("AUTH_INVALID", "The Firebase ID token audience is invalid.");
  }
  if (claims.iss !== `https://securetoken.google.com/${projectId}`) {
    throw new FirebaseAuthError("AUTH_INVALID", "The Firebase ID token issuer is invalid.");
  }
  if (typeof claims.sub !== "string" || claims.sub.length === 0 || claims.sub.length > 128) {
    throw new FirebaseAuthError("AUTH_INVALID", "The Firebase user ID is invalid.");
  }
  if (!Number.isFinite(claims.exp) || claims.exp < nowSeconds - CLOCK_SKEW_SECONDS) {
    throw new FirebaseAuthError("AUTH_EXPIRED", "The Firebase ID token has expired.");
  }
  if (!Number.isFinite(claims.iat) || claims.iat > nowSeconds + CLOCK_SKEW_SECONDS) {
    throw new FirebaseAuthError("AUTH_INVALID", "The Firebase ID token issue time is invalid.");
  }
  if (
    claims.auth_time !== undefined &&
    (!Number.isFinite(claims.auth_time) || claims.auth_time > nowSeconds + CLOCK_SKEW_SECONDS)
  ) {
    throw new FirebaseAuthError("AUTH_INVALID", "The Firebase authentication time is invalid.");
  }
}

function providerNames(claims: FirebaseClaims): string[] {
  const identities = claims.firebase?.identities;
  if (identities) return Object.keys(identities).sort();
  const provider = claims.firebase?.sign_in_provider;
  return provider ? [provider] : [];
}

function decodeJson<T>(encoded: string): T {
  try {
    return JSON.parse(new TextDecoder().decode(base64UrlBytes(encoded))) as T;
  } catch {
    throw new FirebaseAuthError("AUTH_INVALID", "The Firebase ID token is malformed.");
  }
}

function base64UrlBytes(value: string): Uint8Array<ArrayBuffer> {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
  let decoded: string;
  try {
    decoded = atob(padded);
  } catch {
    throw new FirebaseAuthError("AUTH_INVALID", "The Firebase ID token is malformed.");
  }

  const bytes = new Uint8Array(decoded.length);
  for (let index = 0; index < decoded.length; index += 1) {
    bytes[index] = decoded.charCodeAt(index);
  }
  return bytes;
}

function cacheLifetimeMs(cacheControl: string | null): number {
  const match = cacheControl?.match(/(?:^|,)\s*max-age=(\d+)/i);
  const seconds = match?.[1] ? Number(match[1]) : 3600;
  return Math.max(60, Math.min(seconds, 24 * 60 * 60)) * 1000;
}

export class FirebaseAuthError extends Error {
  constructor(
    readonly code: "AUTH_REQUIRED" | "AUTH_INVALID" | "AUTH_EXPIRED" | "APP_CHECK_REQUIRED",
    message: string,
  ) {
    super(message);
  }
}
