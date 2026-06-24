# Account System Implementation Playbook

This folder breaks [ACCOUNT_SYSTEM_PLAN.md](../../ACCOUNT_SYSTEM_PLAN.md) into executable phases. Complete phases in numerical order unless a guide explicitly permits parallel work.

## If this is your first Firebase project

Start with [Phase 1](01_FIREBASE_AND_PLAY_GAMES_SETUP.md) and follow it from top to bottom. It now names the exact website, sidebar item, button, value, and checkpoint for every Firebase, Google Cloud, Play Games, and initial Cloudflare action.

Use this workflow:

1. Complete only one checkpoint at a time.
2. Keep the private worksheet from Phase 1 open in a separate local note.
3. When a guide says **Stop here for Codex**, stop changing settings and tell Codex which checkpoint you reached.
4. Send screenshots if a console does not match the guide. Console labels move occasionally.
5. Never paste passwords, OAuth client secrets, keystores, refresh tokens, or service-account files into chat or Git.
6. Do not create the production project until the development setup works end to end.

You are not expected to write Gradle, Kotlin, Worker, SQL, or authentication code. The guides explicitly separate console actions that require your account access from repository work that Codex will implement.

## The short version of the order

1. You create the free Firebase development project.
2. You register Android and desktop app identities.
3. You enable Email/Password and Play Games authentication.
4. You connect the same OAuth web client to Firebase and Play Games.
5. You register App Check without enforcing it.
6. You create a free Cloudflare account.
7. Codex creates and tests the backend and game integration.
8. You authorize development deployments and test with your Google accounts.
9. Together, we complete privacy declarations and production rollout.

## Ownership legend

- **User:** Requires access to Firebase, Cloudflare, Google Play Console, DNS, legal declarations, signing certificates, or private credentials.
- **Codex:** Repository code, tests, Firebase/Cloudflare templates, local development setup, UI, data migration, and documentation.
- **Together:** Requires values supplied by the user and code/configuration completed by Codex.

Never commit:

- Firebase service-account JSON files.
- Release keystores or passwords.
- Play Console credentials.
- OAuth client secrets.
- Desktop refresh tokens.
- `.env` files containing production secrets.

Firebase client configuration such as `google-services.json` is not an admin secret, but this project should still keep environment-specific copies out of public commits unless deliberately approved.

## Console map

| Need | Website | Usual location |
|---|---|---|
| Firebase project/apps | [Firebase Console](https://console.firebase.google.com/) | Project Overview / Project settings |
| Authentication providers | Firebase Console | Build or Security → Authentication → Sign-in method |
| App Check | Firebase Console | Build or Security → App Check |
| OAuth clients | [Google Cloud Console](https://console.cloud.google.com/apis/credentials) | APIs & Services → Credentials |
| Play Games configuration | [Google Play Console](https://play.google.com/console/) | Grow → Play Games Services → Setup and management |
| App-signing SHA fingerprints | Google Play Console | Setup or Test and release → App integrity |
| Worker and D1 | [Cloudflare Dashboard](https://dash.cloudflare.com/) | Workers & Pages / Storage & Databases → D1 |

If a label differs, use the console search box for the bold page name rather than selecting a similarly named product.

## Phase order

| Phase | Guide | Primary result |
|---|---|---|
| 1 | [Firebase, Cloudflare, and Play Games setup](01_FIREBASE_AND_PLAY_GAMES_SETUP.md) | Free identity and backend projects configured |
| 2 | [Backend foundation](02_BACKEND_FOUNDATION.md) | Worker API, D1 schema, and local development |
| 3 | [Shared client architecture](03_SHARED_CLIENT_ARCHITECTURE.md) | Platform-neutral account and sync interfaces |
| 4 | [Android authentication](04_ANDROID_AUTHENTICATION.md) | Play Games and email linking on Android |
| 5 | [Desktop authentication](05_DESKTOP_AUTHENTICATION.md) | Email/password Firebase REST client |
| 6 | [Account UI](06_ACCOUNT_UI.md) | Account screen and linking/deletion flows |
| 7 | [Cloud progress sync](07_CLOUD_PROGRESS_SYNC.md) | Versioned, merge-safe cross-device saves |
| 8 | [Leaderboard](08_LEADERBOARD.md) | Validated points ranking with five-minute cache |
| 9 | [Privacy, testing, and release](09_PRIVACY_TESTING_RELEASE.md) | Policy compliance and production rollout |

## Branch and PR strategy

The current `leaderboard` branch is based on `visual_overhaul`.

1. Keep account-system work on `leaderboard`.
2. Open or merge `visual_overhaul -> master` first.
3. While visual overhaul is pending, use a stacked PR: `leaderboard -> visual_overhaul`.
4. After visual overhaul reaches `master`, rebase `leaderboard` onto `master` if the first PR was squash-merged.
5. Retarget the account-system PR to `master`.
6. Do not merge backend production configuration before Worker/D1 integration tests pass.

## Definition of done

The feature is complete only when:

- One Firebase user can use Play Games on Android and email/password on desktop.
- Existing anonymous local progress survives first login.
- Phone and PC progress merge without regressions or duplicated attempts.
- Points and completion rewards are calculated server-side once per level.
- Leaderboards use cached validated data and enforce refresh limits.
- Account merge, logout, password reset, and deletion work.
- Offline play remains functional.
- Emulator tests, Android internal testing, and desktop integration tests pass.
- Privacy policy and Google Play declarations are updated.

## Official references

- [Firebase Play Games authentication](https://firebase.google.com/docs/auth/android/play-games)
- [Firebase account linking](https://firebase.google.com/docs/auth/android/account-linking)
- [Firebase App Check with Play Integrity](https://firebase.google.com/docs/app-check/android/play-integrity-provider)
- [Firebase Emulator Suite](https://firebase.google.com/docs/emulator-suite)
- [Firebase pricing](https://firebase.google.com/pricing)
- [Cloudflare Workers pricing](https://developers.cloudflare.com/workers/platform/pricing/)
- [Cloudflare D1 pricing](https://developers.cloudflare.com/d1/platform/pricing/)
