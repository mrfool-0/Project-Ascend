package com.ascend.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import com.ascend.app.ui.components.QuestLaunchOverlay
import com.ascend.app.ui.components.SystemAvatar
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent { AscendTheme { AscendRoot(viewModel) } }
    }
}

private enum class MainDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "HOME", Icons.Outlined.Home),
    NUTRITION("nutrition", "NUTRITION", Icons.Outlined.Restaurant),
    HABITS("habits", "HABITS", Icons.Outlined.CheckCircleOutline),
    SYSTEM("system", "SYSTEM", Icons.Outlined.SmartToy),
}

@Composable
private fun AscendRoot(viewModel: AscendViewModel) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val onboardingError by viewModel.onboardingError.collectAsStateWithLifecycle()
    when {
        preferences == null -> Box(Modifier.fillMaxSize().background(Void), contentAlignment = Alignment.Center) { Text("SYSTEM INITIALIZING", style = MaterialTheme.typography.labelMedium, color = EnergyCyan) }
        preferences?.onboardingComplete != true -> OnboardingScreen(onboardingError, viewModel::finishOnboarding)
        else -> MainNavigation(viewModel, preferences!!)
    }
}

@Composable
private fun MainNavigation(viewModel: AscendViewModel, preferences: com.ascend.app.core.datastore.AppPreferences) {
    val navController = rememberNavController()
    val currentDate by viewModel.currentDate.collectAsStateWithLifecycle()
    val state by viewModel.dashboard.collectAsStateWithLifecycle()
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val savedMeals by viewModel.savedMeals.collectAsStateWithLifecycle()
    val logs by viewModel.foodLogs.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val quests by viewModel.quests.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val unlocked by viewModel.unlockedAchievements.collectAsStateWithLifecycle()
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val activeSets by viewModel.activeSets.collectAsStateWithLifecycle()
    val systemMessages by viewModel.systemMessages.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var overlay by remember { mutableStateOf<UiEvent?>(null) }
    var questLaunching by remember { mutableStateOf(false) }
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

    LaunchedEffect(route, activeWorkout, state.profile, state.workoutHistory, currentDate) {
        if (route == "workout" && activeWorkout == null && state.profile != null) {
            val resumable = state.workoutHistory.firstOrNull { it.completedAt == null && it.localDate == currentDate.toString() }
            if (resumable != null) viewModel.restoreWorkout(resumable.id)
            else navController.popBackStack(MainDestination.HOME.route, false)
        }
    }

    val todayTemplate = remember(state.profile, templates, currentDate) {
        val profile = state.profile
        if (profile == null) null else {
            val scheduled = profile.workoutDays.split(',').mapNotNull { it.toIntOrNull() }.map(DayOfWeek::of).toSet()
            val index = CustomPlanEngine.templateIndexFor(currentDate, LocalDate.parse(profile.programStartDate), scheduled)
            templates.firstOrNull { it.rotationIndex == index }
        }
    }
    fun startWorkout() {
        if (questLaunching) return
        questLaunching = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            delay(1_050)
            questLaunching = false
            viewModel.startWorkout { navController.navigate("workout") { launchSingleTop = true } }
        }
    }

    Box(Modifier.fillMaxSize().background(Void)) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(EnergyViolet.copy(alpha = .105f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(930f, 30f),
                    radius = 820f,
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(EnergyCyan.copy(alpha = .045f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(30f, 1500f),
                    radius = 900f,
                ),
            ),
        )
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHost) },
            bottomBar = {
                if (route in MainDestination.entries.map { it.route }) AscendBottomBar(route) { destination ->
                    if (destination.route != route) {
                        if (destination == MainDestination.HOME) {
                            // Home is the root destination. Restoring its saved stack can reopen the
                            // last sibling tab in a flat graph, so always pop directly to the root.
                            navController.popBackStack(MainDestination.HOME.route, inclusive = false)
                        } else {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = MainDestination.HOME.route,
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(tween(240)) + scaleIn(tween(300), initialScale = .985f) },
                exitTransition = { fadeOut(tween(150)) + scaleOut(tween(180), targetScale = .99f) },
                popEnterTransition = { fadeIn(tween(240)) + scaleIn(tween(300), initialScale = .985f) },
                popExitTransition = { fadeOut(tween(150)) + scaleOut(tween(180), targetScale = .99f) },
            ) {
                composable(MainDestination.HOME.route) {
                    HomeScreen(
                        state, todayTemplate, currentDate, preferences.profileImagePath,
                        { navController.navigate("profile") }, ::startWorkout,
                        { navController.navigate(MainDestination.NUTRITION.route) }, { navController.navigate("progress") },
                        { navController.navigate(MainDestination.HABITS.route) }, viewModel::addWater, viewModel::toggleHabit,
                    )
                }
                composable("quests") { QuestsScreen(state, templates, quests, achievements, unlocked, currentDate, { navController.popBackStack() }, ::startWorkout, viewModel::toggleCustomQuest) }
                composable(MainDestination.NUTRITION.route) {
                    NutritionScreen(
                        state, logs, foods, savedMeals, preferences.mealSections, viewModel::createAndLogFood, viewModel::logFood,
                        viewModel::deleteFoodLog, viewModel::copyPreviousMeal, viewModel::saveMeal, viewModel::logSavedMeal,
                        viewModel::addWater, viewModel::updateTargets,
                    )
                }
                composable("progress") { ProgressScreen(state, templates, { navController.popBackStack() }, viewModel::logWeight) }
                composable(MainDestination.HABITS.route) { HabitsScreen(state, habits, viewModel::toggleHabit, viewModel::createHabit) }
                composable(MainDestination.SYSTEM.route) { SystemScreen(state, systemMessages, viewModel::sendSystemMessage, viewModel::clearSystemMessages) }
                composable("workout") {
                    WorkoutScreen(activeWorkout, activeSets, { navController.popBackStack() }, viewModel::updateSet, viewModel::addWorkoutExercise, viewModel::removeWorkoutExercise) {
                        viewModel.completeWorkout { navController.popBackStack(MainDestination.HOME.route, false) }
                    }
                }
                composable("profile") {
                    ProfileSettingsScreen(
                        state, templates, preferences, unlocked, { navController.popBackStack() }, viewModel::updateNotification,
                        viewModel::updateMealSections,
                        { navController.navigate("progress") }, { navController.navigate("quests") }, viewModel::updateProfileImage,
                    )
                }
            }
        }
        EventOverlay(overlay)
        QuestLaunchOverlay(questLaunching)
    }
}

@Composable
private fun AscendBottomBar(currentRoute: String?, onSelect: (MainDestination) -> Unit) {
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(25.dp), ambientColor = Color.Black.copy(.4f), spotColor = Color.Black.copy(.55f))
                .clip(RoundedCornerShape(25.dp))
                .background(GlassSurface)
                .border(.75.dp, Hairline.copy(.9f), RoundedCornerShape(25.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MainDestination.entries.forEach { destination ->
                val selected = currentRoute == destination.route
                val scale by animateFloatAsState(if (selected) 1f else .94f, spring(stiffness = 500f, dampingRatio = .78f), label = "${destination.route}_scale")
                val tint = if (selected) EnergyCyan else TextTertiary
                Column(
                    Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(if (selected) EnergyViolet.copy(alpha = .13f) else Color.Transparent)
                        .clickable { onSelect(destination) }
                        .scale(scale),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (selected) Box(Modifier.size(32.dp).background(EnergyCyan.copy(.055f), CircleShape))
                        if (destination == MainDestination.SYSTEM) SystemAvatar(size = 25.dp)
                        else Icon(destination.icon, destination.label, Modifier.size(22.dp), tint = tint)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(destination.label.lowercase().replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelSmall, color = tint)
                }
            }
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
