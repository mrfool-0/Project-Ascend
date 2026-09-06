package com.ascend.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ascend.app.core.database.*
import com.ascend.app.core.datastore.AppPreferences
import com.ascend.app.core.notifications.ReminderScheduler
import com.ascend.app.domain.HabitType
import com.ascend.app.domain.MealType
import com.ascend.app.domain.SystemTone
import com.ascend.app.domain.TrainingTime
import com.ascend.app.ui.screens.NewFoodInput
import com.ascend.app.ui.screens.NewHabitInput
import com.ascend.app.ui.screens.NewExerciseInput
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data class QuestComplete(val xp: Int) : UiEvent
    data class LevelUp(val from: Int, val to: Int) : UiEvent
    data object PersonalRecord : UiEvent
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AscendViewModel(application: Application, private val repository: AscendRepository) : AndroidViewModel(application) {
    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()
    private val _onboardingError = MutableStateFlow<String?>(null)
    val onboardingError: StateFlow<String?> = _onboardingError.asStateFlow()
    val preferences: StateFlow<AppPreferences?> = repository.preferencesFlow().map<AppPreferences, AppPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val dashboard = _currentDate.flatMapLatest(repository::dashboard).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())
    val foods = repository.foods().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val savedMeals = repository.savedMeals().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val foodLogs = _currentDate.flatMapLatest(repository::foodLogs).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val habits = repository.habits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val templates = repository.templates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val quests = repository.quests().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val achievements = repository.achievements().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val unlockedAchievements = repository.unlockedAchievements().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val systemMessages = repository.systemMessages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _activeWorkout = MutableStateFlow<WorkoutLaunch?>(null)
    val activeWorkout: StateFlow<WorkoutLaunch?> = _activeWorkout.asStateFlow()
    val activeSets: StateFlow<List<WorkoutSetEntity>> = _activeWorkout.flatMapLatest { launch ->
        if (launch == null) flowOf(emptyList()) else repository.workoutSets(launch.session.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                _currentDate.value = LocalDate.now()
            }
        }
        viewModelScope.launch {
            preferences.filterNotNull().filter { it.onboardingComplete }.first()
            runCatching { repository.seedCoreData() }.onFailure { _events.emit(UiEvent.Message(it.safeMessage())) }
        }
        viewModelScope.launch {
            var previous: Int? = null
            dashboard.filter { it.profile != null }.map { it.level.level }.distinctUntilChanged().collect { level ->
                previous?.let { old -> if (level > old) _events.emit(UiEvent.LevelUp(old, level)) }
                previous = level
            }
        }
    }

    fun finishOnboarding(input: OnboardingProfile) = viewModelScope.launch {
        _onboardingError.value = null
        runCatching {
            repository.finishOnboarding(input)
            val workoutHour = when (input.trainingTime) {
                TrainingTime.MORNING -> 7
                TrainingTime.AFTERNOON -> 13
                TrainingTime.EVENING -> 18
                TrainingTime.CUSTOM -> 17
            }
            listOf("morning" to 8, "workout" to workoutHour, "nutrition" to 14, "evening" to 20).forEach { (category, hour) ->
                ReminderScheduler.configure(getApplication(), category, true, hour)
            }
        }.onFailure { _onboardingError.value = it.safeMessage() }
    }

    fun startWorkout(onReady: () -> Unit) = viewModelScope.launch {
        runCatching { repository.startTodayWorkout(_currentDate.value) }
            .onSuccess { _activeWorkout.value = it; onReady() }
            .onFailure { _events.emit(UiEvent.Message(it.safeMessage())) }
    }

    fun restoreWorkout(sessionId: String) = launchAction { _activeWorkout.value = repository.launchForSession(sessionId) }

    fun updateSet(set: WorkoutSetEntity, weight: Double, reps: Int, complete: Boolean) = viewModelScope.launch {
        runCatching { repository.updateSet(set, weight, reps, complete) }
            .onSuccess { if (it) _events.emit(UiEvent.PersonalRecord) }
            .onFailure { _events.emit(UiEvent.Message(it.safeMessage())) }
    }

    fun completeWorkout(onComplete: () -> Unit) = viewModelScope.launch {
        val launch = _activeWorkout.value ?: return@launch
        runCatching { repository.completeWorkout(launch) }
            .onSuccess { _events.emit(UiEvent.QuestComplete(launch.template.rewardXp)); _activeWorkout.value = null; onComplete() }
            .onFailure { _events.emit(UiEvent.Message(it.safeMessage())) }
    }

    fun addWorkoutExercise(input: NewExerciseInput) = viewModelScope.launch {
        val launch = _activeWorkout.value ?: return@launch
        runCatching {
            repository.addWorkoutExercise(launch, input.name, input.sets, input.minReps, input.maxReps)
            _activeWorkout.value = repository.launchForSession(launch.session.id)
        }.onFailure { _events.emit(UiEvent.Message(it.safeMessage())) }
    }

    fun removeWorkoutExercise(detail: WorkoutExerciseDetail) = viewModelScope.launch {
        val launch = _activeWorkout.value ?: return@launch
        runCatching {
            repository.removeWorkoutExercise(launch, detail)
            _activeWorkout.value = repository.launchForSession(launch.session.id)
        }.onFailure { _events.emit(UiEvent.Message(it.safeMessage())) }
    }

    fun addWater(amount: Int) = launchAction { repository.addWater(amount, _currentDate.value); _events.emit(UiEvent.Message("+$amount ml recorded")) }
    fun toggleHabit(habit: HabitEntity, complete: Boolean) = launchAction { repository.setHabitCompletion(habit, complete, habit.target, _currentDate.value) }
    fun toggleCustomQuest(quest: QuestEntity, complete: Boolean) = launchAction { repository.setCustomQuestCompletion(quest, complete, _currentDate.value) }
    fun createHabit(input: NewHabitInput) = launchAction { repository.createHabit(input.name, input.type, input.target, input.unit, input.difficulty, input.frequency) }
    fun createAndLogFood(input: NewFoodInput) = launchAction {
        repository.createFoodAndLog(
            input.name, input.servingQuantity, input.servingUnit, input.calories,
            input.protein, input.carbs, input.fat, input.servings, input.meal, _currentDate.value,
            input.barcode, input.fiber, input.sugar, input.saturatedFat, input.sodiumMg,
        )
    }
    fun logFood(food: FoodEntity, servings: Double, meal: MealType) = launchAction { repository.logFood(food.id, servings, meal, _currentDate.value) }
    fun deleteFoodLog(log: FoodLogEntity) = launchAction { repository.deleteFoodLog(log) }
    fun copyPreviousMeal(meal: MealType) = launchAction { repository.copyMeal(_currentDate.value.minusDays(1), meal, _currentDate.value); _events.emit(UiEvent.Message("Previous ${meal.name.lowercase()} copied")) }
    fun saveMeal(name: String, logs: List<FoodLogWithFood>) = launchAction { repository.saveMeal(name, logs); _events.emit(UiEvent.Message("Meal saved")) }
    fun logSavedMeal(mealId: String, meal: MealType) = launchAction { repository.logSavedMeal(mealId, meal, _currentDate.value); _events.emit(UiEvent.Message("Saved meal added")) }
    fun logWeight(weight: Double, note: String) = launchAction { repository.addWeight(weight, _currentDate.value, note); _events.emit(UiEvent.Message("Weight recorded • +15 XP")) }
    fun updateTargets(calories: Int, protein: Int, carbs: Int, fat: Int, water: Int) = launchAction { repository.updateNutritionTargets(calories, protein, carbs, fat, water) }
    fun sendSystemMessage(message: String, tone: SystemTone) = launchAction { repository.sendSystemMessage(message, tone, _currentDate.value) }
    fun clearSystemMessages() = launchAction { repository.clearSystemMessages() }

    fun updateNotification(category: String, enabled: Boolean) = launchAction {
        repository.updateNotification(category, enabled)
        val hour = when (category) { "morning" -> 8; "workout" -> 17; "nutrition" -> 14; else -> 20 }
        ReminderScheduler.configure(getApplication(), category, enabled, hour)
    }
    fun updateMealSections(sections: Set<String>) = launchAction { repository.preferences.setMealSections(sections) }
    fun updateProfileImage(path: String?) = launchAction { repository.preferences.setProfileImage(path) }
    fun updateHealthConnect(enabled: Boolean) = launchAction {
        repository.preferences.setHealthConnect(enabled)
        _events.emit(UiEvent.Message(if (enabled) "Health Connect layer prepared; SDK permissions are not requested in V1" else "Health Connect disabled"))
    }

    private fun launchAction(fallback: String = "Unable to complete action", action: suspend () -> Unit) = viewModelScope.launch {
        runCatching { action() }.onFailure { _events.emit(UiEvent.Message(it.message?.takeIf(String::isNotBlank) ?: fallback)) }
    }

    private fun Throwable.safeMessage() = message?.takeIf { it.isNotBlank() } ?: "Unable to complete action"

    class Factory(private val application: AscendApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AscendViewModel(application, application.repository) as T
    }
}
