package com.ascend.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ascend.app.DashboardState
import com.ascend.app.core.database.AchievementEntity
import com.ascend.app.core.database.QuestEntity
import com.ascend.app.core.database.UnlockedAchievementEntity
import com.ascend.app.domain.AscendConfig
import com.ascend.app.domain.QuestType
import com.ascend.app.ui.components.AscendCard
import com.ascend.app.ui.components.MacroProgressBar
import com.ascend.app.ui.components.SectionHeader
import com.ascend.app.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun QuestsScreen(
    state: DashboardState,
    quests: List<QuestEntity>,
    achievements: List<AchievementEntity>,
    unlocked: List<UnlockedAchievementEntity>,
    onStartWorkout: () -> Unit,
) {
    val target = state.target
    val dailyStatus = mapOf(
        "daily_workout" to state.workoutHistory.any { it.localDate == LocalDate.now().toString() && it.completedAt != null },
        "daily_calorie" to (target != null && state.nutrition.calories.toDouble() / target.calories in .9..1.1),
        "daily_protein" to (target != null && state.nutrition.protein >= target.proteinGrams),
        "daily_water" to (target != null && state.waterMl >= target.waterMl),
        "daily_discipline" to (state.habits.isNotEmpty() && state.habitCompletions.size.toDouble() / state.habits.size >= .8),
    )
    val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekSummaries = state.dailySummaries.filter { !LocalDate.parse(it.localDate).isBefore(weekStart) }
    val weeklyProgress = mapOf(
        "weekly_iron" to (weekSummaries.count { it.workoutCompleted }.toDouble() / 6).coerceIn(0.0, 1.0),
        "weekly_nutrition" to (weekSummaries.count { summary -> target != null && summary.calories.toDouble() / target.calories in .9..1.1 }.toDouble() / 5).coerceIn(0.0, 1.0),
        "weekly_consistency" to (weekSummaries.map { it.completionPercent }.average().takeIf { !it.isNaN() } ?: 0.0) / 80.0,
    )
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("QUEST LEDGER", style = MaterialTheme.typography.headlineMedium); Text("OBJECTIVES SHAPE ASCENSION", style = MaterialTheme.typography.labelMedium, color = EnergyCyan) }
        item { SectionHeader("Daily quests", "${dailyStatus.values.count { it }}/${dailyStatus.size}") }
        quests.filter { it.type == QuestType.DAILY.name }.forEach { quest ->
            item(key = quest.id) { QuestPanel(quest, dailyStatus[quest.id] == true, if (quest.id == "daily_workout") onStartWorkout else null) }
        }
        item { SectionHeader("Weekly quests", "RESET MONDAY") }
        quests.filter { it.type == QuestType.WEEKLY.name }.forEach { quest ->
            item(key = quest.id) {
                val progress = (weeklyProgress[quest.id] ?: 0.0).coerceIn(0.0, 1.0)
                AscendCard(Modifier.fillMaxWidth(), accent = if (progress >= 1) EnergyEmerald else EnergyViolet, highlighted = progress >= 1) {
                    Row { Column(Modifier.weight(1f)) { Text(quest.title, style = MaterialTheme.typography.titleLarge); Text(quest.description, color = TextSecondary) }; Text("+${quest.rewardXp} XP", color = EnergyCyan, style = MaterialTheme.typography.labelLarge) }
                    Spacer(Modifier.height(12.dp)); MacroProgressBar("Progress", (progress * 100).toInt(), 100, "%", EnergyViolet)
                }
            }
        }
        item { SectionHeader("Achievements", "${unlocked.size}/${achievements.size}") }
        achievements.forEach { achievement ->
            item(key = achievement.id) {
                val isUnlocked = unlocked.any { it.achievementId == achievement.id }
                AscendCard(Modifier.fillMaxWidth(), accent = if (isUnlocked) EnergyAmber else Hairline, highlighted = isUnlocked) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(achievement.title, style = MaterialTheme.typography.titleLarge, color = if (isUnlocked) EnergyAmber else TextPrimary)
                            Text(achievement.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Text(if (isUnlocked) "UNLOCKED" else "+${achievement.rewardXp} XP", style = MaterialTheme.typography.labelMedium, color = if (isUnlocked) EnergyEmerald else TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestPanel(quest: QuestEntity, complete: Boolean, action: (() -> Unit)?) {
    AscendCard(Modifier.fillMaxWidth(), accent = if (complete) EnergyEmerald else EnergyViolet, highlighted = complete, onClick = if (!complete) action else null) {
        Row {
            Column(Modifier.weight(1f)) {
                Text(quest.title, style = MaterialTheme.typography.titleLarge)
                Text(quest.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Text(if (complete) "COMPLETE" else "+${quest.rewardXp} XP", style = MaterialTheme.typography.labelMedium, color = if (complete) EnergyEmerald else EnergyCyan)
        }
    }
}
