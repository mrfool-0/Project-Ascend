package com.ascend.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ascend.app.DashboardState
import com.ascend.app.core.database.HabitEntity
import com.ascend.app.core.database.WorkoutTemplateEntity
import com.ascend.app.domain.MotivationLibrary
import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun HomeScreen(
    state: DashboardState,
    todayTemplate: WorkoutTemplateEntity?,
    currentDate: LocalDate,
    profileImagePath: String?,
    onOpenProfile: () -> Unit,
    onStartWorkout: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenHabits: () -> Unit,
    onAddWater: (Int) -> Unit,
    onToggleHabit: (HabitEntity, Boolean) -> Unit,
) {
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val workoutDone = state.workoutHistory.any { it.localDate == currentDate.toString() && it.completedAt != null }
    val habitsDone = state.habitCompletions.size
    val habitTotal = state.habits.size

    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    StatusPill("System online", EnergyEmerald)
                    Spacer(Modifier.height(10.dp))
                    Text(greeting, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(state.profile?.displayName ?: "Player", style = MaterialTheme.typography.headlineLarge)
                    Text(currentDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM")), style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                }
                IconButton(onClick = onOpenProfile, modifier = Modifier.size(60.dp)) {
                    PlayerAvatar(profileImagePath, size = 52.dp)
                }
            }
        }

        item {
            TodayQuestCard(todayTemplate, workoutDone, onStartWorkout)
        }

        item {
            SectionHeader("Today", "Live")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SnapshotTile(
                    "Energy",
                    state.nutrition.calories.toString(),
                    "of ${state.target?.calories ?: 0} kcal",
                    Icons.Outlined.Restaurant,
                    EnergyViolet,
                    onOpenNutrition,
                    Modifier.weight(1f),
                )
                SnapshotTile(
                    "Hydration",
                    "${state.waterMl / 1000f}".trimEnd('0').trimEnd('.'),
                    "of ${((state.target?.waterMl ?: 0) / 1000f)} L",
                    Icons.Outlined.LocalDrink,
                    EnergyCyan,
                    { onAddWater(250) },
                    Modifier.weight(1f),
                )
                SnapshotTile(
                    "Habits",
                    "$habitsDone/$habitTotal",
                    if (habitTotal == 0) "Create one" else "completed",
                    Icons.Outlined.CheckCircle,
                    EnergyEmerald,
                    onOpenHabits,
                    Modifier.weight(1f),
                )
            }
        }

        item {
            AscendCard(Modifier.fillMaxWidth(), accent = EnergyViolet, onClick = onOpenProgress) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("LEVEL ${state.level.level}", style = MaterialTheme.typography.labelMedium, color = EnergyViolet)
                        Text(state.level.rank.title, style = MaterialTheme.typography.titleLarge)
                    }
                    StreakIndicator(state.streak.current, state.streak.longest)
                }
                Spacer(Modifier.height(15.dp))
                XpProgressBar(state.level)
            }
        }

        item {
            SectionHeader("Quick actions")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Log food", Icons.Outlined.Restaurant, EnergyViolet, onOpenNutrition, Modifier.weight(1f))
                QuickAction("Add water", Icons.Outlined.LocalDrink, EnergyCyan, { onAddWater(250) }, Modifier.weight(1f))
                QuickAction("Train", Icons.Outlined.FitnessCenter, EnergyEmerald, onStartWorkout, Modifier.weight(1f), enabled = !workoutDone)
            }
        }

        item {
            AscendCard(Modifier.fillMaxWidth(), accent = EnergyCyan) {
                Text("DAILY SIGNAL", style = MaterialTheme.typography.labelMedium, color = EnergyCyan)
                Spacer(Modifier.height(8.dp))
                Text("“${MotivationLibrary.quoteFor(currentDate)}”", style = MaterialTheme.typography.titleLarge)
            }
        }

        item {
            SectionHeader("Habits", "$habitsDone/$habitTotal")
            Spacer(Modifier.height(10.dp))
            if (state.habits.isEmpty()) {
                EmptyState("No habits yet", "Start with one action you can repeat even on a difficult day.", "Create a habit", onOpenHabits)
            } else {
                AscendCard(Modifier.fillMaxWidth()) {
                    state.habits.take(4).forEachIndexed { index, habit ->
                        HabitRow(habit, habit.id in state.habitCompletions) { onToggleHabit(habit, it) }
                        if (index != state.habits.take(4).lastIndex) HorizontalDivider(color = Hairline.copy(.7f))
                    }
                    if (state.habits.size > 4) {
                        TextButton(onClick = onOpenHabits, Modifier.align(Alignment.End)) { Text("View all") }
                    }
                }
            }
        }

        item {
            SectionHeader("Body trend")
            Spacer(Modifier.height(10.dp))
            if (state.weights.isEmpty()) {
                EmptyState("No weight trend", "Add a reading when you are ready. Trends matter more than single days.", "Record weight", onOpenProgress)
            } else {
                AscendCard(Modifier.fillMaxWidth(), onClick = onOpenProgress) {
                    val latest = state.weights.last().weightKg
                    val start = state.weights.first().weightKg
                    val target = state.profile?.targetWeightKg
                    val movingTowardTarget = target == null || abs(latest - target) <= abs(start - target)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("%.1f".format(latest), style = MaterialTheme.typography.displayMedium)
                        Text(" kg", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 7.dp))
                        Spacer(Modifier.weight(1f))
                        StatusPill("%+.1f kg".format(latest - start), if (movingTowardTarget) EnergyEmerald else EnergyAmber)
                    }
                    Spacer(Modifier.height(14.dp))
                    WeightSparkline(state.weights.takeLast(12).map { it.weightKg }, Modifier.fillMaxWidth().height(72.dp))
                }
            }
        }
    }
}

