package com.ascend.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.app.AscendApplication
import com.ascend.app.OnboardingProfile
import com.ascend.app.domain.*
import com.ascend.app.ui.components.AngularShape
import com.ascend.app.ui.components.AscendCard
import com.ascend.app.ui.components.SystemButton
import com.ascend.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

private const val LAST_STEP = 16

@Composable
fun OnboardingScreen(externalError: String? = null, onComplete: (OnboardingProfile) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var ageText by rememberSaveable { mutableStateOf("28") }
    var heightText by rememberSaveable { mutableStateOf("175") }
    var weightText by rememberSaveable { mutableStateOf("75") }
    var targetWeightText by rememberSaveable { mutableStateOf("70") }
    var sex by rememberSaveable { mutableStateOf(BiologicalSex.UNSPECIFIED) }
    var units by rememberSaveable { mutableStateOf(UnitSystem.METRIC) }
    var objective by rememberSaveable { mutableStateOf(Objective.GENERAL_HEALTH) }
    var activity by rememberSaveable { mutableStateOf(ActivityLevel.MODERATE) }
    var experience by rememberSaveable { mutableStateOf(Experience.BEGINNER) }
    var equipment by rememberSaveable { mutableStateOf(Equipment.FULL_GYM) }
    var diet by rememberSaveable { mutableStateOf(DietPreference.NON_VEGETARIAN) }
    var trainingTime by rememberSaveable { mutableStateOf(TrainingTime.EVENING) }
    var focusNames by rememberSaveable { mutableStateOf(setOf(FocusArea.CORE.name, FocusArea.LEGS.name)) }
    var injuryNames by rememberSaveable { mutableStateOf(setOf(InjuryArea.NONE.name)) }
    var injuryNotes by rememberSaveable { mutableStateOf("") }
    var frequency by rememberSaveable { mutableIntStateOf(3) }
    var trainingSplit by rememberSaveable { mutableStateOf(TrainingSplit.AUTO) }
    var workoutDayValues by rememberSaveable { mutableStateOf(setOf(1, 3, 5)) }
    var futureVision by rememberSaveable { mutableStateOf("") }
    var coreReason by rememberSaveable { mutableStateOf("") }
    var minimumPromise by rememberSaveable { mutableStateOf("") }
    var calorieText by rememberSaveable { mutableStateOf("") }
    var proteinText by rememberSaveable { mutableStateOf("") }
    var carbsText by rememberSaveable { mutableStateOf("") }
    var fatText by rememberSaveable { mutableStateOf("") }
    var waterText by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var initializationReady by remember { mutableStateOf(false) }

    LaunchedEffect(step) {
        initializationReady = false
        if (step == 15) { delay(4_100); initializationReady = true }
    }

    fun metricValues(): Triple<Double, Double, Double> {
        val height = heightText.toDoubleOrNull() ?: 0.0
        val weight = weightText.toDoubleOrNull() ?: 0.0
        val target = targetWeightText.toDoubleOrNull() ?: 0.0
        return if (units == UnitSystem.METRIC) Triple(height, weight, target)
        else Triple(height * 2.54, weight * .45359237, target * .45359237)
    }

    fun estimate(): NutritionCalculation? = runCatching {
        val (height, weight) = metricValues()
        NutritionEngine.calculate(weight, height, ageText.toInt(), sex, activity, objective)
    }.getOrNull()

    fun profile(): OnboardingProfile? = runCatching {
        val estimated = estimate() ?: error("Profile values are incomplete")
        val manual = NutritionCalculation(
            estimated.bmr, estimated.maintenanceCalories,
            calorieText.toIntOrNull() ?: estimated.calories,
            proteinText.toIntOrNull() ?: estimated.proteinGrams,
            carbsText.toIntOrNull() ?: estimated.carbohydrateGrams,
            fatText.toIntOrNull() ?: estimated.fatGrams,
            waterText.toIntOrNull() ?: estimated.waterMl,
        )
        val (height, weight, target) = metricValues()
        OnboardingProfile(
            name = name.trim(), birthDate = LocalDate.now().minusYears(ageText.toLong()),
            heightCm = height, weightKg = weight, targetWeightKg = target,
            sex = sex, units = units, objective = objective, activity = activity,
            experience = experience, equipment = equipment, diet = diet, trainingTime = trainingTime,
            focusAreas = focusNames.map(FocusArea::valueOf).toSet(),
            injuries = injuryNames.map(InjuryArea::valueOf).toSet(), injuryNotes = injuryNotes.trim(),
            workoutFrequency = frequency, trainingSplit = trainingSplit,
            workoutDays = workoutDayValues.map(DayOfWeek::of).toSet(),
            futureVision = futureVision.trim(), coreReason = coreReason.trim(), minimumPromise = minimumPromise.trim(),
            manualTargets = manual.takeUnless {
                it.calories == estimated.calories &&
                    it.proteinGrams == estimated.proteinGrams &&
                    it.carbohydrateGrams == estimated.carbohydrateGrams &&
                    it.fatGrams == estimated.fatGrams &&
                    it.waterMl == estimated.waterMl
            },
        )
    }.getOrNull()

    fun next() {
        error = null
        when (step) {
            1 -> if (name.isBlank() || estimate() == null || metricValues().third !in 25.0..400.0) {
                error = "Enter a player name and valid age, height, weight, and target weight"; return
            }
            3 -> if (focusNames.isEmpty()) { error = "Select at least one focus area"; return }
            4 -> if (InjuryArea.OTHER.name in injuryNames && injuryNotes.isBlank()) { error = "Describe the limitation so the plan can respond safely"; return }
            9 -> if (workoutDayValues.size != frequency) { error = "Select exactly $frequency workout days"; return }
            12 -> if (listOf(futureVision, coreReason, minimumPromise).any { it.trim().length < 5 }) {
                error = "Give each mindset prompt an honest answer"; return
            }
            13 -> {
                val values = listOf(calorieText, proteinText, carbsText, fatText, waterText).map { it.toIntOrNull() }
                val calories = values[0] ?: 0
                val protein = values[1] ?: 0
                val carbs = values[2] ?: -1
                val fat = values[3] ?: -1
                val water = values[4] ?: 0
                if (values.any { it == null } || !NutritionTargetRules.isValid(calories, protein, carbs, fat, water)) {
                    error = "Use supported targets and keep macro energy within 70–120% of calories"; return
                }
            }
        }
        if (step == 12) estimate()?.let {
            calorieText = it.calories.toString(); proteinText = it.proteinGrams.toString()
            carbsText = it.carbohydrateGrams.toString(); fatText = it.fatGrams.toString(); waterText = it.waterMl.toString()
        }
        step = (step + 1).coerceAtMost(LAST_STEP)
    }

    val titles = listOf(
        "A new campaign", "Create your player", "Choose your main quest", "Choose focus areas",
        "Health and limitations", "Daily activity", "Training experience", "Available equipment",
        "Training frequency", "Choose workout days", "Nutrition preference", "Training window",
        "Mindset calibration", "Nutrition targets", "Your player report", "System initialization",
        "Secure your progress",
    )

    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(EnergyViolet.copy(.14f), Void), radius = 980f))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 18.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AscendEmblem(38.dp)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ASCEND", style = MaterialTheme.typography.titleLarge)
                        Text("PLAYER CREATION", style = MaterialTheme.typography.labelSmall, color = EnergyCyan)
                    }
                    if (step > 0) Text("${step.toString().padStart(2, '0')} / $LAST_STEP", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { if (step == 0) .03f else step / LAST_STEP.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = EnergyViolet,
                    trackColor = Hairline,
                    strokeCap = StrokeCap.Round,
                )
                Spacer(Modifier.height(16.dp))
                Text(titles[step], style = MaterialTheme.typography.headlineLarge)
            }

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 24.dp),
            ) {
                item {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            (slideInHorizontally(tween(330)) { it / 5 * direction } + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(tween(230)) { -it / 7 * direction } + fadeOut(tween(180)))
                        },
                        label = "player_creation",
                    ) { page ->
                    when (page) {
                        0 -> InitializationIntro()
                        1 -> ProfilePage(name, { name = it }, ageText, { ageText = it }, heightText, { heightText = it }, weightText, { weightText = it }, targetWeightText, { targetWeightText = it }, sex, { sex = it }, units, { units = it })
                        2 -> ChoicePage(Objective.entries, objective, { objective = it }, objectiveCopy)
                        3 -> MultiChoicePage(FocusArea.entries, focusNames, 4) { focusNames = it }
                        4 -> InjuryPage(injuryNames, { injuryNames = it }, injuryNotes, { injuryNotes = it })
                        5 -> ChoicePage(ActivityLevel.entries, activity, { activity = it }, activityCopy)
                        6 -> ChoicePage(Experience.entries, experience, { experience = it }) { experienceCopy(it) }
                        7 -> ChoicePage(Equipment.entries, equipment, { equipment = it }) { equipmentCopy(it) }
                        8 -> FrequencyPage(frequency, trainingSplit, { selected ->
                            frequency = selected
                            workoutDayValues = defaultDays(selected)
                        }, { trainingSplit = it })
                        9 -> WorkoutDaysPage(frequency, workoutDayValues) { workoutDayValues = it }
                        10 -> ChoicePage(DietPreference.entries, diet, { diet = it }) { "Food suggestions will respect this path" }
                        11 -> ChoicePage(TrainingTime.entries.filterNot { it == TrainingTime.CUSTOM }, trainingTime, { trainingTime = it }) { "Sets the timing of workout reminders" }
                        12 -> MindsetPage(futureVision, { futureVision = it }, coreReason, { coreReason = it }, minimumPromise, { minimumPromise = it })
                        13 -> NutritionTargetsPage(
                            estimate(),
                            calorieText, { calorieText = NutritionTargetRules.boundedIntegerInput(it, NutritionTargetRules.MAX_CALORIES) },
                            proteinText, { proteinText = NutritionTargetRules.boundedIntegerInput(it, NutritionTargetRules.MAX_PROTEIN) },
                            carbsText, { carbsText = NutritionTargetRules.boundedIntegerInput(it, NutritionTargetRules.MAX_CARBS) },
                            fatText, { fatText = NutritionTargetRules.boundedIntegerInput(it, NutritionTargetRules.MAX_FAT) },
                            waterText, { waterText = NutritionTargetRules.boundedIntegerInput(it, NutritionTargetRules.MAX_WATER_ML) },
                        )
                        14 -> profile()?.let { PlayerReportPage(it) }
                        15 -> SystemInitializationPage(name.ifBlank { "PLAYER" }, profile())
                        else -> profile()?.let { CloudSavePage(it, onComplete) }
                    }
                }
                }
            }

            (error ?: externalError)?.let { message ->
                Text(message, Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = EnergyCrimson, style = MaterialTheme.typography.bodyMedium)
            }
            if (step != LAST_STEP) {
                Surface(
                    modifier = Modifier.navigationBarsPadding(),
                    color = GlassSurface,
                    shadowElevation = 14.dp,
                ) {
                    if (step == 0) {
                        SystemButton("Begin player creation", ::next, Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp))
                    } else {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SystemButton("Back", { step-- }, Modifier.weight(.32f), secondary = true)
                            SystemButton(
                                when (step) { 13 -> "Generate report"; 14 -> "Initialize"; 15 -> "Secure progress"; else -> "Continue" },
                                ::next,
                                Modifier.weight(.68f),
                                enabled = step != 15 || initializationReady,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InitializationIntro() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AscendCard(Modifier.fillMaxWidth(), highlighted = true) {
            Text("A NEW CAMPAIGN IS READY", style = MaterialTheme.typography.titleLarge, color = EnergyCyan)
            Spacer(Modifier.height(10.dp))
            Text("The system will study your objective, schedule, body metrics, experience, equipment, and limitations—then forge a training and nutrition protocol around you.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            listOf("PERSONALIZED TRAINING", "ADAPTIVE NUTRITION", "MENTAL RESOLVE", "DAILY QUESTS").forEach {
                Text("◆  $it", style = MaterialTheme.typography.labelLarge, color = TextSecondary, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        QuoteCard("The first level begins when intention becomes an action.")
    }
}

@Composable
private fun ProfilePage(
    name: String, setName: (String) -> Unit, age: String, setAge: (String) -> Unit,
    height: String, setHeight: (String) -> Unit, weight: String, setWeight: (String) -> Unit,
    target: String, setTarget: (String) -> Unit, sex: BiologicalSex, setSex: (BiologicalSex) -> Unit,
    units: UnitSystem, setUnits: (UnitSystem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceRow(UnitSystem.entries, units, setUnits)
        AscendTextField(name, setName, "PLAYER NAME")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AscendTextField(age, setAge, "AGE", true, Modifier.weight(1f))
            AscendTextField(height, setHeight, if (units == UnitSystem.METRIC) "HEIGHT CM" else "HEIGHT IN", true, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AscendTextField(weight, setWeight, if (units == UnitSystem.METRIC) "WEIGHT KG" else "WEIGHT LB", true, Modifier.weight(1f))
            AscendTextField(target, setTarget, if (units == UnitSystem.METRIC) "TARGET KG" else "TARGET LB", true, Modifier.weight(1f))
        }
        Text("ASCEND currently supports adults 18+. Biological sex is used only for the calorie estimate; all targets remain editable.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        ChoiceRow(BiologicalSex.entries, sex, setSex)
    }
}

@Composable
private fun <T : Enum<T>> ChoicePage(values: List<T>, selected: T, onSelect: (T) -> Unit, copy: (T) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        values.forEach { value ->
            AscendCard(Modifier.fillMaxWidth(), accent = if (value == selected) EnergyCyan else EnergyViolet, highlighted = value == selected, onClick = { onSelect(value) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = value == selected, onClick = { onSelect(value) })
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(value.name.replace('_', ' '), style = MaterialTheme.typography.titleMedium)
                        Text(copy(value), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun <T : Enum<T>> MultiChoicePage(values: List<T>, selectedNames: Set<String>, max: Int, onChange: (Set<String>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Choose up to $max. Your selections influence exercise balance.", color = TextSecondary)
        values.chunked(2).forEach { rowValues ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowValues.forEach { value ->
                    val active = value.name in selectedNames
                    FilterChip(
                        selected = active,
                        onClick = { onChange(if (active) selectedNames - value.name else if (selectedNames.size < max) selectedNames + value.name else selectedNames) },
                        label = { Text(value.name.replace('_', ' '), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        modifier = Modifier.weight(1f), shape = AngularShape,
                    )
                }
                if (rowValues.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        QuoteCard("Specific direction turns effort into progression.")
    }
}

@Composable
private fun InjuryPage(selected: Set<String>, onChange: (Set<String>) -> Unit, notes: String, onNotes: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AscendCard(Modifier.fillMaxWidth(), accent = EnergyAmber) {
            Text("SAFETY CHECK", style = MaterialTheme.typography.labelLarge, color = EnergyAmber)
            Text("Select current injuries, recurring pain, or medical limitations. This adapts exercise choices but is not diagnosis or medical clearance.", color = TextSecondary)
        }
        InjuryArea.entries.chunked(2).forEach { rowValues ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowValues.forEach { value ->
                    val active = value.name in selected
                    FilterChip(
                        selected = active,
                        onClick = {
                            onChange(when {
                                value == InjuryArea.NONE -> setOf(InjuryArea.NONE.name)
                                active -> (selected - value.name).ifEmpty { setOf(InjuryArea.NONE.name) }
                                else -> (selected - InjuryArea.NONE.name) + value.name
                            })
                        },
                        label = { Text(value.name.replace('_', ' '), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        modifier = Modifier.weight(1f), shape = AngularShape,
                    )
                }
                if (rowValues.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        AscendTextField(notes, onNotes, "OPTIONAL DETAILS / MOVEMENTS TO AVOID")
        if (InjuryArea.CARDIOVASCULAR.name in selected) Text("Obtain medical clearance before vigorous exercise.", color = EnergyCrimson, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun FrequencyPage(
    selected: Int,
    split: TrainingSplit,
    onSelect: (Int) -> Unit,
    onSplit: (TrainingSplit) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        (2..6).forEach { days ->
            AscendCard(Modifier.fillMaxWidth(), highlighted = selected == days, accent = EnergyCyan, onClick = { onSelect(days) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$days", style = MaterialTheme.typography.displayMedium, color = if (selected == days) EnergyCyan else TextPrimary)
                    Spacer(Modifier.width(14.dp))
                    Column { Text("DAYS PER WEEK", style = MaterialTheme.typography.titleMedium); Text(frequencyCopy(days), color = TextSecondary) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("TRAINING ARCHITECTURE", style = MaterialTheme.typography.labelLarge, color = EnergyCyan)
        Text("Choose a primary split. The weekly map recalculates instantly for your selected frequency.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        TrainingSplit.entries.forEach { option ->
            AscendCard(
                Modifier.fillMaxWidth(),
                highlighted = split == option,
                accent = if (option == TrainingSplit.FULL_BODY) EnergyCyan else EnergyViolet,
                onClick = { onSplit(option) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = split == option, onClick = { onSplit(option) })
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(option.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(option.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }
        Text("AVAILABLE PROTOCOLS  //  FULL BODY · PUSH · PULL · LEGS · UPPER · LOWER · RECOVERY", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

@Composable
private fun WorkoutDaysPage(required: Int, selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("SELECTED ${selected.size} / $required", style = MaterialTheme.typography.labelLarge, color = if (selected.size == required) EnergyEmerald else EnergyAmber)
        DayOfWeek.entries.chunked(2).forEach { days ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { day ->
                    val active = day.value in selected
                    FilterChip(
                        selected = active,
                        onClick = { onChange(if (active) selected - day.value else if (selected.size < required) selected + day.value else selected) },
                        label = { Text(day.name, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        modifier = Modifier.weight(1f), shape = AngularShape,
                    )
                }
                if (days.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        AscendCard(Modifier.fillMaxWidth(), accent = EnergyEmerald) {
            Text("RECOVERY DAYS", style = MaterialTheme.typography.labelLarge, color = EnergyEmerald)
            Text("Unselected days become recovery quests. The plan rotates only on your selected training days.", color = TextSecondary)
        }
    }
}

@Composable
private fun MindsetPage(vision: String, onVision: (String) -> Unit, reason: String, onReason: (String) -> Unit, promise: String, onPromise: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("These reflective prompts build a clear identity and an implementation intention. There are no perfect answers.", color = TextSecondary)
        PromptField("01", "Imagine 100 honest days have passed. What can that version of you do, feel, or believe that you cannot yet?", vision, onVision)
        PromptField("02", "When motivation disappears, what reason must still be strong enough to make you return?", reason, onReason)
        PromptField("03", "On your worst day, what is the smallest promise you will still keep to yourself?", promise, onPromise)
        QuoteCard("You are not answering for the system. You are giving your future self a command worth remembering.")
    }
}

@Composable
private fun PromptField(number: String, question: String, value: String, onChange: (String) -> Unit) {
    AscendCard(Modifier.fillMaxWidth(), highlighted = value.length >= 5) {
        Text("NEURAL PROMPT $number", style = MaterialTheme.typography.labelMedium, color = EnergyCyan)
        Spacer(Modifier.height(6.dp)); Text(question, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value, { onChange(it.take(240)) }, Modifier.fillMaxWidth(), minLines = 2, placeholder = { Text("TRANSMIT YOUR ANSWER…") }, shape = AngularShape)
    }
}

@Composable
private fun NutritionTargetsPage(
    estimate: NutritionCalculation?, calories: String, setCalories: (String) -> Unit,
    protein: String, setProtein: (String) -> Unit, carbs: String, setCarbs: (String) -> Unit,
    fat: String, setFat: (String) -> Unit, water: String, setWater: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AscendCard(Modifier.fillMaxWidth(), accent = EnergyCyan, highlighted = true) {
            Text("ESTIMATION TRACE", style = MaterialTheme.typography.labelLarge, color = EnergyCyan)
            Spacer(Modifier.height(8.dp))
            Text("BMR  ${estimate?.bmr ?: "—"} KCAL", style = MaterialTheme.typography.titleMedium)
            Text("MAINTENANCE  ${estimate?.maintenanceCalories ?: "—"} KCAL", color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text("Mifflin–St Jeor planning estimates—not medical prescriptions. You control every target.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AscendTextField(calories, setCalories, "CALORIES", true, Modifier.weight(1f))
            AscendTextField(protein, setProtein, "PROTEIN G", true, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AscendTextField(carbs, setCarbs, "CARBS G", true, Modifier.weight(1f))
            AscendTextField(fat, setFat, "FAT G", true, Modifier.weight(1f))
        }
        AscendTextField(water, setWater, "WATER ML", true)
        val waterMl = water.toIntOrNull() ?: 0
        Text(
            if (waterMl > 0) "LIVE CONVERSION  //  ${NutritionTargetRules.waterLabel(waterMl)}" else "ENTER WATER IN ML · MAX 6.00 L",
            style = MaterialTheme.typography.labelMedium,
            color = EnergyCyan,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2_000, 2_500, 3_000, 3_500).forEach { preset ->
                FilterChip(
                    selected = waterMl == preset,
                    onClick = { setWater(preset.toString()) },
                    label = { Text("${preset / 1_000.0} L") },
                    modifier = Modifier.weight(1f),
                    shape = AngularShape,
                )
            }
        }
        MacroBalanceFeedback(calories, protein, carbs, fat)
    }
}

@Composable
internal fun MacroBalanceFeedback(calories: String, protein: String, carbs: String, fat: String) {
    val target = calories.toIntOrNull() ?: 0
    val proteinValue = protein.toIntOrNull() ?: 0
    val carbValue = carbs.toIntOrNull() ?: 0
    val fatValue = fat.toIntOrNull() ?: 0
    val macroEnergy = NutritionTargetRules.macroCalories(proteinValue, carbValue, fatValue)
    val ratio = if (target > 0) macroEnergy.toFloat() / target else 0f
    val total = macroEnergy.coerceAtLeast(1)
    val aligned = target > 0 && ratio in .70f..1.20f
    AscendCard(Modifier.fillMaxWidth(), accent = if (aligned) EnergyEmerald else EnergyAmber, highlighted = aligned) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LIVE MACRO ENERGY", style = MaterialTheme.typography.labelLarge, color = if (aligned) EnergyEmerald else EnergyAmber)
            Spacer(Modifier.weight(1f))
            Text("$macroEnergy KCAL", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (target == 0) "Enter a calorie target to calculate alignment."
            else "${(ratio * 100).roundToInt()}% of the $target kcal target · ${macroEnergy - target} kcal difference",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1.2f) / 1.2f },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = if (aligned) EnergyEmerald else EnergyAmber,
            trackColor = Hairline,
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(9.dp))
        Text(
            "P ${proteinValue * 4 * 100 / total}%  ·  C ${carbValue * 4 * 100 / total}%  ·  F ${fatValue * 9 * 100 / total}%",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun PlayerReportPage(profile: OnboardingProfile) {
    val age = java.time.Period.between(profile.birthDate, LocalDate.now()).years
    val target = profile.manualTargets ?: NutritionEngine.calculate(
        profile.weightKg, profile.heightCm, age, profile.sex, profile.activity, profile.objective,
    )
    val bmi = profile.weightKg / ((profile.heightCm / 100) * (profile.heightCm / 100))
    val plan = remember(profile) { CustomPlanEngine.generate(profile.workoutFrequency, profile.workoutDays, profile.focusAreas, profile.injuries, profile.equipment, profile.experience, profile.objective, profile.trainingSplit) }
    val attributes = listOf(
        "TRAINING READINESS" to when (profile.experience) { Experience.BEGINNER -> .48f; Experience.INTERMEDIATE -> .7f; Experience.ADVANCED -> .86f },
        "ACTIVITY BASE" to when (profile.activity) { ActivityLevel.SEDENTARY -> .3f; ActivityLevel.LIGHT -> .5f; ActivityLevel.MODERATE -> .72f; ActivityLevel.VERY_ACTIVE -> .9f },
        "RECOVERY SPACE" to ((7 - profile.workoutFrequency) / 5f).coerceIn(.2f, 1f),
        "PLAN SPECIFICITY" to (.62f + profile.focusAreas.size * .08f).coerceAtMost(.94f),
    )
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AscendCard(Modifier.fillMaxWidth(), highlighted = true, accent = EnergyEmerald) {
            Text("ANALYSIS COMPLETE", style = MaterialTheme.typography.labelLarge, color = EnergyEmerald)
            Text(profile.name.uppercase(), style = MaterialTheme.typography.displayMedium)
            Text(plan.title, style = MaterialTheme.typography.titleMedium, color = EnergyCyan)
            Text("${profile.workoutFrequency}-DAY MAP · ${profile.trainingSplit.displayName}", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        AscendCard(Modifier.fillMaxWidth()) {
            Text("PLAYER ATTRIBUTE GRAPH", style = MaterialTheme.typography.labelLarge, color = EnergyViolet)
            Spacer(Modifier.height(12.dp)); AttributeGraph(attributes)
        }
        AscendCard(Modifier.fillMaxWidth()) {
            Text("FUEL DISTRIBUTION", style = MaterialTheme.typography.labelLarge, color = EnergyCyan)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MacroGraph(target.proteinGrams, target.carbohydrateGrams, target.fatGrams, Modifier.size(120.dp))
                Spacer(Modifier.width(18.dp))
                Column {
                    ReportMetric("ENERGY", "${target.calories} kcal"); ReportMetric("PROTEIN", "${target.proteinGrams} g")
                    ReportMetric("HYDRATION", "${target.waterMl} ml"); ReportMetric("BMI REFERENCE", "%.1f".format(bmi))
                }
            }
        }
        AscendCard(Modifier.fillMaxWidth(), accent = EnergyCyan) {
            Text("CUSTOM WEEKLY MAP", style = MaterialTheme.typography.labelLarge, color = EnergyCyan)
            Spacer(Modifier.height(8.dp))
            plan.weeklyDays.forEachIndexed { index, day -> Text("${day.name.padEnd(10)}  //  ${plan.workouts[index].name}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 4.dp)) }
            Text("OTHER DAYS  //  RECOVERY PROTOCOL", style = MaterialTheme.typography.labelMedium, color = EnergyEmerald, modifier = Modifier.padding(top = 4.dp))
        }
        if (plan.safetyNotes.isNotEmpty()) AscendCard(Modifier.fillMaxWidth(), accent = EnergyAmber) {
            Text("SAFETY MODIFIERS", style = MaterialTheme.typography.labelLarge, color = EnergyAmber)
            plan.safetyNotes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(top = 6.dp)) }
        }
        QuoteCard("Your plan is not a prediction of who you will be. It is a map for what to do next.")
    }
}

@Composable
private fun AttributeGraph(values: List<Pair<String, Float>>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        values.forEach { (label, value) ->
            val animated by animateFloatAsState(value, tween(900), label = label)
            Column {
                Row { Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary); Spacer(Modifier.weight(1f)); Text("${(value * 100).roundToInt()}", style = MaterialTheme.typography.labelMedium, color = EnergyCyan) }
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(progress = { animated }, Modifier.fillMaxWidth().height(7.dp), color = EnergyCyan, trackColor = Hairline, strokeCap = StrokeCap.Square)
            }
        }
    }
}

@Composable
private fun MacroGraph(protein: Int, carbs: Int, fat: Int, modifier: Modifier = Modifier) {
    val values = listOf(protein * 4f, carbs * 4f, fat * 9f)
    val total = values.sum().coerceAtLeast(1f)
    Canvas(modifier.semantics { contentDescription = "Macro calorie distribution graph" }) {
        var start = -90f
        listOf(EnergyCyan, EnergyAmber, EnergyEmerald).forEachIndexed { index, color ->
            val sweep = values[index] / total * 360f
            drawArc(color, start, sweep - 3f, false, style = Stroke(size.minDimension * .16f, cap = StrokeCap.Butt)); start += sweep
        }
        drawCircle(EnergyViolet.copy(.18f), radius = size.minDimension * .23f)
    }
}

@Composable
private fun ReportMetric(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    Text(value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun SystemInitializationPage(name: String, profile: OnboardingProfile?) {
    val lines = remember(profile) {
        listOf(
            "IDENTITY LOCKED  //  ${name.uppercase()}",
            "OBJECTIVE MATRIX LINKED",
            "${profile?.workoutFrequency ?: 0}-DAY ${profile?.trainingSplit?.displayName ?: "CUSTOM"} PROTOCOL FORGED",
            "QUEST ENGINE ONLINE",
        )
    }
    var visibleLines by remember { mutableIntStateOf(0) }
    LaunchedEffect(lines) { visibleLines = 0; lines.indices.forEach { index -> delay(if (index == 0) 420 else 680); visibleLines = index + 1 } }
    val completion by animateFloatAsState(visibleLines / lines.size.toFloat(), tween(520), label = "initialization_completion")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(136.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(EnergyViolet.copy(.14f)); drawCircle(EnergyCyan, style = Stroke(2.dp.toPx()))
                drawArc(EnergyViolet, -90f, completion * 360f, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
            }
            AscendEmblem(72.dp)
        }
        Text("INITIALIZING PLAYER…", style = MaterialTheme.typography.headlineMedium, color = EnergyCyan)
        AscendCard(Modifier.fillMaxWidth(), highlighted = visibleLines == lines.size, accent = if (visibleLines == lines.size) EnergyCyan else EnergyViolet) {
            lines.forEachIndexed { index, line ->
                AnimatedVisibility(
                    visible = index < visibleLines,
                    enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 3 },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = if (index % 2 == 0) EnergyCyan else EnergyViolet, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        TypewriterText(line, line + index)
                    }
                }
            }
            if (visibleLines < lines.size) Text("▌", color = EnergyCyan, style = MaterialTheme.typography.titleMedium)
        }
        if (visibleLines == lines.size) {
            Text("YOU ARE SET, PLAYER.", style = MaterialTheme.typography.displayMedium, color = EnergyEmerald, textAlign = TextAlign.Center)
            Text("The campaign begins with the next honest action.", color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TypewriterText(text: String, key: Any) {
    var count by remember(key) { mutableIntStateOf(0) }
    LaunchedEffect(key) { count = 0; while (count < text.length) { delay(13); count++ } }
    Text(text.take(count), style = MaterialTheme.typography.labelMedium, color = if (text.startsWith(">")) EnergyCyan else TextSecondary, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun CloudSavePage(profile: OnboardingProfile, onComplete: (OnboardingProfile) -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val app = context.applicationContext as AscendApplication
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AscendCard(Modifier.fillMaxWidth(), highlighted = true, accent = EnergyCyan) {
            Icon(Icons.Outlined.CloudDone, null, tint = EnergyCyan, modifier = Modifier.size(38.dp)); Spacer(Modifier.height(10.dp))
            Text("SAVE THE CAMPAIGN", style = MaterialTheme.typography.titleLarge)
            Text("Sign in with Google to create a private Firebase progress snapshot tied to your account. ASCEND continues to work offline and syncs when the configured cloud service is available.", color = TextSecondary)
        }
        AscendCard(Modifier.fillMaxWidth(), accent = EnergyEmerald) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Security, null, tint = EnergyEmerald); Spacer(Modifier.width(10.dp)); Text("YOUR CONSENT CONTROLS CLOUD SAVE", style = MaterialTheme.typography.labelLarge, color = EnergyEmerald) }
            Spacer(Modifier.height(8.dp)); Text("Health and progress data is sensitive. Google sign-in is optional; local mode keeps it only on this device.", color = TextSecondary)
        }
        SystemButton(
            if (working) "CONTACTING GOOGLE…" else "SIGN IN WITH GOOGLE & SAVE",
            {
                if (activity == null) { resultMessage = "Google sign-in is unavailable in this context."; return@SystemButton }
                working = true; resultMessage = null
                scope.launch {
                    app.cloudProgress.signInAndCreateBackup(activity, profile)
                        .onSuccess { email -> onComplete(profile.copy(googleAccountEmail = email)) }
                        .onFailure { resultMessage = it.message ?: "Google sign-in failed" }
                    working = false
                }
            }, Modifier.fillMaxWidth(), enabled = !working,
        )
        SystemButton("CONTINUE IN PRIVATE OFFLINE MODE", { onComplete(profile) }, Modifier.fillMaxWidth(), secondary = true, enabled = !working)
        resultMessage?.let { Text(it, color = EnergyAmber, style = MaterialTheme.typography.bodyMedium) }
        if (!app.cloudProgress.isConfigured) Text("Developer setup required for live Google save: add app/google-services.json and configure the OAuth web client ID resource. Offline mode is fully operational.", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        QuoteCard("Protect the progress, but never confuse the record with the work. You are the work.")
    }
}

@Composable
private fun QuoteCard(quote: String) {
    AscendCard(Modifier.fillMaxWidth(), accent = EnergyViolet) {
        Text("“$quote”", style = MaterialTheme.typography.bodyLarge); Spacer(Modifier.height(5.dp)); Text("— ASCEND SYSTEM", style = MaterialTheme.typography.labelMedium, color = EnergyViolet)
    }
}

@Composable
private fun <T : Enum<T>> ChoiceRow(values: List<T>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { value ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(value.name.replace('_', ' '), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center) }, modifier = Modifier.weight(1f), shape = AngularShape)
        }
    }
}

@Composable
@SuppressLint("ModifierParameter")
fun AscendTextField(value: String, onValueChange: (String) -> Unit, label: String, numeric: Boolean = false, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value, onValueChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text), shape = AngularShape,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EnergyViolet, unfocusedBorderColor = Hairline, focusedContainerColor = DeepSurface.copy(.72f), unfocusedContainerColor = DeepSurface.copy(.55f)),
    )
}

@Composable
fun AscendEmblem(size: Dp) {
    Canvas(Modifier.size(size)) {
        val p = Path().apply {
            moveTo(this@Canvas.size.width / 2, 0f); lineTo(this@Canvas.size.width, this@Canvas.size.height)
            lineTo(this@Canvas.size.width * .72f, this@Canvas.size.height * .82f); lineTo(this@Canvas.size.width / 2, this@Canvas.size.height * .34f)
            lineTo(this@Canvas.size.width * .28f, this@Canvas.size.height * .82f); lineTo(0f, this@Canvas.size.height); close()
        }
        drawPath(p, Brush.verticalGradient(listOf(EnergyCyan, EnergyViolet)))
    }
}

private fun defaultDays(frequency: Int): Set<Int> = when (frequency) { 2 -> setOf(2, 5); 3 -> setOf(1, 3, 5); 4 -> setOf(1, 2, 4, 5); 5 -> setOf(1, 2, 3, 5, 6); else -> setOf(1, 2, 3, 4, 5, 6) }
private fun frequencyCopy(days: Int) = when (days) { 2 -> "Efficient full-body foundation"; 3 -> "Balanced strength and recovery"; 4 -> "Upper/lower performance split"; 5 -> "Higher-volume hybrid split"; else -> "Advanced high-frequency rotation" }
private fun experienceCopy(value: Experience) = when (value) { Experience.BEGINNER -> "New to structured resistance training"; Experience.INTERMEDIATE -> "Consistent training for 1–3 years"; Experience.ADVANCED -> "Experienced with programming and load management" }
private fun equipmentCopy(value: Equipment) = when (value) { Equipment.FULL_GYM -> "Machines, cables, barbells and dumbbells"; Equipment.DUMBBELLS -> "A dumbbell-only custom movement pool"; Equipment.HOME_GYM -> "Compact gym and free-weight movements"; Equipment.BODYWEIGHT -> "No equipment required" }

private val objectiveCopy: (Objective) -> String = {
    when (it) {
        Objective.FAT_LOSS -> "A moderate, sustainable calorie deficit"; Objective.MUSCLE_GAIN -> "A controlled surplus supporting training"
        Objective.RECOMPOSITION -> "Near-maintenance body composition focus"; Objective.MAINTAIN_FITNESS -> "Preserve performance and current weight"
        Objective.GENERAL_HEALTH -> "Balanced movement, fuel and recovery"; Objective.BUILD_CONSISTENCY -> "Make repeatable actions the first objective"
    }
}
private val activityCopy: (ActivityLevel) -> String = {
    when (it) { ActivityLevel.SEDENTARY -> "Mostly seated outside planned exercise"; ActivityLevel.LIGHT -> "Light movement or training 1–3 days weekly"; ActivityLevel.MODERATE -> "Regular movement or training 3–5 days weekly"; ActivityLevel.VERY_ACTIVE -> "Hard training or physical work most days" }
}
