# Phase 6: Account UI

## Objective

Add a controller-friendly, mouse-friendly Account screen that exposes authentication, linking, synchronization, merge, and deletion without leaking backend terminology.

## How this phase works

There are no required Firebase or Cloudflare Console changes.

**Codex** implements the screens and connects them to the clients from Phases 3–5.

**User** reviews the experience on mouse/keyboard, touch, and controller. Test the screens in this order:

1. Desktop signed out.
2. Android signed out.
3. Registration and email verification.
4. Signed in with email only.
5. Signed in with Play Games only.
6. Signed in with both providers linked.
7. Offline with a restored session.
8. Merge-required state.
9. Delete-account confirmation.

At each screen verify that hit areas match buttons, focus is visible, Back/Escape is safe, loading never freezes animation, errors explain the next action, and passwords are masked by default.

## Implementation status

Implemented:

- Temporary red **Online** square to the right of the main Play button.
- Dedicated Online screen using the main menu's procedural-background mechanism.
- Signed-out Play Games, email login, email registration, and password-reset actions.
- Native password-masked text prompts suitable for desktop and mobile keyboards.
- Email-verification and username-required states.
- Signed-in profile summary and cached global leaderboard.
- Upload, Fetch, Refresh Leaderboard, Logout, and Delete controls along the bottom.
- Conservative Fetch merge that never reduces local best progress, attempts, coins, or points.
- Upload queue fallback when authoritative Phase 7 validation is not ready.
- Mouse/touch hit targets and Escape/back navigation.
- Temporary shape-rendered controls so final artwork can replace them later.

Current intentional limitations:

- Upload is accepted by the client but remains locked by the backend until Phase 7 adds the authoritative level catalog and idempotent merge.
- Guided merge is not yet exposed because the backend merge protocol is still locked.
- Delete requires recent authentication; if Firebase requests reauthentication, the screen displays the returned error rather than deleting partial data.
- The final Online button texture is deferred at the user's request.

## Entry points

**Codex**

Add Account and Leaderboard buttons to the main menu. Account button state:

- Signed out: “Account.”
- Signing in/syncing: spinner/status marker.
- Signed in: username or account icon.
- Offline/error: non-blocking warning marker.

## Signed-out screen

Android:

- Continue with Play Games.
- Sign in with Email.
- Register with Email.
- Password Reset.

Desktop:

- Sign in with Email.
- Register.
- Password Reset.

Include:

- Back button.
- Privacy policy link.
- Clear network/error status.
- Loading-state input lock.

## Registration and username

Registration fields:

- Email.
- Password.
- Confirm password.
- Unique username.

Client validation improves UX but server validation is authoritative.

Username rules:

- 3–20 characters.
- ASCII letters, digits, underscore.
- Case-insensitive uniqueness.
- Profanity/reserved-name validation.
- Thirty-day rename cooldown.

Do not reveal whether an email exists during password reset.

## Signed-in screen

Display:

- Username.
- Email verification status.
- Linked providers.
- Last successful sync.
- Offline/pending-upload status.
- Local and cloud progress summaries.

Actions:

- Upload Progress.
- Fetch Progress.
- Link Play Games, Android only.
- Enable/Change PC Login.
- Resend Verification.
- Change Username when cooldown permits.
- Password Reset.
- Logout.
- Delete Account.

Buttons show operation-specific status and disable only conflicting operations.

## Sync confirmations

Upload confirmation summarizes:

- Local best progress.
- Pending attempt deltas.
- Cloud data will be safely merged, not overwritten.

Fetch confirmation states:

- Local backup will be created.
- Stronger local/cloud values are preserved.
- The resulting canonical save is written locally.

After success, show concise changes: levels improved, attempts added, rewards reconciled.

## Guided merge UI

Stages:

1. Explain that Play Games and email belong to separate game accounts.
2. Reauthenticate both identities.
3. Display both usernames and save summaries.
4. Confirm canonical account and preserved username.
5. Start recoverable backend merge.
6. Show progress stages.
7. Finish with canonical profile and forced fetch.

Do not offer a destructive “just replace” shortcut.

## Delete account UI

Require:

- Recent reauthentication.
- Typed username confirmation.
- Explicit acknowledgement that cloud progress and leaderboard entry are deleted.

Clarify:

- Local progress may optionally remain on the device as anonymous progress.
- Google/Play Games account itself is not deleted.

Provide the external deletion-request URL.

## Accessibility and responsiveness

- Keyboard/controller focus order.
- Mouse/touch hit areas.
- Password masking and show/hide toggle.
- Safe text scaling at supported resolutions.
- No color-only status communication.
- Network work never freezes animation.

## Completion gate

- Every account state has a usable screen.
- Back/navigation works during recoverable errors.
- Sensitive actions require confirmation.
- Android-only actions are hidden on desktop.
- Offline state preserves local gameplay.

## Approval checkpoint

Before Phase 7, approve the final button names, logout behavior, account-deletion behavior for local progress, username wording/rules, merge confirmation text, and privacy/deletion links.
