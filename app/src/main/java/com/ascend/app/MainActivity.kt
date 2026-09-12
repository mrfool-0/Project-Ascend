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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ascend.app.domain.CustomPlanEngine
import com.ascend.app.ui.components.AngularShape
import com.ascend.app.ui.components.SystemAvatar
import com.ascend.app.ui.components.QuestLaunchOverlay
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
@OptIn(ExperimentalLayoutApi::class)
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
    val systemThinking by viewModel.systemThinking.collectAsStateWithLifecycle()
    val week by viewModel.trainingWeek.collectAsStateWithLifecycle()
    val planLinks by viewModel.workoutLinks.collectAsStateWithLifecycle()
    val exerciseCatalog by viewModel.exerciseCatalog.collectAsStateWithLifecycle()
    val pendingProposal by viewModel.pendingProposal.collectAsStateWithLifecycle()
    val trainingBusy by viewModel.trainingBusy.collectAsStateWithLifecycle()
    val activity = androidx.activity.compose.LocalActivity.current
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val snackbarHost = remember { SnackbarHostState() }
    var overlay by remember { mutableStateOf<UiEvent?>(null) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val motion = LocalMotionEnabled.current
    var questLaunching by remember { mutableStateOf(false) }
    var questPrepared by remember { mutableStateOf(false) }
    var questAnimationComplete by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        activity?.intent?.getStringExtra("ascend_destination")?.takeIf { it in setOf("training", "quests", "nutrition") }?.let {
            navController.navigate(it) { launchSingleTop = true }
            activity.intent.removeExtra("ascend_destination")
        }
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

    val todayTemplate = week.find { it.date == currentDate }?.template
    fun startWorkout() {
        if (questLaunching || todayTemplate == null) return
        questLaunching = true
        questPrepared = false
        questAnimationComplete = false
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            if (viewModel.prepareWorkout()) questPrepared = true
            else questLaunching = false
        }
    }
    LaunchedEffect(questPrepared, questAnimationComplete) {
        if (questLaunching && questPrepared && questAnimationComplete) {
            navController.navigate("workout") { launchSingleTop = true }
            questLaunching = false
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
                if (!WindowInsets.isImeVisible && route in MainDestination.entries.map { it.route }) AscendBottomBar(route) { destination ->
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
                modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding(),
                enterTransition = { fadeIn(tween(if (motion) 240 else 0, delayMillis = if (motion) 90 else 0)) + slideInVertically(tween(if (motion) AscendMotion.Enter else 0)) { it / 35 } },
                exitTransition = { fadeOut(tween(if (motion) 90 else 0)) },
                popEnterTransition = { fadeIn(tween(if (motion) 240 else 0, delayMillis = if (motion) 90 else 0)) },
                popExitTransition = { fadeOut(tween(if (motion) 90 else 0)) },
            ) {
                composable(MainDestination.HOME.route) {
                    HomeScreen(
                        state, todayTemplate, currentDate, preferences.profileImagePath,
                        { navController.navigate("profile") }, ::startWorkout,
                        { navController.navigate(MainDestination.NUTRITION.route) }, { navController.navigate("progress") },
                        { navController.navigate(MainDestination.HABITS.route) }, viewModel::addWater, viewModel::toggleHabit,
                        week = week, onOpenTraining = { navController.navigate("training") },
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
                composable(MainDestination.SYSTEM.route) { SystemScreen(state, systemMessages, systemThinking, viewModel::sendSystemMessage, viewModel::clearSystemMessages,
                    pendingProposal, trainingBusy, viewModel::confirmProposal, { navController.navigate("training") }) }
                composable("training") {
                    TrainingPlanScreen(state.profile, week, templates, planLinks, exerciseCatalog, trainingBusy,
                        { navController.popBackStack() }, { navController.navigate(MainDestination.SYSTEM.route) { launchSingleTop = true } },
                        viewModel::rebuildProgram, viewModel::swapDays, viewModel::editPlanExercise)
                }
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
                        onOpenTraining = { navController.navigate("training") },
                        googleBusy = trainingBusy,
                        onGoogleLink = { activity?.let(viewModel::linkGoogle) },
                    )
                }
            }
        }
        EventOverlay(overlay)
        if (questLaunching) QuestLaunchOverlay(todayTemplate?.name ?: "Your protocol") { questAnimationComplete = true }
    }
}

@Composable
private fun AscendBottomBar(currentRoute: String?, onSelect: (MainDestination) -> Unit) {
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(25.dp), ambientColor = Color.Black.copy(.4f), spotColor = Color.Black.copy(.55f))
                .clip(RoundedCornerShape(25.dp))
                .background(GlassSurface)
                .border(.75.dp, Hairline.copy(.9f), RoundedCornerShape(25.dp))
                .padding(6.dp).selectableGroup(),
                // Expose tab selection to TalkBack, keyboard and switch-access users.
                // The visual highlight and semantic selected state share the same source.
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MainDestination.entries.forEach { destination ->
                val selected = currentRoute == destination.route
                val scale by animateFloatAsState(if (selected) 1f else .94f, spring(stiffness = 500f, dampingRatio = .78f), label = "${destination.route}_scale")
                val tint by animateColorAsState(if (selected) EnergyCyan else TextTertiary, tween(180), label = "tab tint")
                val background by animateColorAsState(if (selected) EnergyViolet.copy(.15f) else Color.Transparent, tween(220), label = "tab surface")
                Column(
                    Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(background)
                        .selectable(selected = selected, role = Role.Tab) { onSelect(destination) }
                        .graphicsLayer { scaleX = scale; scaleY = scale },
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
