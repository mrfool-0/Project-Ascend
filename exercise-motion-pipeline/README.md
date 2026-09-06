# ASCEND Exercise Motion Pipeline

This isolated render-time package keeps Three.js and Remotion out of the Android runtime. It produces deterministic, looping MP4/WebM exercise demonstrations that can be copied into the mobile app as packaged assets.

## Architecture

- `TacticalExercise.tsx` is the parameterized Remotion composition.
- `three/RiggedHumanoid.tsx` accepts any publisher-owned `.glb` path under `public/`, clones the rig, and drives Mixamo-style bone names from the Remotion frame clock. No externally owned model is bundled.
- `three/ProceduralHumanoid.tsx` is a deterministic zero-asset fallback used by the sample composition.
- `web/InteractiveExercisePreview.tsx` is a web-only `@react-three/fiber` + `@react-three/drei` preview wrapper. It accepts a `.glb` URL and optional embedded animation name at runtime. It must not be imported into Remotion renders.

## Standard asset contract

Place licensed or original models under `public/models/`. Prefer a humanoid hierarchy using Mixamo bone names such as `mixamorigHips`, `mixamorigSpine`, `mixamorigLeftUpLeg`, and `mixamorigRightArm`. Pass the path relative to `public`, for example `models/squat.glb`.

The renderer never downloads or hardcodes third-party models. Product teams remain responsible for model, motion, music, and texture licensing.

## Commands

```bash
npm install
npm run lint
npm run dev
npm run render:mp4
npm run render:webm
```

Edit or duplicate `examples/squat.json` to create a render job. The provided sample uses the procedural fallback and therefore requires no external asset.

For Android delivery, copy approved output to `app/src/main/res/raw/exercise_<exercise-slug>.mp4`. ASCEND resolves packaged motion by exercise pattern and retains its lightweight procedural cue when no pre-baked file exists.
