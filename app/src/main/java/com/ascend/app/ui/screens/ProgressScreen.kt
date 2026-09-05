package com.ascend.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ascend.app.DashboardState
import com.ascend.app.core.database.DailySummaryEntity
import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*
import java.time.LocalDate
import kotlin.math.min
import kotlin.math.roundToInt

private enum class ProgressTab { OVERVIEW, BODY, TRAINING, NUTRITION, CONSISTENCY }

@Composable
fun ProgressScreen(state: DashboardState, onLogWeight: (Double, String) -> Unit) {
    var tab by remember { mutableStateOf(ProgressTab.OVERVIEW) }
    var logWeight by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column { Text("PROGRESS ARCHIVE", style = MaterialTheme.typography.headlineMedium); Text("DATA REVEALS THE PATH", style = MaterialTheme.typography.labelMedium, color = EnergyCyan) }
                Spacer(Modifier.weight(1f))
                FilledIconButton(onClick = { logWeight = true }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = EnergyViolet)) { Icon(Icons.Outlined.Add, "Log body weight") }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ProgressTab.entries.forEach { value -> FilterChip(tab == value, { tab = value }, { Text(value.name, style = MaterialTheme.typography.labelMedium) }, shape = AngularShape) }
            }
        }
        when (tab) {
            ProgressTab.OVERVIEW -> overviewItems(state)
            ProgressTab.BODY -> bodyItems(state) { logWeight = true }
            ProgressTab.TRAINING -> trainingItems(state)
            ProgressTab.NUTRITION -> nutritionItems(state)
            ProgressTab.CONSISTENCY -> consistencyItems(state)
        }
    }
    if (logWeight) WeightDialog({ logWeight = false }) { value, note -> onLogWeight(value, note); logWeight = false }
}

