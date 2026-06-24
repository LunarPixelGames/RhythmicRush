# Cross-Platform Accounts, Cloud Saves, and Leaderboards

## Summary

Use a no-billing hybrid stack:

- Android signs in through Google Play Games using Firebase's native Play Games provider.
- Desktop registers and signs in with email/password through Firebase Auth's REST API.
- Players link email/password and Play Games to one Firebase UID for cross-platform access.
- Firebase stays on Spark and supplies Authentication and App Check only.
- Cloudflare Workers validate and merge progress, calculate one-time points, maintain usernames, and serve cached leaderboards.
- Cloudflare D1 stores structured canonical progress rather than trusting raw `progress.ubj` bytes.
- Cloudflare Pages hosts the external account-deletion page.
- Existing local saves continue working offline.

No billing card is required. Free-tier quotas are hard operational limits: if exhausted, cloud operations fail gracefully while local/offline gameplay remains available.

## Client Architecture

### Shared core interfaces

Add an `AccountClient` interface injected into `RhythmicRushGame`, following the existing platform-controller pattern.

It exposes asynchronous operations for:

- Current authentication state and profile.
- Play Games login/linking where supported.
- Email registration, verification, login, password reset, and linking.
- Logout, token refresh, account merge, and deletion.
- Upload, fetch, and sync status.
- Leaderboard retrieval and refresh cooldown.

All callbacks return to the LibGDX thread through `Gdx.app.postRunnable`. Screens must never block the render thread.

Add a core `AccountManager` above the platform client to manage:

- Session state.
- Hybrid synchronization.
- Offline retry queue.
- Cached profile and leaderboard.
- Five-minute leaderboard refresh timer.
- User-facing error/status mapping.

### Android implementation

Use:

- Firebase Authentication.
- Google Play Games Services v2.
- Firebase Play Games credentials.
- Firebase App Check backed by Play Integrity.

Supported flows:

1. Play-Games-first login automatically creates/opens the Firebase account.
2. "Enable PC Login" links a verified email/password to that same UID.
3. PC-first users may sign into email/password on Android, then link Play Games.
4. If Play Games and email already belong to separate accounts, launch guided merge.
5. Firebase manages Android token persistence.

### Desktop implementation

Use Firebase Auth and backend HTTPS endpoints through REST:

- Email registration and verification.
- Email/password login.
- Password reset.
- ID-token refresh.
- Cloud-save and leaderboard function calls.

Store refresh tokens using platform credential storage:

- Windows: DPAPI.
- macOS: Keychain.
- Linux: Secret Service.
- If secure storage is unavailable, keep the session in memory only and require login next launch.

Google Play Games is not used directly in the LWJGL module.

## Account and Linking UX

Add Account and Leaderboard entry points to the main menu.

The Account screen shows:

- Signed-out Android: Play Games Login, Email Login, Email Register.
- Signed-out desktop: Email Login and Register.
- Signed-in identity, unique username, verification state, and linked providers.
- Link Play Games on Android.
- Enable/Change Desktop Login.
- Upload Progress.
- Fetch Progress.
- Logout.
- Delete Account.

Registration requires:

- Verified email for email/password accounts.
- Unique public username, 3–20 characters.
- Letters, numbers, and underscore only.
- Case-insensitive uniqueness.
- Server-side profanity/reserved-name validation.
- Thirty-day username rename cooldown.

### Guided account merge

When credentials already belong to different Firebase users:

1. Reauthenticate both identities.
2. Display both account usernames and progress summaries.
3. Preserve the username of the account initiating the merge.
4. Use the Play Games Firebase UID as canonical when one account has Play Games.
5. Safely merge both saves.
6. Move leaderboard/profile ownership to the canonical UID.
7. Remove the secondary Auth user and release its username after a short tombstone period.
8. Ask for a new desktop password and attach the verified email/password to the canonical UID.
9. Issue a fresh session and force a canonical save fetch.

Persist a recovery ticket before destructive merge steps so an interrupted merge can be resumed safely.

## Cloud Progress

### Local save migration

Keep `saves/progress.ubj` as the game's local offline format.

Extend `ProgressManager` with:

- A versioned export model.
- Safe merge/import.
- Atomic save replacement.
- Timestamped backup before applying fetched progress.
- Device ID and last-upload metadata stored separately from gameplay progress.

Existing saves without metadata migrate automatically on first authenticated sync.

### Canonical save model

Store a structured, versioned save in Cloudflare D1:

- Schema version and server revision.
- Per-level best percentage.
- Per-level cumulative attempt count.
- Completed-level IDs.
- Legacy coin balance.
- One-time earned coin balance.
- One-time points total.
- Updated timestamp and source device.
- Game/content version.

Do not accept client-supplied points as authoritative.

The backend contains a deployed level catalog generated from the game's level metadata:

- Stable level ID.
- Difficulty.
- Completion coin reward.
- Completion point reward.
- Content/version hash.

### Safe merge rules

For every upload or fetch:

- Best percentage: maximum value.
- Completion: true if either side reached 100%.
- Attempts: add attempts from different installations.
- Upload retries remain idempotent using device ID, monotonic device totals, and operation ID.
- Points: recomputed server-side from unique completed levels.
- Coins: each level's completion reward is granted once.
- Legacy coins: preserve the maximum pre-account balance during initial migration.
- Unknown level IDs, negative values, percentages outside 0–100, and implausible payload sizes are rejected.
- Server timestamps and revisions are authoritative.

Although the player-visible policy is additive attempts, per-device bookkeeping prevents retries from adding the same attempts repeatedly.

### Sync behavior

Use hybrid synchronization:

