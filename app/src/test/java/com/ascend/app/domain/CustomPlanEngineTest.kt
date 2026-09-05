package com.ascend.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomPlanEngineTest {
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
}
