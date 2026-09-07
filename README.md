# Project ASCEND

**TURN YOUR LIFE INTO A QUEST**

ASCEND is an offline-first native Android health, training, nutrition, habit, and progression tracker. Its original **ASCEND SYSTEM UI** uses a restrained dark-fantasy RPG language—player status, protocols, quests, ranks, and progression—while keeping health data legible and grounded.

## What is implemented

- Sixteen-stage game-style player creation with safe-area-aware spacing, selectable Full Body or targeted focus areas, injury screening, training frequency, exact workout weekdays, mindset calibration, animated graphs, a generated report, and a staggered tactical initialization sequence
- Mifflin–St Jeor calorie estimation with transparent maintenance calculation; bounded calorie, macro, and water inputs; smart milliliter/liter formatting; and live macro-energy feedback
- Level 1–100 progression, eleven original ranks, a durable XP ledger, duplicate-source protection, streaks and achievements
- A custom 2–6 day program generated from focus, experience, equipment, schedule, reported limitations, and the selected Full Body, Push/Pull/Legs, Upper/Lower, or Automatic architecture; frequency-aware working sets and rotating A/B/C movement patterns keep weekly volume balanced instead of repeating the same session
- Clean, animation-free workout entry and logging for every set, weight and rep; a background-accurate rest timer; completion rewards; progression suggestions; and PR detection
- A searchable offline catalog of global everyday foods—including common grains, fruit, vegetables, dairy, pulses, beef, poultry, seafood, and prepared meals—plus manual custom-food creation, meal logging, scaled servings, saved meals, and hydration
- Checkbox, number, duration and avoidance habit models with configurable difficulty and a 75 XP daily anti-farming cap
- Body-weight logging, trend-focused charting, 7-day averages, consistency heatmap, training/nutrition summaries and ASCEND game scores
- Daily, weekly, and player-created quest ledger; rank badges; level-up/quest/PR presentation; configurable WorkManager reminders; and settings. Quest and Progress are available from the player profile while the bottom bar stays focused on Home, Nutrition, Habits, and SYSTEM.
- ASCEND SYSTEM, a tactical Firebase AI Logic performance OS with a capable on-device fallback and a custom hooded interface identity. It understands misspellings, natural fitness questions, exercise-form requests, compound requests, and follow-ups; reads the exact current sets/reps plus calibrated BMR, macros, hydration, weekly split, streaks, live totals, and recent behavior; offers Ally/Command/Ruthless voice modes; preserves local emergency safeguards; and renders concise robotic typewriter responses.
- Action-capable SYSTEM commands: explicit requests can create scheduled habits or custom daily quests, with bounded XP, duplicate protection, and tap-to-complete/revert ledger behavior. Coaching adapts to logged consistency using transparent motivational interviewing and implementation intentions; covert or coercive manipulation is explicitly forbidden.
- Room persistence and Preferences DataStore; no analytics, ads, or mandatory network access
- A local player profile photo picker that stores the selected image privately and decodes it efficiently for display
- Optional Credential Manager Google sign-in with Firebase Authentication/Firestore progress snapshots; strict owner-only Firestore rules, verified identities, server timestamps, and build-appropriate Firebase App Check providers protect the backend while private offline mode remains fully supported
- Optional Health Connect boundary in settings. V1 deliberately does not request health permissions or include a Health Connect runtime dependency.

## Technology

- Kotlin 2.3.21
- Jetpack Compose + Material 3 (Compose BOM 2026.08.00)
- Manrope and JetBrains Mono variable typography with bundled OFL license notices
- Coil Compose 3.6.2 for asynchronous, downsampled profile-image loading
- Android Gradle Plugin 9.4.0 / Gradle 9.7.1
- Navigation Compose 2.10.0
- Room 2.8.4 with KSP 2.3.9
- DataStore 1.2.1
- Coroutines and StateFlow
- WorkManager 2.11.2
- MVVM with a repository boundary and testable domain engines

The project targets Android API 37 and supports Android 8.0/API 26 and newer. Dependency versions are pinned and there are no dynamic versions.

Third-party attribution is documented in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) and is also summarized in the app's Profile → Account & privacy section. Bundled font license texts remain available inside the APK under `assets/licenses/`.

## Build and run

1. Open this directory in a current stable Android Studio that supports AGP 9.4.
2. Install Android SDK Platform 37 and Build Tools 37.x through SDK Manager.
3. Use JDK 17 or newer (JDK 21 is tested).
4. Sync Gradle, select the `app` run configuration, and run on an API 26+ emulator or device.

