package com.ascend.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ascend.app.DashboardState
import com.ascend.app.core.database.UnlockedAchievementEntity
import com.ascend.app.core.datastore.AppPreferences
import com.ascend.app.domain.MealType
import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*

@Composable
fun ProfileSettingsScreen(
    state: DashboardState,
    preferences: AppPreferences,
    unlocked: List<UnlockedAchievementEntity>,
    onBack: () -> Unit,
    onNotification: (String, Boolean) -> Unit,
    onMealSections: (Set<String>) -> Unit,
    onHealthConnect: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                Text("PLAYER PROFILE", style = MaterialTheme.typography.headlineMedium)
            }
        }
        item {
            AscendCard(Modifier.fillMaxWidth(), highlighted = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AscendEmblem(72.dp); Spacer(Modifier.width(16.dp))
                    Column {
                        Text("PLAYER", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(state.profile?.displayName?.uppercase() ?: "PLAYER", style = MaterialTheme.typography.headlineMedium)
                        Text(state.level.rank.title, style = MaterialTheme.typography.labelLarge, color = EnergyCyan)
                    }
                    Spacer(Modifier.weight(1f)); Text(state.level.level.toString(), style = MaterialTheme.typography.displayLarge, color = EnergyViolet)
                }
                Spacer(Modifier.height(16.dp)); XpProgressBar(state.level)
                Spacer(Modifier.height(16.dp))
                Row {
                    ProfileMetric("LIFETIME XP", "%,d".format(state.lifetimeXp), Modifier.weight(1f))
                    ProfileMetric("STREAK", "${state.streak.current} DAYS", Modifier.weight(1f))
                    ProfileMetric("WORKOUTS", state.workoutHistory.count { it.completedAt != null }.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                ProfileMetric("ACHIEVEMENTS", unlocked.size.toString())
            }
        }
        item { SectionHeader("System settings") }
        listOf("PROFILE", "NUTRITION TARGETS", "TRAINING PROGRAM", "NOTIFICATIONS", "UNITS", "THEME", "HEALTH CONNECT", "DATA MANAGEMENT", "ABOUT").forEach { section ->
            item(key = section) {
                AscendCard(Modifier.fillMaxWidth(), onClick = { expanded = if (expanded == section) null else section }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(section, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Outlined.ChevronRight, null, tint = if (expanded == section) EnergyCyan else TextSecondary)
                    }
                    if (expanded == section) {
                        Spacer(Modifier.height(14.dp)); HorizontalDivider(color = Hairline); Spacer(Modifier.height(12.dp))
                        when (section) {
                            "PROFILE" -> Text("${state.profile?.heightCm?.toInt() ?: 0} cm  •  ${state.profile?.currentWeightKg ?: 0.0} kg  •  ${state.profile?.objective?.replace('_', ' ')}\nFocus: ${state.profile?.focusAreas?.replace(',', ' ')?.replace('_', ' ') ?: "—"}\nLimitations: ${state.profile?.injuries?.replace(',', ' ')?.replace('_', ' ') ?: "NONE"}", color = TextSecondary)
                            "NUTRITION TARGETS" -> Text("${state.target?.calories ?: 0} kcal  •  P ${state.target?.proteinGrams ?: 0}  C ${state.target?.carbohydrateGrams ?: 0}  F ${state.target?.fatGrams ?: 0}. Edit targets from Nutrition.", color = TextSecondary)
                            "TRAINING PROGRAM" -> Text("Custom ${state.profile?.workoutFrequency ?: 0}-day protocol. Training weekdays: ${state.profile?.workoutDays ?: "—"}. Unselected days run the recovery protocol. Program start: ${state.profile?.programStartDate ?: "—"}.", color = TextSecondary)
                            "NOTIFICATIONS" -> NotificationSettings(preferences, onNotification)
                            "UNITS" -> Text("Current system: ${state.profile?.unitSystem ?: "METRIC"}. Stored health values use metric units to preserve accuracy.", color = TextSecondary)
                            "THEME" -> Text("VOID is active. CRIMSON, FROST, EMERALD and GOLD are prepared for future releases.", color = TextSecondary)
                            "HEALTH CONNECT" -> HealthConnectSettings(preferences.healthConnectEnabled, onHealthConnect)
                            "DATA MANAGEMENT" -> Text(if (state.profile?.googleAccountEmail != null) "Local-first storage with an opt-in Firebase progress snapshot linked to ${state.profile.googleAccountEmail}. No analytics, ads, or silent health upload." else "All information is stored locally. Google cloud save was not linked; no health data is uploaded.", color = TextSecondary)
                            "ABOUT" -> Text("ASCEND 1.2\nTURN YOUR LIFE INTO A QUEST\nOriginal ASCEND SYSTEM UI. SYSTEM and nutrition outputs are planning guidance, not medical advice.", color = TextSecondary)
                        }
                    }
                }
            }
        }
        item {
            SectionHeader("Meal sections")
            AscendCard(Modifier.fillMaxWidth()) {
                MealType.entries.forEach { meal ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(meal.name in preferences.mealSections, { checked ->
                            val updated = if (checked) preferences.mealSections + meal.name else preferences.mealSections - meal.name
                            onMealSections(updated)
                        })
                        Text(meal.name)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 4.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary); Text(value, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun NotificationSettings(preferences: AppPreferences, onChange: (String, Boolean) -> Unit) {
    Column {
        NotificationToggle("Morning quests", "morning", preferences.morningNotifications, onChange)
        NotificationToggle("Workout protocol", "workout", preferences.workoutNotifications, onChange)
        NotificationToggle("Nutrition objective", "nutrition", preferences.nutritionNotifications, onChange)
        NotificationToggle("Evening report", "evening", preferences.eveningNotifications, onChange)
        Text("Reminders use neutral language and every category can be disabled.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun NotificationToggle(label: String, key: String, checked: Boolean, onChange: (String, Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, { onChange(key, it) }) }
}

@Composable
private fun HealthConnectSettings(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Optional integration layer", Modifier.weight(1f)); Switch(enabled, onChange) }
        Text("When the Health Connect SDK is added, ASCEND can request only selected steps, weight and exercise permissions. Core features remain fully available without it. No health data leaves the device.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}
