# Google sign-in, cloud progress, and SYSTEM AI setup

ASCEND runs completely offline without Firebase. Live Google sign-in and cloud progress require a Firebase project owned by the app publisher; credentials cannot be safely embedded in a source template.

1. Create a Firebase Android app with package name `com.ascend.app`.
2. Add the debug and release SHA-1/SHA-256 signing fingerprints in Firebase project settings.
3. Enable **Authentication → Sign-in method → Google**.
4. Create a Cloud Firestore database.
5. In **Firebase AI Logic**, choose **Get started**, select the **Gemini Developer API**, and enable it for the project. SYSTEM uses `gemini-3.7-flash` with schema-constrained output.
6. Download `google-services.json` into `app/google-services.json`. The build enables the Google Services plugin automatically when this file exists.
7. Copy the **Web application** OAuth client ID into `app/src/main/res/values/strings.xml` as `google_oauth_web_client_id`.
8. In **Security → App Check**, register the Android app with the Play Integrity provider and its release SHA-256 fingerprint. The release build installs Play Integrity automatically.
9. Deploy the checked-in owner-only rules before distributing the app:

```bash
firebase deploy --only firestore:rules
```

`firestore.rules` denies every unspecified path, requires a verified Firebase identity whose UID matches the player path, allows only the two expected progress documents, validates their field allowlists and numeric ranges, rejects collection listing, and prevents client deletion. Server timestamps are used so clients cannot forge sync times.

Debug builds deliberately use Firebase's App Check debug provider so emulator development remains possible. Launch a debug build, copy the debug token printed by `DebugAppCheckProvider`, and register it under **App Check → Apps → Manage debug tokens**. Treat that token as a secret; never commit it. Before enabling enforcement, monitor valid-request metrics, then enable App Check enforcement for Cloud Firestore, Firebase AI Logic, and Authentication.

The onboarding consent screen clearly separates cloud save from private offline mode. ASCEND uploads a player setup snapshot and compact progress snapshots only after the user chooses Google sign-in. Email addresses are not written into progress documents. Do not weaken the checked-in rules for development convenience in a production project.
