package com.ascend.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ascend.app.*
import com.ascend.app.core.database.*
import com.ascend.app.domain.*
import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class PlanExerciseEdit(val templateId: String, val linkId: String?, val name: String, val sets: Int, val min: Int, val max: Int, val remove: Boolean = false)

@Composable
fun TrainingPlanScreen(
    profile: UserProfileEntity?, week: List<TrainingDay>, templates: List<WorkoutTemplateEntity>,
    links: List<WorkoutExerciseEntity>, exercises: List<ExerciseEntity>, busy: Boolean,
    onBack: () -> Unit, onSystem: () -> Unit, onRebuild: (ProgramSettings) -> Unit,
    onSwap: (LocalDate, LocalDate) -> Unit, onEdit: (PlanExerciseEdit) -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var rebuilding by rememberSaveable { mutableStateOf(false) }
    var swapFrom by rememberSaveable { mutableStateOf<String?>(null) }
    var swapTo by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<PlanExerciseEdit?>(null) }
    var entireWeek by rememberSaveable { mutableStateOf(false) }
    val active = templates.filter { it.active && !it.isRecovery }
    val template = active.find { it.id == selectedId } ?: active.firstOrNull()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { ScreenHeader("Training", "YOUR PROTOCOL · YOUR COMMAND",
            leading = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) }
        item {
            AscendCard(Modifier.fillMaxWidth().reveal(), accent = EnergyCyan, highlighted = true) {
                StatusPill("PROTOCOL ONLINE", EnergyCyan)
                Spacer(Modifier.height(14.dp))
                Text("Build strength.\nKeep command.", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(10.dp))
                Text((profile?.workoutFrequency ?: 0).toString() + " days / week · " + (profile?.sessionMinutes ?: 45) + " min budget · " + profile?.equipment.orEmpty().replace('_', ' '), color = TextSecondary)
                Spacer(Modifier.height(16.dp))
                SystemButton("Rebuild protocol", { rebuilding = true }, Modifier.fillMaxWidth(), enabled = !busy)
                TextButton(onClick = onSystem) { Text("Ask SYSTEM to adjust it ↗", color = EnergyCyan) }
            }
        }
        item { SectionHeader("This week", "LIVE MAP") }
        items(if (entireWeek) week else week.filter { it.date >= LocalDate.now() }.take(2), key = { it.date.toString() }) { day ->
            val today = day.date == LocalDate.now()
            AscendCard(Modifier.fillMaxWidth(), accent = if (today) EnergyCyan else EnergyViolet, highlighted = today,
                onClick = if (!busy && !day.completed && day.date >= LocalDate.now()) ({ swapFrom = day.date.toString() }) else null) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(day.date.format(DateTimeFormatter.ofPattern("EEE · d MMM")).uppercase(), style = MaterialTheme.typography.labelMedium, color = EnergyCyan)
                        Spacer(Modifier.height(6.dp))
                        Text(day.template?.name ?: "Awaiting protocol", style = MaterialTheme.typography.titleMedium)
                        Text(if (day.template?.isRecovery == true) "Rest from lifting · keep walking & mobility" else (day.template?.estimatedMinutes?.toString() ?: "—") + " min" + if (day.date >= LocalDate.now() && !day.completed) " · tap to exchange days" else " · history protected", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    if (day.completed) Icon(Icons.Outlined.CheckCircle, "Completed", tint = EnergyEmerald)
                    else if (day.adjusted) StatusPill("SWAPPED", EnergyAmber)
                }
            }
        }
        item {
            TextButton(onClick = { entireWeek = !entireWeek }) { Text(if (entireWeek) "Show upcoming days" else "View all 7 days", color = EnergyCyan) }
            Text("Swaps affect this week only. Completed sessions stay locked; next week keeps your normal schedule.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(12.dp)); SectionHeader("Your sessions")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                active.forEach { item -> FilterChip(selected = template?.id == item.id, onClick = { selectedId = item.id }, label = { Text(item.name) }) }
            }
            Text("Edits create a new version for future unstarted sessions. Started sessions and previous logs stay intact.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        items(links.filter { it.templateId == template?.id }.sortedBy { it.orderIndex }, key = { it.id }) { link ->
            val ex = exercises.find { it.id == link.exerciseId }
            AscendCard(Modifier.fillMaxWidth()) {
                Text(ex?.muscleGroup?.uppercase().orEmpty(), style = MaterialTheme.typography.labelSmall, color = EnergyCyan)
                Text(ex?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                Text(link.targetSets.toString() + " sets × " + link.minReps + "–" + link.maxReps + " reps", color = TextSecondary)
                Row {
                    TextButton(enabled = !busy, onClick = { editing = PlanExerciseEdit(link.templateId, link.id, ex?.name.orEmpty(), link.targetSets, link.minReps, link.maxReps) }) { Text("Edit prescription") }
                    TextButton(enabled = !busy, onClick = { editing = PlanExerciseEdit(link.templateId, link.id, ex?.name.orEmpty(), link.targetSets, link.minReps, link.maxReps, true) }) { Text("Remove", color = EnergyCrimson) }
                }
            }
        }
        item { template?.let { SystemButton("Add exercise", { editing = PlanExerciseEdit(it.id, null, "", 3, 8, 12) }, Modifier.fillMaxWidth(), enabled = !busy, secondary = true) } }
    }
    if (rebuilding && profile != null) ProgramEditor(profile, { rebuilding = false }) { onRebuild(it); rebuilding = false }
    if (swapFrom != null) AlertDialog(onDismissRequest = { swapFrom = null; swapTo = null },
        title = { Text("Exchange this week's days") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Move " + week.find { it.date.toString() == swapFrom }?.template?.name + " from " + swapFrom + " to:")
            val candidates = week.filter { it.date.toString() != swapFrom && it.date >= LocalDate.now() && !it.completed && it.template?.id != week.find { source -> source.date.toString() == swapFrom }?.template?.id }
            if (candidates.isEmpty()) Text("No different uncompleted protocol remains this week. Rebuild your base schedule if you need a lasting change.", color = EnergyAmber)
            candidates.forEach { day ->
                FilterChip(selected = swapTo == day.date.toString(), onClick = { swapTo = day.date.toString() },
                    label = { Text(day.date.dayOfWeek.name + " · " + day.template?.name) })
            }
            Text("The two protocols exchange places. No habits are removed. Recorded workouts cannot be moved.", color = TextSecondary)
        } },
        confirmButton = { TextButton(enabled = swapTo != null && !busy, onClick = { onSwap(LocalDate.parse(swapFrom), LocalDate.parse(swapTo)); swapFrom = null; swapTo = null }) { Text("Confirm exchange") } },
        dismissButton = { TextButton(onClick = { swapFrom = null; swapTo = null }) { Text("Keep schedule") } })
    editing?.let { input -> ExerciseEditor(input, { editing = null }) { onEdit(it); editing = null } }
}

