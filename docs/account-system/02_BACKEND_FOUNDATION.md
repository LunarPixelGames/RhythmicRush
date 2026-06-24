# Phase 2: Free Backend Foundation

## Objective

Create a Cloudflare Worker and D1 backend that owns all authoritative mutations while Firebase Spark provides identity. No billing account is required.

## Before this phase

Do not start Phase 2 until Phase 1 checkpoints A through H are complete.

You should have:

- A Firebase development project on Spark.
- Its exact project ID and project number.
- Firebase Email/Password and Play Games providers enabled.
- A registered Firebase Android app and desktop web app.
- A free Cloudflare account.
- Node.js available locally; Codex will verify the version before scaffolding.

Do not manually create a Worker or D1 database yet. Their names and bindings must agree with repository configuration.

### Install Node.js if the commands are missing

If `node --version` or `npm --version` says the command is not recognized:

1. Open [Node.js downloads](https://nodejs.org/en/download).
2. Choose the current **LTS** release for Windows.
3. Download the Windows Installer (`.msi`) for x64 unless your Windows device is ARM-based.
4. Run the installer.
5. Accept the license.
6. Keep the default installation folder.
7. Keep **Node.js runtime**, **npm package manager**, and **Add to PATH** enabled.
8. The optional native-tools/Chocolatey checkbox is not required for this backend.
9. Finish the installer.
10. Close every existing PowerShell window.
11. Open a new PowerShell window in the project root.
12. Run:

```powershell
node --version
npm.cmd --version
```

Both commands must print versions. If either still says it is not recognized, restart Windows once and retry before changing PATH manually.

If plain `npm` is blocked because PowerShell script execution is disabled, use `npm.cmd` and `npx.cmd`. Do not weaken the Windows execution policy for this project.

## Exactly how this phase will run

### Step 1: Tell Codex Phase 1 is complete

Send only these non-secret values:

```text
Firebase project ID:
Firebase project number:
Firebase desktop Web API key:
Cloudflare account ID:
```

The Firebase Web API key identifies the Firebase project; it is not an administrator credential. Still, do not post it publicly.

Never send:

- Your Google or Cloudflare password.
- OAuth client secret.
- Firebase refresh or ID tokens.
- A service-account JSON file.
- Android keystore files/passwords.

### Step 2: Codex creates the local backend

**Codex**

Codex will:

1. Create the `backend/` folder shown below.
2. Add TypeScript, Wrangler, local D1 migrations, tests, and safe example configuration.
3. Add real secret/config files to `.gitignore`.
4. Configure a local-only database first.
5. Run local tests only when the user authorizes builds/tests.
6. Tell you when browser authorization is required.

You should not copy random Worker templates from the Cloudflare dashboard; the repository version will be the source of truth.

### Step 3: Authorize Wrangler when requested

Wrangler is Cloudflare's official command-line tool. When Codex says the backend scaffold is ready:

1. Codex starts `npx wrangler login`.
2. Your browser opens a Cloudflare authorization page.
3. Confirm the displayed Cloudflare account is the Rhythmic Rush account.
4. Review the requested permissions.
5. Click **Allow** or **Authorize**.
6. Return to Codex and say authorization completed.

Do not give Codex your Cloudflare password. The browser authorization grants the local Wrangler tool access without revealing it.

If the browser does not open, copy the one-time authorization URL shown by Wrangler into your browser. Do not share that URL.

### Step 4: Codex creates the development D1 database

**Codex**

Recommended resource name:

```text
rhythmic-rush-dev
```

Codex will create it through Wrangler, record the returned database ID in a local development configuration, and apply migrations.

To verify it in the dashboard:

1. Open [Cloudflare Dashboard](https://dash.cloudflare.com/).
2. Select the correct account.
3. Open **Storage & Databases**.
4. Open **D1 SQL Database** or **D1**.
5. Confirm `rhythmic-rush-dev` exists.
6. Open it and confirm the status is healthy.
7. Do not manually edit production-like player rows.

Cloudflare may move D1 under **Workers & Pages → D1**. Use the dashboard search for `D1` if necessary.

### Step 5: Codex deploys the development Worker

**Codex**

Recommended Worker name:

```text
rhythmic-rush-api-dev
```

Codex will bind the Worker to the development D1 database and deploy it.

To verify it:

1. Cloudflare Dashboard → **Workers & Pages**.
2. Open **Overview**.
3. Select `rhythmic-rush-api-dev`.
4. Open **Settings → Bindings**.
5. Confirm a D1 binding exists and points to `rhythmic-rush-dev`.
6. Open **Deployments** and confirm the latest deployment succeeded.
7. Copy the `workers.dev` URL into your private worksheet.

Do not add a custom domain yet.

### Step 6: Add non-secret variables and secrets

Codex will specify each value and whether it is a plain variable or secret.

For dashboard entry:

1. Cloudflare Dashboard → **Workers & Pages**.
2. Open `rhythmic-rush-api-dev`.
3. Open **Settings → Variables and Secrets**.
4. Click **Add**.
5. Choose **Variable** for public configuration such as the Firebase project ID.
6. Choose **Secret** only for genuinely secret server values generated during implementation.
7. Enter the exact name Codex supplies.
8. Paste the value.
9. Click **Save and deploy** if prompted.

Never place secrets directly in `wrangler.toml`, source code, screenshots, Git, or chat.

Expected public environment values include:

```text
ENVIRONMENT=development
FIREBASE_PROJECT_ID=<your exact Firebase project ID>
```

The Worker verifies Firebase tokens using Google's public keys. It does not require a Firebase service-account key.

### Step 7: Verify the free plan

1. Cloudflare Dashboard → account menu → **Billing** or **Plans**.
2. Confirm Workers is on **Free**.
3. Confirm D1 shows Free-plan usage.
4. Do not select Workers Paid.
5. Do not attach a paid subscription for this project.

Cloudflare may ask for payment details for unrelated domain products. They are not required for this Worker/D1 setup.

### Step 8: Local and development verification

**Codex**

Codex verifies:

1. Local Worker starts against local D1.
2. Migrations create all expected tables and indexes.
3. Requests without a Firebase token are rejected.
4. Valid development tokens identify the correct Firebase UID.
5. Invalid project, expired, or malformed tokens are rejected.
6. Payload and rate limits are enforced.
7. No client can access D1 directly.

**User**

When asked, sign into a development game build and perform one harmless test action, such as loading the empty account profile. Do not add real production player data.

### Step 9: Stop before production

Do not create:

- `rhythmic-rush-prod` D1.
- `rhythmic-rush-api` production Worker.
- A custom API domain.
- Production secrets.

Production resources are created during Phase 9 only after development testing passes.

## Repository layout

**Codex**

Create:

```text
backend/
  package.json
  package-lock.json
  tsconfig.json
  wrangler.toml.example
  migrations/
  src/
    index.ts
    auth/
    progress/
    leaderboard/
    usernames/
    accounts/
    shared/
  test/
  deletion-page/
```

Use TypeScript, Wrangler, D1 migrations, Vitest, and Miniflare/local Wrangler testing. Commit lockfiles and example configuration only.

## Firebase identity verification

Every request carries a Firebase ID token. The Worker:

1. Reads `Authorization: Bearer <token>`.
2. Verifies the signature with Firebase Secure Token public keys using Web Crypto.
3. Caches public keys according to their HTTP cache headers.
4. Checks issuer, Firebase project audience, expiration, issued-at time, and subject.
5. Uses Firebase UID as the canonical user key.
6. Verifies a Firebase App Check token for Android requests where available.

Desktop requests are authenticated but cannot provide Play Integrity. They remain subject to strict server validation.

## API contract

Versioned endpoints:

- `POST /v1/usernames/reserve`
- `POST /v1/progress/upload`
- `POST /v1/progress/fetch`
- `GET /v1/leaderboard`
- `POST /v1/accounts/merge`
- `DELETE /v1/account`

Requests include API version, request ID, game/content version, device ID where applicable, and an idempotency key for mutations. Responses include server time, request ID, typed data, and stable error codes.

## D1 schema

Create indexed SQL tables:

- `users`
- `usernames`
- `saves`
- `level_progress`
- `device_attempts`
- `operations`
- `leaderboard_entries`
- `leaderboard_cache`
- `merge_tickets`
- `rate_limits`

Clients never receive D1 credentials or direct database access.

Use transactions for username reservation, save merge/rewards, leaderboard updates, merge stages, and deletion.

## Server validation

The Worker owns username uniqueness, save validation, idempotent attempt deltas, one-time rewards, points from the level catalog, leaderboard eligibility/cache, rate limits, merges, and deletion.

Never trust client-supplied points or reward totals.

## Account merge without Firebase Admin

The client obtains fresh ID tokens for both Firebase accounts. The Worker verifies both, creates a recovery ticket, and transactionally merges D1 data into the canonical UID.

After D1 merge:

1. Client deletes the secondary Firebase Auth user with its fresh token.
2. Client restores the canonical Firebase session.
3. Client links email/password to the canonical user.
4. Client confirms completion to the Worker.

Never delete the secondary Auth identity before the D1 merge commits.

## Level catalog

Generate a backend catalog containing stable level ID, difficulty, coin reward, point reward, content hash, and active flag. Build fails on duplicate IDs, invalid difficulty, missing rewards, or incompatible unversioned changes.

## Rate limits and idempotency

Initial limits:

- Username reservation: 10/hour/user.
- Progress upload: 30/hour/user with burst protection.
- Manual fetch: 30/hour/user.
- Leaderboard refresh: once per five minutes/user.
- Merge start: 3/day/user.
- Deletion: recent reauthentication required.

Retried mutations with the same idempotency key return the original outcome.

## Free-tier safeguards

Current Workers Free limits include:

- 100,000 requests/day.
- 10 ms CPU per invocation.
- 5 million D1 rows read/day.
- 100,000 D1 rows written/day.
- 5 GB total D1 storage.

Requirements:

- No table scans in request paths.
- Indexed leaderboard queries.
- Bounded payloads/results.
- Bounded idempotency history.
- Compact rate limits.
- Shared leaderboard cache.

When limits are exhausted, return a temporary-unavailable response. Clients retain progress locally and retry later.

## Local development and CI

Scripts:

```text
npm.cmd run check
npm.cmd test
npm.cmd run dev
npm.cmd run db:migrate:local
npm.cmd run db:migrate:remote
npm.cmd run deploy:dev
```

Tests cover Firebase JWT fixtures, username races, malformed payloads, retries, concurrent uploads, one-time rewards, leaderboard caching, merge recovery, deletion, and unavailable-backend behavior.

## Beginner checkpoint worksheet

Record privately:

```text
Cloudflare account ID:
Development D1 name:
Development D1 database ID:
Development Worker name:
Development workers.dev URL:
Latest D1 migration number:
Latest successful deployment date:
```

If any value is unclear, stop and ask Codex to inspect the generated configuration rather than creating another resource.

## Completion gate

- Worker runs locally against local D1.
- Firebase tokens are verified without client secrets.
- Every database mutation uses authenticated endpoints.
- Indexed paths fit the free Worker CPU budget.
- Tests run without production credentials.
- Deployment requires no paid plan.
