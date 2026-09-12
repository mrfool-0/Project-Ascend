package com.ascend.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomPlanEngineTest {
    @Test fun `full body split scales across three four and five day maps`() {
        listOf(3, 4, 5).forEach { frequency ->
            val map = CustomPlanEngine.weeklyArchitecture(frequency, TrainingSplit.FULL_BODY)
            assertEquals(frequency, map.size)
            assertTrue(map.all { it.startsWith("FULL BODY") })
            assertEquals(map.size, map.distinct().size)
        }
    }

    @Test fun `full body focus resolves automatic architecture into balanced varied sessions`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val plan = CustomPlanEngine.generate(
            frequency = 3,
            workoutDays = days,
            focusAreas = setOf(FocusArea.FULL_BODY),
            injuries = setOf(InjuryArea.NONE),
            equipment = Equipment.FULL_GYM,
            experience = Experience.INTERMEDIATE,
            trainingSplit = TrainingSplit.AUTO,
        )
        val sessions = plan.workouts.filterNot { it.recovery }

        assertTrue(plan.title.contains("FULL BODY"))
        assertTrue(sessions.all { it.name.startsWith("FULL BODY") })
        assertTrue(sessions.all { workout ->
            val groups = workout.exercises.map { it.muscleGroup }.toSet()
            "Chest" in groups || "Shoulders" in groups
        })
        assertTrue(sessions.all { workout -> workout.exercises.any { it.muscleGroup == "Back" } })
        assertTrue(sessions.all { workout -> workout.exercises.any { it.muscleGroup == "Legs" } })
        assertTrue(sessions.all { workout -> workout.exercises.any { it.muscleGroup in setOf("Hamstrings", "Glutes") } })
        assertTrue(sessions.all { workout -> workout.exercises.any { it.muscleGroup == "Core" } })
        assertEquals(sessions.size, sessions.map { it.exercises.map(PlannedExercise::name) }.distinct().size)
    }

    @Test fun `high frequency full body plan controls per session volume`() {
        val days = DayOfWeek.entries.take(5).toSet()
        val plan = CustomPlanEngine.generate(
            frequency = 5,
            workoutDays = days,
            focusAreas = setOf(FocusArea.FULL_BODY),
            injuries = setOf(InjuryArea.NONE),
            equipment = Equipment.DUMBBELLS,
            experience = Experience.ADVANCED,
            objective = Objective.MUSCLE_GAIN,
        )

        val exercises = plan.workouts.filterNot { it.recovery }.flatMap { it.exercises }
        assertTrue(exercises.all { it.sets <= 2 })
        assertTrue(plan.safetyNotes.any { "lower per-session" in it })
    }

    @Test fun `selected architecture recalculates weekly protocols`() {
        assertEquals(listOf("PUSH", "PULL", "LEGS"), CustomPlanEngine.weeklyArchitecture(3, TrainingSplit.PUSH_PULL_LEGS))
        assertEquals(listOf("UPPER A", "LOWER A", "UPPER B", "LOWER B"), CustomPlanEngine.weeklyArchitecture(4, TrainingSplit.UPPER_LOWER))
        assertEquals(listOf("PUSH", "PULL", "LEGS", "UPPER", "LOWER"), CustomPlanEngine.weeklyArchitecture(5, TrainingSplit.AUTO))
    }

    @Test fun `generates exact training frequency plus recovery`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val plan = CustomPlanEngine.generate(3, days, setOf(FocusArea.GLUTES), setOf(InjuryArea.NONE), Equipment.DUMBBELLS, Experience.BEGINNER)
        assertEquals(4, plan.workouts.size)
        assertEquals(3, plan.workouts.count { !it.recovery })
        assertTrue(plan.workouts.last().recovery)
        assertTrue(plan.workouts.flatMap { it.exercises }.all { "Barbell" !in it.name && "Cable" !in it.name })
    }

    @Test fun `unselected weekday resolves to recovery template`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val start = LocalDate.of(2026, 9, 7)
        assertEquals(3, CustomPlanEngine.templateIndexFor(start.plusDays(1), start, days))
        assertEquals(0, CustomPlanEngine.templateIndexFor(start, start, days))
        assertEquals(1, CustomPlanEngine.templateIndexFor(start.plusDays(2), start, days))
    }

    @Test fun `reported knee limitation substitutes squat patterns`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        val plan = CustomPlanEngine.generate(2, days, setOf(FocusArea.LEGS), setOf(InjuryArea.KNEE), Equipment.FULL_GYM, Experience.INTERMEDIATE)
        val names = plan.workouts.flatMap { it.exercises }.map { it.name }
        assertTrue(names.none { it.contains("Squat") || it.contains("Leg Press") || it.contains("Lunge") })
        assertTrue(plan.safetyNotes.isNotEmpty())
    }

    @Test fun `beginner plan rotates every selected focus into the program`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val plan = CustomPlanEngine.generate(
            3, days, setOf(FocusArea.CHEST, FocusArea.BACK, FocusArea.CORE),
            setOf(InjuryArea.NONE), Equipment.BODYWEIGHT, Experience.BEGINNER,
        )
        val names = plan.workouts.filterNot { it.recovery }.flatMap { it.exercises }.map { it.name }
        assertTrue("Wide Push-up" in names)
        assertTrue("Superman Row" in names)
        assertTrue("Heel Tap" in names)
        assertTrue(plan.workouts.filterNot { it.recovery }.all { it.exercises.size <= 5 })
    }

    @Test fun `home gym plan excludes commercial gym only movements`() {
        val days = DayOfWeek.entries.take(6).toSet()
        val plan = CustomPlanEngine.generate(
            frequency = 6,
            workoutDays = days,
            focusAreas = setOf(
                FocusArea.CHEST,
                FocusArea.BACK,
                FocusArea.SHOULDERS,
                FocusArea.ARMS,
                FocusArea.GLUTES,
                FocusArea.LEGS,
            ),
            injuries = setOf(InjuryArea.NONE),
            equipment = Equipment.HOME_GYM,
            experience = Experience.ADVANCED,
        )

        val exerciseNames = plan.workouts.filterNot { it.recovery }.flatMap { it.exercises }.map { it.name }
        val commercialGymOnlyMarkers = listOf("Barbell", "Cable", "Machine", "Lat Pulldown", "Leg Press")

        assertTrue(exerciseNames.isNotEmpty())
        assertTrue(exerciseNames.any { "Dumbbell" in it || "Goblet" in it })
        assertTrue(exerciseNames.none { name -> commercialGymOnlyMarkers.any(name::contains) })
    }

    @Test fun `cardiovascular limitation produces recovery only plan`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val plan = CustomPlanEngine.generate(
            frequency = 3,
            workoutDays = days,
            focusAreas = setOf(FocusArea.ENDURANCE),
            injuries = setOf(InjuryArea.CARDIOVASCULAR),
            equipment = Equipment.FULL_GYM,
            experience = Experience.ADVANCED,
        )

        assertEquals(4, plan.workouts.size)
        assertTrue(plan.workouts.all { it.recovery })
        assertTrue(plan.workouts.all { it.exercises.isEmpty() })
        assertTrue(plan.safetyNotes.any { "Medical clearance" in it })
    }

    @Test fun `build consistency reduces weekly training volume`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val generalPlan = CustomPlanEngine.generate(
            frequency = 4,
            workoutDays = days,
            focusAreas = setOf(FocusArea.CHEST),
            injuries = setOf(InjuryArea.NONE),
            equipment = Equipment.FULL_GYM,
            experience = Experience.ADVANCED,
            objective = Objective.GENERAL_HEALTH,
        )
        val consistencyPlan = CustomPlanEngine.generate(
            frequency = 4,
            workoutDays = days,
            focusAreas = setOf(FocusArea.CHEST),
            injuries = setOf(InjuryArea.NONE),
            equipment = Equipment.FULL_GYM,
            experience = Experience.ADVANCED,
            objective = Objective.BUILD_CONSISTENCY,
        )

        val generalSets = generalPlan.workouts.filterNot { it.recovery }.sumOf { workout ->
            workout.exercises.sumOf { it.sets }
        }
        val consistencyExercises = consistencyPlan.workouts.filterNot { it.recovery }.flatMap { it.exercises }
        val consistencySets = consistencyExercises.sumOf { it.sets }

        assertTrue(consistencyExercises.isNotEmpty())
        assertTrue(consistencyExercises.all { it.sets <= 2 })
        assertTrue(consistencySets < generalSets)
    }
}
