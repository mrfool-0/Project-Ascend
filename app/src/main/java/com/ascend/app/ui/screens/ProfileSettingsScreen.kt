package com.ascend.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ascend.app.DashboardState
import com.ascend.app.core.database.UnlockedAchievementEntity
import com.ascend.app.core.database.WorkoutTemplateEntity
import com.ascend.app.core.datastore.AppPreferences
import com.ascend.app.domain.MealType
import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek

@Composable
fun ProfileSettingsScreen(
    state: DashboardState,
    templates: List<WorkoutTemplateEntity>,
    preferences: AppPreferences,
    unlocked: List<UnlockedAchievementEntity>,
    onBack: () -> Unit,
    onNotification: (String, Boolean) -> Unit,
    onMealSections: (Set<String>) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenQuests: () -> Unit,
    onProfileImage: (String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageError by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { persistProfileImage(context, uri) } }
                .onSuccess { path ->
                    preferences.profileImagePath?.takeIf { it != path }?.let { runCatching { File(it).delete() } }
                    onProfileImage(path)
                    imageError = null
                }
                .onFailure { imageError = it.message ?: "Unable to save that image" }
        }
    }
    val profile = state.profile
    val recoveryTemplateIds = remember(templates) { templates.filter { it.isRecovery }.mapTo(mutableSetOf()) { it.id } }
    val workoutCount = state.workoutHistory.count { it.completedAt != null && it.templateId !in recoveryTemplateIds }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            ScreenHeader(
                title = "Profile",
                subtitle = "Player command center",
                leading = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
            )
        }

        item {
            AscendCard(Modifier.fillMaxWidth(), highlighted = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        PlayerAvatar(preferences.profileImagePath, size = 92.dp)
                        FilledIconButton(
                            onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.align(Alignment.BottomEnd).size(32.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = EnergyViolet, contentColor = Color.White),
                        ) { Icon(Icons.Outlined.Edit, "Change profile image", Modifier.size(16.dp)) }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile?.displayName ?: "Player", style = MaterialTheme.typography.headlineMedium)
                        Text(state.level.rank.title, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        StatusPill(
                            if (profile?.googleAccountEmail != null) "Cloud linked" else "Local profile",
                            if (profile?.googleAccountEmail != null) EnergyEmerald else EnergyCyan,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${state.level.level}", style = MaterialTheme.typography.displayMedium, color = EnergyViolet)
                        Text("LEVEL", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
                imageError?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = EnergyCrimson, style = MaterialTheme.typography.bodySmall)
                }
                if (preferences.profileImagePath != null) {
                    TextButton(
                        onClick = {
                            preferences.profileImagePath.let { runCatching { File(it).delete() } }
                            onProfileImage(null)
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Remove photo", color = TextSecondary) }
                }
                Spacer(Modifier.height(16.dp))
                XpProgressBar(state.level)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Lifetime XP", "%,d".format(state.lifetimeXp), Modifier.weight(1f), EnergyCyan)
                MetricTile("Streak", "${state.streak.current} days", Modifier.weight(1f), EnergyAmber, "Best ${state.streak.longest}")
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Workouts", workoutCount.toString(), Modifier.weight(1f), EnergyEmerald)
                MetricTile("Achievements", unlocked.size.toString(), Modifier.weight(1f), EnergyViolet)
            }
        }

        item {
            SectionHeader("Explore")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileShortcut("Progress", "Charts & trends", Icons.Outlined.QueryStats, EnergyCyan, onOpenProgress, Modifier.weight(1f))
                ProfileShortcut("Quest ledger", "Goals & badges", Icons.Outlined.MilitaryTech, EnergyViolet, onOpenQuests, Modifier.weight(1f))
            }
        }

        item {
            SectionHeader("Your plan")
            Spacer(Modifier.height(10.dp))
            AscendCard(Modifier.fillMaxWidth()) {
                PlanLine(Icons.Outlined.Flag, "Goal", profile?.objective?.humanize() ?: "Not set")
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Hairline.copy(.65f))
                PlanLine(Icons.Outlined.FitnessCenter, "Training", "${profile?.workoutFrequency ?: 0} days · ${formatWorkoutDays(profile?.workoutDays)}")
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Hairline.copy(.65f))
                PlanLine(Icons.Outlined.CenterFocusStrong, "Focus", profile?.focusAreas?.humanizeList() ?: "Balanced")
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Hairline.copy(.65f))
                PlanLine(Icons.Outlined.HealthAndSafety, "Considerations", profile?.injuries?.humanizeList()?.ifBlank { "None reported" } ?: "None reported")
            }
        }

        item {
            SectionHeader("Preferences")
            Spacer(Modifier.height(10.dp))
            SettingsGroup {
                SettingsExpandableRow(
                    key = "notifications",
                    expanded = expanded == "notifications",
                    onToggle = { expanded = if (expanded == "notifications") null else "notifications" },
                    icon = Icons.Outlined.NotificationsNone,
                    title = "Notifications",
                    subtitle = "Choose which reminders can reach you",
                ) {
                    NotificationSettings(preferences, onNotification)
                }
                SettingsDivider()
                SettingsExpandableRow(
                    key = "meals",
                    expanded = expanded == "meals",
                    onToggle = { expanded = if (expanded == "meals") null else "meals" },
                    icon = Icons.Outlined.RestaurantMenu,
                    title = "Meal sections",
                    subtitle = "Personalize your nutrition log",
                ) {
                    MealSectionSettings(preferences.mealSections, onMealSections)
                }
                SettingsDivider()
                SettingsInfoRow(Icons.Outlined.Straighten, "Units", profile?.unitSystem?.lowercase()?.replaceFirstChar(Char::uppercase) ?: "Metric")
            }
        }

        item {
            SectionHeader("Account & privacy")
            Spacer(Modifier.height(10.dp))
            SettingsGroup {
                SettingsInfoRow(
                    if (profile?.googleAccountEmail != null) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                    "Progress storage",
                    profile?.googleAccountEmail ?: "Stored only on this device",
                    if (profile?.googleAccountEmail != null) EnergyEmerald else EnergyCyan,
                )
                SettingsDivider()
                SettingsInfoRow(Icons.Outlined.HealthAndSafety, "Health Connect", "Not connected", TextTertiary)
                SettingsDivider()
                SettingsExpandableRow(
                    key = "privacy",
                    expanded = expanded == "privacy",
                    onToggle = { expanded = if (expanded == "privacy") null else "privacy" },
                    icon = Icons.Outlined.Shield,
                    title = "Data & privacy",
                    subtitle = "Local-first by default",
                ) {
                    Text(
                        "Your profile, workouts, food log and chat history remain on this device. A compact progress snapshot is uploaded only after you explicitly link Google. Food photos and chat context are sent to Firebase AI only when you use those features.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                SettingsDivider()
                SettingsExpandableRow(
                    key = "about",
                    expanded = expanded == "about",
                    onToggle = { expanded = if (expanded == "about") null else "about" },
                    icon = Icons.Outlined.Info,
                    title = "About ASCEND",
                    subtitle = "Version 1.4 · native Android",
                ) {
                    Text(
                        "Turn your life into a quest. SYSTEM and nutrition estimates provide planning guidance and do not replace qualified medical care.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                SettingsDivider()
                SettingsExpandableRow(
                    key = "licenses",
                    expanded = expanded == "licenses",
                    onToggle = { expanded = if (expanded == "licenses") null else "licenses" },
                    icon = Icons.Outlined.Code,
                    title = "Open-source notices",
                    subtitle = "Libraries, fonts and licenses",
                ) {
                    Text(
                        "AndroidX, Jetpack Compose, Kotlin and Coil are used under Apache License 2.0. Manrope and JetBrains Mono are used under the SIL Open Font License 1.1. Full font license texts are bundled with ASCEND; complete attribution is included in the project's THIRD_PARTY_NOTICES.md.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileShortcut(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(118.dp),
        shape = MaterialTheme.shapes.large,
        color = RaisedSurface.copy(.82f),
        border = androidx.compose.foundation.BorderStroke(.75.dp, Hairline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row {
                Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(.12f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(20.dp), tint = color)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.ArrowOutward, null, Modifier.size(18.dp), tint = TextTertiary)
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun PlanLine(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(EnergyViolet.copy(.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = EnergyViolet)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(RaisedSurface.copy(.7f)).border(.75.dp, Hairline, MaterialTheme.shapes.large),
        content = content,
    )
}

@Composable
private fun SettingsDivider() = HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Hairline.copy(.7f))

@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String, value: String, accent: Color = TextSecondary) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(21.dp), tint = accent)
        Spacer(Modifier.width(13.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, modifier = Modifier.widthIn(max = 190.dp))
    }
}

@Composable
private fun SettingsExpandableRow(
    key: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, spring(stiffness = 600f), label = "$key chevron")
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(21.dp), tint = EnergyCyan)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Outlined.ChevronRight, null, Modifier.rotate(rotation), tint = TextTertiary)
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(start = 50.dp, end = 16.dp, bottom = 16.dp)) { content() }
        }
    }
}

