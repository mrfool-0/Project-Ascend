package com.ascend.app.ui.screens

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ascend.app.WorkoutLaunch
import com.ascend.app.core.database.WorkoutSetEntity
import com.ascend.app.core.database.WorkoutExerciseDetail
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
    var timerEndMillis by rememberSaveable { mutableLongStateOf(0L) }
    var timerClock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var recoveryChecks by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showAddExercise by remember { mutableStateOf(false) }
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
            LinearProgressIndicator({ animatedProgress }, Modifier.fillMaxWidth().height(7.dp), color = EnergyViolet, trackColor = Hairline, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        item {
            AscendCard(Modifier.fillMaxWidth(), accent = EnergyCyan) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, null, tint = EnergyCyan)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("REST TIMER", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(if (timerSeconds > 0) "%02d:%02d".format(timerSeconds / 60, timerSeconds % 60) else "Ready", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.weight(1f))
                    listOf(90, 120, 180).forEach { seconds ->
                        TextButton(onClick = { timerEndMillis = System.currentTimeMillis() + seconds * 1_000L; timerClock = System.currentTimeMillis() }) { Text("${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.labelMedium) }
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
                ExerciseCard(detail, exerciseSets, onUpdateSet, onRemoveExercise)
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
}

@Composable
private fun ExerciseCard(
    detail: WorkoutExerciseDetail,
    sets: List<WorkoutSetEntity>,
    onUpdateSet: (WorkoutSetEntity, Double, Int, Boolean) -> Unit,
    onRemove: (WorkoutExerciseDetail) -> Unit,
) {
    var showMovementCue by rememberSaveable(detail.link.id) { mutableStateOf(false) }
    AscendCard(Modifier.fillMaxWidth(), accent = if (sets.all { it.completed }) EnergyEmerald else EnergyViolet, highlighted = sets.all { it.completed }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(detail.exercise.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { onRemove(detail) }) { Icon(Icons.Outlined.DeleteOutline, "Remove ${detail.exercise.name}", tint = EnergyCrimson) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${sets.size} sets · ${detail.link.minReps}–${detail.link.maxReps} reps", style = MaterialTheme.typography.bodySmall, color = EnergyCyan)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showMovementCue = !showMovementCue }) { Text(if (showMovementCue) "Hide cue" else "Movement cue") }
        }
        AnimatedVisibility(showMovementCue) {
            Column {
                Spacer(Modifier.height(8.dp))
                ExerciseMotionDemo(detail.exercise.name, detail.exercise.muscleGroup)
                Spacer(Modifier.height(12.dp))
            }
        }
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
            Text("SYSTEM SUGGESTION", style = MaterialTheme.typography.labelMedium, color = EnergyAmber)
            Text("Progression available. Consider a small load increase next session.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("ADD EXERCISE", style = MaterialTheme.typography.titleLarge)
                AscendTextField(name, { name = it }, "EXERCISE NAME")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AscendTextField(sets, { sets = it }, "SETS", true, Modifier.weight(1f))
                    AscendTextField(minReps, { minReps = it }, "MIN REPS", true, Modifier.weight(1f))
                    AscendTextField(maxReps, { maxReps = it }, "MAX REPS", true, Modifier.weight(1f))
                }
                SystemButton("ADD TO PROTOCOL", {
                    val input = NewExerciseInput(name, sets.toIntOrNull() ?: 0, minReps.toIntOrNull() ?: 0, maxReps.toIntOrNull() ?: 0)
                    if (input.name.isNotBlank() && input.sets > 0 && input.minReps > 0 && input.maxReps >= input.minReps) onAdd(input)
                }, Modifier.fillMaxWidth())
                TextButton(onClick = onDismiss, Modifier.align(Alignment.End)) { Text("CANCEL") }
            }
        }
    }
}

@Composable
private fun SetInputRow(set: WorkoutSetEntity, onUpdateSet: (WorkoutSetEntity, Double, Int, Boolean) -> Unit) {
    var weight by remember(set.id, set.weightKg) { mutableStateOf(if (set.weightKg == 0.0) "" else set.weightKg.toString()) }
    var reps by remember(set.id, set.reps) { mutableStateOf(if (set.reps == 0) "" else set.reps.toString()) }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(set.setNumber.toString().padStart(2, '0'), Modifier.width(44.dp), style = MaterialTheme.typography.titleMedium, color = if (set.completed) EnergyEmerald else TextPrimary)
        CompactNumberField(weight, { weight = it.filter { c -> c.isDigit() || c == '.' } }, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        CompactNumberField(reps, { reps = it.filter(Char::isDigit) }, Modifier.weight(1f))
        Checkbox(
            checked = set.completed,
            onCheckedChange = { checked -> onUpdateSet(set, weight.toDoubleOrNull() ?: 0.0, reps.toIntOrNull() ?: 0, checked) },
            enabled = set.completed || (weight.toDoubleOrNull() ?: 0.0) >= 0 && (reps.toIntOrNull() ?: 0) > 0,
            colors = CheckboxDefaults.colors(checkedColor = EnergyEmerald, checkmarkColor = Void),
        )
    }
}

@Composable
private fun CompactNumberField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value, onValueChange, modifier.height(50.dp), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EnergyViolet, unfocusedBorderColor = Hairline),
    )
}
