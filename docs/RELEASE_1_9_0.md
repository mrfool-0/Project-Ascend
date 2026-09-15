# ASCEND 1.9.0 — SYSTEM and workout editing

Install `ASCEND-v1.9.0-debug.apk` over the existing app; do not uninstall first. Version code 12 uses the same development certificate as the previous APK. This is a direct-install development build, not a Play Store production release.

## Changes

- “Change all my session sets to 3 sets instead of 2” previews a strength-set change across active training protocols, preserving reps and cardio.
- “Add treadmill to all my existing sessions” asks for minutes, then previews timed workout blocks rather than creating a habit.
- Timed cardio cards, editing and completed-duration logging use minutes instead of weights/reps.
- Greetings receive greeting responses. Ambiguous scope and multiple edits ask for clarification.
- Remove habits using the trash icon or an explicit named SYSTEM request. Removal requires confirmation and preserves past completions and earned XP.
- Confirmed plan edits preserve started/recorded sessions and recovery days. Existing exact-name treadmill blocks are not duplicated.
- Online AI connection failures are shown explicitly; supported direct edits remain available offline.

## Verification and limitations

Build, lint and 65 JVM tests passed. A live Firebase/Gemini encouragement probe passed on retry (6.27 seconds); the first attempt returned a server error. Online availability is not guaranteed. The test emulator has no signed-in Google user and its App Check debug attestation remains unregistered; this is not a physical-phone Google sign-in certification.

All 30 regular Android tests passed; the optional live test was skipped in that regular run and verified separately. Habit-removal and timed-cardio UI screenshots were reviewed. APK SHA-256: `3e7943813af7b4a48a516051ab2013166c5468c90423ba4e4d00a23e6d3d0a0a`.

Schema 7 migration preserves existing records. Updates apply to future unstarted sessions, not already-started sessions. A current session can therefore keep its previous prescription intentionally.

GitHub publication is pending the user's saved github.com browser restriction being lifted. No cloud security protections were disabled for this build.
