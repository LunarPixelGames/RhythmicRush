# Phase 9: Privacy, Testing, and Release

## Objective

Complete compliance, end-to-end validation, staged rollout, monitoring, and recovery procedures before production account creation is enabled.

## Important stop sign

Do not create or enable production accounts merely because development login works. Complete this phase in order.

## Part A: Create production only after development passes

### Firebase production project

1. Open [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project**.
3. Name it `Rhythmic Rush`.
4. Choose a production project ID such as `rhythmic-rush` if available.
5. Disable Google Analytics unless separately needed.
6. Confirm the plan is **Spark**.
7. Repeat Phase 1 app registration, SHA fingerprints, Email/Password, Play Games, desktop web app, and App Check steps with production values.
8. Keep development and production worksheets separate.
9. Never point a development build at production accidentally.

### Cloudflare production resources

Codex creates these only after explicit approval:

```text
D1: rhythmic-rush
Worker: rhythmic-rush-api
```

Verify them through Cloudflare Dashboard as described in Phase 2. Apply production migrations before enabling traffic.

## Privacy policy

**Together**

Update `PRIVACY.md` and hosted policy to describe:

- Email address and verification status.
- Google Play Games/Firebase identifiers.
- Public username.
- Local and cloud progress.
- Leaderboard score.
- Installation UUID.
- Authentication, rate-limit, App Check, and abuse-prevention metadata.
- Firebase/Google and Cloudflare as processors/providers.
- Retention and deletion behavior.
- Contact method.

Do not claim data is anonymous when it is tied to an account UID.

## Google Play Data Safety

**User**

Open [Google Play Console](https://play.google.com/console/), select **Rhythmic Rush**, then open **Policy and programs → App content → Data safety** and click **Start**, **Manage**, or **Edit**.

Update the declaration based on final implementation:

- Data collected.
- Purpose.
- Whether data is shared.
- Encryption in transit.
- Account deletion.
- Optional versus required collection.

Answers must match the production build and privacy policy exactly.

Likely categories include account information, user IDs, game progress/app activity, diagnostics/security metadata, and public username. Re-audit the final code; do not copy those guesses blindly.

## Account deletion

### Add the deletion URL in Play Console

1. Deploy the external deletion page through Cloudflare Pages.
2. Open it in a private/incognito browser and confirm it works without a game login.
3. Copy its HTTPS URL.
4. Google Play Console → **Policy and programs → App content**.
5. Open **Data safety** or the separate **Account deletion** declaration.
6. Enter the external deletion URL.
7. Explain which data is deleted and any justified retention.
8. Save the declaration.

The implementation must provide:

- In-app Delete Account action.
- External deletion-request page hosted on Cloudflare Pages.
- Recent reauthentication.
- Immediate session/token revocation.
- Backend cleanup of profile, save, devices, leaderboard, merge tickets, and rate-limit records.
- Username tombstone before reuse.
- Clear statement that Google/Play Games accounts are not deleted.

### Immediate deletion lifecycle

The current implementation deletes cloud account data immediately after strong in-game confirmation.

The final deletion implementation must:

1. Require recent authentication.
2. Ask for multiple in-game confirmations before deletion.
3. Delete the server-side profile, save, progress, attempts, rewards, operations, leaderboard entry, merge tickets, rate-limit rows, and username reservation.
4. Release the username immediately for reuse.
5. Sign the player out locally.
6. Attempt to delete the Firebase Authentication account from the client session.
7. Keep local anonymous progress playable after cloud-account deletion.

Implemented backend controls:

- `ENABLE_ACCOUNT_DELETION=false` blocks deletion requests.
- `ENABLE_ACCOUNT_DELETION=true` allows immediate cloud-account deletion.

Do not claim there is a 72-hour recovery window. Deletion is intended to be permanent.

## Automated testing

**Codex**

Core tests:

- Account state machine.
- Async cancellation.
- Save serialization/migration.
- Merge behavior.
- Queue persistence.
- Cache cooldown.

Worker/D1 local integration tests:

- Firebase token requirements.
- D1 transaction behavior.
- Username race.
- Upload idempotency.
- Multi-device attempts.
- One-time rewards.
- Revision conflicts.
- Leaderboard ordering/cache.
- Merge recovery.
- Deletion.

Desktop integration:

- Register, verify, login, refresh, logout.
- Secure-store fallback.
- Emulator endpoint switching.
- Network timeout/offline recovery.

Android tests:

- Firebase email flow.
- Play Games sign-in through internal testing.
- Provider linking.
- App Check debug and Play Integrity monitoring.
- Activity recreation during login.

## Manual cross-platform matrix

Test:

1. New Android Play Games user enables PC login.
2. Same user logs into PC and fetches progress.
3. User progresses independently on both devices.
4. Attempts add, best percentages merge, rewards remain one-time.
5. PC-first email user later links Play Games.
6. Two populated accounts complete guided merge.
7. Offline completion uploads after reconnection.
8. Password reset and email verification.
9. Account deletion removes leaderboard entry.
10. Old anonymous save remains playable without login.

## Staged rollout

1. Emulator-only development.
2. Firebase Spark dev project plus development Worker/D1.
3. Google Play internal-testing track.
4. Closed test with a small account cohort.
5. Production backend deployed with account UI hidden behind a feature flag.
6. Enable read-only login/profile.
7. Enable cloud fetch/upload.
8. Enable leaderboard.
9. Enable merge and deletion after dedicated testing.

Keep rollback switches for:

- Disable new registration.
- Disable uploads while retaining fetch.
- Disable leaderboard refresh.
- Disable account merge.
- Force minimum game/content version.

## Monitoring and cost controls

Configure:

- Firebase usage monitoring while remaining on Spark; no billing account is attached.
- Worker error/latency dashboards.
- Auth failure metrics.
- App Check metrics before enforcement.
- D1 row read/write monitoring.
- Rate-limit rejection counts.
- Merge/deletion audit logs without sensitive payloads.

Remain on hard-capped free plans and degrade gracefully when quotas are exhausted.

### Where to check usage

Firebase:

1. Firebase Console → gear → **Usage and billing**.
2. Confirm **Spark** and review Authentication usage.
3. Firebase Console → **App Check** to review valid/invalid request metrics.

Cloudflare:

1. Cloudflare Dashboard → **Workers & Pages** → production Worker.
2. Open **Metrics and logs** or **Observability**.
3. Review requests, errors, and CPU time.
4. Cloudflare Dashboard → **D1** → production database → **Metrics**.
5. Review rows read, rows written, and storage.

Check these daily during the first rollout and after each large release.

## Incident and recovery notes

Document:

- How to disable mutations.
- How to restore a user's save revision.
- How to resume a merge ticket.
- How to remove an abusive leaderboard entry.
- How to rotate compromised deployment credentials.
- How to notify users about outages without promising unsafe manual edits.

## Production gate

- Emulator and CI tests pass.
- Internal Android and desktop matrix passes.
- Security rules reviewed.
- No admin credentials in repository or binaries.
- Privacy policy and Data Safety declarations published.
- Account deletion URL is live.
- Budget alerts and rollback flags are verified.
- Production launch approved only after a backup/export strategy exists.

## Final release click path

1. Google Play Console → **Test and release → Production**.
2. Click **Create new release**.
3. Upload the approved production Android App Bundle.
4. Add user-facing release notes.
5. Resolve warnings and errors.
6. Confirm Data Safety, privacy policy, and deletion URL are accepted.
7. Choose a staged rollout rather than 100% for the first account release.
8. Start rollout.
9. Monitor Firebase Authentication/App Check and Cloudflare Worker/D1.
10. Increase rollout only after login, sync, and deletion remain healthy.

Do not enforce App Check merely because the app was released. First confirm valid Play Integrity traffic from the production build.
