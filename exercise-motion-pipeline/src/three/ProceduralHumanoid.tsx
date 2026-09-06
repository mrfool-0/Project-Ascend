type Props = {
  readonly movement: "squat" | "push" | "pull" | "hinge" | "lunge" | "curl" | "press" | "neutral";
  readonly progress: number;
  readonly accentColor: string;
};

const Limb: React.FC<{
  readonly position: [number, number, number];
  readonly rotation: [number, number, number];
  readonly length: number;
  readonly color: string;
}> = ({ position, rotation, length, color }) => (
  <mesh position={position} rotation={rotation}>
    <capsuleGeometry args={[0.115, length, 8, 16]} />
    <meshStandardMaterial color="#D9E0EA" emissive={color} emissiveIntensity={0.13} roughness={0.52} />
  </mesh>
);

export const ProceduralHumanoid: React.FC<Props> = ({ movement, progress, accentColor }) => {
  const phase = 0.5 - Math.cos(progress * Math.PI * 2) * 0.5;
  const lowered = movement === "squat" ? phase * 0.62 : movement === "lunge" ? phase * 0.35 : 0;
  const hinge = movement === "hinge" ? phase * 0.62 : 0;
  const press = movement === "press" ? phase : 0;
  const curl = movement === "curl" ? phase : 0;
  const armSwing = movement === "pull" || movement === "push" ? phase * 0.82 : 0;
  const thigh = movement === "squat" ? phase * 0.72 : movement === "lunge" ? phase * 0.58 : 0.03;
  const knee = movement === "squat" ? phase * 1.06 : movement === "lunge" ? phase * 0.82 : 0;

  return (
    <group position={[0, -lowered, 0]} rotation={[hinge, 0, 0]}>
      <mesh position={[0, 1.28, 0]}>
        <sphereGeometry args={[0.24, 32, 32]} />
        <meshStandardMaterial color="#EAF0F7" emissive={accentColor} emissiveIntensity={0.09} roughness={0.5} />
      </mesh>
      <mesh position={[0, 0.66, 0]}>
        <capsuleGeometry args={[0.27, 0.76, 12, 24]} />
        <meshStandardMaterial color="#111A28" emissive={accentColor} emissiveIntensity={0.48} roughness={0.35} />
      </mesh>
      <Limb position={[-0.43, 0.78 + press * 0.38, 0]} rotation={[0, 0, 0.28 + press * 1.0 - armSwing]} length={0.62} color={accentColor} />
      <Limb position={[0.43, 0.78 + press * 0.38, 0]} rotation={[0, 0, -0.28 - press * 1.0 + armSwing]} length={0.62} color={accentColor} />
      <Limb position={[-0.55, 0.24 + press * 0.62 + curl * 0.18, 0]} rotation={[0, 0, curl * 1.18 - armSwing * 0.6]} length={0.54} color={accentColor} />
      <Limb position={[0.55, 0.24 + press * 0.62 + curl * 0.18, 0]} rotation={[0, 0, -curl * 1.18 + armSwing * 0.6]} length={0.54} color={accentColor} />
      <Limb position={[-0.22, -0.18, 0]} rotation={[thigh, 0, 0.05]} length={0.75} color={accentColor} />
      <Limb position={[0.22, -0.18, 0]} rotation={[thigh, 0, -0.05]} length={0.75} color={accentColor} />
      <Limb position={[-0.23, -0.98 + lowered * 0.5, 0.1]} rotation={[-knee, 0, 0]} length={0.76} color={accentColor} />
      <Limb position={[0.23, -0.98 + lowered * 0.5, 0.1]} rotation={[-knee, 0, 0]} length={0.76} color={accentColor} />
    </group>
  );
};
