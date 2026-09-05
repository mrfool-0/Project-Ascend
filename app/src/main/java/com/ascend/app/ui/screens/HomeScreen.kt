package com.ascend.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.PersonOutline
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

@Composable
fun HomeScreen(
    state: DashboardState,
    todayTemplate: WorkoutTemplateEntity?,
    onOpenProfile: () -> Unit,
    onStartWorkout: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenHabits: () -> Unit,
    onAddWater: (Int) -> Unit,
    onToggleHabit: (HabitEntity, Boolean) -> Unit,
) {
    val greeting = when (LocalTime.now().hour) { in 5..11 -> "GOOD MORNING"; in 12..16 -> "GOOD AFTERNOON"; else -> "GOOD EVENING" }
    val workoutDone = state.workoutHistory.any { it.localDate == LocalDate.now().toString() && it.completedAt != null }
    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(EnergyEmerald, CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text("SYSTEM ONLINE", style = MaterialTheme.typography.labelMedium, color = EnergyEmerald)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text("$greeting,", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Text(state.profile?.displayName?.uppercase() ?: "PLAYER", style = MaterialTheme.typography.headlineLarge)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenProfile, Modifier.size(52.dp).background(RaisedSurface, AngularShape)) {
                    Icon(Icons.Outlined.PersonOutline, "Open player profile", tint = EnergyCyan)
                }
            }
        }
        item {
            AscendCard(Modifier.fillMaxWidth(), accent = EnergyViolet) {
                Text("DAILY SYSTEM MESSAGE", style = MaterialTheme.typography.labelMedium, color = EnergyViolet)
                Spacer(Modifier.height(7.dp))
                Text("“${MotivationLibrary.quoteFor(LocalDate.now())}”", style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            AscendCard(Modifier.fillMaxWidth(), highlighted = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("PLAYER STATUS", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(if (state.level.isMaxLevel) "MAX" else state.level.level.toString(), style = MaterialTheme.typography.displayLarge, color = TextPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("LEVEL", style = MaterialTheme.typography.labelLarge, color = EnergyViolet, modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    RankBadge(state.level.level, state.level.rank.title)
                }
                Spacer(Modifier.height(14.dp))
                XpProgressBar(state.level)
                Spacer(Modifier.height(18.dp))
                StreakIndicator(state.streak.current, state.streak.longest)
            }
        }
        item {
            SectionHeader("Today's primary quest", if (workoutDone) "COMPLETE" else "+${todayTemplate?.rewardXp ?: 200} XP")
            Spacer(Modifier.height(10.dp))
            AscendCard(Modifier.fillMaxWidth(), accent = if (workoutDone) EnergyEmerald else EnergyViolet, highlighted = !workoutDone) {
                Text(todayTemplate?.name ?: "SYNCING PROTOCOL", style = MaterialTheme.typography.headlineMedium)
                Text(if (todayTemplate?.isRecovery == true) "REST / RECOVERY" else "STRENGTH PROTOCOL", style = MaterialTheme.typography.labelMedium, color = EnergyCyan)
                Spacer(Modifier.height(15.dp))
                Row {
                    QuestMeta("DURATION", "${todayTemplate?.estimatedMinutes ?: 0} MIN")
                    Spacer(Modifier.width(28.dp))
                    QuestMeta("DIFFICULTY", if (todayTemplate?.isRecovery == true) "RECOVERY" else "NORMAL")
                }
                Spacer(Modifier.height(18.dp))
                SystemButton(if (workoutDone) "QUEST COMPLETE" else "ENTER QUEST", onStartWorkout, Modifier.fillMaxWidth(), enabled = !workoutDone)
            }
        }
        item {
            SectionHeader("Quick actions")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                QuickAction("LOG FOOD", Icons.Outlined.Add, onOpenNutrition)
                QuickAction("LOG WEIGHT", Icons.Outlined.Add, onOpenProgress)
                QuickAction("+250 ML", Icons.Outlined.LocalDrink) { onAddWater(250) }
                QuickAction("START", Icons.Outlined.FitnessCenter, onStartWorkout)
                QuickAction("HABITS", Icons.Outlined.Add, onOpenHabits)
            }
        }
        item {
            SectionHeader("Nutrition objective", "LIVE")
            Spacer(Modifier.height(12.dp))
            AscendCard(Modifier.fillMaxWidth()) {
                val target = state.target
                MacroProgressBar("Calories", state.nutrition.calories, target?.calories ?: 0, "KCAL", EnergyViolet)
                Spacer(Modifier.height(14.dp))
                MacroProgressBar("Protein", state.nutrition.protein, target?.proteinGrams ?: 0, "G", EnergyCyan)
                Spacer(Modifier.height(14.dp))
                MacroProgressBar("Carbs", state.nutrition.carbs, target?.carbohydrateGrams ?: 0, "G", EnergyAmber)
                Spacer(Modifier.height(14.dp))
                MacroProgressBar("Fat", state.nutrition.fat, target?.fatGrams ?: 0, "G", EnergyEmerald)
            }
        }
        item {
            SectionHeader("Hydration")
            Spacer(Modifier.height(10.dp))
            AscendCard(Modifier.fillMaxWidth(), accent = EnergyCyan) {
                MacroProgressBar("Water", state.waterMl, state.target?.waterMl ?: 0, "ML", EnergyCyan)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SystemButton("+250 ML", { onAddWater(250) }, Modifier.weight(1f), secondary = true)
                    SystemButton("+500 ML", { onAddWater(500) }, Modifier.weight(1f), secondary = true)
                }
            }
        }
        item {
            SectionHeader("Today's habits", "${state.habitCompletions.size}/${state.habits.size}")
            Spacer(Modifier.height(10.dp))
            if (state.habits.isEmpty()) EmptyState("No habits", "Build discipline one protocol at a time.", "Create habit", onOpenHabits)
            else AscendCard(Modifier.fillMaxWidth()) {
                state.habits.take(5).forEachIndexed { index, habit ->
                    HabitRow(habit, habit.id in state.habitCompletions) { onToggleHabit(habit, it) }
                    if (index != state.habits.take(5).lastIndex) HorizontalDivider(color = Hairline)
                }
            }
        }
        item {
            SectionHeader("Current body weight", "TREND")
            Spacer(Modifier.height(10.dp))
            if (state.weights.isEmpty()) EmptyState("No weight data", "Your progress has not been recorded yet.", "Record first weight", onOpenProgress)
            else AscendCard(Modifier.fillMaxWidth(), onClick = onOpenProgress) {
                val latest = state.weights.last().weightKg
                val start = state.weights.first().weightKg
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("%.1f".format(latest), style = MaterialTheme.typography.displayMedium)
                    Text(" KG", style = MaterialTheme.typography.labelLarge, color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
                    Spacer(Modifier.weight(1f))
                    Text("%+.1f KG".format(latest - start), style = MaterialTheme.typography.titleMedium, color = if (latest <= start) EnergyEmerald else EnergyAmber)
                }
                Spacer(Modifier.height(14.dp))
                WeightSparkline(state.weights.takeLast(12).map { it.weightKg }, Modifier.fillMaxWidth().height(68.dp))
            }
        }
    }
}

