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
        objective: Objective = Objective.GENERAL_HEALTH,
        trainingSplit: TrainingSplit = TrainingSplit.AUTO,
        sessionMinutes: Int = 45,
    ): CustomPlan {
        require(frequency in 2..6)
        require(workoutDays.size == frequency)
        require(sessionMinutes in 30..90)
        val normalizedFocus = FocusRules.normalize(focusAreas)
        val effectiveSplit = if (trainingSplit == TrainingSplit.AUTO && FocusArea.FULL_BODY in normalizedFocus) {
            TrainingSplit.FULL_BODY
        } else {
            trainingSplit
        }
        val base = weeklyArchitecture(frequency, effectiveSplit)
        val setCount = prescribedWorkingSets(frequency, experience, objective)
        val exerciseLimit = when {
            effectiveSplit == TrainingSplit.FULL_BODY -> 7
            experience == Experience.BEGINNER || objective == Objective.BUILD_CONSISTENCY -> 4
            else -> 6
        }
        val sessions = base.mapIndexed { index, name ->
            if (InjuryArea.CARDIOVASCULAR in injuries) {
                return@mapIndexed PlannedWorkout("CLEARANCE / RECOVERY", 20, emptyList(), recovery = true)
            }
            val exercises = exercisesFor(name, equipment, setCount, index)
                .let { addFocusExercise(it, normalizedFocus, equipment, setCount, index, exerciseLimit) }
                .map { adaptForInjuries(it, injuries, equipment) }
                .map { exercise ->
                    if (objective == Objective.BUILD_CONSISTENCY) {
                        exercise.copy(sets = minOf(exercise.sets, setCount))
                    } else {
                        exercise
                    }
                }
                .distinctBy { it.name }
                .take(exerciseLimit)
            val affordableSets = ((sessionMinutes - 12) / (exercises.size.coerceAtLeast(1) * 2)).coerceAtLeast(1)
            val timed = exercises.map { it.copy(sets = minOf(it.sets, affordableSets)) }
            PlannedWorkout(name, 12 + timed.sumOf { it.sets * 2 }, timed)
        }
        val safety = buildList {
            if (injuries.any { it != InjuryArea.NONE }) add("Movements were conservatively substituted around reported limitations. Stop if pain appears and get clinical clearance where appropriate.")
            if (InjuryArea.CARDIOVASCULAR in injuries) add("Medical clearance is required before vigorous training; ASCEND cannot assess cardiovascular safety.")
            add("Begin below maximum effort, preserve technique, and progress only after every target rep is controlled across two sessions.")
            if (frequency >= 5) add("High-frequency programming uses lower per-session set counts so weekly work remains recoverable.")
            if (effectiveSplit == TrainingSplit.FULL_BODY && workoutDays.any { day -> day.plus(1) in workoutDays }) add("Your selection includes consecutive full-body days. Consider spacing them out; use the weekly map to protect recovery.")
            if (equipment == Equipment.BODYWEIGHT) add("No equipment selected: prone back work is included, but it is not equivalent to loaded rows or pulldowns.")
            add("Session estimates include warm-up and rest. Full Body covers major muscle groups through compound movements and accessories.")
        }
        return CustomPlan(
            title = "${frequency}-DAY ${effectiveSplit.displayName} ${objective.name.replace('_', ' ')} PROTOCOL",
            workouts = sessions + PlannedWorkout("RECOVERY PROTOCOL", 20, emptyList(), recovery = true),
            weeklyDays = workoutDays.sortedBy { it.value },
            safetyNotes = safety,
        )
    }

    private fun prescribedWorkingSets(frequency: Int, experience: Experience, objective: Objective): Int = when {
        objective == Objective.BUILD_CONSISTENCY -> 2
        frequency >= 5 -> 2
        experience == Experience.BEGINNER -> 2
        else -> 3
    }

    fun weeklyArchitecture(frequency: Int, trainingSplit: TrainingSplit): List<String> {
        require(frequency in 2..6)
        return when (trainingSplit) {
            TrainingSplit.FULL_BODY -> (0 until frequency).map { "FULL BODY ${('A'.code + it).toChar()}" }
            TrainingSplit.UPPER_LOWER -> (0 until frequency).map { index ->
                val cycle = index / 2
                "${if (index % 2 == 0) "UPPER" else "LOWER"} ${('A'.code + cycle).toChar()}"
            }
            TrainingSplit.PUSH_PULL_LEGS -> when (frequency) {
                2 -> listOf("FULL BODY A", "FULL BODY B")
                3 -> listOf("PUSH", "PULL", "LEGS")
                4 -> listOf("PUSH", "PULL", "LEGS", "FULL BODY")
                5 -> listOf("PUSH", "PULL", "LEGS", "UPPER", "LOWER")
                else -> listOf("PUSH A", "PULL A", "LEGS A", "PUSH B", "PULL B", "LEGS B")
            }
            TrainingSplit.AUTO -> when (frequency) {
                2 -> listOf("FULL BODY A", "FULL BODY B")
                3 -> listOf("UPPER", "LOWER", "FULL BODY")
                4 -> listOf("UPPER A", "LOWER A", "UPPER B", "LOWER B")
                5 -> listOf("PUSH", "PULL", "LEGS", "UPPER", "LOWER")
                else -> listOf("PUSH A", "PULL A", "LEGS A", "PUSH B", "PULL B", "LEGS B")
            }
        }
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
        val accessorySets = minOf(sets, 3)
        val pushPrimary = movementVariant(
            variant,
            gym = listOf("Barbell Bench Press", "Seated Overhead Press", "Incline Dumbbell Press"),
            bodyweight = listOf("Push-up", "Pike Push-up", "Tempo Push-up"),
            dumbbell = listOf("Dumbbell Floor Press", "Dumbbell Shoulder Press", "Neutral-grip Floor Press"),
            equipment = equipment,
            group = if (variant % 3 == 1) "Shoulders" else "Chest",
            sets = sets,
            min = 6,
            max = 12,
        )
        val pushSecondary = movementVariant(
            variant + 1,
            gym = listOf("Machine Chest Press", "Dumbbell Bench Press", "Chest Press"),
            bodyweight = listOf("Incline Push-up", "Kneeling Push-up", "Close-grip Push-up"),
            dumbbell = listOf("Neutral-grip Floor Press", "Dumbbell Floor Press", "Dumbbell Squeeze Press"),
            equipment = equipment,
            group = "Chest",
            sets = sets,
            min = 8,
            max = 12,
        )
        val pullPrimary = movementVariant(
            variant,
            gym = listOf("Lat Pulldown", "Chest Supported Row", "Seated Cable Row"),
            bodyweight = listOf("Prone W Raise", "Prone Y-T-W", "Reverse Snow Angel"),
            dumbbell = listOf("One-arm Dumbbell Row", "Bent-over Dumbbell Row", "Dumbbell Pullover"),
            equipment = equipment,
            group = "Back",
            sets = sets,
            min = 8,
            max = 12,
        )
        val pullSecondary = movementVariant(
            variant + 1,
            gym = listOf("Machine Row", "Neutral-grip Pulldown", "Face Pull"),
            bodyweight = listOf("Reverse Snow Angel", "Prone W Raise", "Superman Row"),
            dumbbell = listOf("Dumbbell Row", "Rear Delt Fly", "Dumbbell High Row"),
            equipment = equipment,
            group = "Back",
            sets = accessorySets,
            min = 10,
            max = 15,
        )
        val kneeDominant = movementVariant(
            variant,
            gym = listOf("Barbell Squat", "Leg Press", "Bulgarian Split Squat"),
            bodyweight = listOf("Tempo Squat", "Reverse Lunge", "Split Squat"),
            dumbbell = listOf("Goblet Squat", "Dumbbell Reverse Lunge", "Dumbbell Split Squat"),
            equipment = equipment,
            group = "Legs",
            sets = sets,
            min = 6,
            max = 12,
        )
        val hipDominant = movementVariant(
            variant,
            gym = listOf("Romanian Deadlift", "Hip Thrust", "Leg Curl"),
            bodyweight = listOf("Single-leg Hip Hinge", "Glute Bridge", "Sliding Leg Curl"),
            dumbbell = listOf("Dumbbell Romanian Deadlift", "Dumbbell Hip Thrust", "Dumbbell Leg Curl"),
            equipment = equipment,
            group = if (variant % 3 == 1) "Glutes" else "Hamstrings",
            sets = sets,
            min = 8,
            max = 12,
        )
        val calf = move("Standing Calf Raise", "Single-leg Calf Raise", "Dumbbell Calf Raise", equipment, "Calves", accessorySets, 12, 20)
        val shoulderAccessory = move("Cable Lateral Raise", "Prone Y Raise", "Dumbbell Lateral Raise", equipment, "Shoulders", accessorySets, 12, 20)
        val armAccessory = movementVariant(
            variant,
            gym = listOf("Triceps Pushdown", "Cable Curl", "Rope Hammer Curl"),
            bodyweight = listOf("Diamond Push-up", "Self-resisted Biceps Curl", "Close-grip Push-up"),
            dumbbell = listOf("Dumbbell Triceps Extension", "Dumbbell Curl", "Hammer Curl"),
            equipment = equipment,
            group = "Arms",
            sets = accessorySets,
            min = 10,
            max = 15,
        )
        val core = listOf(
            PlannedExercise("Dead Bug", "Core", accessorySets, 8, 12),
            PlannedExercise("Heel Tap", "Core", accessorySets, 10, 20),
            PlannedExercise("Bird Dog", "Core", accessorySets, 8, 12),
        )[Math.floorMod(variant, 3)]

        return when {
            name.startsWith("PUSH") -> listOf(pushPrimary, pushSecondary, shoulderAccessory, armAccessory, core)
            name.startsWith("PULL") -> listOf(pullPrimary, pullSecondary, shoulderAccessory, armAccessory, core)
            name.startsWith("LEGS") || name.startsWith("LOWER") -> listOf(kneeDominant, hipDominant, calf, core)
            name.startsWith("UPPER") -> listOf(pushPrimary, pushSecondary, pullPrimary, pullSecondary, core)
            else -> listOf(if (pushPrimary.muscleGroup == "Chest") pushPrimary else pushSecondary, pullPrimary, kneeDominant, hipDominant, core, shoulderAccessory, armAccessory)
        }
    }

    private fun movementVariant(
        variant: Int,
        gym: List<String>,
        bodyweight: List<String>,
        dumbbell: List<String>,
        equipment: Equipment,
        group: String,
        sets: Int,
        min: Int,
        max: Int,
    ): PlannedExercise {
        require(gym.size == bodyweight.size && gym.size == dumbbell.size && gym.isNotEmpty())
        val index = Math.floorMod(variant, gym.size)
        return move(gym[index], bodyweight[index], dumbbell[index], equipment, group, sets, min, max)
    }

    private fun addFocusExercise(
        current: List<PlannedExercise>,
        focus: Set<FocusArea>,
        equipment: Equipment,
        sets: Int,
        workoutIndex: Int,
        exerciseLimit: Int,
    ): List<PlannedExercise> {
        val selectedFocus = focus.filterNot { it == FocusArea.FULL_BODY }.sortedBy { it.ordinal }.takeIf { it.isNotEmpty() }
            ?.let { it[Math.floorMod(workoutIndex, it.size)] }
        val focusSets = minOf(sets, 3)
        val focusMove = when (selectedFocus) {
            FocusArea.FULL_BODY -> null
            FocusArea.GLUTES -> move("Hip Thrust", "Glute Bridge", "Dumbbell Hip Thrust", equipment, "Glutes", sets, 8, 15)
            FocusArea.CHEST -> move("Chest Fly", "Wide Push-up", "Dumbbell Fly", equipment, "Chest", focusSets, 10, 15)
            FocusArea.BACK -> move("Machine Row", "Superman Row", "Dumbbell Pullover", equipment, "Back", focusSets, 10, 15)
            FocusArea.SHOULDERS -> move("Machine Lateral Raise", "Prone Y Raise", "Dumbbell Lateral Raise", equipment, "Shoulders", focusSets, 12, 20)
            FocusArea.ARMS -> move("Cable Curl", "Close-grip Push-up", "Hammer Curl", equipment, "Arms", focusSets, 10, 15)
            FocusArea.CORE -> PlannedExercise("Heel Tap", "Core", focusSets, 10, 20)
            FocusArea.LEGS -> move("Leg Extension", "Reverse Lunge", "Dumbbell Split Squat", equipment, "Legs", focusSets, 10, 15)
            FocusArea.ENDURANCE -> PlannedExercise("Zone 2 Finisher", "Endurance", 1, 10, 20)
            FocusArea.MOBILITY -> PlannedExercise("Mobility Flow", "Mobility", 1, 8, 12)
            null -> null
        }
        if (focusMove == null || current.any { it.name == focusMove.name }) return current
        val coreIndex = current.indexOfFirst { it.muscleGroup == "Core" }
        val latestRetainedIndex = (exerciseLimit - 1).coerceAtLeast(0)
        val insertionIndex = minOf(if (coreIndex == -1) current.size else coreIndex, latestRetainedIndex)
        return current.toMutableList().apply { add(insertionIndex, focusMove) }
    }

    private fun adaptForInjuries(exercise: PlannedExercise, injuries: Set<InjuryArea>, equipment: Equipment): PlannedExercise = when {
        InjuryArea.KNEE in injuries && exercise.name.contains(Regex("Squat|Lunge|Leg Press")) -> PlannedExercise("Pain-free Glute Bridge", "Glutes", exercise.sets, 10, 15)
        InjuryArea.LOWER_BACK in injuries && exercise.name.contains(Regex("Deadlift|Hip Hinge|Barbell Row")) -> PlannedExercise("Supported Hamstring Curl", "Hamstrings", exercise.sets, 10, 15)
        InjuryArea.SHOULDER in injuries && exercise.name.contains(Regex("Overhead|Pike|Lateral")) -> PlannedExercise("Pain-free Scapular Control", "Shoulders", minOf(exercise.sets, 2), 10, 15)
        InjuryArea.WRIST in injuries && exercise.name.contains("Push-up") -> move("Machine Chest Press", "Forearm Plank", "Neutral-grip Floor Press", equipment, "Chest", exercise.sets, 8, 12)
        InjuryArea.ANKLE in injuries && exercise.name.contains(Regex("Calf|Lunge")) -> PlannedExercise("Seated Leg Extension", "Legs", exercise.sets, 10, 15)
        InjuryArea.HIP in injuries && exercise.name.contains(Regex("Squat|Lunge|Deadlift|Hip Hinge|Leg Press")) -> PlannedExercise("Clinician-approved Hip Isometric", "Glutes", 2, 10, 20)
        else -> exercise
    }

    private fun move(gym: String, bodyweight: String, dumbbell: String, equipment: Equipment, group: String, sets: Int, min: Int, max: Int) =
        PlannedExercise(
            when (equipment) {
                Equipment.FULL_GYM -> gym
                Equipment.HOME_GYM -> dumbbell
                Equipment.DUMBBELLS -> dumbbell
                Equipment.BODYWEIGHT -> bodyweight
            },
            group, sets, min, max,
        )
}