@Composable
private fun NotificationSettings(preferences: AppPreferences, onChange: (String, Boolean) -> Unit) {
    Column {
        NotificationToggle("Morning plan", "morning", preferences.morningNotifications, onChange)
        NotificationToggle("Workout", "workout", preferences.workoutNotifications, onChange)
        NotificationToggle("Nutrition", "nutrition", preferences.nutritionNotifications, onChange)
        NotificationToggle("Evening review", "evening", preferences.eveningNotifications, onChange)
    }
}

@Composable
private fun NotificationToggle(label: String, key: String, checked: Boolean, onChange: (String, Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked, { onChange(key, it) })
    }
}

@Composable
private fun MealSectionSettings(enabled: Set<String>, onChange: (Set<String>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MealType.entries.forEach { meal ->
            val checked = meal.name in enabled
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(meal.name.lowercase().replaceFirstChar(Char::uppercase), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Checkbox(checked, { selected -> onChange(if (selected) enabled + meal.name else enabled - meal.name) })
            }
        }
    }
}

private fun persistProfileImage(context: Context, uri: Uri): String {
    val destination = File(context.filesDir, "player_profile_${System.currentTimeMillis()}.image")
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count <= 0) break
                    total += count
                    require(total <= 12L * 1024 * 1024) { "Choose an image smaller than 12 MB" }
                    output.write(buffer, 0, count)
                }
                require(total > 0) { "The selected image is empty" }
            }
        } ?: error("The selected image could not be read")
        return destination.absolutePath
    } catch (failure: Throwable) {
        destination.delete()
        throw failure
    }
}

private fun String.humanize(): String = lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)

private fun String.humanizeList(): String = split(',').filter(String::isNotBlank).joinToString(" · ") { it.humanize() }

private fun formatWorkoutDays(raw: String?): String {
    val names = raw?.split(',')?.mapNotNull(String::toIntOrNull)?.mapNotNull { runCatching { DayOfWeek.of(it) }.getOrNull() }
        ?.map { it.name.take(3).lowercase().replaceFirstChar(Char::uppercase) }.orEmpty()
    return names.ifEmpty { listOf("Schedule pending") }.joinToString(" · ")
}
