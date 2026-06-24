# Phase 4: Android Authentication

## Objective

Implement Firebase Authentication on Android with Play Games as the preferred identity and verified email/password as the linked desktop credential.

## Before Codex starts

Confirm Phase 1 checkpoints B through G are complete:

- Firebase Android package is exactly `io.github.msameer0.rhythmicrush`.
- Debug SHA-1 and SHA-256 are registered.
- Play App Signing SHA-1 and SHA-256 are registered.
- Email/Password is enabled and passwordless Email Link is disabled.
- Play Games provider is enabled.
- Your Google account is listed as a Play Games tester.
- App Check is registered but enforcement remains off.

Place the downloaded `google-services.json` only in the location Codex requests. Do not paste its contents into chat.

## What Codex changes

Codex handles all Gradle, Android lifecycle, Firebase SDK, Play Games SDK, token, and callback code. You do not need to follow the generic Gradle snippets shown by Firebase Console.

## Implementation status

Implemented:

- Google services Gradle plugin and Firebase BoM.
- Firebase Authentication.
- Play Games Services v2.
- App Check debug provider for debug builds.
- App Check Play Integrity provider for release builds.
- Firebase session restoration without blocking game startup.
- Email registration, login, logout, verification, password reset, and email linking.
- Play Games sign-in and provider linking using v2 `requestServerSideAccess`.
- Firebase ID tokens and App Check tokens attached to Cloudflare Worker requests.
- Typed handling for cancellation, network errors, invalid credentials, and provider collisions.
- Authenticated profile, progress fetch, leaderboard, upload request, and deletion client calls.

Account merge remains intentionally unavailable until the two-session recovery protocol is implemented. Progress upload remains rejected by the backend until Phase 7 supplies the authoritative level catalog.

## Required file before building

The project does not currently contain `android/google-services.json`.

Download a fresh copy after all SHA fingerprints and OAuth clients are configured:

1. Firebase Console → **Project settings → General**.
2. Under **Your apps**, select `Rhythmic Rush Android Dev`.
3. Click **Download google-services.json**.
4. Confirm the downloaded filename is exactly `google-services.json`, with no `(1)` suffix.
5. Place it at:

```text
E:\GameDev\RhythmicRush\android\google-services.json
```

The file is Git-ignored in this repository. Do not place it under `android/src/main`, `assets`, or the project root.

Before building, open it locally and confirm:

```text
project_id = rhythmic-rush-dev
package_name = io.github.msameer0.rhythmicrush
```

Do not paste the full file into chat.

## Dependencies and Gradle

**Codex**

Add current compatible versions of:

- Google services Gradle plugin.
- Firebase BoM.
- Firebase Authentication.
- Firebase App Check Play Integrity.
- Play Games Services v2 authentication.
- Kotlin coroutine/task adapters only if they do not leak into core.

Use the Firebase BoM to keep Firebase artifacts compatible. Keep Android-only dependencies out of `core`.

## Play Games login

Implement `AndroidAccountClient`.

Flow:

1. Initialize Play Games SDK during Activity creation.
2. Check automatic sign-in status.
3. On explicit login, launch the Play Games sign-in UI.
4. Obtain the Play Games authentication credential required by Firebase.
5. Sign into Firebase.
6. Refresh Firebase ID token.
7. Fetch/create the backend user profile.
8. If username is absent, route to username creation.
9. Trigger initial safe merge-fetch.

Handle:

- User cancellation.
- No Play Games profile.
- Misconfigured OAuth fingerprint.
- Network timeout.
- Firebase provider disabled.
- Credential already linked to another account.

Never treat Play Games display name as the public username automatically.

## Email/password on Android

Support:

- Registration.
- Login.
- Verification email.
- Password reset.
- Reauthentication.

Play-Games-first flow:

1. User is signed in with Play Games.
2. Selects “Enable PC Login.”
3. Enters email and a new password.
4. Link email/password credential to current Firebase user.
5. Send verification email.
6. Mark desktop login pending until verified.

