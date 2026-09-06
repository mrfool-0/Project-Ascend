package com.ascend.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ascend.app.DashboardState
import com.ascend.app.core.database.AchievementEntity
import com.ascend.app.core.database.QuestEntity
import com.ascend.app.core.database.UnlockedAchievementEntity
import com.ascend.app.core.database.WorkoutTemplateEntity
import com.ascend.app.domain.AscendConfig
import com.ascend.app.domain.QuestType
import com.ascend.app.ui.components.AscendCard
import com.ascend.app.ui.components.MacroProgressBar
import com.ascend.app.ui.components.SectionHeader
import com.ascend.app.ui.components.ScreenHeader
import com.ascend.app.ui.components.StatusPill
import com.ascend.app.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit

@Composable
fun QuestsScreen(
    state: DashboardState,
    templates: List<WorkoutTemplateEntity>,
    quests: List<QuestEntity>,
    achievements: List<AchievementEntity>,
    unlocked: List<UnlockedAchievementEntity>,
    currentDate: LocalDate,
    onBack: () -> Unit,
    onStartWorkout: () -> Unit,
    onToggleCustomQuest: (QuestEntity, Boolean) -> Unit,
) {
    val target = state.target
    val dailyStatus = mapOf(
        "daily_workout" to state.workoutHistory.any { it.localDate == currentDate.toString() && it.completedAt != null },
        "daily_calorie" to (target != null && state.nutrition.calories.toDouble() / target.calories in .9..1.1),
        "daily_protein" to (target != null && state.nutrition.protein >= target.proteinGrams),
        "daily_water" to (target != null && state.waterMl >= target.waterMl),
        "daily_discipline" to (state.habits.isNotEmpty() && state.habitCompletions.size.toDouble() / state.habits.size >= .8),
    )
    val weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekSummaries = state.dailySummaries.filter {
        val date = runCatching { LocalDate.parse(it.localDate) }.getOrNull()
        date != null && !date.isBefore(weekStart) && !date.isAfter(currentDate)
    }
    val plannedTraining = (state.profile?.workoutFrequency ?: 1).coerceAtLeast(1)
    val recoveryTemplateId = templates.firstOrNull { it.rotationIndex == plannedTraining }?.id ?: "template_$plannedTraining"
    val completedProtocols = state.workoutHistory.count {
        val date = runCatching { LocalDate.parse(it.localDate) }.getOrNull()
        it.completedAt != null && it.templateId != recoveryTemplateId && date != null && !date.isBefore(weekStart) && !date.isAfter(currentDate)
    }
    val elapsedWeekDays = (ChronoUnit.DAYS.between(weekStart, currentDate).toInt() + 1).coerceIn(1, 7)
    val weeklyAdherence = weekSummaries.sumOf { it.completionPercent }.toDouble() / (elapsedWeekDays * 100.0)
    val weeklyProgress = mapOf(
        "weekly_iron" to (completedProtocols.toDouble() / plannedTraining).coerceIn(0.0, 1.0),
        "weekly_nutrition" to (weekSummaries.count { summary -> target != null && summary.calories.toDouble() / target.calories in .9..1.1 }.toDouble() / 5).coerceIn(0.0, 1.0),
        "weekly_consistency" to weeklyAdherence / .8,
    )
    val dailyQuests = quests.filter { it.type == QuestType.DAILY.name }
    val dailyCompleted = dailyQuests.count { quest -> dailyStatus[quest.id] ?: (quest.id in state.questCompletions) }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ScreenHeader(
                title = "Quest ledger",
                subtitle = "Objectives shape ascension",
                leading = { FilledTonalIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
            )
        }
        item { SectionHeader("Daily quests", "$dailyCompleted/${dailyQuests.size}") }
        dailyQuests.forEach { quest ->
            item(key = quest.id) {
                val isCustom = quest.id.startsWith("custom_")
                val complete = dailyStatus[quest.id] ?: (quest.id in state.questCompletions)
                val action = when {
                    isCustom -> ({ onToggleCustomQuest(quest, !complete) })
                    quest.id == "daily_workout" && !complete -> onStartWorkout
                    else -> null
                }
                QuestPanel(quest, complete, action)
            }
        }
        item { SectionHeader("Weekly quests", "RESET MONDAY") }
        quests.filter { it.type == QuestType.WEEKLY.name }.forEach { quest ->
            item(key = quest.id) {
                val progress = (weeklyProgress[quest.id] ?: 0.0).coerceIn(0.0, 1.0)
                val complete = progress >= 1.0
                AscendCard(
                    Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                        stateDescription = if (complete) "Complete" else "${(progress * 100).toInt()} percent complete"
                    },
                    accent = if (complete) EnergyEmerald else EnergyViolet,
                    highlighted = complete,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(quest.title, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(3.dp))
                            Text(quest.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Spacer(Modifier.width(12.dp))
                        StatusPill(if (complete) "Complete" else "+${quest.rewardXp} XP", if (complete) EnergyEmerald else EnergyCyan)
                    }
                    Spacer(Modifier.height(12.dp)); MacroProgressBar("Progress", (progress * 100).toInt(), 100, "%", EnergyViolet)
                }
            }
        }
        item { SectionHeader("Achievements", "${unlocked.size}/${achievements.size}") }
        achievements.sortedByDescending { achievement -> unlocked.any { it.achievementId == achievement.id } }.forEach { achievement ->
            item(key = achievement.id) {
                val isUnlocked = unlocked.any { it.achievementId == achievement.id }
                AscendCard(
                    Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                        stateDescription = if (isUnlocked) "Unlocked" else "Locked"
                    },
                    accent = if (isUnlocked) EnergyAmber else Hairline,
                    highlighted = isUnlocked,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(achievement.title, style = MaterialTheme.typography.titleLarge, color = if (isUnlocked) EnergyAmber else TextPrimary)
                            Spacer(Modifier.height(3.dp))
                            Text(achievement.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Spacer(Modifier.width(12.dp))
                        StatusPill(if (isUnlocked) "Unlocked" else "+${achievement.rewardXp} XP", if (isUnlocked) EnergyEmerald else TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestPanel(quest: QuestEntity, complete: Boolean, action: (() -> Unit)?) {
    AscendCard(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            stateDescription = if (complete) "Complete" else "Incomplete"
        },
        accent = if (complete) EnergyEmerald else EnergyViolet,
        highlighted = complete,
        onClick = action,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(quest.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(3.dp))
                Text(quest.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Spacer(Modifier.width(12.dp))
            StatusPill(if (complete) "Complete" else "+${quest.rewardXp} XP", if (complete) EnergyEmerald else EnergyCyan)
        }
    }
}
