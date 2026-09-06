import { ThreeCanvas } from "@remotion/three";
import { zColor } from "@remotion/zod-types";
import {
  AbsoluteFill,
  Easing,
  Interactive,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { z } from "zod";
import { RiggedHumanoid } from "./three/RiggedHumanoid";

export const tacticalExerciseSchema = z.object({
  exerciseName: z.string().min(1),
  movement: z.enum(["squat", "push", "pull", "hinge", "lunge", "curl", "press", "neutral"]),
  modelPath: z.string(),
  animationName: z.string(),
  accentColor: zColor(),
  secondaryColor: zColor(),
});

export type TacticalExerciseProps = z.infer<typeof tacticalExerciseSchema>;

export const TacticalExercise: React.FC<TacticalExerciseProps> = ({
  exerciseName,
  movement,
  modelPath,
  animationName,
  accentColor,
  secondaryColor,
}) => {
  const frame = useCurrentFrame();
  const { durationInFrames, height, width } = useVideoConfig();
  const cycle = (frame % durationInFrames) / durationInFrames;

  return (
    <AbsoluteFill
      name="Tactical exercise loop"
      style={{
        backgroundColor: "#05070D",
        overflow: "hidden",
      }}
    >
      <AbsoluteFill
        style={{
          backgroundImage: `radial-gradient(circle at 50% 45%, ${accentColor}24 0%, #8876ff12 33%, transparent 67%)`,
          opacity: interpolate(frame, [0, 30, 90, durationInFrames - 1], [0.55, 1, 1, 0.55], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      />
      <ThreeCanvas
        width={width}
        height={height}
        camera={{ position: [0, 1.25, 5.8], fov: 32 }}
      >
        <ambientLight intensity={0.9} />
        <directionalLight position={[4, 7, 6]} intensity={2.2} color={accentColor} />
        <directionalLight position={[-4, 3, 2]} intensity={1.4} color={secondaryColor} />
        <pointLight position={[0, -1, 2]} intensity={1.2} color={accentColor} />
        <RiggedHumanoid
          modelUrl={modelPath.trim() ? staticFile(modelPath) : null}
          animationName={animationName}
          movement={movement}
          progress={cycle}
          accentColor={accentColor}
        />
        <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -1.82, 0]}>
          <circleGeometry args={[2.25, 64]} />
          <meshStandardMaterial color="#09121A" emissive={accentColor} emissiveIntensity={0.1} />
        </mesh>
      </ThreeCanvas>

      <Interactive.Div
        name="Exercise label"
        style={{
          position: "absolute",
          left: 64,
          right: 64,
          bottom: 58,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-end",
          opacity: interpolate(frame, [0, 18], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        <div>
          <div style={{ color: accentColor, fontFamily: "monospace", fontSize: 20, letterSpacing: 4 }}>
            MOVEMENT PROTOCOL
          </div>
          <div style={{ color: "#F5F7FB", fontFamily: "Arial, sans-serif", fontSize: 48, fontWeight: 700, marginTop: 8 }}>
            {exerciseName.toUpperCase()}
          </div>
        </div>
        <div style={{ color: "#8691A6", fontFamily: "monospace", fontSize: 20, letterSpacing: 2 }}>
          LOOP // {Math.round(cycle * 100).toString().padStart(2, "0")}
        </div>
      </Interactive.Div>
    </AbsoluteFill>
  );
};
