# SYSTEM 1.9 repair

## Implemented

- Explicit workout edits route ahead of generic coaching and model suggestions. “Change all my session sets to 3 sets instead of 2” previews a program-wide strength-set update.
- “Add treadmill to all my existing sessions” asks for duration; “10 minutes” retains the pending scope and previews timed workout blocks, never a habit. Existing identically named treadmill blocks are not duplicated.
- Bulk changes are transactional, version active non-recovery protocols, preserve recorded sessions and remap future overrides. Timed cardio is excluded from strength-set updates and strength PR calculations.
- Timed exercise cards/editor/logging use minutes. Schema 7 adds duration fields without dropping existing records.
- Habit cards have a confirmed Remove action, including on off-days. SYSTEM also accepts explicit named habit removal. Removal archives the habit while preserving completion history and earned XP; deleting all habits no longer reseeds defaults.
- Whole-message greetings bypass fuzzy workout keywords. Online failures are visible instead of silently presenting limited on-device replies as live AI.
- Compound edits and ambiguous/restricted bulk scope ask for clarification rather than silently changing the entire program.

## Connection and verification status

The earlier live-model probe timed out after 45 seconds; Android logs also reported the Firebase App Check API as disabled. On September 15, the Google Cloud console for `project-ascend0` visibly showed **API Enabled** for `firebaseappcheck.googleapis.com`. No billing upgrade or security-rule reduction was made. API availability alone does not prove the chatbot connection works.

On September 15, the final source rebuilt successfully with `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`. All 65 JVM tests passed. The emulator upgrade install succeeded without uninstalling the app. The APK's SHA-1 and SHA-256 match the previous development signing certificate.

The full device run completed with 30 regular tests passing and one opt-in live test skipped (runner summary: 31 tests). The live test was run separately as described below. Habit-removal and timed-cardio screenshots were visually reviewed. The packaged APK matches the build byte-for-byte; SHA-256: `3e7943813af7b4a48a516051ab2013166c5468c90423ba4e4d00a23e6d3d0a0a`.

The first live probe returned a model `ServerException`; the retry passed in 6.27 seconds, returning nonempty encouragement with no mutation proposal. This is a live Firebase/Gemini integration check with a fictitious profile, not a guarantee of uptime or physical-device authentication. The emulator still logs a rejected App Check debug attestation; production app attestation remains separate setup work. No enforcement setting was weakened to make the test pass.

GitHub publication is blocked by a saved browser permission denying access to github.com. The local APK can be handed off independently; do not claim a GitHub release exists until publication is allowed and verified.

## Reproduction commands

1. Build with the bundled JDK: `JAVA_HOME="$PWD/work/toolchains/jdk/Contents/Home" ./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --console=plain`.
2. Upgrade-install both APKs with `adb install -r`; preserve app data. Run instrumentation directly, not a task that uninstalls the app afterward.
3. Run `adb shell am instrument -w -e class com.ascend.app.SystemAiLiveTest -e liveAi true com.ascend.app.test/androidx.test.runner.AndroidJUnitRunner`. It uses a fictitious profile and does not write player chats or plans. Investigate the returned connection error if unsuccessful; do not bypass App Check or authentication.
4. Inspect habit-removal and treadmill-minutes fixture screenshots, verify the signing certificate, then package the matching 1.9 APK and commit/push the source to the existing repository.

Live AI remains probabilistic and availability-dependent. Direct edits cover explicit supported commands; they are not a replacement for a verified language-model connection. All proposed SYSTEM mutations still require confirmation.
