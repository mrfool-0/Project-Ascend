package com.ascend.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.*

class TrainingRulesTest {
    @Test fun `full body covers every region without competing selections`() {
        val full = FocusRules.toggle(setOf(FocusArea.CHEST), FocusArea.FULL_BODY)
        assertEquals(setOf(FocusArea.FULL_BODY), full)
        assertTrue(FocusRules.muscles.all { FocusRules.includes(full, it) })
        assertEquals(full, FocusRules.normalize(full + FocusArea.ARMS))
        assertEquals(full, FocusRules.normalize(FocusRules.muscles))
    }
    @Test fun `removing one region switches to precise regional focus`() {
        val selected = FocusRules.toggle(setOf(FocusArea.FULL_BODY), FocusArea.CHEST)
        assertEquals(FocusRules.muscles - FocusArea.CHEST, selected)
        assertEquals(setOf(FocusArea.FULL_BODY), FocusRules.toggle(selected, FocusArea.CHEST))
        assertTrue(FocusRules.toggle(setOf(FocusArea.FULL_BODY), FocusArea.FULL_BODY).isEmpty())
    }
    @Test fun `swaps are limited to remaining dates of this week`() {
        val monday = LocalDate.of(2026, 9, 7)
        WeekRules.validateSwap(monday, monday.plusDays(6), monday)
        assertFailsWith<IllegalArgumentException> { WeekRules.validateSwap(monday, monday, monday) }
        assertFailsWith<IllegalArgumentException> { WeekRules.validateSwap(monday, monday.plusDays(7), monday) }
        assertFailsWith<IllegalArgumentException> { WeekRules.validateSwap(monday, monday.plusDays(2), monday.plusDays(1)) }
        assertFailsWith<IllegalArgumentException> { WeekRules.validateSwap(monday.plusDays(6), monday.plusDays(7), monday.plusDays(6)) }
    }
    @Test fun `full body uses every major movement within time budget`() {
        Equipment.entries.forEach { equipment ->
            listOf(30, 45, 60, 75, 90).forEach { minutes ->
                val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
                val plan = CustomPlanEngine.generate(3, days, setOf(FocusArea.FULL_BODY), setOf(InjuryArea.NONE), equipment, Experience.INTERMEDIATE, sessionMinutes = minutes)
                plan.workouts.filterNot { it.recovery }.forEach { workout ->
                    val groups = workout.exercises.map { it.muscleGroup }.toSet()
                    assertTrue(groups.containsAll(setOf("Chest", "Back", "Legs", "Core", "Shoulders", "Arms")))
                    assertTrue(groups.any { it in setOf("Glutes", "Hamstrings") })
                    assertTrue(workout.estimatedMinutes <= minutes)
                    assertTrue(workout.exercises.all { it.sets >= 1 })
                }
            }
        }
    }
}
