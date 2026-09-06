package com.ascend.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class StreakQuestTest {
    private val today = LocalDate.of(2026, 9, 5)

    @Test fun `current streak allows today to still be in progress`() {
        val result = StreakEngine.calculate(listOf(today.minusDays(3), today.minusDays(2), today.minusDays(1)), today)
        assertEquals(3, result.current)
        assertEquals(3, result.longest)
    }

    @Test fun `daily completion respects calorie tolerance and habit threshold`() {
        val state = QuestEngine.daily(DailyCompletionInput(true, false, .95, 1.0, 1.1, 5, 4))
        assertTrue(state.perfectDay)
        val underEating = QuestEngine.daily(DailyCompletionInput(true, false, .4, 1.0, 1.0, 5, 4))
        assertFalse(underEating.calories)
    }

    @Test fun `weekly quest thresholds are enforced`() {
        val result = QuestEngine.weekly(WeeklyQuestProgress(6, 5, .8, 3))
        assertTrue(result.values.all { it })
        assertFalse(QuestEngine.weekly(WeeklyQuestProgress(5, 4, .79, 2)).values.any { it })
    }

    @Test fun `weekly training target follows the player frequency`() {
        val result = QuestEngine.weekly(WeeklyQuestProgress(3, 0, 0.0, 0, workoutTarget = 3))
        assertTrue(result.getValue("IRON WEEK"))
    }

    @Test fun `future completions never inflate a current streak`() {
        val result = StreakEngine.calculate(listOf(today.minusDays(1), today.plusDays(1), today.plusDays(2)), today)
        assertEquals(1, result.current)
        assertEquals(1, result.longest)
    }
}
