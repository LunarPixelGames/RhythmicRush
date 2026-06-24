# Rhythmic Rush Backend

This directory contains the free Cloudflare Worker and D1 foundation for accounts, cloud progress, and leaderboards.

The Worker trusts only verified Firebase ID tokens. It does not use a Firebase service-account key, Firebase Admin SDK, Firestore, Cloud Functions, or a paid Firebase plan.

## Current implementation status

Implemented:

- Native Web Crypto verification of Firebase RS256 ID tokens.
- Audience, issuer, subject, issue-time, authentication-time, and expiry checks.
- Automatic D1 user/save initialization after authenticated requests.
- Authenticated profile endpoint.
- Validated unique username reservation.
- Structured cloud-progress fetch.
- Top-100/current-player leaderboard reads.
- Backend cloud-data deletion.
- D1 schema, indexes, rate-limit storage, idempotency storage, merge tickets, and leaderboard cache tables.
- Strict JSON size/content checks and typed errors.

Intentionally disabled until later phases:

- Progress upload/reward mutation, pending a generated authoritative level catalog.
- Account merge, pending its two-session recovery protocol.
- App Check enforcement, pending verified App Check JWT handling and real Android metrics.

Account merge remains disabled and returns `FEATURE_NOT_READY`; it does not accept unvalidated data.

## Before running anything

Install the current Node.js LTS release from [nodejs.org](https://nodejs.org/). During installation, keep the option that adds Node.js to `PATH`.

Open a new PowerShell window and verify:

```powershell
node --version
npm.cmd --version
```

Use `npm.cmd` and `npx.cmd` in PowerShell if Windows blocks the `.ps1` wrappers. There is no need to weaken the PowerShell execution policy.

Do not continue until both commands print versions.

## Install development tools

From the project root:

```powershell
cd backend
npm.cmd install
```

This generates `package-lock.json`. Commit that lockfile with the backend.

Installing packages is not the same as deploying. Nothing is sent to Cloudflare by this command.

With npm 11, the first install may report pending install scripts for `esbuild`, `workerd`, and `sharp`. Approve the exact packages:

```powershell
npm.cmd approve-scripts esbuild workerd sharp
```

These are dependencies of Wrangler's Worker bundling/local runtime. Do not use a blanket approval command for unrelated future packages.

## Create local Wrangler configuration

Copy the example:

```powershell
Copy-Item wrangler.toml.example wrangler.toml
```

`wrangler.toml` is intentionally ignored by Git because it contains environment-specific resource IDs.

For local-only work, the example values are enough except for the D1 `database_id`, which is filled after the remote development database is created.

The Worker needs:

```text
ENVIRONMENT=development
FIREBASE_PROJECT_ID=rhythmic-rush-dev
FIREBASE_PROJECT_NUMBER=<development Firebase project number>
APP_CHECK_MODE=off
ALLOWED_ORIGIN=*
```

It does not need the Firebase Web API key. That key belongs to the future desktop client configuration.

## Local database

After dependencies are installed:

```powershell
npm.cmd run db:migrate:local
npm.cmd run dev
```

Wrangler will print a local URL. Open `/health` to confirm the Worker is running.

## Cloudflare login and development resources

Do this only when Codex asks for the deployment handoff:

```powershell
npx.cmd wrangler login
npx.cmd wrangler d1 create rhythmic-rush-dev
```

Copy the returned D1 database ID into the ignored `wrangler.toml`, then run:

```powershell
npm.cmd run db:migrate:remote
npm.cmd run deploy:dev
```

The intended Worker name is `rhythmic-rush-api-dev`.

## API routes

| Method | Route | State |
|---|---|---|
| `GET` | `/health` | Public health check |
| `GET` | `/v1/profile` | Implemented |
| `POST` | `/v1/usernames/reserve` | Implemented |
| `POST` | `/v1/progress/fetch` | Implemented |
| `POST` | `/v1/progress/upload` | Implemented with catalog validation and idempotent merge |
| `GET` | `/v1/leaderboard` | Read foundation implemented |
| `POST` | `/v1/accounts/merge` | Locked until merge implementation |
| `DELETE` | `/v1/account` | Backend-data deletion implemented |

All `/v1` routes require:

```text
Authorization: Bearer <Firebase ID token>
```

Mutations that support retries also require:

```text
X-Idempotency-Key: <stable operation UUID>
```

## Safe configuration rules

Never commit:

- `.dev.vars`
- `.env`
- `wrangler.toml`
- OAuth client secrets
- Firebase ID/refresh tokens
- Firebase service-account JSON
- Cloudflare API tokens

The Cloudflare account ID, Firebase project ID/number, and Firebase client API key are identifiers rather than administrator secrets, but keep environment-specific values in ignored configuration unless deliberately publishing them.

## Checks

When the user chooses to run them:

```powershell
npm.cmd run check
npm.cmd test
```

No Gradle or game build is required for these backend-only checks.
