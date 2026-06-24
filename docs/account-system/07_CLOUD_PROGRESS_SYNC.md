# Phase 7: Cloud Progress Synchronization

## Objective

Migrate the current local `progress.ubj` into a versioned, merge-safe model while preserving offline play and preventing duplicated attempts or completion rewards.

## Implementation status

Implemented:

- Local save schema version 2.
- Separate per-installation attempt counters and aggregate displayed attempts.
- Atomic temporary-file writes with validation.
- Timestamped cloud-merge backups, retaining the latest five.
- Legacy save migration without losing local progress.
- Local one-time completion reward flag, fixing repeated rewards when replaying completed levels.
- Authoritative D1 level catalog for all six current levels.
- Unique server completion-reward ledger.
- Idempotent upload operations and monotonic device-attempt validation.
- Maximum best-percentage merge and aggregate cross-device attempts.
- Server-derived coins, points, completed-level count, and leaderboard entry.
- Automatic first sync after a fully signed-in account is ready.
- Automatic upload after a new personal best or completion.
- Persistent queued snapshot after offline/transient failure.
- Conservative manual Fetch with a recoverable local backup.
- Deleted usernames become reusable after a 30-day tombstone period.

The development Worker and migration `0002_progress_sync.sql` are deployed.

## Safety rule for this phase

Back up real save files before testing.

**User**

1. Close the game completely.
2. Copy the existing `progress.ubj` to a separate backup folder outside the active save directory.
3. Name it with the date, platform, and device, for example `progress-before-cloud-pc-2026-06-23.ubj`.
4. Do the same on Android if it has valuable progress.
5. Do not use your only copy of a valuable save for the first migration test.

**Codex**

Codex will inspect the current save paths and provide the exact Windows/Android locations before testing. Codex implements atomic writes and automatic backups before cloud data can replace the active local file.

Current locations:

- Desktop Gradle run: `E:\GameDev\RhythmicRush\assets\saves\progress.ubj`
- Desktop cloud-merge backups: `E:\GameDev\RhythmicRush\assets\saves\backups\`
- Android app-private save: `files/saves/progress.ubj` inside the Rhythmic Rush app sandbox.

Android app-private files can be retrieved through Android Studio's Device Explorer on a debuggable build. For a Play release build, use a disposable test account/save rather than relying on direct file extraction.

## Current local data

The existing save contains:

- `coins`
- `points`
- Per-level `bestPercent`
- Per-level `totalAttempts`

It has no schema version, device identity, server revision, completed-level reward ledger, or sync metadata.

## Local model changes

**Codex**

Add a versioned export/import layer around `ProgressManager`; do not make backend DTOs depend on LibGDX `ObjectMap`.

Local gameplay save continues to use `progress.ubj`.

Separate metadata contains:

- Local schema version.
- Installation/device UUID.
- Last cloud revision.
- Last accepted attempt totals per level.
- Pending operation IDs.
- Last successful sync timestamp.
- Content version.

Write local saves atomically:

1. Serialize to a temporary sibling file.
2. Flush and close.
3. Validate by reading it back.
4. Move current save to timestamped backup.
5. Replace current save.
6. Keep a bounded number of backups.

## Upload payload

Include:

- Local schema/content version.
- Device ID.
- Idempotency key.
- Last known server revision.
- Per-level best percentage.
- Per-level monotonic attempt total for this installation.
- Locally observed completion state.
- Legacy coin value during first migration only.

Never upload client-calculated authoritative points or completion reward totals.

## Server merge algorithm

For each known level:

- `bestPercent = max(cloud, uploaded)`.
- Completion becomes true at 100%.
- First transition to completion grants catalog coins and points once.
- Attempt delta equals uploaded device total minus last accepted device total.
- Reject decreasing device attempt totals unless a documented device-reset flow is used.
- Replayed idempotency key returns prior result.

Global:

- Aggregate attempts are derived from accepted device deltas.
- Legacy coins preserve the maximum initial pre-account value.
- Earned completion coins and points are catalog-derived.
- Unknown levels are ignored or rejected according to API version; v1 should reject with a clear update-required error.

## Fetch and local merge

Fetch returns canonical structured progress and server revision.

Client:

1. Exports current local save.
2. Creates backup.
3. Merges best percentages with maximum.
4. Applies server aggregate attempts while retaining current device counter metadata.
5. Applies authoritative points/rewards.
6. Atomically writes the result.
7. Updates local revision metadata.
8. Refreshes all screens that cache progress.

Fetch never reduces best percentage.

## Hybrid sync triggers

- After successful login/session restoration: fetch/merge.
- After level completion or reward mutation: queue upload.
- On entering a menu: flush queued upload.
- Manual Upload/Fetch: immediate operation.
- Network restoration: retry queued operation with same idempotency key.
- Logout: attempt final flush, but never block logout indefinitely.

Coalesce frequent saves into one pending snapshot.

## Conflict and failure behavior

- HTTP timeout: retain queue and local save.
- Authentication expired: refresh once, then require login.
- Revision conflict: fetch latest, merge locally, retry once with same logical operation.
- Unsupported content version: disable cloud mutation and prompt game update.
- Corrupt local save: offer latest valid backup.
- Backend validation error: preserve local data and show actionable message.

## Migration

First authenticated sync:

1. Detect legacy save.
2. Assign device ID.
3. Treat existing total attempts as this device's initial monotonic totals.
4. Upload best percentages and legacy coin floor.
5. Backend calculates completed-level rewards without double-granting legacy currency.
6. Fetch canonical result.
7. Mark migration complete.

## Completion gate

- Existing saves migrate without loss.
- Upload retries never duplicate attempts.
- Phone and PC attempt totals add.
- Best progress never regresses.
- Rewards are granted once.
- Offline play and later retry work.
- Fetch always creates a recoverable backup.

## Exact manual test order

Use disposable test saves first:

1. PC: play part of Level A and note percentage/attempts.
2. Android: reach a higher percentage of Level A and make attempts on Level B.
3. Sign both devices into the same linked account.
4. Fetch on PC and confirm Level A keeps the higher percentage.
5. Upload on Android, then fetch on PC.
6. Confirm attempts combine rather than replace one another.
7. Complete one level on both devices while one is offline.
8. Reconnect and sync both.
9. Confirm completion coins/points are granted only once.
10. Repeat an upload after a simulated timeout and confirm nothing duplicates.
11. Play offline and confirm local saving remains functional.
12. Restore the network and confirm the queued upload succeeds.

For a mismatch, stop syncing that test account and preserve both saves, their backups, the visible server revision/sync time, the exact action order, and any non-secret Worker request IDs.
