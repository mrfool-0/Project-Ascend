package com.ascend.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class PlannedExercise(
    val name: String,
    val muscleGroup: String,
    val sets: Int,
    val minReps: Int,
    val maxReps: Int,
)

data class PlannedWorkout(
    val name: String,
    val estimatedMinutes: Int,
    val exercises: List<PlannedExercise>,
    val recovery: Boolean = false,
)

data class CustomPlan(
    val title: String,
    val workouts: List<PlannedWorkout>,
    val weeklyDays: List<DayOfWeek>,
    val safetyNotes: List<String>,
)

object CustomPlanEngine {
    fun generate(
        frequency: Int,
        workoutDays: Set<DayOfWeek>,
        focusAreas: Set<FocusArea>,
        injuries: Set<InjuryArea>,
        equipment: Equipment,
        experience: Experience,
    ): CustomPlan {
        require(frequency in 2..6)
        require(workoutDays.size == frequency)
        val base = when (frequency) {
            2 -> listOf("FULL BODY A", "FULL BODY B")
            3 -> listOf("UPPER", "LOWER", "FULL BODY")
            4 -> listOf("UPPER A", "LOWER A", "UPPER B", "LOWER B")
            5 -> listOf("PUSH", "PULL", "LEGS", "UPPER", "LOWER")
            else -> listOf("PUSH A", "PULL A", "LEGS A", "PUSH B", "PULL B", "LEGS B")
        }
        val setCount = when (experience) {
            Experience.BEGINNER -> 3
            Experience.INTERMEDIATE -> 3
            Experience.ADVANCED -> 4
        }
        val sessions = base.mapIndexed { index, name ->
            val exercises = exercisesFor(name, equipment, setCount, index)
                .let { addFocusExercise(it, focusAreas, equipment, setCount) }
                .map { adaptForInjuries(it, injuries, equipment) }
                .distinctBy { it.name }
                .take(if (experience == Experience.BEGINNER) 5 else 6)
            PlannedWorkout(name, 12 + exercises.sumOf { it.sets * 3 }, exercises)
        }
        val safety = buildList {
            if (injuries.any { it != InjuryArea.NONE }) add("Movements were conservatively substituted around reported limitations. Stop if pain appears and get clinical clearance where appropriate.")
            if (InjuryArea.CARDIOVASCULAR in injuries) add("Medical clearance is required before vigorous training; ASCEND cannot assess cardiovascular safety.")
            add("Begin below maximum effort, preserve technique, and progress only after every target rep is controlled.")
        }
        return CustomPlan(
            title = "${frequency}-DAY ${focusAreas.firstOrNull()?.name?.replace('_', ' ') ?: "BALANCED"} PROTOCOL",
            workouts = sessions + PlannedWorkout("RECOVERY PROTOCOL", 20, emptyList(), recovery = true),
            weeklyDays = workoutDays.sortedBy { it.value },
            safetyNotes = safety,
        )
    }

    fun templateIndexFor(date: LocalDate, programStart: LocalDate, scheduledDays: Set<DayOfWeek>): Int {
        val days = scheduledDays.sortedBy { it.value }
        if (days.isEmpty()) return 0
        val position = days.indexOf(date.dayOfWeek)
        if (position == -1) return days.size
        val startWeek = programStart.minusDays((programStart.dayOfWeek.value - 1).toLong())
        val dateWeek = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val week = ChronoUnit.WEEKS.between(startWeek, dateWeek).toInt()
        return Math.floorMod(week * days.size + position, days.size)
    }

