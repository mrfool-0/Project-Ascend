# Google sign-in, cloud progress, and SYSTEM AI setup

ASCEND runs completely offline without Firebase. Live Google sign-in and cloud progress require a Firebase project owned by the app publisher; credentials cannot be safely embedded in a source template.

1. Create a Firebase Android app with package name `com.ascend.app`.
2. Add the debug and release SHA-1/SHA-256 signing fingerprints in Firebase project settings.
3. Enable **Authentication → Sign-in method → Google**.
4. Create a Cloud Firestore database.
5. In **Firebase AI Logic**, choose **Get started**, select the **Gemini Developer API**, and enable it for the project. SYSTEM uses `gemini-3.7-flash` with schema-constrained output.
6. Download `google-services.json` into `app/google-services.json`. The build enables the Google Services plugin automatically when this file exists.
7. The updated JSON must contain an `oauth_client` with `client_type: 3` (Web application). ASCEND automatically reads the generated `default_web_client_id`; manually copying it is no longer necessary. The optional `google_oauth_web_client_id` string remains an explicit override. Android OAuth still requires the correct signing fingerprints from step 2.
8. In **Security → App Check**, register the Android app with the Play Integrity provider and its release SHA-256 fingerprint. The release build installs Play Integrity automatically.
9. Deploy the checked-in owner-only rules before distributing the app:

```bash
firebase deploy --only firestore:rules
```

`firestore.rules` denies every unspecified path, requires a verified Firebase identity whose UID matches the player path, allows only the two expected progress documents, validates their field allowlists and numeric ranges, rejects collection listing, and prevents client deletion. Server timestamps are used so clients cannot forge sync times.

Debug builds deliberately use Firebase's App Check debug provider so emulator development remains possible. Launch a debug build, copy the debug token printed by `DebugAppCheckProvider`, and register it under **App Check → Apps → Manage debug tokens**. Treat that token as a secret; never commit it. Before enabling enforcement, monitor valid-request metrics, then enable App Check enforcement for Cloud Firestore, Firebase AI Logic, and Authentication.

The onboarding consent screen clearly separates cloud save from private offline mode. ASCEND uploads a player setup snapshot and compact progress snapshots only after the user chooses Google sign-in. Email addresses are not written into progress documents. Do not weaken the checked-in rules for development convenience in a production project.

## Verification and limits

Version 1.8.1 uses `GetSignInWithGoogleOption` for both explicit Google buttons, rather than the bottom-sheet `GetGoogleIdOption`. This supports the dedicated interactive account-selection flow. A `NoCredentialException` does not by itself prove that the device has no Google account; verify the app's package, actual APK signing SHA-1, Web OAuth client and Play services before blaming the account. Cancellation is reported separately from configuration failure. See [Android's button-flow documentation](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation#create-si-w-g).

On 12 September 2026, the project's Android SHA table was verified empty. With the owner's approval, the current development APK's SHA-1 and SHA-256 were registered and a fresh configuration downloaded. It now includes an Android OAuth client for `com.ascend.app` and the matching certificate. Google provider status was verified Enabled. These are backend configuration checks, not proof of a completed device login. Future signing certificates need their own registration.

On 14 September 2026, Firebase Authentication showed an existing Google user while Firestore still displayed **Create database**. The UI was awaiting a Firestore write without a deadline after authentication. With the owner's approval, the default Standard Firestore database was created in `asia-south1` (Mumbai) on Spark, initially deny-all; the checked-in private rules were then published. Live Rules Playground simulations confirmed verified-owner reads allowed, anonymous reads denied, and different-user reads denied. Simulations do not create player data and are not a substitute for a physical-device write test.

Version 1.8.2 separates Google authentication from cloud backup. The UI reports account selection, Firebase authentication and snapshot saving as distinct stages. Account selection has a 90-second deadline, Firebase authentication 20 seconds, and explicit backup confirmation 12 seconds. Deadlines are recoverable errors; leaving the screen still propagates coroutine cancellation. A failed backup no longer signs a successfully authenticated user out. Onboarding offers **Continue · Backup pending** after successful login without a confirmed backup. Profile persists the linked account locally before attempting the snapshot and shows a separate persisted backup status, never just an email/green cloud icon as proof of saving.

Local workout/food/habit logging enqueues compact Firestore snapshots without waiting for the server. Firestore's pending writes may finish later; the app explicitly discloses this and updates confirmation only after a successful acknowledgement. Timeout does not cancel a queued write. Stage-only `AscendAuth` logs contain no tokens, email addresses or player fields. See [Firestore offline behavior](https://firebase.google.com/docs/firestore/manage-data/enable-offline).

Profile → Account & privacy supports linking Google after offline onboarding and retrying a progress sync. A successful save is shown only after Firestore acknowledges the write; sign-in alone is not treated as a backup. Full database restore, workout-plan transfer, chat backup and profile-photo cloud storage are not implemented. Keep the existing installation to retain those local records.

1.8.2 verification: debug assembly and Android lint passed (zero errors; four dependency-version advisories), 56 JVM tests and 23 Android instrumentation tests passed. Seven new cloud-operation tests cover never-finishing auth and write Tasks, successful null `Task<Void>` writes, rejected writes, late completion, and parent cancellation. The API 26 emulator lacks Google Play services, so an end-to-end account selection → Firebase snapshot confirmation still needs the user's Google-enabled phone.

Run `./gradlew signingReport` and register the actual debug certificate for this APK; release/Play builds need their own certificates. Test sign-in and sync on an account-enabled physical device before distribution. A downloaded web OAuth client is necessary but does not prove the Android certificate, provider enablement, App Check or Firestore deployment is correct.

SYSTEM sends the current message, recent conversation and calibrated fitness context to Firebase AI Logic. The model proposes bounded actions; it never writes to Room directly. Changes require a visible confirmation, expire after 15 minutes, and are rejected if the underlying plan changed. Offline fallback is rule-based, not equivalent to the live model.
