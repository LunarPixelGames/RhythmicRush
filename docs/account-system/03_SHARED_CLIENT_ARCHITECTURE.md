# Phase 3: Shared Client Architecture

## Objective

Introduce platform-neutral account, cloud-save, and leaderboard abstractions in `core` without coupling game screens to Firebase SDK classes.

## What you do in this phase

There are no Firebase Console changes in Phase 3.

1. Tell Codex that Phase 2 is complete.
2. Keep the Phase 1 private worksheet available in case Codex asks you to confirm a project ID or development API URL.
3. Review proposed account-screen wording or behavior choices.
4. Run the game/build only when you are ready, then report the complete error text and affected platform.

Do not manually add Firebase dependencies to `core`, paste API keys into Kotlin/Java files, or move `google-services.json` without Codex directing the exact destination.

## What Codex implements

Codex will inspect the existing launchers and service-injection pattern, implement the interfaces/state model below, wire harmless unavailable/placeholder clients first, and keep the game playable without a network connection.

At handoff, Codex will list every changed file, any private local configuration you must create, the exact development values that belong in it, and a short smoke test.

## Implementation status

Implemented on the `leaderboard` branch:

- Shared `AccountClient`, callbacks, cancellable operations, capabilities, DTOs, states, and typed errors.
- Shared `AccountManager` with session restoration, state listeners, operation locking, login/logout entry points, progress fetch coordination, leaderboard cache/cooldown, and persisted pending sync requests.
- Installation UUID stored in `saves/account_meta.json`.
- Leaderboard cache stored in `saves/leaderboard_cache.json` after the first snapshot.
- Versioned pending requests stored in `saves/sync_queue.json` after the first queued upload.
- `FakeAccountClient` and `InMemorySecureTokenStore` test doubles.
- Development configuration providers in Android and desktop launchers.
- Safe unavailable clients until Phases 4 and 5 provide real authentication.
- Background account initialization immediately after local progress loads.

No Firebase SDK is imported by `core`. The Firebase desktop Web API key remains outside committed source.

## Core interfaces

**Codex**

Add an injected `AccountClient` alongside existing platform services.

Required capabilities:

- Observe authentication/session state.
- Email register/login/logout/password reset.
- Send and check email verification.
- Link or unlink supported providers.
- Start guided merge.
- Refresh authentication.
- Call authenticated backend endpoints.
- Report platform capabilities.

The interface must not expose Firebase-specific classes. Use project-owned DTOs and error enums.

Add `PlatformAccountCapabilities`:

- Play Games login supported.
- Email login supported.
- Secure persistent token storage supported.
- App Check supported.
- Provider linking supported.

## Asynchronous model

Use a small project-owned async abstraction compatible with Java 8 and LibGDX:

- Operations execute off the render thread.
- Completion callbacks are delivered through `Gdx.app.postRunnable`.
- Each operation has loading, success, typed failure, and cancellation.
- Screens may detach listeners when hidden/disposed.
- Only one destructive account operation runs at a time.

Do not use blocking `.get()`, `runBlocking`, or network work from `render()`/`update()`.

## AccountManager

Add a shared `AccountManager` owned by `RhythmicRushGame`.

Responsibilities:

- Current `AccountState`.
- Current public profile.
- Auth token/session refresh coordination.
- Initial post-login merge-fetch.
- Pending upload queue.
- Last sync status/time.
- Local leaderboard cache.
- Five-minute refresh cooldown.
- Mapping backend error codes to UI messages.
- Preventing duplicate concurrent requests.

State model:

- Unavailable.
- Signed out.
- Authenticating.
- Needs email verification.
- Needs username.
- Signed in.
- Syncing.
- Merge required/in progress.
- Deleting.
- Recoverable error.

## Data transfer objects

Define immutable/versioned DTOs:

- `AccountProfile`
- `LinkedProvider`
- `CloudProgress`
- `LevelCloudProgress`
- `SyncRequest`
- `SyncResult`
- `LeaderboardEntry`
- `LeaderboardSnapshot`
- `AccountMergePreview`
- `AccountOperationError`

Keep wire DTOs separate from mutable runtime `ProgressManager` objects.

## Configuration

Add a configuration provider injected per platform:

- Environment name.
- Firebase project ID.
- Web API key for desktop Auth REST.
- Cloudflare Worker API base URL.
- Firebase Auth emulator and local Worker ports.
- API/content version.

Production secrets must not be required in core. Invalid or absent config should put accounts into `Unavailable`, not crash game startup.

## Device identity

Create a random installation UUID on first launch.

- Store outside `progress.ubj`.
- Preserve across upgrades.
- Regenerate after app-data deletion.
- Never use hardware IDs, advertising IDs, email, or Play Games ID.
- Include it only in authenticated progress synchronization.

## Local cache files

Use separate local files:

```text
saves/progress.ubj
saves/account_meta.json
saves/leaderboard_cache.json
saves/sync_queue.json
```

`account_meta.json` stores non-secret metadata only. Desktop refresh tokens go to secure platform storage, not this file.

## Game integration

Modify `RhythmicRushGame` constructor to accept the account client/config provider, as it already accepts ads and updates.

Loading sequence:

1. Load local progress.
2. Initialize `AccountManager`.
3. Restore a platform session asynchronously.
4. Never delay reaching the main menu on network availability.
5. If a session restores, perform hybrid sync in the background.

## Test doubles

Add:

- `FakeAccountClient`
- In-memory secure-token store.
- Deterministic fake backend responses.
- Offline/error simulation.

Core tests should cover state transitions without Firebase or network access.

## Completion gate

- Android and desktop launchers compile with placeholder clients.
- Core imports no Firebase packages.
- Main menu works when account configuration is absent or offline.
- Account state and async errors are testable.
- No networking occurs on the render thread.

## Your manual smoke test

After Codex says Phase 3 is ready:

1. Launch the desktop game normally.
2. Confirm the main menu appears even if the internet is disconnected.
3. Open the Account entry if it is visible.
4. The Account entry is not expected until Phase 6; if one is visible, confirm it shows a safe unavailable/signed-out state rather than crashing.
5. Return to the main menu.
6. Start a local level and confirm existing progress still loads.
7. Report any freeze, crash, missing progress, or navigation problem.
