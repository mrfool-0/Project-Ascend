# ASCEND 1.7 — interface and interaction audit

Reviewed 8 September 2026. This is a native Android/Compose refinement, not a web port or a production-security certification.

## Delivered

- A finite 1.16-second quest-entry glitch: two short chromatic text-displacement bursts, a single scan-line pass, hooded SYSTEM identity and a stable background. No repeating strobe or exercise-demo animations.
- Explicit variable-font weight axes, clearer secondary text, quieter card gradients, spring press feedback, staggered entry reveals and fade-through navigation.
- Consumed Scaffold insets, one IME-padding owner and a keyboard-aware bottom bar. SYSTEM's typewriter reserves the reply's full layout to avoid jumping lines.
- Modern centered profile identity, transparent default avatar, 48 dp photo-edit touch target, equal-height metric tiles and readable account details. Progress categories wrap instead of hiding offscreen.
- Onboarding scroll resets, staggered initialization, reduced-motion paths and truthful report graphs showing estimated scheduled session times instead of invented readiness scores. Recovery is excluded from the scheduled-session total.
- Session/set creation in one Room transaction, duplicate-launch guards, shared input validation, explicit unlocking of completed sets, exercise-removal confirmation and a cancelable rest timer.
- Explicit SYSTEM request state, duplicate-send protection and clear-chat confirmation. A failed/interrupted reply no longer leaves the composer permanently disabled by an unmatched player message.
- Unconfigured Google save is disabled with a clear explanation; offline creation remains available.

## Research applied

Primary-source research favored the existing native stack; no new runtime animation library or paid asset dependency was needed.

- [Android: variable fonts](https://developer.android.com/develop/ui/compose/text/fonts) — use explicit `FontVariation.weight` for bundled font files.
- [Android: Material insets](https://developer.android.com/develop/ui/compose/system/material-insets) and [inset consumption](https://developer.android.com/develop/ui/compose/system/insets-ui) — apply and consume Scaffold padding before handling the keyboard.
- [Androidify's Compose motion design](https://android-developers.googleblog.com/2025/05/androidify-building-delightful-ui-with-compose.html) — use a consistent spring vocabulary, with deliberately restrained travel and overshoot here.
- [Compose animation guide](https://developer.android.com/develop/ui/compose/animation/quick-guide) — prefer drawing-layer transforms for decorative movement.
- [MotionDurationScale](https://developer.android.com/reference/kotlin/androidx/compose/ui/MotionDurationScale) — honor Android's animation setting; custom delay-based effects also have an immediate path.
- [W3C: interaction animation](https://www.w3.org/WAI/WCAG22/Understanding/animation-from-interactions.html) and [flash thresholds](https://www.w3.org/WAI/WCAG22/Understanding/three-flashes-or-below-threshold.html) — conservative guidance for optional motion and avoiding strobing, not a claim of WCAG certification for this native app.

The visual direction is an engineering/design judgment, informed by these sources and emulator inspection. It is not a measured claim of universal usability or physical-device frame performance.

## Verification

- 51 unit tests: zero failures, errors or skips.
- 5 instrumented tests on Android 8.0/API 26: completed-set locking/unlocking; delete confirmation/cancel; immediate reduced-motion entry; Room rollback on invalid set insertion; successful atomic session creation.
- `lintDebug`: no issues found.
- Debug APK, test APK and minified unsigned release APK build successfully.
- Manual emulator review: player creation/Full Body, targets, player report, initialization, home, quest entry, strength/recovery logging, nutrition, habits, profile, progress, quest ledger and SYSTEM.
- SYSTEM's test query returned the fictional player's actual 150 g protein target; the input stayed above the open keyboard. This is a sampled live response, not comprehensive AI correctness testing.
- Profile, home, quest ledger and workout layouts checked at 130% font scale. Reduced-motion setting checked; temporary emulator settings restored.

Reproduce local checks with a configured Android SDK and JDK:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.ascend.app.test/androidx.test.runner.AndroidJUnitRunner
```

Use a dedicated test device. Manual instrument invocation was used to avoid the connected-test runner's install/cleanup behavior on an already-installed app; the instrumented database fixtures themselves are in-memory.

## Remaining boundaries

Google OAuth web-client configuration and an end-to-end cloud-save test are still required. Release signing/distribution, newer Android devices, landscape/tablet layouts, complete TalkBack coverage and physical-device performance measurements are not verified here. Nutrition and coaching remain estimates/planning assistance, not medical assessment. Set drafts persist through recomposition, but only explicitly completed/unlocked sets are committed through the workout editor; this is not an autosave guarantee for abandoned drafts.
