# Project ASCEND

**TURN YOUR LIFE INTO A QUEST**

ASCEND is an offline-first native Android health, training, nutrition, habit, and progression tracker. Its original **ASCEND SYSTEM UI** uses a restrained dark-fantasy RPG language—player status, protocols, quests, ranks, and progression—while keeping health data legible and grounded.

## What is implemented

- Sixteen-stage game-style player creation with focus areas, injury screening, training frequency, exact workout weekdays, mindset calibration, animated graphs, a generated report and typewriter initialization
- Mifflin–St Jeor calorie estimation with transparent maintenance calculation and editable calorie, macro and water targets
- Level 1–100 progression, eleven original ranks, a durable XP ledger, duplicate-source protection, streaks and achievements
- A custom 2–6 day program generated from focus, experience, equipment, schedule and reported limitations; other days automatically use the recovery protocol
- Workout session logging for every set, weight and rep; rest timer; completion rewards; progression suggestions and PR detection
- A searchable offline catalog of everyday foods, meal logging, scaled servings, saved meals and hydration, plus an opt-in Firebase AI Logic photo scanner that identifies a visible meal and estimates calories and nutrients for review before saving
- Checkbox, number, duration and avoidance habit models with configurable difficulty and a 75 XP daily anti-farming cap
- Body-weight logging, trend-focused charting, 7-day averages, consistency heatmap, training/nutrition summaries and ASCEND game scores
- Daily and weekly quest ledger, rank badges, level-up/quest/PR presentation, configurable WorkManager reminders, and settings
- ASCEND SYSTEM, a context-aware Firebase AI Logic chatbot with a capable on-device fallback; it understands natural fitness questions and follow-ups, reads the player's plan and live daily totals, offers Ally/Command/Ruthless voice modes, preserves local emergency safeguards, and renders robotic typewriter responses
- Room persistence and Preferences DataStore; no analytics, ads, or mandatory network access
- Optional Credential Manager Google sign-in with Firebase Authentication/Firestore progress snapshots; private offline mode remains fully supported
- Optional Health Connect boundary in settings. V1 deliberately does not request health permissions or include a Health Connect runtime dependency.

## Technology

- Kotlin 2.3.21
- Jetpack Compose + Material 3 (Compose BOM 2026.08.00)
- Android Gradle Plugin 9.4.0 / Gradle 9.7.1
- Navigation Compose 2.10.0
- Room 2.8.4 with KSP 2.3.9
- DataStore 1.2.1
- Coroutines and StateFlow
- WorkManager 2.11.2
- MVVM with a repository boundary and testable domain engines

The project targets Android API 37 and supports Android 8.0/API 26 and newer. Dependency versions are pinned and there are no dynamic versions.

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
│   ├── components/     ASCEND SYSTEM UI primitives
│   ├── screens/        Onboarding and feature screens
│   └── theme/          VOID palette and typography
├── AscendRepository    Offline data orchestration and XP transaction rules
├── AscendViewModel     Stable reactive screen state and user actions
└── MainActivity        Compose navigation and system-level presentation
```

Room owns structured health/progression records. DataStore owns lightweight preferences that are not relational. UI code observes repository flows through the ViewModel; composables never access the database directly. Repositories form the seam for future sync or food-provider adapters.

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
- duplicate XP rejection and reversion
- estimated 1RM and PR detection
- weight trend calculation

## Privacy and safety

ASCEND is local-first and has no advertising, tracking, analytics, or external food lookup API. Google sign-in/cloud save is optional and remains inert until the publisher supplies a Firebase project and the player consents. When Firebase AI Logic is configured, SYSTEM sends the player's message and compact plan context for natural-language replies, and Food Vision sends only the photo the player explicitly chooses; local safety responses and the offline chatbot remain available without it. SYSTEM is not a clinician. Injury answers conservatively surface medical-clearance guidance where appropriate. Nutrition and photo-analysis values are estimates, and packaging labels remain authoritative.

## Roadmap

The repository interfaces leave room for opt-in Health Connect import, a verified online food database, encrypted export, alternative themes, richer body measurements, editable programs and wearable support. Social systems, leaderboards and subscriptions remain outside this release.