@Composable
private fun ProgramEditor(profile: UserProfileEntity, dismiss: () -> Unit, save: (ProgramSettings) -> Unit) {
    var days by remember { mutableStateOf(profile.workoutDays.split(',').mapNotNull(String::toIntOrNull).map(DayOfWeek::of).toSet()) }
    var focus by remember { mutableStateOf(FocusRules.normalize(profile.focusAreas.split(',').mapNotNull { runCatching { FocusArea.valueOf(it) }.getOrNull() }.toSet())) }
    var split by remember { mutableStateOf(TrainingSplit.valueOf(profile.trainingSplit)) }
    var equipment by remember { mutableStateOf(Equipment.valueOf(profile.equipment)) }
    var experience by remember { mutableStateOf(Experience.valueOf(profile.experience)) }
    var objective by remember { mutableStateOf(Objective.valueOf(profile.objective)) }
    var minutes by remember { mutableIntStateOf(profile.sessionMinutes) }
    var preview by remember { mutableStateOf<CustomPlan?>(null) }
    val editorScroll = rememberScrollState()
    LaunchedEffect(preview) { editorScroll.scrollTo(0) }
    val settings = ProgramSettings(days, focus, split, equipment, experience, objective, minutes)
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (preview == null) "Calibrate your protocol" else "Review your new protocol") },
        text = { Column(Modifier.verticalScroll(editorScroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (preview == null) {
                ChoiceMenu("Goal", objective, Objective.entries) { objective = it }
                ChoiceMenu("Experience", experience, Experience.entries) { experience = it }
                ChoiceMenu("Available equipment", equipment, Equipment.entries) { equipment = it }
                FocusSelector(focus) { focus = it }
                ChoiceMenu("Weekly split", split, TrainingSplit.entries) { split = it }
                Text("Training days · choose 2–6", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { day -> FilterChip(selected = day in days, onClick = { days = if (day in days) days - day else days + day }, label = { Text(day.name.take(3)) }) }
                }
                Text("Time per session", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(30, 45, 60, 75).forEach { value -> FilterChip(selected = value == minutes, onClick = { minutes = value }, label = { Text("$value min") }) }
                }
                Text("Reported injuries are retained. Nutrition targets do not change automatically. Rebuilding replaces future prescriptions and clears this week's swaps.", color = TextSecondary)
            } else {
                preview?.workouts?.filterNot { it.recovery }?.forEach { workout ->
                    Text(workout.name + " · " + workout.estimatedMinutes + " min", style = MaterialTheme.typography.titleSmall, color = EnergyCyan)
                    workout.exercises.forEach { Text(it.name + " · " + it.sets + " × " + it.minReps + "–" + it.maxReps, style = MaterialTheme.typography.bodySmall) }
                }
                preview?.safetyNotes?.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = EnergyAmber) }
            }
        } },
        confirmButton = { TextButton(enabled = days.size in 2..6 && focus.isNotEmpty(), onClick = {
            if (preview != null) save(settings) else preview = CustomPlanEngine.generate(days.size, days, focus,
                profile.injuries.split(',').mapNotNull { runCatching { InjuryArea.valueOf(it) }.getOrNull() }.toSet(),
                equipment, experience, objective, split, minutes)
        }) { Text(if (preview == null) "Generate preview" else "Confirm new protocol") } },
        dismissButton = { TextButton(onClick = { if (preview != null) preview = null else dismiss() }) { Text(if (preview != null) "Edit answers" else "Cancel") } })
}

