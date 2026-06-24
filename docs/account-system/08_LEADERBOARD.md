# Phase 8: Leaderboard

## Objective

Implement a global points leaderboard using server-validated, one-time completion rewards with local and backend caching.

## Implementation status

Implemented:

- Deterministic global top-100 ordering.
- Shared five-minute D1 cache with a short regeneration lease.
- Separate current-player rank query when outside the top 100.
- Persistent client cache for immediate/offline display.
- Ten-row pages with mouse-wheel and button navigation.
- Fixed-1080p three-panel layout for player details, rankings, and account actions.
- Current-player highlighting and eligibility state.
- Moderation fields that can remove a player from rankings without disabling cloud saves.
- Five-minute manual refresh cooldown.

Account deletion recovery is deliberately handled in Phase 9 because it requires a
transactional email sender, recovery-token storage, delayed Firebase deletion, and a
scheduled purge. It must not be approximated by immediate deletion.

## What you configure

Nothing in Firebase Console. Do not enable Firestore, Realtime Database, or another Firebase leaderboard product; the leaderboard lives in Cloudflare D1.

**Codex** creates the schema, validated score updates, cache, API, and game screen.

**User** supplies or approves the point reward assigned to every official level. Codex will generate a reviewable catalog from the game's level metadata before deployment.

## Ranking

Order by:

1. Points descending.
2. Completed-level count descending.
3. Earlier timestamp at which the current score was achieved.
4. UID only as a deterministic final tie-breaker, never displayed.

The backend recomputes points from known completed levels. Client-supplied totals are ignored.

## Public entry

Expose only:

- Rank.
- Username.
- Points.
- Completed-level count.
- Optional public badge fields added later.

Never expose:

- Email.
- Provider IDs.
- Device IDs.
- Auth timestamps.
- Save payload.
- Attempts unless deliberately added later.

## Backend cache

`GET /v1/leaderboard` behavior:

1. Verify authentication.
2. Enforce per-user five-minute refresh rate.
3. Read the shared D1 cache row.
4. If cache is younger than five minutes, return it.
5. Otherwise acquire a D1 regeneration lease/transaction.
6. Query top 100 eligible entries.
7. Write new cache.
8. Query current player's rank/entry separately if outside top 100.
9. Return cache timestamp and next allowed refresh.

Concurrent refreshes must not regenerate the cache repeatedly.

## Client cache

Persist:

- Top entries.
- Current player's rank.
- Cache generation timestamp.
- Last successful request timestamp.
- Next refresh time.

On screen open:

- Render local cache immediately.
- If no cache exists, fetch.
- Do not automatically fetch merely because the screen reopened.
- Show stale/offline status.
- Refresh button displays remaining cooldown.

Use server time to calculate cooldown and avoid local-clock manipulation.

## UI

Leaderboard screen includes:

- Back button.
- “Global Points” heading.
- Rows for rank, username, points, completions.
- Highlighted current player.
- Separate current-player row when outside top 100.
- Last updated timestamp.
- Refresh button/countdown.
- Loading, empty, offline, signed-out, and error states.

Signed-out users may be allowed to view cached public results, but fresh requests should require authentication in v1.

## Eligibility and moderation

Exclude entries when:

- Account is deleted, merging, or suspended.
- Username is not finalized.
- Email verification is required and incomplete.
- Save validation fails.
- Content version is unsupported.

Username moderation must support backend removal/rename without editing save data.

## Anti-cheat boundary

Pragmatic v1 validation:

- Known level IDs.
- Percentage bounds.
- Monotonic progression/revisions.
- One-time reward ledger.
- Idempotent attempt deltas.
- App Check on Android.
- Rate limits and payload limits.

Document that modified desktop clients remain capable of fabricating plausible completions. Replay verification is explicitly deferred.

## Completion gate

- Correct top-100 ordering.
- Current player rank works outside top 100.
- Five-minute local and server cooldowns work.
- Concurrent refresh requests generate one cache.
- Deleted/ineligible accounts disappear.
- No private account fields are returned.
- Offline cache remains viewable.

## Exact manual test

1. Prepare three disposable accounts with different completed levels.
2. Sync Account A with the highest points.
3. Sync Account B with fewer points.
4. Sync Account C with the same points as B but a different completion count or achievement time.
5. Verify the displayed order against the ranking rules.
6. Press Refresh twice and confirm the second press shows the five-minute cooldown.
7. Reopen the screen and confirm cached rows appear without a fresh request.
8. Disconnect the network and confirm cached rows remain readable and marked offline/stale.
9. Delete one disposable account and confirm it disappears after cache regeneration.
10. Confirm rows expose no email, UID, provider ID, or device ID.
