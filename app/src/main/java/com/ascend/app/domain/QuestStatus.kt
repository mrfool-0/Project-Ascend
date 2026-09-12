package com.ascend.app.domain

import com.ascend.app.DashboardState
import com.ascend.app.core.database.QuestEntity
import java.time.LocalDate

/** One status calculation for the ledger and reminder worker. Never infer completion from text. */
object QuestStatus {
    fun isScheduled(quest: QuestEntity, date: LocalDate) = quest.active && when (quest.frequency) {
        HabitFrequency.WEEKDAYS.name -> date.dayOfWeek.value <= 5
        HabitFrequency.THREE_TIMES_WEEKLY.name -> date.dayOfWeek.value in setOf(1, 3, 5)
        else -> true
    }
    fun daily(state: DashboardState, date: LocalDate): Map<String, Boolean> {
        val target = state.target
        return mapOf(
            "daily_workout" to state.workoutHistory.any { it.localDate == date.toString() && it.completedAt != null },
            "daily_calorie" to (target != null && target.calories > 0 && state.nutrition.calories.toDouble() / target.calories in .9..1.1),
            "daily_protein" to (target != null && state.nutrition.protein >= target.proteinGrams),
            "daily_water" to (target != null && state.waterMl >= target.waterMl),
            "daily_discipline" to (state.habits.isNotEmpty() && state.habitCompletions.size.toDouble() / state.habits.size >= .8),
        )
    }
    fun pending(state: DashboardState, quests: List<QuestEntity>, date: LocalDate): List<String> {
        if (state.profile == null) return emptyList()
        val status = daily(state, date)
        val remaining = quests.filter { isScheduled(it, date) && it.type == QuestType.DAILY.name && !(status[it.id] ?: (it.id in state.questCompletions)) }
            .map { if (it.id.startsWith("custom_")) it.title + " · " + it.description.substringBefore(" • tap") else it.title }
        val habits = state.habits.filter { it.id !in state.habitCompletions }.map { it.name }
        return (remaining + habits).distinct()
    }
}