private fun androidx.compose.foundation.lazy.LazyListScope.overviewItems(state: DashboardState) {
    item {
        AscendCard(Modifier.fillMaxWidth(), highlighted = true) {
            Row {
                BigMetric("LEVEL", state.level.level.toString(), EnergyViolet, Modifier.weight(1f))
                BigMetric("LIFETIME XP", "%,d".format(state.lifetimeXp), EnergyCyan, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp)); XpProgressBar(state.level)
        }
    }
    item {
        SectionHeader("Campaign statistics")
        Spacer(Modifier.height(9.dp))
        AscendCard(Modifier.fillMaxWidth()) {
            MetricGrid(
                "CURRENT STREAK" to "${state.streak.current} DAYS", "LONGEST" to "${state.streak.longest} DAYS",
                "WORKOUTS" to state.workoutHistory.count { it.completedAt != null }.toString(),
                "TRAINING VOLUME" to "${(state.trainingVolume / 1_000).roundToInt()}K KG",
                "WEIGHT CHANGE" to weightChange(state),
                "AVG ADHERENCE" to "${averageAdherence(state)}%",
            )
        }
    }
    item { SectionHeader("ASCEND scores", "GAME METRICS") }
    item { AscendScores(state) }
    item { SectionHeader("Consistency matrix", "8 WEEKS") }
    item { Heatmap(state) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.bodyItems(state: DashboardState, add: () -> Unit) {
    item { SectionHeader("Body progress", "TREND FOCUSED") }
    if (state.weights.isEmpty()) item { EmptyState("No weight data", "Your progress has not been recorded yet.", "Record first weight", add) }
    else {
        item {
            AscendCard(Modifier.fillMaxWidth(), highlighted = true) {
                Row {
                    BigMetric("CURRENT", "%.1f KG".format(state.weights.last().weightKg), EnergyCyan, Modifier.weight(1f))
                    BigMetric("TARGET", "%.1f KG".format(state.profile?.targetWeightKg ?: 0.0), EnergyViolet, Modifier.weight(1f))
                }
                Spacer(Modifier.height(22.dp))
                WeightChart(state.weights.map { it.weightKg }, Modifier.fillMaxWidth().height(180.dp))
            }
        }
        item {
            AscendCard(Modifier.fillMaxWidth()) {
                val recent = state.weights.filter { LocalDate.parse(it.localDate) >= LocalDate.now().minusDays(6) }.map { it.weightKg }
                MetricGrid(
                    "STARTING" to "%.1f KG".format(state.weights.first().weightKg),
                    "TOTAL CHANGE" to weightChange(state),
                    "7-DAY AVERAGE" to if (recent.isEmpty()) "—" else "%.1f KG".format(recent.average()),
                    "ENTRIES" to state.weights.size.toString(),
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.trainingItems(state: DashboardState) {
    val complete = state.workoutHistory.filter { it.completedAt != null }
    val week = complete.count { LocalDate.parse(it.localDate) >= LocalDate.now().minusDays(6) }
    item { SectionHeader("Training analytics", "RECORDED") }
    item {
        AscendCard(Modifier.fillMaxWidth(), highlighted = true) {
            MetricGrid("WORKOUTS / 7D" to week.toString(), "TOTAL WORKOUTS" to complete.size.toString(), "VOLUME" to "${state.trainingVolume.roundToInt()} KG", "AVG / WEEK" to "%.1f".format(complete.size / ((state.dailySummaries.size / 7.0).coerceAtLeast(1.0))))
        }
    }
    item { SectionHeader("Recent sessions") }
    state.workoutHistory.take(12).forEach { session ->
        item(key = session.id) {
            AscendCard(Modifier.fillMaxWidth(), accent = if (session.completedAt != null) EnergyEmerald else EnergyAmber) {
                Row { Column(Modifier.weight(1f)) { Text(session.templateId.removePrefix("template_").let { "TRAINING PROTOCOL ${it.toIntOrNull()?.plus(1) ?: ""}" }, style = MaterialTheme.typography.titleMedium); Text(session.localDate, color = TextSecondary) }; Text(if (session.completedAt != null) "COMPLETE" else "INCOMPLETE", color = if (session.completedAt != null) EnergyEmerald else EnergyAmber, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.nutritionItems(state: DashboardState) {
    val summaries = state.dailySummaries.takeLast(30)
    val active = summaries.filter { it.calories > 0 }
    item { SectionHeader("Nutrition analytics", "30 DAY") }
    item {
        AscendCard(Modifier.fillMaxWidth(), highlighted = true) {
            MetricGrid(
                "AVG CALORIES" to (active.map { it.calories }.average().takeIf { !it.isNaN() }?.roundToInt()?.toString() ?: "—"),
                "AVG PROTEIN" to (active.map { it.proteinGrams }.average().takeIf { !it.isNaN() }?.roundToInt()?.let { "$it G" } ?: "—"),
                "ADHERENCE" to "${averageAdherence(state)}%", "DAYS LOGGED" to active.size.toString(),
            )
        }
    }
    item { SectionHeader("Recent energy intake") }
    item { BarChart(summaries.map { it.calories }, state.target?.calories ?: 1, Modifier.fillMaxWidth().height(180.dp)) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.consistencyItems(state: DashboardState) {
    item { SectionHeader("Consistency matrix", "SELECTABLE HISTORY") }
    item { Heatmap(state) }
    item {
        AscendCard(Modifier.fillMaxWidth()) {
            StreakIndicator(state.streak.current, state.streak.longest)
            Spacer(Modifier.height(15.dp))
            Text("A day enters your streak when at least half of core objectives are complete. Lifetime XP is never removed when a streak ends.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun AscendScores(state: DashboardState) {
    val workoutRate = (state.workoutHistory.count { it.completedAt != null } * 4).coerceAtMost(100)
    val nutrition = averageAdherence(state)
    val discipline = if (state.habits.isEmpty()) 0 else (state.habitCompletions.size * 100 / state.habits.size)
    val consistency = (state.streak.current * 4).coerceAtMost(100)
    AscendCard(Modifier.fillMaxWidth()) {
        Text("Gamification scores based on activity — not physiological measurements.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(14.dp))
        listOf("STRENGTH" to workoutRate, "ENDURANCE" to (workoutRate * .8).roundToInt(), "NUTRITION" to nutrition, "DISCIPLINE" to discipline, "RECOVERY" to ((consistency + nutrition) / 2), "CONSISTENCY" to consistency).forEach { (label, value) ->
            MacroProgressBar(label, value, 100, "", if (value >= 70) EnergyEmerald else EnergyViolet)
            Spacer(Modifier.height(11.dp))
        }
    }
}

@Composable
private fun Heatmap(state: DashboardState) {
    val byDate = state.dailySummaries.associateBy { it.localDate }
    val days = (55 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    var selected by remember { mutableStateOf<DailySummaryEntity?>(null) }
    AscendCard(Modifier.fillMaxWidth()) {
        Canvas(
            Modifier.fillMaxWidth().height(112.dp).pointerInput(days, byDate) {
                detectTapGestures { offset ->
                    val gap = 5.dp.toPx()
                    val cell = min((size.width - gap * 7) / 8, (size.height - gap * 6) / 7)
                    val week = (offset.x / (cell + gap)).toInt().coerceIn(0, 7)
                    val day = (offset.y / (cell + gap)).toInt().coerceIn(0, 6)
                    val index = week * 7 + day
                    selected = days.getOrNull(index)?.toString()?.let(byDate::get)
                }
            },
        ) {
            val gap = 5.dp.toPx(); val cell = min((size.width - gap * 7) / 8, (size.height - gap * 6) / 7)
            days.forEachIndexed { index, date ->
                val week = index / 7; val day = index % 7
                val summary = byDate[date.toString()]
                val alpha = when (summary?.completionPercent ?: 0) { 0 -> .08f; in 1..39 -> .25f; in 40..69 -> .48f; in 70..89 -> .72f; else -> 1f }
                drawRect(EnergyViolet.copy(alpha), Offset(week * (cell + gap), day * (cell + gap)), Size(cell, cell))
            }
        }
        Text("Each cell represents daily completion intensity.", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        selected?.let { day ->
            Spacer(Modifier.height(10.dp))
            Text(day.localDate, style = MaterialTheme.typography.titleMedium, color = EnergyCyan)
            val weight = state.weights.lastOrNull { it.localDate == day.localDate }?.weightKg?.let { "  •  %.1f kg".format(it) } ?: ""
            Text("${day.completionPercent}% complete  •  ${day.xpEarned} XP$weight", style = MaterialTheme.typography.bodyMedium)
            Text("Workout ${if (day.workoutCompleted) "complete" else "not complete"}  •  ${day.calories} kcal  •  ${day.proteinGrams} g protein  •  ${day.waterMl} ml water  •  ${day.habitsCompleted} habits", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun WeightChart(values: List<Double>, modifier: Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val low = values.min() - .5; val high = values.max() + .5; val range = high - low
        repeat(4) { y -> drawLine(Hairline, Offset(0f, y * size.height / 3), Offset(size.width, y * size.height / 3), 1f) }
        val points = values.mapIndexed { i, value -> Offset(i * size.width / (values.size - 1), size.height - ((value - low) / range * size.height).toFloat()) }
        val path = Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) } }
        drawPath(path, EnergyCyan, style = Stroke(4f))
        points.forEach { drawCircle(EnergyViolet, 5f, it) }
    }
}

@Composable
private fun BarChart(values: List<Int>, target: Int, modifier: Modifier) {
    AscendCard(Modifier.fillMaxWidth()) {
        Canvas(modifier) {
            if (values.isEmpty()) return@Canvas
            val max = maxOf(values.maxOrNull() ?: 1, target) * 1.2f
            val gap = 3.dp.toPx(); val width = (size.width - gap * (values.size - 1)) / values.size
            values.forEachIndexed { index, value ->
                val h = value / max * size.height
                drawRect(if (value.toDouble() / target in .9..1.1) EnergyEmerald else EnergyViolet, Offset(index * (width + gap), size.height - h), Size(width, h))
            }
            val targetY = size.height - target / max * size.height
            drawLine(EnergyCyan.copy(.7f), Offset(0f, targetY), Offset(size.width, targetY), 2f)
        }
    }
}

@Composable
private fun BigMetric(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier) { Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary); Text(value, style = MaterialTheme.typography.headlineLarge, color = color) }
}

@Composable
private fun MetricGrid(vararg metrics: Pair<String, String>) {
    metrics.toList().chunked(2).forEachIndexed { rowIndex, row ->
        Row(Modifier.fillMaxWidth()) {
            row.forEach { (label, value) -> Column(Modifier.weight(1f).padding(vertical = 8.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary); Text(value, style = MaterialTheme.typography.titleMedium) } }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        if (rowIndex < metrics.size / 2) HorizontalDivider(color = Hairline)
    }
}

@Composable
private fun WeightDialog(onDismiss: () -> Unit, onSave: (Double, String) -> Unit) {
    var value by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = AngularShape, color = DeepSurface) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("RECORD BODY WEIGHT", style = MaterialTheme.typography.titleLarge)
                AscendTextField(value, { value = it }, "WEIGHT KG", true)
                AscendTextField(note, { note = it }, "OPTIONAL NOTE")
                Text("Daily fluctuations are normal. ASCEND emphasizes trends over single readings.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                SystemButton("RECORD +15 XP", { value.toDoubleOrNull()?.takeIf { it in 25.0..400.0 }?.let { onSave(it, note) } }, Modifier.fillMaxWidth())
            }
        }
    }
}

private fun weightChange(state: DashboardState): String = if (state.weights.size < 2) "—" else "%+.1f KG".format(state.weights.last().weightKg - state.weights.first().weightKg)
private fun averageAdherence(state: DashboardState): Int {
    val target = state.target?.calories ?: return 0
    val active = state.dailySummaries.filter { it.calories > 0 }.takeLast(30)
    return if (active.isEmpty()) 0 else (active.map { com.ascend.app.domain.NutritionEngine.adherence(it.calories, target) }.average() * 100).roundToInt()
}
