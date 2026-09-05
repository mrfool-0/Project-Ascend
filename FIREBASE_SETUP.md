# Google sign-in, cloud progress, and Food Vision setup

ASCEND runs completely offline without Firebase. Live Google sign-in and cloud progress require a Firebase project owned by the app publisher; credentials cannot be safely embedded in a source template.

1. Create a Firebase Android app with package name `com.ascend.app`.
2. Add the debug and release SHA-1/SHA-256 signing fingerprints in Firebase project settings.
3. Enable **Authentication → Sign-in method → Google**.
4. Create a Cloud Firestore database.
5. In **Firebase AI Logic**, choose **Get started**, select the **Gemini Developer API**, and enable it for the project. Food Vision uses the multimodal `gemini-3.7-flash` model with schema-constrained output.
6. Download `google-services.json` into `app/google-services.json`. The build enables the Google Services plugin automatically when this file exists.
7. Copy the **Web application** OAuth client ID into `app/src/main/res/values/strings.xml` as `google_oauth_web_client_id`.
8. Configure Firebase App Check before production distribution to protect AI and backend resources from abuse.
9. Use per-user Firestore rules before distributing the app:

```text
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /players/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

The onboarding consent screen clearly separates cloud save from private offline mode. ASCEND uploads a player setup snapshot and compact progress snapshots only after the user chooses Google sign-in. Food photos are sent to Firebase AI Logic only after the player taps Camera or Gallery in Food Vision; ASCEND does not save the image. Nutrient results are estimates based on the visible portion and must be reviewed before logging. Do not weaken the rules above for development convenience in a production project.
