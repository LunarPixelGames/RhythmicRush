export interface Env {
  DB: D1Database;
  ENVIRONMENT: string;
  FIREBASE_PROJECT_ID: string;
  FIREBASE_PROJECT_NUMBER: string;
  APP_CHECK_MODE: "off" | "monitor" | "enforce";
  ALLOWED_ORIGIN: string;
  ENABLE_NEW_ACCOUNTS?: string;
  ENABLE_PROGRESS_UPLOADS?: string;
  ENABLE_LEADERBOARD_REFRESH?: string;
  ENABLE_ACCOUNT_DELETION?: string;
}

export interface FirebaseClaims {
  aud: string;
  auth_time?: number;
  email?: string;
  email_verified?: boolean;
  exp: number;
  firebase?: {
    identities?: Record<string, string[]>;
    sign_in_provider?: string;
  };
  iat: number;
  iss: string;
  sub: string;
  user_id?: string;
}

export interface AuthenticatedUser {
  uid: string;
  email?: string;
  emailVerified: boolean;
  providers: string[];
  claims: FirebaseClaims;
}

export type ErrorCode =
  | "ACCOUNT_DELETED"
  | "ADMIN_REQUIRED"
  | "AUTH_REQUIRED"
  | "AUTH_INVALID"
  | "AUTH_EXPIRED"
  | "APP_CHECK_REQUIRED"
  | "BAD_REQUEST"
  | "CONFLICT"
  | "EMAIL_VERIFICATION_REQUIRED"
  | "FEATURE_NOT_READY"
  | "METHOD_NOT_ALLOWED"
  | "NOT_FOUND"
  | "RATE_LIMITED"
  | "SERVER_ERROR"
  | "UPDATE_REQUIRED";

export interface ApiResponse<T> {
  ok: boolean;
  requestId: string;
  serverTime: number;
  data?: T;
  error?: {
    code: ErrorCode;
    message: string;
    retryAfterSeconds?: number;
  };
}
