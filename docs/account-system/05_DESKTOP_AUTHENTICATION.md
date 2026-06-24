# Phase 5: Desktop Authentication

## Objective

Implement email/password Firebase authentication for the LWJGL desktop build without adding Android or Firebase SDK dependencies to core.

## What you do in Firebase

No new Firebase app or provider is needed if Phase 1 is complete.

1. Firebase Console → **Authentication → Sign-in method**.
2. Confirm **Email/Password** is enabled.
3. Open it and confirm **Email link (passwordless sign-in)** is disabled.
4. Firebase Console → **Project settings → General**.
5. Under **Your apps**, confirm the desktop web app exists.
6. Keep its public Web API key and Firebase project ID in your private worksheet.

Do not create a second Firebase project for desktop. Android and PC must use the same project so both providers can link to one UID.

## What Codex implements

Codex writes the REST client, secure session storage, local-development switching, and error handling. Never paste a Web API key directly into committed source unless the environment/configuration design explicitly calls for it.

## Implementation status

Implemented:

- Firebase email registration and sign-in through the official Identity Toolkit REST endpoints.
- Refresh-token exchange through Secure Token REST.
- Session restoration during startup without blocking the loading screen.
- Verification emails, password-reset emails, profile refresh, and email/password linking.
- Authenticated Cloudflare profile, progress, leaderboard, upload, and deletion requests.
- Connection/read timeouts, bounded responses, TLS hostname verification, stable user agent, and typed errors.
- Windows refresh-token encryption using DPAPI.
- macOS Keychain and Linux Secret Service adapters with memory-only fallback.
- Environment-specific ignored desktop configuration.
- Firebase Auth emulator and local Worker URL overrides.

Play Games login remains Android-only. Merge remains unavailable until the recoverable two-account merge flow is implemented.

## Local development configuration

The development configuration is stored at:

```text
E:\GameDev\RhythmicRush\lwjgl3\account-dev.properties
```

It is Git-ignored. The committed template is `lwjgl3/account-dev.properties.example`.

Use the Firebase **web app/browser Web API key**, not the Android API key from `google-services.json`.

If the file is missing or invalid, the game still starts and the account state becomes Unavailable.

## REST client

**Codex**

Implement `DesktopAccountClient` using standard HTTPS and JSON.

Firebase Auth operations:

- Sign up with email/password.
- Sign in with email/password.
- Send verification email.
- Send password-reset email.
- Refresh ID token.
- Fetch account/provider information.
- Reauthenticate before sensitive operations.
- Logout locally.

Cloudflare Worker operations use the Firebase ID token as a bearer credential.

Use:

- Connection and read timeouts.
- Bounded response sizes.
- TLS hostname verification.
- Stable user agent containing game/version/platform.
- Retry only for idempotent transient failures.
- Exponential backoff with jitter.

Never retry registration, merge, deletion, or upload blindly without the endpoint idempotency key.

## Session storage

Create `SecureTokenStore` in the desktop module.

Implement:

- Windows DPAPI.
- macOS Keychain command/API integration.
- Linux Secret Service where available.
- Memory-only fallback.

Store only refresh token and minimal environment/account keying. Never store plaintext password.

If secure storage fails:

- Show “Remember me unavailable.”
- Continue current session in memory.
- Require login after restart.

## Restore flow

At startup:

1. Read refresh token from secure storage.
2. Exchange it for a fresh ID token off-thread.
3. Fetch account profile.
4. Enter signed-in state.
5. Run hybrid sync.

If token is revoked or expired:

- Delete local token.
- Return to signed-out state.
- Preserve local progress.

## Email verification

An unverified user may authenticate but cannot:

- Reserve/finalize public username.
- Upload ranked progress.
- Enter leaderboard.
- Merge/delete without reauthentication.

The UI offers “Resend verification” and “I have verified; refresh.”

## Emulator support

Desktop configuration must switch Firebase Auth and Worker URLs to localhost development endpoints without code changes. Add a safe visible indicator when local mode is active.

## Completion gate

- Desktop register/login/reset works against Auth emulator.
- Token refresh restores sessions.
- Secure-store fallback is graceful.
- No passwords or tokens appear in logs.
- Desktop can call an authenticated test function.
- Revoked sessions preserve local saves and sign out cleanly.

## Your desktop test sequence

The Account screen does not exist until Phase 6, so full interaction testing waits for that phase. For Phase 5 now:

1. Launch the desktop game.
2. Confirm it reaches the main menu normally.
3. Confirm the log does not say account configuration is unavailable.
4. Confirm existing local progress still loads and saves.

After Phase 6 adds the Account screen:

1. Launch the development desktop build.
2. Open **Account → Register**.
3. Use a test email address you control and a unique test password.
4. Confirm the game asks for email verification.
5. Open the verification email and verify the address.
6. Return to the game and press the verification-refresh action.
7. Log out, close the game, reopen it, and sign in.
8. If Remember Me is enabled, close and reopen again to test restoration.
9. Use Password Reset and confirm the email arrives.
10. Confirm local level progress remains unchanged through login/logout.

Then open Firebase Console → **Authentication → Users**, find the test email, confirm it is verified, and—if linked on Android—confirm the same UID lists both providers.