    private fun exercisesFor(name: String, equipment: Equipment, sets: Int, variant: Int): List<PlannedExercise> {
        val push = listOf(
            move("Barbell Bench Press", "Push-up", "Dumbbell Floor Press", equipment, "Chest", sets, 6, 12),
            move("Seated Overhead Press", "Pike Push-up", "Dumbbell Shoulder Press", equipment, "Shoulders", sets, 8, 12),
            move("Cable Lateral Raise", "Lateral Raise", "Dumbbell Lateral Raise", equipment, "Shoulders", 3, 12, 20),
            move("Triceps Pushdown", "Diamond Push-up", "Dumbbell Triceps Extension", equipment, "Arms", 3, 10, 15),
        )
        val pull = listOf(
            move("Lat Pulldown", "Inverted Row", "One-arm Dumbbell Row", equipment, "Back", sets, 8, 12),
            move("Chest Supported Row", "Prone Y-T-W", "Dumbbell Row", equipment, "Back", sets, 8, 12),
            move("Face Pull", "Reverse Snow Angel", "Rear Delt Fly", equipment, "Shoulders", 3, 12, 20),
            move("Cable Curl", "Towel Curl Isometric", "Dumbbell Curl", equipment, "Arms", 3, 8, 15),
        )
        val legs = listOf(
            move(if (variant % 2 == 0) "Barbell Squat" else "Leg Press", "Tempo Squat", "Goblet Squat", equipment, "Legs", sets, 6, 12),
            move("Romanian Deadlift", "Single-leg Hip Hinge", "Dumbbell Romanian Deadlift", equipment, "Hamstrings", sets, 8, 12),
            move("Leg Curl", "Sliding Leg Curl", "Dumbbell Leg Curl", equipment, "Hamstrings", 3, 10, 15),
            move("Standing Calf Raise", "Single-leg Calf Raise", "Dumbbell Calf Raise", equipment, "Calves", 3, 12, 20),
        )
        val core = PlannedExercise("Dead Bug", "Core", 3, 8, 12)
        return when {
            name.startsWith("PUSH") -> push + core
            name.startsWith("PULL") -> pull + core
            name.startsWith("LEGS") || name.startsWith("LOWER") -> legs + core
            name.startsWith("UPPER") -> push.take(2) + pull.take(2) + core
            else -> listOf(push[0], pull[0], legs[0], legs[1], core)
        }
    }

    private fun addFocusExercise(
        current: List<PlannedExercise>,
        focus: Set<FocusArea>,
        equipment: Equipment,
        sets: Int,
    ): List<PlannedExercise> {
        val focusMove = when {
            FocusArea.GLUTES in focus -> move("Hip Thrust", "Glute Bridge", "Dumbbell Hip Thrust", equipment, "Glutes", sets, 8, 15)
            FocusArea.CHEST in focus -> move("Chest Fly", "Wide Push-up", "Dumbbell Fly", equipment, "Chest", 3, 10, 15)
            FocusArea.BACK in focus -> move("Machine Row", "Superman Row", "Dumbbell Pullover", equipment, "Back", 3, 10, 15)
            FocusArea.SHOULDERS in focus -> move("Machine Lateral Raise", "Pike Hold", "Dumbbell Lateral Raise", equipment, "Shoulders", 3, 12, 20)
            FocusArea.ARMS in focus -> move("Cable Curl", "Close-grip Push-up", "Hammer Curl", equipment, "Arms", 3, 10, 15)
            FocusArea.CORE in focus -> PlannedExercise("Side Plank", "Core", 3, 20, 40)
            FocusArea.LEGS in focus -> move("Leg Extension", "Reverse Lunge", "Dumbbell Split Squat", equipment, "Legs", 3, 10, 15)
            FocusArea.ENDURANCE in focus -> PlannedExercise("Zone 2 Finisher", "Endurance", 1, 10, 20)
            FocusArea.MOBILITY in focus -> PlannedExercise("Mobility Flow", "Mobility", 1, 8, 12)
            else -> null
        }
        return if (focusMove == null || current.any { it.muscleGroup == focusMove.muscleGroup }) current else current + focusMove
    }

    private fun adaptForInjuries(exercise: PlannedExercise, injuries: Set<InjuryArea>, equipment: Equipment): PlannedExercise = when {
        InjuryArea.KNEE in injuries && exercise.name.contains(Regex("Squat|Lunge|Leg Press")) -> PlannedExercise("Pain-free Glute Bridge", "Glutes", 3, 10, 15)
        InjuryArea.LOWER_BACK in injuries && exercise.name.contains(Regex("Deadlift|Hip Hinge|Barbell Row")) -> PlannedExercise("Supported Hamstring Curl", "Hamstrings", 3, 10, 15)
        InjuryArea.SHOULDER in injuries && exercise.name.contains(Regex("Overhead|Pike|Lateral")) -> PlannedExercise("Pain-free Scapular Control", "Shoulders", 2, 10, 15)
        InjuryArea.WRIST in injuries && exercise.name.contains("Push-up") -> move("Machine Chest Press", "Forearm Plank", "Neutral-grip Floor Press", equipment, "Chest", 3, 8, 12)
        InjuryArea.ANKLE in injuries && exercise.name.contains(Regex("Calf|Lunge")) -> PlannedExercise("Seated Leg Extension", "Legs", 3, 10, 15)
        else -> exercise
    }

    private fun move(gym: String, bodyweight: String, dumbbell: String, equipment: Equipment, group: String, sets: Int, min: Int, max: Int) =
        PlannedExercise(
            when (equipment) {
                Equipment.FULL_GYM, Equipment.HOME_GYM -> gym
                Equipment.DUMBBELLS -> dumbbell
                Equipment.BODYWEIGHT -> bodyweight
            },
            group, sets, min, max,
        )
}