Command line:

```bash
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

```text
com.ascend.app/
├── core/
│   ├── database/       Room entities, DAO, database
│   ├── datastore/      Preferences and feature settings
│   └── notifications/  WorkManager reminder scheduling
├── domain/             Nutrition, level, rank, quest, streak, PR and trend engines
├── ui/
│   ├── components/     Premium UI primitives and avatars
│   ├── screens/        Onboarding and feature screens
│   └── theme/          Obsidian palette, shape system and typography
├── AscendRepository    Offline data orchestration and XP transaction rules
├── AscendViewModel     Stable reactive screen state and user actions
└── MainActivity        Compose navigation and system-level presentation
```

Room owns structured health/progression records. DataStore owns lightweight preferences that are not relational. UI code observes repository flows through the ViewModel; composables never access the database directly.

## Database

The schema includes user profile and nutrition target, foods/logs/saved meals, weight and body measurements, exercises/templates/sessions/sets, habits/completions, quests/completions, XP transactions, achievements/unlocks, and daily summaries.

Daily records use a local calendar date string (`YYYY-MM-DD`) in addition to an event timestamp. This preserves the day the user intended even if their timezone changes later.

XP events use a unique `(sourceType, sourceId, sourceDate)` index. Inserting the same reward again is ignored. Reverting a recurring action removes its exact ledger row, so check → uncheck → check restores one reward instead of farming XP. Level is always derived from lifetime ledger XP; it is never stored separately.

## Gamification model

The XP needed to leave level `L` is centralized in `LevelEngine`:

```text
100 + round(10 × L^1.2)
```

Levels stop at 100. Ranks are Recruit, Striker, Vanguard, Elite, Champion, Ascendant, Mythic, Paragon, Apex, Sovereign, and Transcendent. Habit XP is capped at 75 per local day. Quest rewards are granted once per source/day. Lifetime XP is never removed because a streak ends.

ASCEND scores (Strength, Endurance, Nutrition, Discipline, Recovery and Consistency) are explicitly game metrics derived from logged activity, not medical or physiological measurements.

## Nutrition model

ASCEND estimates BMR with Mifflin–St Jeor, applies the selected activity multiplier, then applies a moderate objective adjustment. Protein begins near `2.0 g/kg`, fat near `0.8 g/kg`, and remaining calories become carbohydrate at 4/4/9 kcal per gram. Water begins near `35 ml/kg`, rounded to 250 ml.

All outputs are labeled as estimates and remain editable. Calorie adherence rewards the configured target band (90–110% by default); eating dramatically below or above the goal does not earn a better score. Protein and hydration produce one bounded daily reward rather than scaling without limit.

## Tests

Local unit tests cover:

- XP thresholds, level transitions and level 100 cap
- rank boundaries
- Mifflin–St Jeor and macro energy coherence
- calorie adherence behavior
- current/longest streak calculation
- daily and weekly quest thresholds
- player-frequency weekly quest thresholds and future-date exclusion
- duplicate XP rejection and reversion
- estimated 1RM and PR detection
- weight trend calculation
- nutrition-target bounds, macro coherence, and liter-to-milliliter conversion
- Full Body and selected-split weekly-map scaling across 3/4/5-day routines
- balanced Full Body movement coverage, workout variation, and frequency-aware volume
- typo-tolerant SYSTEM intent detection, contextual follow-ups, and exercise-form answers
- safe natural-language parsing for habit and custom-quest creation commands

## Privacy and safety

ASCEND is local-first and has no advertising, tracking, analytics, camera-based food analysis, or external food lookup API. Google sign-in/cloud save is optional and remains inert until the publisher supplies a Firebase project and the player consents. When Firebase AI Logic is configured, SYSTEM sends the player's message and compact plan context for natural-language replies; local safety responses and the offline chatbot remain available without it. A chosen player photo stays in private local app storage and is not included in progress sync. SYSTEM is not a clinician. Injury answers conservatively surface medical-clearance guidance where appropriate. Nutrition values are estimates, and packaging labels remain authoritative.

## Roadmap

The repository interfaces leave room for opt-in Health Connect import, a verified online food database, encrypted export, alternative themes, richer body measurements, editable programs and wearable support. Social systems, leaderboards and subscriptions remain outside this release.
