import "./index.css";
import { Composition, Folder } from "remotion";
import {
  TacticalExercise,
  tacticalExerciseSchema,
} from "./TacticalExercise";

export const RemotionRoot: React.FC = () => {
  return (
    <Folder name="Exercise-Loops">
      <Composition
        id="TacticalExercise"
        component={TacticalExercise}
        durationInFrames={120}
        fps={30}
        width={1080}
        height={1080}
        schema={tacticalExerciseSchema}
        defaultProps={{
          exerciseName: "Bodyweight Squat",
          movement: "squat",
          modelPath: "",
          animationName: "",
          accentColor: "#43DCF7",
          secondaryColor: "#8876FF",
        }}
      />
    </Folder>
  );
};