PC-first flow:

1. User signs into email/password on Android.
2. Selects “Link Play Games.”
3. Obtain Play Games credential.
4. Link to current Firebase user.
5. If credential belongs to another UID, begin guided merge.

## App Check

Initialize Play Integrity App Check before protected backend calls.

Environment behavior:

- Debug/internal builds may use explicitly configured debug tokens.
- Production uses Play Integrity.
- Begin in monitoring mode.
- Enforce only after valid-token metrics are healthy.

Cloudflare Worker requests include Firebase ID token and App Check token where applicable.

### Register the debug App Check token

The first debug launch uses Firebase's debug provider and prints a debug token to Android Logcat.

1. Keep App Check enforcement off.
2. Install and launch a debug build once.
3. Android Studio → **View → Tool Windows → Logcat**.
4. Select the Rhythmic Rush process.
5. Search for `DebugAppCheckProvider`.
6. Copy the UUID-like debug token. This is not the Firebase ID token.
7. Firebase Console → **App Check**.
8. Select the Rhythmic Rush Android app.
9. Open the overflow menu → **Manage debug tokens**.
10. Click **Add debug token**.
11. Name it for the machine/device, such as `Sameer Windows debug`.
12. Paste the token and save.
13. Relaunch the debug build.

Treat debug tokens as private development credentials. Do not commit or post them.

## Activity lifecycle

Authentication UI must:

- Survive Activity recreation.
- Reject duplicate login taps while a flow is active.
- Return results to core on the LibGDX thread.
- Avoid retaining destroyed Activity references.
- Cancel/clean listeners when the game exits.

## Internal-testing checklist

**User**

### Upload the test build

After Codex supplies the build and says it is ready:

1. Open [Google Play Console](https://play.google.com/console/).
2. Select **Rhythmic Rush**.
3. Open **Test and release → Testing → Internal testing**. Some layouts show **Release → Testing → Internal testing**.
4. Open the **Testers** tab.
5. Create or select an email list.
6. Add every Google account that will test Play Games login.
7. Save changes.
8. Open the **Releases** tab.
9. Click **Create new release**.
10. Upload the Android App Bundle supplied by the project build.
11. Add a release name such as `account-dev-1`.
12. Add brief testing notes.
13. Click **Next**, resolve required errors, and click **Start rollout to Internal testing**.

### Install through Google Play

1. Return to the internal-testing **Testers** tab.
2. Copy the opt-in/join link.
3. Open it on the Android device using an invited Google account.
4. Accept the invitation.
5. Install or update Rhythmic Rush from Google Play.
6. Do not test only by sideloading; the Play-signed build has different signing fingerprints.

### Run the login checks

Test with at least:

- A fresh Play Games tester.
- A Play Games account already linked to email.
- An email account later linked to Play Games.
- Two separate accounts requiring merge.
- A tester not whitelisted in Play Games.
- Debug and Play-signed internal builds.

For each test, record the build version, Google test account, installation source, action attempted, exact visible error, and Firebase UID if a debug screen shows it. Never send passwords or authentication tokens.

### Check Firebase users

1. Firebase Console → **Authentication**.
2. Open the **Users** tab.
3. Confirm a successful login creates one user.
4. Open that user and inspect linked providers.
5. After email linking, confirm Play Games and password/email belong to the same Firebase UID.
6. Do not manually delete test users while a merge test is in progress.

### Check App Check metrics

1. Firebase Console → **App Check**.
2. Open the Android app.
3. Review request metrics after test traffic appears.
4. Leave enforcement off.
5. Send Codex a screenshot if most requests are invalid or unknown.

## Completion gate

- Play Games login creates/restores the correct Firebase UID.
- Email/password can be linked for desktop access.
- Existing separate accounts enter guided merge instead of losing data.
- App Check tokens reach emulator/debug and production monitoring.
- Sign-in cancellation and network errors leave the game usable.

Stop here and report results before enabling App Check enforcement or creating production credentials.