@Composable
private fun QuestMeta(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = AngularShape, color = RaisedSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Hairline), modifier = Modifier.height(46.dp),
    ) {
        Row(Modifier.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(17.dp), tint = EnergyCyan); Spacer(Modifier.width(7.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun HabitRow(habit: HabitEntity, complete: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onToggle(!complete) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(complete, { onToggle(it) }, colors = CheckboxDefaults.colors(checkedColor = EnergyEmerald, checkmarkColor = Void))
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(habit.name, style = MaterialTheme.typography.bodyLarge, color = if (complete) TextSecondary else TextPrimary)
            Text("${habit.target.toInt()} ${habit.unit}  •  +${com.ascend.app.domain.HabitDifficulty.valueOf(habit.difficulty).xp} XP", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        if (complete) Text("COMPLETE", style = MaterialTheme.typography.labelMedium, color = EnergyEmerald)
    }
}

@Composable
fun WeightSparkline(values: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min(); val max = values.max(); val range = (max - min).takeIf { it > 0 } ?: 1.0
        val points = values.mapIndexed { index, value ->
            Offset(index * size.width / (values.size - 1), size.height - ((value - min) / range * size.height * .75f).toFloat() - size.height * .12f)
        }
        val path = Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) } }
        drawPath(path, Brush.horizontalGradient(listOf(EnergyViolet, EnergyCyan)), style = Stroke(3f))
        points.forEach { drawCircle(EnergyCyan, 3.5f, it) }
    }
}
