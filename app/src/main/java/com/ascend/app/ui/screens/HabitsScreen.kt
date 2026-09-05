package com.ascend.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ascend.app.DashboardState
import com.ascend.app.core.database.HabitEntity
import com.ascend.app.domain.HabitDifficulty
import com.ascend.app.domain.HabitFrequency
import com.ascend.app.domain.HabitType
import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*

data class NewHabitInput(val name: String, val type: HabitType, val target: Double, val unit: String, val difficulty: HabitDifficulty, val frequency: HabitFrequency)

@Composable
fun HabitsScreen(state: DashboardState, onToggle: (HabitEntity, Boolean) -> Unit, onCreate: (NewHabitInput) -> Unit) {
    var showCreate by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("DISCIPLINE PROTOCOL", style = MaterialTheme.typography.headlineMedium)
                    Text("CONSISTENCY CREATES POWER", style = MaterialTheme.typography.labelMedium, color = EnergyCyan)
                }
                Spacer(Modifier.weight(1f))
                FilledIconButton(onClick = { showCreate = true }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = EnergyViolet)) {
                    Icon(Icons.Outlined.Add, "Create habit")
                }
            }
        }
        item {
            AscendCard(Modifier.fillMaxWidth(), accent = EnergyAmber, highlighted = state.streak.current >= 7) {
                StreakIndicator(state.streak.current, state.streak.longest)
                Spacer(Modifier.height(14.dp))
                MacroProgressBar("Today's discipline", state.habitCompletions.size, state.habits.size, "HABITS", EnergyEmerald)
                Spacer(Modifier.height(8.dp))
                Text("Habit XP is capped at 75 per day to keep progression fair.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
        item { SectionHeader("Active protocols", "${state.habits.size}") }
        if (state.habits.isEmpty()) item { EmptyState("No habits", "Build discipline one protocol at a time.", "Create habit", onAction = { showCreate = true }) }
        else items(state.habits, key = { it.id }) { habit ->
            val complete = habit.id in state.habitCompletions
            AscendCard(Modifier.fillMaxWidth(), accent = if (complete) EnergyEmerald else EnergyViolet, highlighted = complete) {
                HabitRow(habit, complete) { onToggle(habit, it) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(habit.type.replace('_', ' '), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(habit.frequency, style = MaterialTheme.typography.labelMedium, color = EnergyCyan)
                }
            }
        }
    }
    if (showCreate) NewHabitDialog({ showCreate = false }) { onCreate(it); showCreate = false }
}

@Composable
private fun NewHabitDialog(onDismiss: () -> Unit, onCreate: (NewHabitInput) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(HabitType.CHECKBOX) }
    var difficulty by remember { mutableStateOf(HabitDifficulty.NORMAL) }
    var frequency by remember { mutableStateOf(HabitFrequency.EVERY_DAY) }
    var target by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("done") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = AngularShape, color = DeepSurface, border = androidx.compose.foundation.BorderStroke(1.dp, EnergyViolet.copy(.6f))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("CREATE HABIT", style = MaterialTheme.typography.titleLarge)
                AscendTextField(name, { name = it }, "HABIT NAME")
                Text("TRACKING TYPE", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                EnumDropdown(HabitType.entries, type) { type = it; if (it == HabitType.DURATION) unit = "min" else if (it == HabitType.NUMBER) unit = "count" else unit = "done" }
                if (type == HabitType.NUMBER || type == HabitType.DURATION) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AscendTextField(target, { target = it }, "TARGET", true, Modifier.weight(1f))
                        AscendTextField(unit, { unit = it }, "UNIT", false, Modifier.weight(1f))
                    }
                }
                Text("DIFFICULTY", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                EnumDropdown(HabitDifficulty.entries, difficulty) { difficulty = it }
                Text("FREQUENCY", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                EnumDropdown(HabitFrequency.entries, frequency) { frequency = it }
                Text("Reward: +${difficulty.xp} XP", color = EnergyCyan, style = MaterialTheme.typography.labelLarge)
                SystemButton("CREATE PROTOCOL", {
                    val amount = if (type == HabitType.CHECKBOX || type == HabitType.AVOIDANCE) 1.0 else target.toDoubleOrNull()
                    if (name.isNotBlank() && amount != null && amount > 0) onCreate(NewHabitInput(name, type, amount, unit, difficulty, frequency))
                }, Modifier.fillMaxWidth())
                TextButton(onClick = onDismiss, Modifier.align(Alignment.End)) { Text("CANCEL") }
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
            selected.name.replace('_', ' '), {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, shape = AngularShape,
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            values.forEach { item -> DropdownMenuItem({ Text(item.name.replace('_', ' ')) }, { onSelect(item); expanded = false }) }
        }
    }
}
