package com.ascend.app.ui.screens

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ascend.app.WorkoutLaunch
import com.ascend.app.core.database.WorkoutSetEntity
import com.ascend.app.core.database.WorkoutExerciseDetail
import com.ascend.app.domain.WorkoutInputRules
import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*
import kotlinx.coroutines.delay

data class NewExerciseInput(val name: String, val sets: Int, val minReps: Int, val maxReps: Int)

@Composable
fun WorkoutScreen(
    launch: WorkoutLaunch?,
    sets: List<WorkoutSetEntity>,
    onBack: () -> Unit,
    onUpdateSet: (WorkoutSetEntity, Double, Int, Boolean) -> Unit,
    onAddExercise: (NewExerciseInput) -> Unit,
    onRemoveExercise: (WorkoutExerciseDetail) -> Unit,
    onComplete: () -> Unit,
) {
    if (launch == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("INITIALIZING QUEST…", color = TextSecondary) }
        return
    }
    var timerEndMillis by rememberSaveable(launch.session.id) { mutableLongStateOf(0L) }
    var timerClock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var recoveryChecks by rememberSaveable(launch.session.id) { mutableStateOf(emptyList<String>()) }
    var showAddExercise by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<WorkoutExerciseDetail?>(null) }
    val context = LocalContext.current
    val timerSeconds = ((timerEndMillis - timerClock + 999) / 1_000).toInt().coerceAtLeast(0)
    LaunchedEffect(timerEndMillis) {
        if (timerEndMillis <= System.currentTimeMillis()) return@LaunchedEffect
        while (System.currentTimeMillis() < timerEndMillis) {
            timerClock = System.currentTimeMillis()
            delay(250)
        }
        timerClock = System.currentTimeMillis()
        val vibrator = context.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
    }
    val requiredComplete = if (launch.template.isRecovery) recoveryChecks.size == 5 else sets.isNotEmpty() && sets.all { it.completed }
    val progress = if (launch.template.isRecovery) recoveryChecks.size / 5f else if (sets.isEmpty()) 0f else sets.count { it.completed }.toFloat() / sets.size
    val animatedProgress by animateFloatAsState(progress, tween(450), label = "workout progress")

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeader(
                title = launch.template.name,
                subtitle = "Active quest · +${launch.template.rewardXp} XP",
                leading = { FilledTonalIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                trailing = { StatusPill("${(progress * 100).toInt()}%", if (requiredComplete) EnergyEmerald else EnergyCyan) },
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator({ animatedProgress }, Modifier.fillMaxWidth().height(5.dp), color = EnergyViolet, trackColor = Hairline, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, drawStopIndicator = {})
            Spacer(Modifier.height(8.dp))
            Text(if (launch.template.isRecovery) "Recovery is part of the plan." else "${sets.count { it.completed }} / ${sets.size} sets logged · Your pace. Your progress.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        if (!launch.template.isRecovery) item {
            AscendCard(Modifier.fillMaxWidth().reveal(1), accent = EnergyCyan) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, null, tint = EnergyCyan)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("REST TIMER", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(if (timerSeconds > 0) "%02d:%02d".format(timerSeconds / 60, timerSeconds % 60) else "Ready", style = MaterialTheme.typography.titleLarge)
                    }
                    if (timerSeconds > 0) IconButton(onClick = { timerEndMillis = 0L }) { Icon(Icons.Outlined.Close, "Stop rest timer", tint = TextSecondary) }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(90, 120, 180).forEach { seconds ->
                        SystemButton("${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}", { timerEndMillis = System.currentTimeMillis() + seconds * 1_000L; timerClock = System.currentTimeMillis() }, Modifier.weight(1f), secondary = true)
                    }
                }
            }
        }
        if (launch.template.isRecovery) {
            item {
                SectionHeader("Recovery objectives")
                Spacer(Modifier.height(9.dp))
                AscendCard(Modifier.fillMaxWidth()) {
                    listOf("Complete daily walking target", "10 minutes mobility", "Hit protein goal", "Hit hydration goal", "Respect sleep target").forEachIndexed { index, label ->
                        val key = index.toString()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(key in recoveryChecks, { checked -> recoveryChecks = if (checked) recoveryChecks + key else recoveryChecks - key })
                            Text(label)
                        }
                    }
                }
            }
        } else {
            items(launch.exercises, key = { it.link.id }) { detail ->
                val exerciseSets = sets.filter { it.exerciseId == detail.exercise.id }.sortedBy { it.setNumber }
                Box(Modifier.animateItem()) { ExerciseCard(detail, exerciseSets, onUpdateSet) { pendingRemoval = it } }
            }
            item {
                SystemButton("+ ADD EXERCISE", { showAddExercise = true }, Modifier.fillMaxWidth(), secondary = true)
            }
        }
        item {
            SystemButton("COMPLETE QUEST", onComplete, Modifier.fillMaxWidth(), enabled = requiredComplete)
            AnimatedVisibility(!requiredComplete) {
                Text(
                    if (launch.template.isRecovery) "Complete all safe recovery objectives to close the protocol." else "Complete every working set to unlock the quest reward.",
                    Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                )
            }
        }
    }
    if (showAddExercise) AddExerciseDialog({ showAddExercise = false }) { onAddExercise(it); showAddExercise = false }
    pendingRemoval?.let { detail ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove exercise?") },
            text = { Text("${detail.exercise.name} and its sets will be removed from this session and future uses of this protocol. Past sessions stay intact.") },
            confirmButton = { TextButton(onClick = { onRemoveExercise(detail); pendingRemoval = null }) { Text("Remove", color = EnergyCrimson) } },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Keep exercise") } },
        )
    }
}

