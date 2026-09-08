package com.ascend.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ascend.app.DashboardState
import com.ascend.app.core.database.FoodEntity
import com.ascend.app.core.database.FoodLogEntity
import com.ascend.app.core.database.FoodLogWithFood
import com.ascend.app.core.database.SavedMealEntity
import com.ascend.app.domain.MealType
import com.ascend.app.domain.NutritionTargetRules
import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime

data class NewFoodInput(
    val name: String, val servingQuantity: Double, val servingUnit: String,
    val calories: Double, val protein: Double, val carbs: Double, val fat: Double,
    val servings: Double, val meal: MealType,
    val barcode: String? = null,
    val fiber: Double? = null, val sugar: Double? = null,
    val saturatedFat: Double? = null, val sodiumMg: Double? = null,
)

@Composable
fun NutritionScreen(
    state: DashboardState,
    logs: List<FoodLogWithFood>,
    foods: List<FoodEntity>,
    savedMeals: List<SavedMealEntity>,
    enabledSections: Set<String>,
    onCreateAndLog: (NewFoodInput) -> Unit,
    onLogExisting: (FoodEntity, Double, MealType) -> Unit,
    onDeleteLog: (FoodLogEntity) -> Unit,
    onCopyPrevious: (MealType) -> Unit,
    onSaveMeal: (String, List<FoodLogWithFood>) -> Unit,
    onLogSavedMeal: (String, MealType) -> Unit,
    onAddWater: (Int) -> Unit,
    onUpdateTargets: (Int, Int, Int, Int, Int) -> Unit,
) {
    var addMeal by remember { mutableStateOf<MealType?>(null) }
    var editingTargets by remember { mutableStateOf(false) }
    var customWater by remember { mutableStateOf(false) }
    val suggestedMeal = when (LocalTime.now().hour) {
        in 4..10 -> MealType.BREAKFAST
        in 11..15 -> MealType.LUNCH
        in 16..21 -> MealType.DINNER
        else -> MealType.SNACKS
    }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            ScreenHeader(
                title = "Nutrition",
                subtitle = LocalDate.now().toString(),
                trailing = { FilledTonalIconButton(onClick = { editingTargets = true }) { Icon(Icons.Outlined.Edit, "Edit nutrition targets") } },
            )
        }
        item {
            AscendCard(Modifier.fillMaxWidth().reveal(1), highlighted = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TODAY'S ENERGY", style = MaterialTheme.typography.labelMedium, color = EnergyViolet)
                    Spacer(Modifier.weight(1f))
                    StatusPill("Live", EnergyEmerald)
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(state.nutrition.calories.toString(), style = MaterialTheme.typography.displayMedium)
                    Text(" / ${state.target?.calories ?: 0} kcal", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 7.dp))
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacroRing("Protein", state.nutrition.protein, state.target?.proteinGrams ?: 0, EnergyCyan, Modifier.weight(1f))
                    MacroRing("Carbs", state.nutrition.carbs, state.target?.carbohydrateGrams ?: 0, EnergyAmber, Modifier.weight(1f))
                    MacroRing("Fat", state.nutrition.fat, state.target?.fatGrams ?: 0, EnergyEmerald, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutrientMini("Fiber", "${state.nutrition.fiber} g", Modifier.weight(1f))
                    NutrientMini("Sugar", "${state.nutrition.sugar} g", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutrientMini("Sat. fat", "${state.nutrition.saturatedFat} g", Modifier.weight(1f))
                    NutrientMini("Sodium", "${state.nutrition.sodiumMg} mg", Modifier.weight(1f))
                }
            }
        }
        item {
            SystemButton("Add food", { addMeal = suggestedMeal }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(7.dp))
            Text("Adding to ${suggestedMeal.name.lowercase()}. Use a meal section below to log a different meal.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        item {
            SectionHeader("Hydration", "${state.waterMl} / ${state.target?.waterMl ?: 0} ML")
            Spacer(Modifier.height(9.dp))
            AscendCard(Modifier.fillMaxWidth(), accent = EnergyCyan) {
                MacroProgressBar("Water", state.waterMl, state.target?.waterMl ?: 0, "ML", EnergyCyan)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SystemButton("+250 ml", { onAddWater(250) }, Modifier.weight(1f), secondary = true)
                    SystemButton("+500 ml", { onAddWater(500) }, Modifier.weight(1f), secondary = true)
                    SystemButton("Custom", { customWater = true }, Modifier.weight(1f), secondary = true)
                }
            }
        }
        MealType.entries.filter { it.name in enabledSections }.forEach { meal ->
            item(key = "header_${meal.name}") {
                val mealLogs = logs.filter { it.log.mealType == meal.name }
                SectionHeader(meal.name, "${mealLogs.sumOf { it.food.calories * it.log.servings }.toInt()} KCAL")
                Spacer(Modifier.height(8.dp))
                AscendCard(Modifier.fillMaxWidth()) {
                    if (mealLogs.isEmpty()) {
                        Text("NO FOOD LOGGED", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Text("Fuel determines performance.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    } else mealLogs.forEachIndexed { index, entry ->
                        FoodLogRow(entry, onDeleteLog)
                        if (index != mealLogs.lastIndex) HorizontalDivider(color = Hairline)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SystemButton("Add food", { addMeal = meal }, Modifier.weight(1f), secondary = true)
                        if (mealLogs.isNotEmpty()) {
                            TextButton(onClick = { onSaveMeal("${meal.name} ${LocalDate.now()}", mealLogs) }) { Text("SAVE MEAL", style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                    TextButton(onClick = { onCopyPrevious(meal) }, Modifier.align(Alignment.End)) { Text("COPY PREVIOUS MEAL", style = MaterialTheme.typography.labelMedium, color = EnergyCyan) }
                }
            }
        }
    }
    if (addMeal != null) FoodDialog(addMeal!!, foods, savedMeals, { addMeal = null }, onCreateAndLog, onLogExisting, onLogSavedMeal)
    if (editingTargets) TargetDialog(state, { editingTargets = false }, onUpdateTargets)
    if (customWater) NumberDialog("CUSTOM WATER", "MILLILITERS", { customWater = false }) { onAddWater(it); customWater = false }
}

@Composable
private fun FoodLogRow(entry: FoodLogWithFood, onDelete: (FoodLogEntity) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(entry.food.name, style = MaterialTheme.typography.bodyLarge)
            Text("%.1f × %s %s".format(entry.log.servings, formatQuantity(entry.food.servingQuantity), entry.food.servingUnit), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${(entry.food.calories * entry.log.servings).toInt()} KCAL", style = MaterialTheme.typography.titleMedium)
            Text("P ${(entry.food.proteinGrams * entry.log.servings).toInt()}  C ${(entry.food.carbohydrateGrams * entry.log.servings).toInt()}  F ${(entry.food.fatGrams * entry.log.servings).toInt()}", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            if (entry.food.fiberGrams != null || entry.food.sodiumMg != null) Text("FIBER ${((entry.food.fiberGrams ?: 0.0) * entry.log.servings).toInt()}G  NA ${((entry.food.sodiumMg ?: 0.0) * entry.log.servings).toInt()}MG", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        IconButton(onClick = { onDelete(entry.log) }) { Icon(Icons.Outlined.DeleteOutline, "Delete food entry", tint = EnergyCrimson) }
    }
}

@Composable
private fun FoodDialog(
    meal: MealType,
    foods: List<FoodEntity>,
    savedMeals: List<SavedMealEntity>,
    onDismiss: () -> Unit,
    onCreate: (NewFoodInput) -> Unit,
    onExisting: (FoodEntity, Double, MealType) -> Unit,
    onSavedMeal: (String, MealType) -> Unit,
) {
    var tab by remember { mutableIntStateOf(if (foods.isEmpty()) 1 else 0) }
    var servings by remember { mutableStateOf("1") }
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("100") }
    var unit by remember { mutableStateOf("g") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }
    var saturatedFat by remember { mutableStateOf("") }
    var sodium by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = DeepSurface, border = androidx.compose.foundation.BorderStroke(.75.dp, Hairline)) {
            Column(Modifier.padding(24.dp).fillMaxWidth().heightIn(max = 720.dp)) {
                Text("Add to ${meal.name.lowercase().replaceFirstChar(Char::uppercase)}", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, color = EnergyCrimson, style = MaterialTheme.typography.bodySmall)
                }
                PrimaryTabRow(tab, containerColor = Color.Transparent) {
                    Tab(tab == 0, { tab = 0 }, text = { Text("CATALOG") })
                    Tab(tab == 1, { tab = 1 }, text = { Text("CREATE") })
                    Tab(tab == 2, { tab = 2 }, text = { Text("SAVED") })
                }
                Spacer(Modifier.height(12.dp))
                if (tab == 0) {
                    OutlinedTextField(
                        search, { search = it }, Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("Search everyday foods") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = AngularShape,
                    )
                    Spacer(Modifier.height(8.dp))
                    AscendTextField(servings, { servings = it }, "SERVINGS", true)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(foods.filter { search.isBlank() || it.name.contains(search, ignoreCase = true) }, key = { it.id }) { food ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    val amount = servings.toDoubleOrNull()
                                    if (amount != null && amount > 0) { onExisting(food, amount, meal); onDismiss() }
                                }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(food.name)
                                    Text("${food.calories.toInt()} kcal • P ${food.proteinGrams.toInt()} • C ${food.carbohydrateGrams.toInt()} • F ${food.fatGrams.toInt()} / ${formatQuantity(food.servingQuantity)} ${food.servingUnit}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                }
                                Icon(Icons.Outlined.Add, "Add ${food.name}", tint = EnergyCyan)
                            }
                            HorizontalDivider(color = Hairline)
                        }
                    }
                } else if (tab == 1) {
                    Column(
                        Modifier.heightIn(max = 390.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AscendTextField(name, { name = it }, "FOOD NAME")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AscendTextField(quantity, { quantity = it }, "SERVING", true, Modifier.weight(1f))
                            AscendTextField(unit, { unit = it }, "UNIT", false, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AscendTextField(calories, { calories = it }, "KCAL", true, Modifier.weight(1f))
                            AscendTextField(servings, { servings = it }, "SERVINGS", true, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AscendTextField(protein, { protein = it }, "PROTEIN", true, Modifier.weight(1f))
                            AscendTextField(carbs, { carbs = it }, "CARBS", true, Modifier.weight(1f))
                            AscendTextField(fat, { fat = it }, "FAT", true, Modifier.weight(1f))
                        }
                        Text("OPTIONAL LABEL NUTRIENTS", style = MaterialTheme.typography.labelMedium, color = EnergyCyan)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AscendTextField(fiber, { fiber = it }, "FIBER G", true, Modifier.weight(1f))
                            AscendTextField(sugar, { sugar = it }, "SUGAR G", true, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AscendTextField(saturatedFat, { saturatedFat = it }, "SAT FAT G", true, Modifier.weight(1f))
                            AscendTextField(sodium, { sodium = it }, "SODIUM MG", true, Modifier.weight(1f))
                        }
                        SystemButton("CREATE & LOG", {
                            runCatching {
                                NewFoodInput(
                                    name, quantity.toDouble(), unit, calories.toDouble(),
                                    protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0,
                                    fat.toDoubleOrNull() ?: 0.0, servings.toDouble(), meal, null,
                                    fiber.toDoubleOrNull(), sugar.toDoubleOrNull(), saturatedFat.toDoubleOrNull(), sodium.toDoubleOrNull(),
                                )
                            }.onSuccess { onCreate(it); onDismiss() }.onFailure { error = "Enter valid food and serving values" }
                        }, Modifier.fillMaxWidth())
                    }
                } else if (savedMeals.isEmpty()) {
                    Text("NO SAVED MEALS", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Text("Log foods in a meal, then use SAVE MEAL to create a reusable combination.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                } else LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(savedMeals, key = { it.id }) { saved ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onSavedMeal(saved.id, meal); onDismiss() }.padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) { Text(saved.name, style = MaterialTheme.typography.titleMedium); Text("ADD COMPLETE MEAL", style = MaterialTheme.typography.labelMedium, color = TextSecondary) }
                            Icon(Icons.Outlined.Add, "Add ${saved.name}", tint = EnergyCyan)
                        }
                    }
                }
                TextButton(onClick = onDismiss, Modifier.align(Alignment.End)) { Text("CANCEL") }
            }
        }
    }
}

@Composable
private fun TargetDialog(state: DashboardState, onDismiss: () -> Unit, onSave: (Int, Int, Int, Int, Int) -> Unit) {
    val target = state.target ?: return
    var calories by remember { mutableStateOf(target.calories.toString()) }
    var protein by remember { mutableStateOf(target.proteinGrams.toString()) }
    var carbs by remember { mutableStateOf(target.carbohydrateGrams.toString()) }
    var fat by remember { mutableStateOf(target.fatGrams.toString()) }
    var water by remember { mutableStateOf(target.waterMl.toString()) }
    val values = listOf(calories, protein, carbs, fat, water).map { it.toIntOrNull() }
    val valid = values.all { it != null } &&
        NutritionTargetRules.isValid(values[0]!!, values[1]!!, values[2]!!, values[3]!!, values[4]!!)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = DeepSurface, border = androidx.compose.foundation.BorderStroke(.75.dp, Hairline)) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Edit nutrition targets", style = MaterialTheme.typography.headlineMedium)
                Text("Estimates remain visible in your profile. Manual targets take effect immediately.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                AscendTextField(calories, { calories = NutritionTargetRules.boundedIntegerInput(it, NutritionTargetRules.MAX_CALORIES) }, "CALORIES", true)
                AscendTextField(protein, { protein = NutritionTargetRules.boundedIntegerInput(it, NutritionTargetRules.MAX_PROTEIN) }, "PROTEIN G", true)
                AscendTextField(carbs, { carbs = NutritionTargetRules.boundedIntegerInput(it, NutritionTargetRules.MAX_CARBS) }, "CARBS G", true)
                AscendTextField(fat, { fat = NutritionTargetRules.boundedIntegerInput(it, NutritionTargetRules.MAX_FAT) }, "FAT G", true)
                AscendTextField(water, { water = NutritionTargetRules.boundedIntegerInput(it, NutritionTargetRules.MAX_WATER_ML) }, "WATER ML", true)
                Text(
                    values[4]?.let(NutritionTargetRules::waterLabel) ?: "Enter water in milliliters · maximum 6.00 L",
                    style = MaterialTheme.typography.labelMedium,
                    color = EnergyCyan,
                )
                MacroBalanceFeedback(calories, protein, carbs, fat)
                if (!valid) Text("Calories 1,000–8,000 · protein 20–400 g · carbs 0–1,000 g · fat 20–250 g · water 0.5–6.0 L. Macro energy must remain within 70–120% of calories.", style = MaterialTheme.typography.bodySmall, color = EnergyAmber)
                SystemButton(
                    "Save targets",
                    { onSave(values[0]!!, values[1]!!, values[2]!!, values[3]!!, values[4]!!); onDismiss() },
                    Modifier.fillMaxWidth(),
                    enabled = valid,
                )
                TextButton(onClick = onDismiss, Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel") }
            }
        }
    }
}

@Composable
fun NumberDialog(title: String, label: String, onDismiss: () -> Unit, onSubmit: (Int) -> Unit) {
    var value by remember { mutableStateOf("") }
    val amount = value.toIntOrNull()?.takeIf { it in 1..2_000 }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = DeepSurface, border = androidx.compose.foundation.BorderStroke(.75.dp, Hairline)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title.lowercase().replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.headlineMedium)
                AscendTextField(value, { value = it.filter(Char::isDigit) }, label, true)
                Text("Enter 1–2,000 ml.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                SystemButton("Confirm", { amount?.let(onSubmit) }, Modifier.fillMaxWidth(), enabled = amount != null)
                TextButton(onClick = onDismiss, Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel") }
            }
        }
    }
}

private fun formatQuantity(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

@Composable
private fun NutrientMini(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.small, color = Void.copy(.38f), border = androidx.compose.foundation.BorderStroke(.5.dp, Hairline)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 9.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
    }
}
