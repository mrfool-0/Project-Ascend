import { useGLTF } from "@react-three/drei";
import { useLayoutEffect, useMemo } from "react";
import { clone } from "three/examples/jsm/utils/SkeletonUtils.js";
import type { Bone, Object3D } from "three";
import { ProceduralHumanoid } from "./ProceduralHumanoid";

type Movement = "squat" | "push" | "pull" | "hinge" | "lunge" | "curl" | "press" | "neutral";

type Props = {
  readonly modelUrl: string | null;
  readonly animationName: string;
  readonly movement: Movement;
  readonly progress: number;
  readonly accentColor: string;
};

const bone = (root: Object3D, name: string): Bone | undefined => {
  let match: Bone | undefined;
  root.traverse((child) => {
    if (!match && child.type === "Bone" && (child.name === name || child.name.endsWith(name))) {
      match = child as Bone;
    }
  });
  return match;
};

const poseMixamoRig = (root: Object3D, movement: Movement, progress: number) => {
  const phase = 0.5 - Math.cos(progress * Math.PI * 2) * 0.5;
  const wave = Math.sin(progress * Math.PI * 2);
  const hips = bone(root, "mixamorigHips");
  const spine = bone(root, "mixamorigSpine");
  const leftThigh = bone(root, "mixamorigLeftUpLeg");
  const rightThigh = bone(root, "mixamorigRightUpLeg");
  const leftKnee = bone(root, "mixamorigLeftLeg");
  const rightKnee = bone(root, "mixamorigRightLeg");
  const leftArm = bone(root, "mixamorigLeftArm");
  const rightArm = bone(root, "mixamorigRightArm");
  const leftForearm = bone(root, "mixamorigLeftForeArm");
  const rightForearm = bone(root, "mixamorigRightForeArm");

  if (hips) hips.position.y = -phase * (movement === "squat" || movement === "lunge" ? 0.42 : 0.08);
  if (spine) spine.rotation.x = movement === "hinge" ? phase * 0.72 : phase * 0.08;
  if (leftThigh) leftThigh.rotation.x = movement === "squat" ? -phase * 1.05 : movement === "lunge" ? -phase * 0.8 : 0;
  if (rightThigh) rightThigh.rotation.x = movement === "squat" ? -phase * 1.05 : movement === "lunge" ? phase * 0.45 : 0;
  if (leftKnee) leftKnee.rotation.x = movement === "squat" ? phase * 1.5 : movement === "lunge" ? phase * 1.25 : 0;
  if (rightKnee) rightKnee.rotation.x = movement === "squat" ? phase * 1.5 : movement === "lunge" ? phase * 0.7 : 0;
  if (leftArm) leftArm.rotation.z = movement === "press" ? -1.2 + phase * 0.8 : movement === "pull" ? -0.45 - phase * 0.75 : -0.18;
  if (rightArm) rightArm.rotation.z = movement === "press" ? 1.2 - phase * 0.8 : movement === "pull" ? 0.45 + phase * 0.75 : 0.18;
  if (leftForearm) leftForearm.rotation.z = movement === "curl" ? -phase * 1.75 : movement === "push" ? -phase * 1.15 : wave * 0.04;
  if (rightForearm) rightForearm.rotation.z = movement === "curl" ? phase * 1.75 : movement === "push" ? phase * 1.15 : -wave * 0.04;
};

const GlbHumanoid: React.FC<Omit<Props, "modelUrl"> & { readonly modelUrl: string }> = ({
  modelUrl,
  movement,
  progress,
}) => {
  const gltf = useGLTF(modelUrl);
  const rig = useMemo(() => clone(gltf.scene), [gltf.scene]);

  useLayoutEffect(() => {
    poseMixamoRig(rig, movement, progress);
  }, [movement, progress, rig]);

  return <primitive object={rig} position={[0, -1.75, 0]} scale={1.65} />;
};

export const RiggedHumanoid: React.FC<Props> = (props) => {
  if (!props.modelUrl) {
    return <ProceduralHumanoid movement={props.movement} progress={props.progress} accentColor={props.accentColor} />;
  }
  return <GlbHumanoid {...props} modelUrl={props.modelUrl} />;
};