@Composable
private fun <T : Enum<T>> ChoiceMenu(label: String, selected: T, values: List<T>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column { Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Box { OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selected.name.replace('_', ' ')) }
            DropdownMenu(expanded, { expanded = false }) { values.forEach { value -> DropdownMenuItem(text = { Text(value.name.replace('_', ' ')) }, onClick = { onSelect(value); expanded = false }) } }
        }
    }
}

@Composable
private fun ExerciseEditor(input: PlanExerciseEdit, dismiss: () -> Unit, save: (PlanExerciseEdit) -> Unit) {
    var name by remember { mutableStateOf(input.name) }; var sets by remember { mutableStateOf(input.sets.toString()) }
    var min by remember { mutableStateOf(input.min.toString()) }; var max by remember { mutableStateOf(input.max.toString()) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (input.remove) "Remove exercise?" else "Exercise prescription") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (input.remove) Text(input.name + " will be removed from future sessions. Past logs stay intact.") else {
                OutlinedTextField(name, { name = it.take(80) }, label = { Text("Exercise name") })
                listOf(Triple("Sets", sets, { v: String -> sets = v }), Triple("Min reps", min, { v: String -> min = v }), Triple("Max reps", max, { v: String -> max = v })).forEach { (label, value, update) ->
                    OutlinedTextField(value, { if (it.length <= 3 && it.all(Char::isDigit)) update(it) }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
        } },
        confirmButton = { TextButton(enabled = input.remove || WorkoutInputRules.isValidExercise(name, sets.toIntOrNull(), min.toIntOrNull(), max.toIntOrNull()),
            onClick = { save(input.copy(name = name, sets = sets.toIntOrNull() ?: input.sets, min = min.toIntOrNull() ?: input.min, max = max.toIntOrNull() ?: input.max)) }) { Text(if (input.remove) "Remove" else "Save prescription") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}
