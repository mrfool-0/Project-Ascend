# ASCEND 1.8 — Training and SYSTEM audit

## Scope

Original dark-fantasy performance OS styling, not copied anime artwork. Full Body is a canonical selection covering all seven regions; removing one region switches to explicit priorities. The plan engine uses experience, equipment, objective, split, training days, injury disclosures and session budget. Sessions include upper push/pull, knee/hip work, core, shoulder and arm accessories. Estimates include warm-up and rest; these are constrained templates, not a clinical assessment or a guarantee of an optimal program.

Training opens from Home, Profile or SYSTEM. Upcoming days lead into the session editor; all seven dates remain available. Rebuilding previews prescriptions before saving. Editing forks a template, preserving started sessions and historical sets. New versions update future overrides and duration estimates. Nutrition targets remain unchanged.

## One authoritative weekly map

`TrainingSchedule` resolves recorded session → date override → active base program. Home, Training, session launch, SYSTEM and reminders use this resolver. Swaps are transactional and limited to distinct dates in the remaining Monday–Sunday week. Recorded reps, weight or completed sets block movement; untouched shells may be replaced. Daily walking/mobility habits and next week's base weekdays do not change.

Schema migrations 4→5 add versioned templates, time budget, overrides and proposals; 5→6 adds actual quest cadence and recovers cadence from legacy custom-quest descriptions. Parent records use upsert where replacement would damage child history. Weekly training rewards count real non-recovery templates, including archived versions, instead of hardcoded IDs.

## SYSTEM

Firebase AI Logic / Gemini 3.7 Flash generates contextual replies and schema-constrained action proposals. Low thinking level reduces latency; requests retain a 30-second timeout and a visibly labeled on-device fallback. Live-model availability is not guaranteed. Explicit motivational requests can receive original 80–160-word responses; ordinary answers remain short. Ally, Command and Ruthless change tone, not user control or health boundaries. Pain, illness, injury and distress are not excuses to be overridden; recovery is part of the protocol.

The model sees calibrated metrics, recent messages/behavior, active prescriptions with IDs, habits and the dated week map. Model output never writes directly. A validated proposal is persisted, previewed and applied only after explicit confirmation. It expires after 15 minutes, detects intervening state changes and cannot execute twice. The local parser handles clear add-habit/quest and today/tomorrow swap requests; it is not a substitute for an LLM. Whole-program changes use the calibration editor.

## Reminders and authentication

Evening reminders inspect actual scheduled unfinished quests and habits, including user-created named objectives. Weekday/three-times-weekly quests are excluded on off-days. Repeated delivery is suppressed per category/date; lock-screen public content hides details. Android 13+ notification permission is respected, with pre-13 delivery supported. WorkManager is approximate, not an exact alarm or a guarantee of delivery after force-stop.

Google setup reads the generated web OAuth client from the user's ignored Firebase JSON. The resource is retained during shrinking. Profile supports post-onboarding linking and sync retries. Cloud writes are awaited before success is shown. This remains a compact metrics snapshot, not full database backup/restore. Signing fingerprints, provider enablement, owner-only deployed Firestore rules and App Check are external prerequisites; see [Firebase setup](../FIREBASE_SETUP.md).

## Verification (12 September 2026)

- 56 local unit tests cover Full Body selection, all-equipment movement coverage, time budgets, week boundaries, conservative habit quantity edits and existing domain regressions.
- 14 Android instrumented tests pass on API 26: atomic session writes, recorded-work swap rejection, untouched-shell cleanup, week isolation, versioned history preservation, duration recalculation, nutrition preservation on rebuild, confirmation/cancel/stale/duplicate handling, scheduled reminder selection and a schema-4 migration fixture retaining profile and workout data.
- Debug build and lint pass: zero errors, four dependency-update notices. Pinned dependencies were not indiscriminately upgraded.
- Emulator upgrade retained the fictional NOVA profile. Manual verification covered plan regeneration, editable sets, a confirmed SYSTEM-created reading habit and a confirmed rest/training swap reflected in Home's weekly map.
- A live AI motivational response referenced the player's name, personal motivation and actual recovery day. A live AI habit update correctly selected the existing reading habit and proposed 15 pages; confirmation updated the visible habit. Some requests returned server errors and used the fallback; its contradictory action wording was corrected. Both transports remain visibly distinguishable.

## Remaining release checks

The test device reports that the Firebase App Check API is disabled for the configured project. Enable/register it and verify valid debug/release requests before enabling enforcement. The Firebase JSON alone does not complete backend setup.

The API-26 test emulator has no available Google credential provider, so it cannot complete an account sign-in. The app now reports that prerequisite clearly and retains local progress. This is not a verified successful Google login.

Google sign-in needs successful account-enabled physical-device verification after SHA registration. Notification delivery under manufacturer battery restrictions, Android 13+ permission UX and larger accessibility fonts need physical-device coverage. Full cloud restore, chat/photo backup, medically individualized programming and verified exercise demonstration assets remain outside this build. Workout animations remain removed; the finite quest-entry glitch and reduced-motion support remain.

## Primary references

- [Firebase structured output](https://firebase.google.com/docs/ai-logic/generate-structured-output?platform=android): typed response contracts.
- [Firebase thinking configuration](https://firebase.google.com/docs/ai-logic/thinking): low-level thinking configuration for latency-sensitive chat.
- [Firebase Android Google sign-in](https://firebase.google.com/docs/auth/android/google-signin): generated OAuth client and Android configuration.
- [Android persistent work](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work): inexact recurring work and OS constraints.
- [ACSM 2026 resistance-training summary](https://www.acsm.org/wp-content/uploads/2026/03/Resistance-Training-Position-Stand-infographic.pdf): regular major-muscle-group training and gradual progression. The app's particular templates are engineering choices, not an ACSM-endorsed prescription.