- After login: automatically fetch and safely merge.
- After level completion or reward changes: queue an upload.
- When returning to a menu: flush pending progress.
- Offline: keep playing and queue a retry.
- Manual Upload: immediately validate and merge local progress into cloud.
- Manual Fetch: back up local progress, merge cloud and local, then atomically save the canonical result.
- Never silently discard the stronger side of a save.

Show operation state, last successful sync time, offline status, and actionable errors.

## Cloudflare Backend

Add a `backend/` Cloudflare TypeScript project containing a Worker API, D1 migrations, tests, and a Pages account-deletion site.

### D1 tables

- `users`: private profile and linked-provider metadata keyed by Firebase UID.
- `usernames`: unique normalized username reservations.
- `saves`: canonical progress and server revision.
- `level_progress`: canonical per-level progress.
- `device_attempts`: monotonic attempt counters per user/device/level.
- `operations`: bounded idempotency history.
- `leaderboard_entries`: public ranking data.
- `leaderboard_cache`: cached top entries and generation timestamp.
- `merge_tickets`: recoverable account-merge state.
- `rate_limits`: server-enforced action timestamps.

Clients never receive direct D1 access. All reads and mutations pass through the Worker API.

### HTTPS Worker endpoints

Implement authenticated endpoints:

- `reserveUsername`
- `uploadProgress`
- `fetchProgress`
- `getLeaderboard`
- `mergeAccounts`
- `deleteAccount`

Every endpoint:

- Verifies Firebase ID tokens.
- Verifies App Check on Android where available.
- Enforces payload size/schema.
- Uses idempotency keys for mutations.
- Applies per-user and per-device rate limits.
- Emits structured error codes for the client.
- Logs only non-sensitive operational metadata.

Configure strict Worker CPU/request limits and indexed D1 queries to stay within free quotas.

## Leaderboard

The first leaderboard ranks by:

1. Validated one-time points.
2. Number of completed levels.
3. Earlier achievement timestamp.

Display:

- Top 100 players.
- Rank, unique username, points, and completed-level count.
- Current player's rank even when outside the top 100.
- Cached-at timestamp.
- Refresh countdown.

Caching behavior:

- Persist the latest leaderboard locally.
- Opening the screen immediately displays cached data without a request.
- If no cache exists, perform one initial fetch.
- Refresh is disabled for five minutes after a successful request.
- The server independently enforces the five-minute user cooldown.
- The backend cache row is regenerated at most once per five-minute window; all players share it.
- Stale cached data remains visible when offline.

Pragmatic anti-cheat validation includes known-level checks, one-time rewards, monotonic progress, payload validation, operation replay protection, App Check on Android, and strict rate limits. It deters ordinary save editing but does not make modified desktop clients mathematically cheat-proof.

## Privacy, Recovery, and Account Deletion

Update the privacy policy and Google Play Data Safety declaration to cover:

- Email address.
- Play Games identifier.
- Username.
- Progress and leaderboard data.
- Device-generated installation ID.
- Authentication and abuse-prevention metadata.

Add:

- In-app account deletion with recent reauthentication.
- External deletion page hosted through Cloudflare Pages or the existing project site.
- Immediate Auth revocation.
- Removal of save, profile, device, and leaderboard documents.
- Username tombstone before reuse.
- Clear distinction between deleting the game account and deleting the underlying Google account.

Never place Worker secrets or Firebase service-account credentials in any client module. Firebase client API keys may be distributed but must be restricted to the correct Android package, signing certificates, and approved API usage.

## Test Plan

### Authentication

- New Play Games account on Android.
- New email account on desktop.
- Email verification and password reset.
- Link desktop login to a Play Games account.
- Link Play Games to a PC-first account.
- Guided merge of two populated accounts.
- Expired, revoked, and refreshed sessions.
- Logout and secure token deletion.

### Cloud saves

- Migrate an existing anonymous `progress.ubj`.
- Upload from phone, continue on PC, then fetch on phone.
- Concurrent uploads from both devices.
- Repeated upload retry does not duplicate attempts.
- Independent phone and PC attempts add correctly.
- Best percentages never regress.
- Completion rewards are granted once.
- Corrupt/oversized/unknown-level payloads are rejected.
- Fetch creates a backup and writes atomically.
- Offline progress uploads after reconnecting.

### Leaderboard and security

- Correct point recomputation from completed levels.
- Tie-breaking order.
- Top 100 and personal rank.
- Local and backend five-minute caching.
- Refresh button countdown.
- Direct database access is impossible from clients; unauthorized Worker calls are denied.
- Rate-limit and idempotency enforcement.
- Username races, invalid names, and rename cooldown.
- Deleted/merged accounts disappear from ranking.

Use the Firebase Auth emulator plus Wrangler/Miniflare and local D1 for automated integration tests. Test Play Games authentication using Google Play internal-testing builds and configured tester accounts.

## Assumptions

- Firebase Spark supplies identity because it officially supports Play Games authentication and identity linking.
- Cloudflare Workers and D1 supply the validated backend on hard-capped free plans.
- `progress.ubj` remains the local format; D1 stores validated structured progress rather than a blindly trusted binary blob.
- Points and completion coins become one-time rewards per level.
- The initial leaderboard is global points only.
- No server-side gameplay replay verification is included in v1.

## References

- [Firebase Play Games authentication](https://firebase.google.com/docs/auth/android/play-games)
- [Firebase account linking](https://firebase.google.com/docs/auth/android/account-linking)
- [Firebase pricing](https://firebase.google.com/pricing)
- [Cloudflare Workers pricing](https://developers.cloudflare.com/workers/platform/pricing/)
- [Cloudflare D1 pricing](https://developers.cloudflare.com/d1/platform/pricing/)
- [Google Play account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111)
