package com.ascend.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ascend.app.DashboardState
import com.ascend.app.core.database.HabitEntity
import com.ascend.app.domain.HabitDifficulty
import com.ascend.app.domain.HabitFrequency
import com.ascend.app.domain.HabitType
import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*

data class NewHabitInput(
    val name: String,
    val type: HabitType,
    val target: Double,
    val unit: String,
    val difficulty: HabitDifficulty,
    val frequency: HabitFrequency,
)

@Composable
fun HabitsScreen(
    state: DashboardState,
    allHabits: List<HabitEntity>,
    onToggle: (HabitEntity, Boolean) -> Unit,
    onCreate: (NewHabitInput) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    val scheduledIds = state.habits.mapTo(mutableSetOf()) { it.id }
    val completed = state.habitCompletions.size
    val planned = state.habits.size

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            ScreenHeader(
                title = "Habits",
                subtitle = "Small actions, repeated",
                trailing = {
                    FilledIconButton(onClick = { showCreate = true }) { Icon(Icons.Outlined.Add, "Create habit") }
                },
            )
        }
        item {
            AscendCard(Modifier.fillMaxWidth().reveal(1), accent = EnergyEmerald, highlighted = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StreakIndicator(state.streak.current, state.streak.longest, Modifier.weight(1f))
                    StatusPill(if (planned == 0) "No habits today" else "$completed of $planned", if (completed == planned && planned > 0) EnergyEmerald else EnergyCyan)
                }
                Spacer(Modifier.height(16.dp))
                MacroProgressBar("Today's rhythm", completed, planned.coerceAtLeast(1), "", EnergyEmerald)
                Spacer(Modifier.height(9.dp))
                Text("Aim for repeatable, not perfect. Daily habit XP is capped at 75.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        item { SectionHeader("Your habits", "${allHabits.size} active") }
        if (allHabits.isEmpty()) {
            item { EmptyState("Build your first habit", "Choose one action small enough to keep on a hard day.", "Create a habit", { showCreate = true }) }
        } else {
            items(allHabits, key = { it.id }) { habit ->
                val scheduled = habit.id in scheduledIds
                val complete = habit.id in state.habitCompletions
                Box(Modifier.animateItem()) { HabitPanel(habit, scheduled, complete) { if (scheduled) onToggle(habit, it) } }
            }
        }
    }
    if (showCreate) NewHabitDialog({ showCreate = false }) {
        onCreate(it)
        showCreate = false
    }
}

@Composable
private fun HabitPanel(habit: HabitEntity, scheduled: Boolean, complete: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = RaisedSurface.copy(.74f),
        border = androidx.compose.foundation.BorderStroke(.75.dp, if (complete) EnergyEmerald.copy(.35f) else Hairline),
        onClick = { if (scheduled) onToggle(!complete) },
        enabled = scheduled,
    ) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = complete,
                onCheckedChange = { if (scheduled) onToggle(it) },
                enabled = scheduled,
                colors = CheckboxDefaults.colors(checkedColor = EnergyEmerald, checkmarkColor = Void),
            )
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium, color = if (scheduled) TextPrimary else TextSecondary)
                Text(
                    "${habit.target.clean()} ${habit.unit} · ${habit.frequency.readable()} · +${HabitDifficulty.valueOf(habit.difficulty).xp} XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            StatusPill(if (complete) "Done" else if (scheduled) "Today" else "Rest", if (complete) EnergyEmerald else if (scheduled) EnergyCyan else TextTertiary)
        }
    }
}

@Composable
private fun NewHabitDialog(onDismiss: () -> Unit, onCreate: (NewHabitInput) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(HabitType.CHECKBOX) }
    var difficulty by remember { mutableStateOf(HabitDifficulty.NORMAL) }
    var frequency by remember { mutableStateOf(HabitFrequency.EVERY_DAY) }
    var target by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("done") }
    val measurable = type == HabitType.NUMBER || type == HabitType.DURATION
    val valid = name.trim().length >= 3 && (!measurable || (target.toDoubleOrNull() ?: 0.0) > 0) && unit.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = DeepSurface, border = androidx.compose.foundation.BorderStroke(.75.dp, Hairline)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("Create a habit", style = MaterialTheme.typography.headlineMedium)
                Text("Make the action concrete enough to know when it is done.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                AscendTextField(name, { name = it }, "Habit name")
                Text("Tracking", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                EnumDropdown(HabitType.entries, type) {
                    type = it
                    unit = when (it) { HabitType.DURATION -> "min"; HabitType.NUMBER -> "count"; else -> "done" }
                }
                AnimatedVisibility(measurable) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AscendTextField(target, { target = it }, "Target", true, Modifier.weight(1f))
                        AscendTextField(unit, { unit = it }, "Unit", false, Modifier.weight(1f))
                    }
                }
                Text("Frequency", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                EnumDropdown(HabitFrequency.entries.filterNot { it == HabitFrequency.CUSTOM }, frequency) { frequency = it }
                Text("Difficulty", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                EnumDropdown(HabitDifficulty.entries, difficulty) { difficulty = it }
                StatusPill("${difficulty.xp} XP per completion", EnergyCyan)
                SystemButton(
                    "Create habit",
                    {
                        onCreate(
                            NewHabitInput(
                                name.trim(),
                                type,
                                if (measurable) target.toDouble() else 1.0,
                                unit.trim(),
                                difficulty,
                                frequency,
                            ),
                        )
                    },
                    Modifier.fillMaxWidth(),
                    enabled = valid,
                )
                TextButton(onClick = onDismiss, Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Enum<T>> EnumDropdown(values: List<T>, selected: T, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            selected.name.readable(),
            {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = MaterialTheme.shapes.medium,
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            values.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name.readable()) },
                    onClick = { onSelect(item); expanded = false },
                )
            }
        }
    }
}

private fun String.readable() = lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
private fun Double.clean() = if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