@Composable
private fun ExerciseCard(
    detail: WorkoutExerciseDetail,
    sets: List<WorkoutSetEntity>,
    onUpdateSet: (WorkoutSetEntity, Double, Int, Boolean) -> Unit,
    onRemove: (WorkoutExerciseDetail) -> Unit,
) {
    val complete = sets.isNotEmpty() && sets.all { it.completed }
    AscendCard(Modifier.fillMaxWidth().animateContentSize(AscendMotion.spatial()), accent = if (complete) EnergyEmerald else EnergyViolet, highlighted = complete) {
        Text(if (complete) "EXERCISE COMPLETE" else detail.exercise.muscleGroup.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (complete) EnergyEmerald else EnergyViolet)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(detail.exercise.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { onRemove(detail) }) { Icon(Icons.Outlined.DeleteOutline, "Remove ${detail.exercise.name}", tint = TextTertiary, modifier = Modifier.size(20.dp)) }
        }
        Text("${sets.size} ${if (sets.size == 1) "set" else "sets"} · ${detail.link.minReps}–${detail.link.maxReps} reps", style = MaterialTheme.typography.bodySmall, color = EnergyCyan)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("SET", Modifier.width(44.dp), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text("KG", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.width(8.dp))
            Text("REPS", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.width(52.dp))
        }
        sets.forEach { set -> SetInputRow(set, onUpdateSet) }
        if (sets.isNotEmpty() && sets.all { it.completed && it.reps >= detail.link.maxReps }) {
            Spacer(Modifier.height(8.dp))
            Text("TARGET REPS REACHED", style = MaterialTheme.typography.labelMedium, color = EnergyEmerald)
            Text("Every set reached the target. Review your recent training in Progress before deciding on the next load.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun AddExerciseDialog(onDismiss: () -> Unit, onAdd: (NewExerciseInput) -> Unit) {
    var name by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("3") }
    var minReps by remember { mutableStateOf("8") }
    var maxReps by remember { mutableStateOf("12") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = AngularShape, color = DeepSurface, border = androidx.compose.foundation.BorderStroke(1.dp, EnergyViolet)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ADD EXERCISE", style = MaterialTheme.typography.titleLarge)
                AscendTextField(name, { name = it.take(80) }, "EXERCISE NAME")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AscendTextField(sets, { sets = it.filter(Char::isDigit).take(2) }, "SETS", true, Modifier.weight(1f))
                    AscendTextField(minReps, { minReps = it.filter(Char::isDigit).take(3) }, "MIN", true, Modifier.weight(1f))
                    AscendTextField(maxReps, { maxReps = it.filter(Char::isDigit).take(3) }, "MAX", true, Modifier.weight(1f))
                }
                SystemButton("ADD TO PROTOCOL", {
                    val input = NewExerciseInput(name, sets.toIntOrNull() ?: 0, minReps.toIntOrNull() ?: 0, maxReps.toIntOrNull() ?: 0)
                    if (input.name.isNotBlank() && input.sets > 0 && input.minReps > 0 && input.maxReps >= input.minReps) onAdd(input)
                }, Modifier.fillMaxWidth(), enabled = WorkoutInputRules.isValidExercise(name, sets.toIntOrNull(), minReps.toIntOrNull(), maxReps.toIntOrNull()))
                Text("1–10 sets · 1–100 reps. Added to this session and future uses of this protocol.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                TextButton(onClick = onDismiss, Modifier.align(Alignment.End)) { Text("CANCEL") }
            }
        }
    }
}

