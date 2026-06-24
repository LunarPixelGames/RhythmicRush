# Phase 1: First-Time Firebase, Cloudflare, and Play Games Setup

## Purpose

This is a click-by-click guide for someone who has never used Firebase or Cloudflare.

Do not try to complete everything in one sitting. Follow each checkpoint in order. When a section says **Stop here for Codex**, do not guess at Gradle/code changes; tell Codex that the preceding checkpoint is complete.

The planned free stack is:

- Firebase Spark: Authentication and App Check only.
- Google Play Games Services: Android gaming identity.
- Cloudflare Workers Free: account/save/leaderboard API.
- Cloudflare D1 Free: database.
- Cloudflare Pages Free: deletion-request webpage.

Do not enable Firebase Blaze, Cloud Functions, Firestore, Realtime Database, or Storage.

## Important names used by this project

Copy these exactly when instructed:

```text
Android package/application ID:
io.github.msameer0.rhythmicrush

Development Firebase display name:
Rhythmic Rush Dev

Suggested development Firebase project ID:
rhythmic-rush-dev

Suggested Android app nickname:
Rhythmic Rush Android Dev

Suggested Firebase web app nickname:
Rhythmic Rush Desktop Dev
```

Firebase project IDs must be globally unique. If `rhythmic-rush-dev` is unavailable, Firebase will suggest a suffix. Record the final project ID exactly.

## Part A: Create the development Firebase project

Start with one development project. Do not create production yet. We will duplicate the proven setup only after development login works.

### A1. Open Firebase

