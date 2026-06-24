# Production replication checklist

Use this when the development account system is fully tested and you are ready to create production infrastructure.

Do not reuse development Firebase, Play Games Services, Cloudflare D1, or Worker resources for production.

## 1. Firebase production project

1. Open Firebase Console.
2. Click Add project.
3. Name it `Rhythmic Rush`.
4. Use a production project ID such as `rhythmic-rush` if available.
5. Keep the plan on Spark.
6. Disable Google Analytics unless you intentionally need it.
7. Add the Android app:
   - Package name: `io.github.msameer0.rhythmicrush`
   - App nickname: `Rhythmic Rush Android`
8. Add the release SHA-1 and SHA-256 values from Google Play App Signing.
9. Add the upload/debug SHA values only if the exact production testing path needs them.
10. Download the production `google-services.json`.
11. Do not commit `google-services.json`.
12. Enable Firebase Authentication providers:
   - Email/Password
   - Play Games
13. Do not enable passwordless email link unless the game implements that flow.
14. Configure authorized domains only for domains you own.
15. Create/register a web app for the desktop Firebase REST login.
16. Copy the production web API key into your local production desktop config only.

## 2. Google Play Games Services production config

1. Open Google Play Console.
2. Select Rhythmic Rush.
3. Open Grow users -> Play Games Services -> Setup and management -> Configuration.
4. Use the production Google Cloud/Firebase project.
5. Add credentials:
   - Android credential for the production package and Play App Signing SHA-1.
   - Android credential for internal/closed testing if Google requires a separate SHA.
   - Game server credential only if the final backend needs server auth codes.
6. Add tester accounts while still in test mode.
7. Publish the Play Games Services configuration only after credentials show ready.

## 3. Cloudflare production resources

Create separate production resources:

- Worker name: `rhythmic-rush-api`
- D1 database name: `rhythmic-rush`

In `backend/wrangler.toml`, production values should look like this, with real IDs filled in locally only:

```toml
name = "rhythmic-rush-api"
main = "src/index.ts"
compatibility_date = "2026-06-23"
workers_dev = true
account_id = "YOUR_CLOUDFLARE_ACCOUNT_ID"

[vars]
ENVIRONMENT = "production"
FIREBASE_PROJECT_ID = "rhythmic-rush"
FIREBASE_PROJECT_NUMBER = "YOUR_PRODUCTION_FIREBASE_PROJECT_NUMBER"
APP_CHECK_MODE = "monitor"
ALLOWED_ORIGIN = "https://your-production-domain.example"
ENABLE_NEW_ACCOUNTS = "false"
ENABLE_PROGRESS_UPLOADS = "false"
ENABLE_LEADERBOARD_REFRESH = "false"
ENABLE_ACCOUNT_DELETION = "false"

[[d1_databases]]
binding = "DB"
database_name = "rhythmic-rush"
database_id = "YOUR_PRODUCTION_D1_DATABASE_ID"
migrations_dir = "migrations"
```

Keep production feature switches off for the first deploy. Turn them on one at a time after testing.

## 4. Apply production migrations

From `backend`:

1. Confirm `wrangler.toml` points to production.
2. Run the remote migrations.
3. Verify these tables exist:
   - `users`
   - `usernames`
   - `saves`
   - `level_progress`
   - `device_attempts`
   - `completion_rewards`
   - `leaderboard_entries`
   - `leaderboard_cache`
   - `deletion_requests`
   - `account_tombstones`

Do not manually copy development D1 rows into production.

## 5. Immediate account deletion

Account deletion no longer uses recovery email.

Production behavior:

- The game asks for multiple confirmations.
- The backend immediately deletes cloud save/account rows.
- The leaderboard row is removed.
- The username reservation is deleted, so the username is available again.
- The client signs out and attempts to delete the Firebase Authentication user.
- Local progress remains on the device.

Production config:

```text
ENABLE_ACCOUNT_DELETION=true
```

Rollback:

```text
ENABLE_ACCOUNT_DELETION=false
```

Do not claim users can recover a deleted account after confirmation.

## 6. Deletion testing

Before production rollout:

1. Create a test account.
2. Choose a username.
3. Upload progress so the account appears on the leaderboard.
4. Delete the account in-game.
5. Confirm:
   - the user is signed out,
   - cloud fetch/upload requires sign-in,
   - the leaderboard entry disappears,
   - the same username can be selected again by another account,
   - local progress remains playable.

## 7. Feature switch rollout order

Recommended production order:

1. Deploy Worker with every mutation disabled.
2. Enable profile/login only.
3. Enable uploads:
   - `ENABLE_PROGRESS_UPLOADS=true`
4. Enable leaderboard cached reads.
5. Enable manual leaderboard refresh:
   - `ENABLE_LEADERBOARD_REFRESH=true`
6. Enable new account creation:
   - `ENABLE_NEW_ACCOUNTS=true`
7. Enable account deletion only after immediate-deletion testing passes:
   - `ENABLE_ACCOUNT_DELETION=true`

Rollback switches:

```text
ENABLE_NEW_ACCOUNTS=false
ENABLE_PROGRESS_UPLOADS=false
ENABLE_LEADERBOARD_REFRESH=false
ENABLE_ACCOUNT_DELETION=false
```

## 8. Store listing and policy

Before production release:

1. Update the hosted privacy policy.
2. Update Google Play Data Safety.
3. Add the external account deletion URL if Google Play requires one.
4. Confirm the in-game delete button matches the policy.
5. Use staged rollout, not 100%.
