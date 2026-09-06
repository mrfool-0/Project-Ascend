import { OrbitControls, useAnimations, useGLTF } from "@react-three/drei";
import { Canvas } from "@react-three/fiber";
import { Suspense, useEffect } from "react";

type PreviewProps = {
  readonly glbPath: string;
  readonly animationName?: string;
  readonly className?: string;
};

const PreviewModel: React.FC<Pick<PreviewProps, "glbPath" | "animationName">> = ({ glbPath, animationName }) => {
  const model = useGLTF(glbPath);
  const { actions, names } = useAnimations(model.animations, model.scene);

  useEffect(() => {
    const selected = actions[animationName && names.includes(animationName) ? animationName : names[0]];
    selected?.reset().fadeIn(0.2).play();
    return () => {
      selected?.fadeOut(0.15);
    };
  }, [actions, animationName, names]);

  return <primitive object={model.scene} position={[0, -1.7, 0]} />;
};

/** Web-only interactive fallback. Never import this component into a Remotion composition. */
export const InteractiveExercisePreview: React.FC<PreviewProps> = ({ glbPath, animationName, className }) => (
  <div className={className} style={{ width: "100%", aspectRatio: "1 / 1", background: "#05070D" }}>
    <Canvas camera={{ position: [0, 1.2, 5.5], fov: 34 }}>
      <ambientLight intensity={0.8} />
      <directionalLight position={[4, 6, 5]} intensity={2} color="#43DCF7" />
      <Suspense fallback={null}>
        <PreviewModel glbPath={glbPath} animationName={animationName} />
      </Suspense>
      <OrbitControls enablePan={false} minDistance={3.5} maxDistance={8} />
    </Canvas>
  </div>
);