1. Open [Firebase Console](https://console.firebase.google.com/).
2. Sign in using the Google account that owns or manages Rhythmic Rush in Google Play Console.
3. Click **Create a project** or **Add project**.

### A2. Name the project

1. Project name: `Rhythmic Rush Dev`.
2. Expand or click the edit control beside the generated project ID.
3. Try `rhythmic-rush-dev`.
4. If unavailable, accept a sensible Firebase-generated suffix.
5. Record the final project ID in a private note.
6. Click **Continue**.

The project ID cannot be changed later. The display name can.

### A3. Disable optional products

Firebase may offer Gemini assistance and Google Analytics.

- Gemini in Firebase: optional; disabling it does not affect the account system.
- Google Analytics: select **Disable Google Analytics for this project**.

Analytics is not required for authentication, saves, or leaderboards.

Click **Create project**, wait for completion, then click **Continue**.

### A4. Confirm the free plan

1. In Firebase Console, click the gear beside **Project Overview**.
2. Open **Usage and billing** or check the plan shown near the project settings.
3. Confirm the plan says **Spark**.
4. Do not click Upgrade.
5. Do not attach a billing account.

### Checkpoint A

Record:

```text
Firebase display name:
Firebase project ID:
Firebase project number:
Current plan: Spark
```

Find project ID/number through:

1. Gear beside **Project Overview**.
2. **Project settings**.
3. **General** tab.
4. Look under **Your project**.

## Part B: Register the Android app in Firebase

### B1. Start app registration

1. Return to **Project Overview**.
2. Click the Android robot icon.
3. If the icon is not visible, click **Add app**, then select **Android**.

### B2. Enter Android details

Enter:

```text
Android package name:
io.github.msameer0.rhythmicrush

App nickname:
Rhythmic Rush Android Dev
```

The package name is case-sensitive and already confirmed from `android/build.gradle`.

Leave the SHA certificate field empty for this first registration screen if you do not have the fingerprints yet. We will add both fingerprints in Part C.

Click **Register app**.

### B3. Download the Firebase configuration

1. Click **Download google-services.json**.
2. Keep the filename exactly `google-services.json`.
3. Do not rename it to `google-services (1).json`.
4. Save it somewhere you can find, but do not commit it yet.
5. Do not manually paste it into the project until Codex configures environment handling.

The file contains project identifiers rather than an admin password, but we still want deliberate dev/prod handling.

### B4. Stop before changing Gradle

Firebase will display instructions about:

- Google services Gradle plugin.
- Firebase BoM.
- Firebase dependencies.

Do not edit those files manually.

Click **Next** through the instructions and then **Continue to console**.

### Checkpoint B

You should now see one Android app under:

1. Gear beside **Project Overview**.
2. **Project settings**.
3. **General**.
4. Scroll to **Your apps**.

Confirm its package is exactly `io.github.msameer0.rhythmicrush`.

## Part C: Add signing certificate fingerprints

Firebase and Play Games need SHA fingerprints to recognize legitimate Android builds.

There are two important certificate sources:

- Debug certificate: used when running locally.
- Google Play App Signing certificate: used by builds installed from Google Play.

### C1. Get the local debug SHA values

From the project root on Windows, run:

```powershell
.\gradlew.bat :android:signingReport
```

Find the block whose variant is `debug`.

Record:

```text
Debug SHA-1:
Debug SHA-256:
```

If the command cannot run, ask Codex to inspect the signing report. Do not create random certificates.

### C2. Add debug fingerprints to Firebase

1. Open Firebase Console.
2. Select `Rhythmic Rush Dev`.
3. Click the gear beside **Project Overview**.
4. Open **Project settings**.
5. Stay on **General**.
6. Scroll to **Your apps**.
7. Select/expand `Rhythmic Rush Android Dev`.
8. Find **SHA certificate fingerprints**.
9. Click **Add fingerprint**.
10. Paste the debug SHA-1 and save.
11. Click **Add fingerprint** again.
12. Paste the debug SHA-256 and save.

### C3. Get Google Play App Signing fingerprints

If Rhythmic Rush is already created in Google Play Console:

1. Open [Google Play Console](https://play.google.com/console/).
2. Select Rhythmic Rush.
3. Open **Setup → App integrity**. In some console layouts this appears under **Test and release → Setup → App integrity**.
4. Find **App signing key certificate**.
5. Copy its SHA-1.
6. Copy its SHA-256.

Do not use the upload-key certificate when Firebase asks for the certificate of builds installed from Play. The important production values are under **App signing key certificate**.

### C4. Add Play fingerprints to Firebase

Repeat the Firebase steps from C2 and add:

- Play App Signing SHA-1.
- Play App Signing SHA-256.

If the app is not yet in Play Console, mark this step pending. Debug email login can still be developed, but production Play Games login cannot be considered complete.

### C5. Download the updated JSON again

After adding fingerprints:

1. Firebase **Project settings → General → Your apps**.
2. Open the Android app.
3. Click **Download google-services.json** again.
4. Replace your earlier downloaded copy.

### Checkpoint C

Firebase Android app should list:

- Debug SHA-1.
- Debug SHA-256.
- Play App Signing SHA-1, if available.
- Play App Signing SHA-256, if available.

## Part D: Enable Firebase Authentication

### D1. Open Authentication

Depending on the Firebase Console layout:

1. Left sidebar → **Build → Authentication**, or
2. Left sidebar → **Security → Authentication**.

Click **Get started** if shown.

### D2. Enable Email/Password correctly

1. Open the **Sign-in method** tab.
2. Click **Add new provider** or select **Email/Password**.
3. Turn on the main **Email/Password** switch.
4. Leave **Email link (passwordless sign-in)** switched off.
5. Click **Save**.

We use an email and password on desktop. Passwordless links would require a separate deep-link flow.

### D3. Enable Play Games

The provider list includes both **Google** and **Play Games**.

- Enable **Play Games**.
- Do not enable ordinary **Google** for v1.
- Do not enable Phone, Anonymous, Facebook, Game Center, Apple, GitHub, Microsoft, Twitter, Yahoo, OpenID Connect, or SAML.

Play Games asks for a web server client ID and client secret. If you do not have them yet, leave this dialog open or cancel and complete Part E first.

### D4. Authentication settings

Open the Authentication **Settings** tab.

Check:

- Authorized domains: leave Firebase defaults for now.
- User actions/email templates: default templates are okay during development.
- Password policy: keep defaults initially; we will choose and document the in-game policy during UI implementation.
- User account linking: do not manually change advanced collision behavior unless the implementation guide says to.

### Checkpoint D

In **Authentication → Sign-in method**, you should eventually have:

```text
Email/Password: Enabled
Email link passwordless: Disabled
Play Games: Enabled after Part E
Google: Disabled
All other providers: Disabled
```

## Part E: Connect Firebase Authentication to Google Play Games

This is the easiest section to misconfigure. The same web OAuth client must be used in Firebase and Play Games.

### E1. Open Google Cloud credentials for the Firebase project

1. In Firebase, open **Project settings → General**.
2. Confirm the current project is `Rhythmic Rush Dev`.
3. Open [Google Cloud Console Credentials](https://console.cloud.google.com/apis/credentials).
4. At the top project selector, choose the Google Cloud project backing `Rhythmic Rush Dev`.

If unsure, match the Firebase project ID/project number.

### E2. Find or create the web-server OAuth client

Under **OAuth 2.0 Client IDs**, find:

```text
Web client (auto created for Google Sign-in)
```

Open it and record:

```text
Web server client ID:
Web server client secret:
```

Treat the client secret as private. Do not commit or screenshot it publicly.

If a suitable Web application client already exists, do not create a duplicate. Use that client and continue to E3.

If **OAuth 2.0 Client IDs** says there are no clients, create the missing client:

1. On the Credentials page, click **Configure consent screen** in the warning banner.
2. If the newer Google Auth Platform opens, click **Get started**.
3. App name: `Rhythmic Rush Dev`.
4. User support email: choose your developer/support Google email.
5. Audience/user type: **External** unless this game is restricted to one Google Workspace organization.
6. Developer contact email: enter an email you monitor.
7. Accept the Google API Services user-data policy acknowledgement if shown.
8. Click **Create**, **Save**, or **Finish**.
9. For a development project, leave publishing status as **Testing**.
10. Open **Audience** or **Test users** and add every Google account that will test the game.
11. Return to **APIs & Services → Credentials**.
12. Click **Create credentials → OAuth client ID**.
13. Application type: **Web application**.
14. Name: `Rhythmic Rush Play Games Web Dev`.
15. Leave **Authorized JavaScript origins** empty.
16. Leave **Authorized redirect URIs** empty.
17. Click **Create**.
18. Copy the resulting client ID and client secret into the private worksheet.

This manually created Web application client serves the same web-server purpose as the auto-created client described by Firebase documentation. Do not use either Firebase API key from the **API Keys** table; API keys are not OAuth client IDs.

### E3. Finish enabling Play Games in Firebase

1. Firebase → **Authentication → Sign-in method**.
2. Select **Play Games**.
3. Enable it.
4. Paste the web server client ID.
5. Paste the web server client secret.
6. Choose a project support email.
7. Click **Save**.

### E4. Open Play Games Services configuration

1. Open Google Play Console.
2. Select Rhythmic Rush.
3. Left sidebar → **Grow → Play Games services → Setup and management → Configuration**.
4. When asked **Which Play Games Services project do you want to use?**, choose **Create new Play Game Services project**.
5. Despite its name, this option creates the Play Games Services layer and then lets you link it to an existing Google Cloud project.
6. Do not choose **Use an existing Play Games Services project** unless another Play Console app already has a Play Games Services configuration that this app intentionally shares.
7. Continue to the Google Cloud project selector.
8. Select the existing Google Cloud project backing `Rhythmic Rush Dev`. Match its Firebase project ID/project number.
9. Save or click **Use**.

Do not select an unrelated existing Cloud project.

If `Rhythmic Rush Dev` does not appear after choosing **Create new Play Game Services project**:

1. Confirm the Google account currently signed into Play Console has Owner or Editor access to the Firebase/Google Cloud project.
2. Open Google Cloud Console and select `Rhythmic Rush Dev`.
3. Open **IAM & Admin → IAM** and confirm the same email is listed.
4. Confirm the **Google Play Games Services API** is enabled through **APIs & Services → Enabled APIs & services**.
5. Return to Play Console, refresh, and repeat E4.

### E5. Add the game-server credential

On the Play Games Services configuration page:

1. Click **Add credential**.
2. Credential type: **Game server**.
3. OAuth client: select the same web client ID from E2.
4. Save.

### E6. Add the Android credential

Firebase may not automatically create Android OAuth clients after fingerprints are added. If the Play Console dropdown says **No OAuth clients available**, create them manually in the same Google Cloud project.

#### E6a. Create the debug Android OAuth client

1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Confirm the selected project is the Google Cloud project backing `Rhythmic Rush Dev`.
3. Open **Google Auth Platform → Clients**. In an older layout, use **APIs & Services → Credentials**.
4. Click **Create client** or **Create credentials → OAuth client ID**.
5. Application type: **Android**.
6. Name: `Rhythmic Rush Android Debug Dev`.
7. Package name: `io.github.msameer0.rhythmicrush`.
8. SHA-1 certificate fingerprint: paste the local **debug SHA-1** from `:android:signingReport`.
9. Click **Create**.

Only SHA-1 is entered when creating an Android OAuth client. Keep the SHA-256 registered in Firebase, but do not paste it into the SHA-1 field.

#### E6b. Create the Play-signed Android OAuth client

Repeat E6a with:

```text
Name:
Rhythmic Rush Android Play Dev

Package:
io.github.msameer0.rhythmicrush

SHA-1:
The App signing key certificate SHA-1 from Google Play Console
```

Do not use the upload-key SHA-1.

#### E6c. Add both credentials in Play Console

Return to **Play Games Services → Setup and management → Configuration**.

Create the debug credential:

1. Click **Add credential**.
2. Type: **Android**.
3. Name: `Rhythmic Rush Debug`.
4. Anti-piracy: **Off** during development.
5. OAuth client: select `Rhythmic Rush Android Debug Dev`.
6. Click **Save changes**.

Create the Play-signed credential:

1. Click **Add credential** again.
2. Type: **Android**.
3. Name: `Rhythmic Rush Play`.
4. OAuth client: select `Rhythmic Rush Android Play Dev`.
5. Save.

The two credentials have the same package name but different SHA-1 fingerprints:

- Debug credential: locally installed debug builds.
- Play credential: builds installed through Google Play.

If the clients still do not appear:

1. Confirm they were created in the same Google Cloud project linked to this Play Games Services project.
2. Confirm their application type is **Android**, not Web application.
3. Confirm the package name exactly matches `io.github.msameer0.rhythmicrush`.
4. Refresh Play Console or reopen the Add credential page.
5. Wait a few minutes for Google Console propagation.
6. Do not select an Android client belonging to another app or Cloud project.

### E7. Add testers

1. Play Console → **Play Games services → Setup and management → Testers**.
2. Add the Google email addresses that will test login.
3. Save.

Before Play Games configuration is published, only testers can sign in.

### E8. Complete Play Games properties and tester access

After credentials are ready, the Publishing page may show four required properties. These are the public Play Games profile for the game, not extra authentication credentials.

Click the arrow beside each item and provide:

1. **Description:** a short description such as:
   `A fast-paced rhythm platformer where every jump, portal, and obstacle follows the beat.`
2. **Game category:** choose **Music** or the closest available rhythm/music category.
3. **Icon:** upload the Rhythmic Rush square game icon. Use a high-quality PNG with no transparency problems; follow the exact pixel dimensions shown by Play Console.
4. **Feature graphic:** upload a wide Rhythmic Rush banner using the exact dimensions shown by Play Console. Avoid placing important text or the logo close to the edges.

Use development-quality artwork if final store artwork is not ready, but do not upload unrelated placeholder branding.

The Properties header may say **Firebase not linked**. That label refers to an optional Play Games Services/Firebase feature integration and is not required for Firebase Authentication with Play Games. Do not enable Firebase databases or Saved Games merely to clear that label.

Before publishing:

1. Click **Manage testers**.
2. Add every Google account that will test Play Games login.
3. Save the tester list.
4. Confirm the credentials page still says **Ready to publish**.

For initial development, tester accounts can use draft Play Games Services settings. It is safe to leave the configuration in Draft until the metadata and credentials have been reviewed.

When ready for broader/internal testing, return to **Play Games Services → Setup and management → Publishing**, resolve all required properties, and click **Publish**. This publishes Play Games Services configuration; it does not publish the Android app publicly.

Keep the Android app itself on the Google Play internal-testing track until account flows are verified.

### Checkpoint E

Confirm:

- Firebase Play Games provider is enabled.
- Firebase provider uses the web-server OAuth client ID from E2.
- Play Games game-server credential uses that exact web client ID.
- Android credential uses the correct package and SHA-1.
- Your Google account is listed as a tester.

## Part F: Register a Firebase web app for desktop configuration

The LWJGL desktop game will call Firebase Authentication REST endpoints. Registering a web app gives us the public project configuration needed for those calls.

### F1. Register the web app

1. Firebase → **Project Overview**.
2. Click **Add app**.
3. Select the web icon `</>`.
4. App nickname: `Rhythmic Rush Desktop Dev`.
5. Do not enable Firebase Hosting.
6. Click **Register app**.

### F2. Record the public configuration

Firebase displays:

```javascript
const firebaseConfig = {
  apiKey: "...",
  authDomain: "...",
  projectId: "...",
  storageBucket: "...",
  messagingSenderId: "...",
  appId: "..."
};
```

Record:

- `apiKey`
- `authDomain`
- `projectId`
- `appId`
- `messagingSenderId`/project number

We do not need Storage or Messaging for v1.

If you close the screen, find the values again through:

1. **Project settings → General**.
2. Scroll to **Your apps**.
3. Select `Rhythmic Rush Desktop Dev`.
4. Find **SDK setup and configuration**.

### Checkpoint F

You should have two Firebase apps in one dev project:

- Android: `io.github.msameer0.rhythmicrush`
- Web: `Rhythmic Rush Desktop Dev`

## Part G: App Check — register but do not enforce yet

Do this after the Android app and SHA-256 fingerprints are registered.

### G1. Open App Check

1. Firebase left sidebar → **Build/Security → App Check**.
2. Open the **Apps** tab.
3. Find `Rhythmic Rush Android Dev`.
4. Click **Register**.
5. Provider: **Play Integrity**.

### G2. Configure initial settings

For initial development:

- Token TTL: keep the default one hour.
- Do not enable enforcement yet.
- Do not require Strong Integrity.
- Keep the default integrity settings unless distribution requirements prove otherwise.

App Check enforcement before the SDK is integrated would block legitimate development builds.

### G3. Debug builds

Local/debug installations may not pass production Play Integrity. Codex will later add the App Check debug provider for debug builds and Play Integrity for release builds.

Do not paste App Check debug tokens into Git.

### Checkpoint G

App Check should show the Android app registered with Play Integrity, but enforcement should remain off.

## Part H: Create the Cloudflare account

Cloudflare setup is intentionally minimal now. Codex will generate the Worker project and D1 migrations in Phase 2.

### H1. Create/sign into Cloudflare

1. Open [Cloudflare Dashboard](https://dash.cloudflare.com/).
2. Create an account or sign in.
3. Verify the account email.
4. Do not upgrade Workers.
5. Do not attach a paid Workers plan.

### H2. Find the account ID

Cloudflare UI changes occasionally. Common locations:

- Dashboard home/right sidebar under **Account ID**.
- **Workers & Pages → Overview**.
- Copy it from account/API details.

Record:

```text
Cloudflare account ID:
```

### H3. Do not manually create production resources yet

Do not create random Workers, D1 schemas, KV namespaces, or Pages projects yet. Codex will scaffold the repository and use Wrangler so resource names and bindings match the code.

When Phase 2 code is ready, Codex will ask you to:

1. Authorize Wrangler through the browser.
2. Run or approve Worker creation.
3. Create dev/prod D1 databases with exact names.
4. copy generated database IDs into untracked configuration.
5. deploy the deletion page.

### Checkpoint H

You only need:

- Verified Cloudflare account.
- Account ID recorded.
- Workers plan still Free.

## Part I: Values worksheet

Create a private note. Do not commit secrets.

```text
FIREBASE_ENV=dev
FIREBASE_PROJECT_ID=
FIREBASE_PROJECT_NUMBER=
FIREBASE_WEB_API_KEY=
FIREBASE_WEB_APP_ID=
FIREBASE_AUTH_DOMAIN=

ANDROID_PACKAGE=io.github.msameer0.rhythmicrush
DEBUG_SHA1=
DEBUG_SHA256=
PLAY_APP_SIGNING_SHA1=
PLAY_APP_SIGNING_SHA256=

PLAY_GAMES_PROJECT_ID=
PLAY_GAMES_WEB_CLIENT_ID=
PLAY_GAMES_WEB_CLIENT_SECRET=DO_NOT_COMMIT

CLOUDFLARE_ACCOUNT_ID=
ACCOUNT_API_BASE_URL=NOT_CREATED_YET
CLOUDFLARE_D1_DATABASE_ID=NOT_CREATED_YET
```

Codex will add a safe `.env.example` without real secrets during implementation.

## Part J: What not to enable

For this architecture, leave these disabled/unconfigured:

- Firebase Blaze billing.
- Cloud Functions.
- Firestore.
- Realtime Database.
- Cloud Storage.
- Phone authentication.
- Anonymous authentication.
- Email-link/passwordless authentication.
- Ordinary Google authentication provider.
- App Check enforcement before client integration.
- Paid Cloudflare Workers plan.

## Phase 1 completion gate

Phase 1 is complete when:

- Firebase dev project exists on Spark.
- Android app and web app are registered.
- Correct debug and Play signing fingerprints are added.
- Email/Password is enabled.
- Email-link/passwordless is disabled.
- Play Games is enabled with the correct web client.
- Play Games game-server and Android credentials exist.
- Tester accounts are added.
- App Check is registered but not enforced.
- Cloudflare account exists on Free.
- Configuration worksheet is filled privately.

Do not create the production Firebase/Cloudflare setup yet. First implement and verify all flows against development.

## Troubleshooting quick reference

### Play Games provider asks for client ID and secret

Use the **Web client (auto created for Google Sign-in)** from the same Firebase/Google Cloud project. Do not use the Android client ID.

### Android OAuth client is missing in Play Console

Check:

- Firebase package name.
- SHA-1 added.
- Correct Cloud project selected.
- Wait a few minutes and refresh.

### `DEVELOPER_ERROR` during Play Games login

Usually caused by:

- Wrong package name.
- Wrong SHA-1.
- Wrong web server client ID.
- Tester not added.
- Build signed with a certificate not registered.

### Email/password works but Play Games does not

Email/password does not verify the Play Games OAuth configuration. Recheck Part E independently.

### App Check rejects debug builds

Enforcement should remain off until Codex adds the debug provider and registers a debug token.

## Official references

- [Add Firebase to Android](https://firebase.google.com/docs/android/setup)
- [Firebase Email/Password Auth](https://firebase.google.com/docs/auth/android/password-auth)
- [Firebase Play Games Auth](https://firebase.google.com/docs/auth/android/play-games)
- [Firebase App Check with Play Integrity](https://firebase.google.com/docs/app-check/android/play-integrity-provider)
- [Cloudflare Workers getting started](https://developers.cloudflare.com/workers/get-started/guide/)
- [Cloudflare D1 getting started](https://developers.cloudflare.com/d1/get-started/)
