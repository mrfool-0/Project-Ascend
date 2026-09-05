package com.ascend.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ascend.app.domain.CustomPlanEngine
import com.ascend.app.ui.components.AngularShape
import com.ascend.app.ui.screens.*
import com.ascend.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.DayOfWeek

class MainActivity : ComponentActivity() {
    private val viewModel: AscendViewModel by viewModels { AscendViewModel.Factory(application as AscendApplication) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AscendTheme { AscendRoot(viewModel) } }
    }
}

private enum class MainDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "HOME", Icons.Outlined.Home),
    QUESTS("quests", "QUESTS", Icons.Outlined.AutoAwesome),
    NUTRITION("nutrition", "NUTRITION", Icons.Outlined.Restaurant),
    PROGRESS("progress", "PROGRESS", Icons.AutoMirrored.Outlined.ShowChart),
    HABITS("habits", "HABITS", Icons.Outlined.CheckCircleOutline),
    COACH("coach", "COACH", Icons.Outlined.Psychology),
}

@Composable
private fun AscendRoot(viewModel: AscendViewModel) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    when {
        preferences == null -> Box(Modifier.fillMaxSize().background(Void), contentAlignment = Alignment.Center) { Text("SYSTEM INITIALIZING", style = MaterialTheme.typography.labelMedium, color = EnergyCyan) }
        preferences?.onboardingComplete != true -> OnboardingScreen(viewModel::finishOnboarding)
        else -> MainNavigation(viewModel, preferences!!)
    }
}

@Composable
private fun MainNavigation(viewModel: AscendViewModel, preferences: com.ascend.app.core.datastore.AppPreferences) {
    val navController = rememberNavController()
    val state by viewModel.dashboard.collectAsStateWithLifecycle()
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val savedMeals by viewModel.savedMeals.collectAsStateWithLifecycle()
    val logs by viewModel.foodLogs.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val quests by viewModel.quests.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val unlocked by viewModel.unlockedAchievements.collectAsStateWithLifecycle()
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val activeSets by viewModel.activeSets.collectAsStateWithLifecycle()
    val coachMessages by viewModel.coachMessages.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var overlay by remember { mutableStateOf<UiEvent?>(null) }
    val haptic = LocalHapticFeedback.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && listOf(preferences.morningNotifications, preferences.workoutNotifications, preferences.nutritionNotifications, preferences.eveningNotifications).any { it }) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> snackbarHost.showSnackbar(event.text)
                else -> {
                    overlay = event; haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    delay(2_200); overlay = null
                }
            }
        }
    }

    val todayTemplate = remember(state.profile, templates) {
        val profile = state.profile
        if (profile == null) null else {
            val scheduled = profile.workoutDays.split(',').mapNotNull { it.toIntOrNull() }.map(DayOfWeek::of).toSet()
            val index = CustomPlanEngine.templateIndexFor(LocalDate.now(), LocalDate.parse(profile.programStartDate), scheduled)
            templates.firstOrNull { it.rotationIndex == index }
        }
    }
    fun startWorkout() = viewModel.startWorkout { navController.navigate("workout") { launchSingleTop = true } }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Void, androidx.compose.ui.graphics.Color(0xFF070B16), Void)))) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHost) },
            bottomBar = {
                if (route in MainDestination.entries.map { it.route }) AscendBottomBar(route) { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
                }
            },
        ) { padding ->
            NavHost(navController, MainDestination.HOME.route, Modifier.padding(padding)) {
                composable(MainDestination.HOME.route) {
                    HomeScreen(
                        state, todayTemplate, { navController.navigate("profile") }, ::startWorkout,
                        { navController.navigate(MainDestination.NUTRITION.route) }, { navController.navigate(MainDestination.PROGRESS.route) },
                        { navController.navigate(MainDestination.HABITS.route) }, viewModel::addWater, viewModel::toggleHabit,
                    )
                }
                composable(MainDestination.QUESTS.route) { QuestsScreen(state, quests, achievements, unlocked, ::startWorkout) }
                composable(MainDestination.NUTRITION.route) {
                    NutritionScreen(
                        state, logs, foods, savedMeals, preferences.mealSections, viewModel::createAndLogFood, viewModel::logFood,
                        viewModel::deleteFoodLog, viewModel::copyPreviousMeal, viewModel::saveMeal, viewModel::logSavedMeal,
                        viewModel::addWater, viewModel::updateTargets,
                    )
                }
                composable(MainDestination.PROGRESS.route) { ProgressScreen(state, viewModel::logWeight) }
                composable(MainDestination.HABITS.route) { HabitsScreen(state, viewModel::toggleHabit, viewModel::createHabit) }
                composable(MainDestination.COACH.route) { CoachScreen(state, coachMessages, viewModel::sendCoachMessage, viewModel::clearCoachMessages) }
                composable("workout") {
                    WorkoutScreen(activeWorkout, activeSets, { navController.popBackStack() }, viewModel::updateSet, viewModel::addWorkoutExercise, viewModel::removeWorkoutExercise) {
                        viewModel.completeWorkout { navController.popBackStack(MainDestination.HOME.route, false) }
                    }
                }
                composable("profile") {
                    ProfileSettingsScreen(
                        state, preferences, unlocked, { navController.popBackStack() }, viewModel::updateNotification,
                        viewModel::updateMealSections, viewModel::updateHealthConnect,
                    )
                }
            }
        }
        EventOverlay(overlay)
    }
}

@Composable
private fun AscendBottomBar(currentRoute: String?, onSelect: (MainDestination) -> Unit) {
    NavigationBar(containerColor = DeepSurface.copy(alpha = .98f), tonalElevation = 0.dp) {
        MainDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, destination.label) },
                label = { Text(destination.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = EnergyCyan, selectedTextColor = EnergyCyan, indicatorColor = EnergyViolet.copy(.16f), unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary),
            )
        }
    }
}

@Composable
private fun EventOverlay(event: UiEvent?) {
    AnimatedVisibility(event != null, Modifier.fillMaxSize(), enter = fadeIn(tween(180)), exit = fadeOut(tween(300))) {
        Box(Modifier.fillMaxSize().background(Void.copy(.92f)), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AscendEmblem(82.dp); Spacer(Modifier.height(22.dp))
                when (event) {
                    is UiEvent.LevelUp -> {
                        Text("LEVEL UP", style = MaterialTheme.typography.displayMedium, color = EnergyCyan)
                        Text("LEVEL ${event.from}  →  LEVEL ${event.to}", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(10.dp)); Text("A new progression tier has been reached.", color = TextSecondary)
                    }
                    is UiEvent.QuestComplete -> {
                        Text("QUEST COMPLETE", style = MaterialTheme.typography.displayMedium, color = EnergyEmerald, textAlign = TextAlign.Center)
                        Text("+${event.xp} XP", style = MaterialTheme.typography.headlineLarge, color = EnergyCyan)
                    }
                    UiEvent.PersonalRecord -> {
                        Text("NEW RECORD", style = MaterialTheme.typography.displayMedium, color = EnergyAmber)
                        Text("+25 BONUS XP", style = MaterialTheme.typography.titleLarge, color = EnergyCyan)
                    }
                    else -> Unit
                }
            }
        }
    }
}