@Composable
private fun TodayQuestCard(template: WorkoutTemplateEntity?, complete: Boolean, onStart: () -> Unit) {
    val accent by animateColorAsState(if (complete) EnergyEmerald else EnergyViolet, tween(350), label = "quest accent")
    AscendCard(Modifier.fillMaxWidth(), accent = accent, highlighted = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(if (complete) "Completed" else "Primary quest", accent)
            Spacer(Modifier.weight(1f))
            Text("+${template?.rewardXp ?: 0} XP", style = MaterialTheme.typography.labelMedium, color = if (complete) TextTertiary else EnergyCyan)
        }
        Spacer(Modifier.height(17.dp))
        AnimatedContent(
            targetState = template?.name ?: "Preparing your protocol",
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "quest title",
        ) { title -> Text(title, style = MaterialTheme.typography.headlineLarge) }
        Spacer(Modifier.height(7.dp))
        Text(
            if (template?.isRecovery == true) "Recovery · ${template.estimatedMinutes} min" else "Strength · ${template?.estimatedMinutes ?: 0} min",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(18.dp))
        SystemButton(if (complete) "Quest completed" else "Enter quest", onStart, Modifier.fillMaxWidth(), enabled = !complete)
    }
}

@Composable
private fun SnapshotTile(
    label: String,
    value: String,
    supporting: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(116.dp),
        shape = MaterialTheme.shapes.medium,
        color = RaisedSurface.copy(alpha = .78f),
        border = androidx.compose.foundation.BorderStroke(.75.dp, Hairline),
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, Modifier.size(20.dp), tint = color)
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge)
                Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = TextTertiary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(82.dp),
        shape = MaterialTheme.shapes.medium,
        color = RaisedSurface.copy(alpha = if (enabled) .7f else .35f),
        border = androidx.compose.foundation.BorderStroke(.75.dp, Hairline),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(21.dp), tint = if (enabled) color else TextTertiary)
            Spacer(Modifier.height(7.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (enabled) TextPrimary else TextTertiary)
        }
    }
}

@Composable
fun HabitRow(habit: HabitEntity, complete: Boolean, onToggle: (Boolean) -> Unit) {
    val alpha by animateFloatAsState(if (complete) .62f else 1f, spring(stiffness = 600f), label = "habit alpha")
    Row(
        Modifier.fillMaxWidth().clickable { onToggle(!complete) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            complete,
            { onToggle(it) },
            colors = CheckboxDefaults.colors(checkedColor = EnergyEmerald, checkmarkColor = Void, uncheckedColor = TextTertiary),
        )
        Column(Modifier.weight(1f).padding(start = 3.dp)) {
            Text(habit.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary.copy(alpha))
            Text(
                "${habit.target.toInt()} ${habit.unit} · +${com.ascend.app.domain.HabitDifficulty.valueOf(habit.difficulty).xp} XP",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha),
            )
        }
        if (complete) StatusPill("Done", EnergyEmerald)
    }
}

@Composable
fun WeightSparkline(values: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        repeat(3) { line ->
            val y = size.height * line / 2f
            drawLine(Hairline.copy(.55f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
        if (values.isEmpty()) return@Canvas
        if (values.size == 1) {
            drawCircle(EnergyCyan.copy(.2f), 8.dp.toPx(), center)
            drawCircle(EnergyCyan, 4.dp.toPx(), center)
            return@Canvas
        }
        val low = values.min()
        val high = values.max()
        val range = (high - low).takeIf { it > 0 } ?: 1.0
        val points = values.mapIndexed { index, value ->
            Offset(
                index * size.width / (values.size - 1),
                size.height - ((value - low) / range * size.height * .7f).toFloat() - size.height * .15f,
            )
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, Brush.horizontalGradient(listOf(EnergyViolet, EnergyCyan)), style = Stroke(2.dp.toPx()))
        drawCircle(EnergyCyan, 4.dp.toPx(), points.last())
        drawCircle(EnergyCyan.copy(.2f), 8.dp.toPx(), points.last())
    }
}