@Composable
private fun SetInputRow(set: WorkoutSetEntity, onUpdateSet: (WorkoutSetEntity, Double, Int, Boolean) -> Unit) {
    var weight by rememberSaveable(set.id, set.weightKg) { mutableStateOf(if (set.weightKg == 0.0) "" else set.weightKg.toString()) }
    var reps by rememberSaveable(set.id, set.reps) { mutableStateOf(if (set.reps == 0) "" else set.reps.toString()) }
    val parsedWeight = if (weight.isBlank()) 0.0 else weight.toDoubleOrNull()
    val parsedReps = reps.toIntOrNull()
    val valid = WorkoutInputRules.isValidSet(parsedWeight, parsedReps, true)
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(set.setNumber.toString().padStart(2, '0'), Modifier.width(44.dp), style = MaterialTheme.typography.titleMedium, color = if (set.completed) EnergyEmerald else TextPrimary)
        CompactNumberField(weight, { if (WorkoutInputRules.weightInput(it)) weight = it }, "Set ${set.setNumber} weight in kilograms", Modifier.weight(1f), !set.completed, weight.isNotBlank() && (parsedWeight == null || parsedWeight !in 0.0..1500.0))
        Spacer(Modifier.width(8.dp))
        CompactNumberField(reps, { if (it.all(Char::isDigit) && it.length <= 4) reps = it }, "Set ${set.setNumber} repetitions", Modifier.weight(1f), !set.completed, reps.isNotBlank() && (parsedReps == null || parsedReps !in 1..1000), decimal = false)
        Checkbox(
            checked = set.completed,
            onCheckedChange = { checked -> onUpdateSet(set, parsedWeight ?: 0.0, parsedReps ?: 0, checked) },
            enabled = set.completed || valid,
            modifier = Modifier.semantics { contentDescription = if (set.completed) "Unlock set ${set.setNumber} to edit" else "Complete set ${set.setNumber}" },
            colors = CheckboxDefaults.colors(checkedColor = EnergyEmerald, checkmarkColor = Void),
        )
    }
}

@Composable
private fun CompactNumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier, enabled: Boolean, error: Boolean, decimal: Boolean = true) {
    OutlinedTextField(
        value, onValueChange, modifier.heightIn(min = 54.dp).semantics { contentDescription = label }, singleLine = true,
        enabled = enabled, isError = error, shape = CompactShape,
        placeholder = { Text(if (decimal) "0" else "—", color = TextTertiary) },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number, imeAction = ImeAction.Next),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EnergyViolet, unfocusedBorderColor = Hairline,
            disabledBorderColor = EnergyEmerald.copy(.22f), disabledTextColor = TextPrimary,
            disabledContainerColor = EnergyEmerald.copy(.04f),
        ),
    )
}
