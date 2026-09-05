package com.ascend.app

import com.ascend.app.cloud.GoogleProgressService
import com.ascend.app.cloud.SystemAiService
import com.ascend.app.core.database.*
import com.ascend.app.core.datastore.AppPreferences
import com.ascend.app.core.datastore.UserPreferences
import com.ascend.app.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.Period
import java.time.DayOfWeek
import java.util.UUID

data class OnboardingProfile(
    val name: String,
    val birthDate: LocalDate,
    val heightCm: Double,
    val weightKg: Double,
    val targetWeightKg: Double,
    val sex: BiologicalSex,
    val units: UnitSystem,
    val objective: Objective,
    val activity: ActivityLevel,
    val experience: Experience,
    val equipment: Equipment,
    val diet: DietPreference,
    val trainingTime: TrainingTime,
    val focusAreas: Set<FocusArea>,
    val injuries: Set<InjuryArea>,
    val injuryNotes: String,
    val workoutFrequency: Int,
    val workoutDays: Set<DayOfWeek>,
    val futureVision: String,
    val coreReason: String,
    val minimumPromise: String,
    val googleAccountEmail: String? = null,
    val manualTargets: NutritionCalculation? = null,
)

data class NutritionTotals(
    val calories: Int = 0, val protein: Int = 0, val carbs: Int = 0, val fat: Int = 0,
    val fiber: Int = 0, val sugar: Int = 0, val saturatedFat: Int = 0, val sodiumMg: Int = 0,
)

data class DashboardState(
    val profile: UserProfileEntity? = null,
    val target: NutritionTargetEntity? = null,
    val nutrition: NutritionTotals = NutritionTotals(),
    val waterMl: Int = 0,
    val habits: List<HabitEntity> = emptyList(),
    val habitCompletions: Set<String> = emptySet(),
    val weights: List<WeightEntryEntity> = emptyList(),
    val level: LevelProgress = LevelEngine.fromLifetimeXp(0),
    val lifetimeXp: Int = 0,
    val streak: StreakResult = StreakResult(0, 0),
    val trainingVolume: Double = 0.0,
    val workoutHistory: List<WorkoutSessionEntity> = emptyList(),
    val dailySummaries: List<DailySummaryEntity> = emptyList(),
)

data class WorkoutLaunch(
    val session: WorkoutSessionEntity,
    val template: WorkoutTemplateEntity,
    val exercises: List<WorkoutExerciseDetail>,
    val sets: List<WorkoutSetEntity>,
)

private data class DashboardCore(
    val profile: UserProfileEntity?,
    val target: NutritionTargetEntity?,
    val logs: List<FoodLogWithFood>,
    val habits: List<HabitEntity>,
    val completions: List<HabitCompletionEntity>,
)

private data class DashboardStats(
    val weights: List<WeightEntryEntity>,
    val xp: Int,
    val volume: Double?,
    val workouts: List<WorkoutSessionEntity>,
    val summaries: List<DailySummaryEntity>,
)

class AscendRepository(
    private val dao: AscendDao,
    val preferences: UserPreferences,
    private val cloudProgress: GoogleProgressService,
    private val systemAi: SystemAiService,
) {
    fun preferencesFlow(): Flow<AppPreferences> = preferences.values
    fun foods(): Flow<List<FoodEntity>> = dao.observeFoods()
    fun savedMeals(): Flow<List<SavedMealEntity>> = dao.observeSavedMeals()
    fun foodLogs(date: LocalDate): Flow<List<FoodLogWithFood>> = dao.observeFoodLogs(date.toString())
    fun habits(): Flow<List<HabitEntity>> = dao.observeHabits()
    fun habitCompletions(date: LocalDate): Flow<List<HabitCompletionEntity>> = dao.observeHabitCompletions(date.toString())
    fun weights(): Flow<List<WeightEntryEntity>> = dao.observeWeights()
    fun templates(): Flow<List<WorkoutTemplateEntity>> = dao.observeTemplates()
    fun workoutHistory(): Flow<List<WorkoutSessionEntity>> = dao.observeWorkoutHistory()
    fun achievements(): Flow<List<AchievementEntity>> = dao.observeAchievements()
    fun unlockedAchievements(): Flow<List<UnlockedAchievementEntity>> = dao.observeUnlockedAchievements()
    fun quests(): Flow<List<QuestEntity>> = dao.observeQuests()
    fun questCompletions(date: LocalDate): Flow<List<QuestCompletionEntity>> = dao.observeQuestCompletions(date.toString())
    fun xpLedger(): Flow<List<XpTransactionEntity>> = dao.observeXpLedger()
    fun workoutSets(sessionId: String): Flow<List<WorkoutSetEntity>> = dao.observeWorkoutSets(sessionId)
    fun systemMessages(): Flow<List<SystemMessageEntity>> = dao.observeSystemMessages()

    fun dashboard(today: LocalDate): Flow<DashboardState> {
        val core = combine(
            dao.observeProfile(), dao.observeNutritionTarget(), dao.observeFoodLogs(today.toString()),
            dao.observeHabits(), dao.observeHabitCompletions(today.toString()),
        ) { profile, target, logs, habits, completions -> DashboardCore(profile, target, logs, habits, completions) }
        val stats = combine(
            dao.observeWeights(), dao.observeLifetimeXp(), dao.observeTrainingVolume(),
            dao.observeWorkoutHistory(), dao.observeDailySummaries(),
        ) { weights, xp, volume, workouts, summaries -> DashboardStats(weights, xp, volume, workouts, summaries) }
        return combine(core, stats) { coreState, statsState ->
            val totals = coreState.logs.toTotals()
            val scheduledHabits = coreState.habits.filter { it.scheduledOn(today) }
        DashboardState(
            profile = coreState.profile,
            target = coreState.target,
            nutrition = totals,
            waterMl = statsState.summaries.firstOrNull { it.localDate == today.toString() }?.waterMl ?: 0,
            habits = scheduledHabits,
            habitCompletions = coreState.completions.filter { it.completed }.map { it.habitId }.toSet(),
            weights = statsState.weights,
            level = LevelEngine.fromLifetimeXp(statsState.xp),
            lifetimeXp = statsState.xp,
            streak = StreakEngine.calculate(statsState.summaries.filter { it.completionPercent >= 50 }.map { LocalDate.parse(it.localDate) }, today),
            trainingVolume = statsState.volume ?: 0.0,
            workoutHistory = statsState.workouts,
            dailySummaries = statsState.summaries,
        )
        }
    }

    suspend fun finishOnboarding(input: OnboardingProfile) {
        val age = Period.between(input.birthDate, LocalDate.now()).years
        val estimate = NutritionEngine.calculate(input.weightKg, input.heightCm, age, input.sex, input.activity, input.objective)
        val targets = input.manualTargets ?: estimate
        require(input.name.isNotBlank()) { "Player name is required" }
        require(input.focusAreas.isNotEmpty()) { "Choose at least one focus area" }
        require(input.workoutFrequency in 2..6 && input.workoutDays.size == input.workoutFrequency) { "Workout days must match the selected frequency" }
        require(input.futureVision.isNotBlank() && input.coreReason.isNotBlank() && input.minimumPromise.isNotBlank()) { "Complete the mindset calibration" }
        require(input.targetWeightKg in 25.0..400.0) { "Target weight is outside the supported range" }
        require(targets.calories in 1_000..10_000 && targets.proteinGrams in 0..1_000 && targets.carbohydrateGrams in 0..2_000 && targets.fatGrams in 0..500 && targets.waterMl in 500..10_000) { "Nutrition targets are outside supported ranges" }
        require(targets.proteinGrams * 4 + targets.carbohydrateGrams * 4 + targets.fatGrams * 9 <= targets.calories * 1.5) { "Macro targets are not plausible for the calorie target" }
        dao.upsertProfile(
            UserProfileEntity(
                displayName = input.name.trim(), birthDate = input.birthDate.toString(), heightCm = input.heightCm,
                currentWeightKg = input.weightKg, targetWeightKg = input.targetWeightKg,
                biologicalSex = input.sex.name, unitSystem = input.units.name, objective = input.objective.name,
                activityLevel = input.activity.name, experience = input.experience.name, equipment = input.equipment.name,
                dietPreference = input.diet.name, trainingTime = input.trainingTime.name,
                focusAreas = input.focusAreas.joinToString(",") { it.name },
                injuries = input.injuries.joinToString(",") { it.name }, injuryNotes = input.injuryNotes.trim(),
                workoutFrequency = input.workoutFrequency,
                workoutDays = input.workoutDays.sortedBy { it.value }.joinToString(",") { it.value.toString() },
                futureVision = input.futureVision.trim(), coreReason = input.coreReason.trim(),
                minimumPromise = input.minimumPromise.trim(), googleAccountEmail = input.googleAccountEmail,
                programStartDate = LocalDate.now().toString(), createdAt = System.currentTimeMillis(),
            ),
        )
        dao.upsertNutritionTarget(
            NutritionTargetEntity(
                calories = targets.calories, proteinGrams = targets.proteinGrams,
                carbohydrateGrams = targets.carbohydrateGrams, fatGrams = targets.fatGrams,
                waterMl = targets.waterMl, estimatedBmr = estimate.bmr,
                estimatedMaintenance = estimate.maintenanceCalories, manuallyEdited = input.manualTargets != null,
            ),
        )
        dao.insertWeight(WeightEntryEntity(id(), input.weightKg, LocalDate.now().toString(), System.currentTimeMillis(), "Starting weight"))
        seedCustomWorkoutProgram(input)
        seedCoreData()
        dao.insertSystemMessage(
            SystemMessageEntity(
                id(), "SYSTEM",
                "Player profile synchronized. I built your ${input.workoutFrequency}-day protocol around ${input.focusAreas.joinToString { it.name.lowercase().replace('_', ' ') }}. Ask me about today's training, calories, protein, recovery, or consistency.",
                now(),
            ),
        )
        preferences.completeOnboarding()
    }

    suspend fun updateNutritionTargets(calories: Int, protein: Int, carbs: Int, fat: Int, waterMl: Int) {
        require(calories in 1_000..10_000 && protein in 0..1_000 && carbs in 0..2_000 && fat in 0..500 && waterMl in 500..10_000)
        val current = dao.observeNutritionTarget().first() ?: return
        dao.upsertNutritionTarget(current.copy(calories = calories, proteinGrams = protein, carbohydrateGrams = carbs, fatGrams = fat, waterMl = waterMl, manuallyEdited = true))
        refreshDailyState(LocalDate.now())
    }

    suspend fun seedCoreData() {
        if (dao.observeTemplates().first().isEmpty()) seedWorkoutProgram()
        if (dao.observeHabits().first().isEmpty()) {
            listOf(
                HabitEntity(id(), "Read 20 minutes", HabitType.DURATION.name, 20.0, "min", HabitDifficulty.NORMAL.name, "DAILY", createdAt = now()),
                HabitEntity(id(), "10 minutes mobility", HabitType.DURATION.name, 10.0, "min", HabitDifficulty.NORMAL.name, "DAILY", createdAt = now() + 1),
                HabitEntity(id(), "No junk food", HabitType.AVOIDANCE.name, 1.0, "done", HabitDifficulty.MEDIUM.name, "DAILY", createdAt = now() + 2),
            ).forEach { dao.upsertHabit(it) }
        }
        if (dao.observeFoods().first().isEmpty()) commonFoods().forEach { dao.upsertFood(it) }
        dao.upsertQuests(defaultQuests(dao.observeProfile().first()?.workoutFrequency ?: 6))
        dao.upsertAchievements(defaultAchievements())
    }

    suspend fun addFood(food: FoodEntity) {
        validateFood(food)
        dao.upsertFood(food)
    }

    suspend fun findFoodByBarcode(barcode: String): FoodEntity? = dao.foodByBarcode(barcode.trim())

    suspend fun sendSystemMessage(message: String, tone: SystemTone, date: LocalDate) {
        val clean = message.trim()
        require(clean.isNotEmpty() && clean.length <= 500) { "Message must be between 1 and 500 characters" }
        val conversation = dao.recentSystemMessages().sortedBy { it.createdAt }
        val conversationHistory = conversation.filter { it.role == "PLAYER" }.map { it.message }
        dao.insertSystemMessage(SystemMessageEntity(id(), "PLAYER", clean, now()))
        val profile = dao.observeProfile().first() ?: return
        val target = dao.observeNutritionTarget().first() ?: return
        val totals = dao.observeFoodLogs(date.toString()).first().toTotals()
        val summary = dao.dailySummary(date.toString()) ?: emptySummary(date)
        val scheduledDays = profile.workoutDays.split(',').mapNotNull { it.toIntOrNull() }.map { DayOfWeek.of(it) }.toSet()
        val index = CustomPlanEngine.templateIndexFor(date, LocalDate.parse(profile.programStartDate), scheduledDays)
        val workout = dao.templateForIndex(index)?.name ?: "RECOVERY PROTOCOL"
        val tomorrowIndex = CustomPlanEngine.templateIndexFor(date.plusDays(1), LocalDate.parse(profile.programStartDate), scheduledDays)
        val tomorrowWorkout = dao.templateForIndex(tomorrowIndex)?.name ?: "RECOVERY PROTOCOL"
        val systemContext = SystemContext(
            profile.displayName, Objective.valueOf(profile.objective), target.calories, target.proteinGrams,
            totals.calories, totals.protein, summary.waterMl, target.waterMl,
            StreakEngine.calculate(dao.observeDailySummaries().first().filter { it.completionPercent >= 50 }.map { LocalDate.parse(it.localDate) }, date).current,
            workout, tomorrowWorkout, profile.workoutFrequency,
            profile.focusAreas.split(',').mapNotNull { runCatching { FocusArea.valueOf(it) }.getOrNull() }.toSet(),
            profile.injuries.split(',').mapNotNull { runCatching { InjuryArea.valueOf(it) }.getOrNull() }.toSet(),
            profile.coreReason, profile.futureVision, profile.minimumPromise, conversationHistory,
        )
        val localResponse = SystemEngine.respond(
            clean,
            systemContext,
            tone,
        )
        val response = if (SystemEngine.requiresImmediateSafetyResponse(clean)) localResponse else {
            systemAi.respond(clean, systemContext, tone, conversation).getOrDefault(localResponse)
        }
        delay(420)
        dao.insertSystemMessage(SystemMessageEntity(id(), "SYSTEM", response, now() + 1))
    }

    suspend fun clearSystemMessages() = dao.clearSystemMessages()

    suspend fun createFoodAndLog(
        name: String, servingQuantity: Double, unit: String, calories: Double,
        protein: Double, carbs: Double, fat: Double, servings: Double, meal: MealType, date: LocalDate,
        barcode: String? = null, fiber: Double? = null, sugar: Double? = null,
        saturatedFat: Double? = null, sodiumMg: Double? = null,
    ) {
        val food = FoodEntity(
            id(), name.trim(), servingQuantity, unit.trim(), calories, protein, carbs, fat,
            fiberGrams = fiber, saturatedFatGrams = saturatedFat, sugarGrams = sugar, sodiumMg = sodiumMg,
            barcode = barcode?.trim()?.takeIf(String::isNotBlank), createdAt = now(),
        )
        validateFood(food)
        dao.upsertFood(food)
        logFood(food.id, servings, meal, date)
    }

    suspend fun logFood(foodId: String, servings: Double, meal: MealType, date: LocalDate) {
        require(servings > 0 && servings <= 100)
        dao.insertFoodLog(FoodLogEntity(id(), foodId, date.toString(), meal.name, servings, now()))
        refreshDailyState(date)
    }

    suspend fun deleteFoodLog(log: FoodLogEntity) {
        dao.deleteFoodLog(log)
        refreshDailyState(LocalDate.parse(log.localDate))
    }

    suspend fun copyMeal(sourceDate: LocalDate, meal: MealType, targetDate: LocalDate) {
        dao.foodLogsForDate(sourceDate.toString()).filter { it.mealType == meal.name }.forEach {
            dao.insertFoodLog(it.copy(id = id(), localDate = targetDate.toString(), loggedAt = now()))
        }
        refreshDailyState(targetDate)
    }

    suspend fun saveMeal(name: String, logs: List<FoodLogWithFood>) {
        require(name.isNotBlank() && logs.isNotEmpty())
        val mealId = id()
        dao.upsertSavedMeal(SavedMealEntity(mealId, name.trim(), now()))
        dao.upsertSavedMealItems(logs.groupBy { it.log.foodId }.map { (foodId, values) ->
            SavedMealItemEntity(mealId, foodId, values.sumOf { it.log.servings })
        })
    }

    suspend fun logSavedMeal(mealId: String, meal: MealType, date: LocalDate) {
        val items = dao.savedMealItems(mealId)
        require(items.isNotEmpty()) { "Saved meal has no items" }
        items.forEach { item ->
            if (dao.foodById(item.foodId) != null) {
                dao.insertFoodLog(FoodLogEntity(id(), item.foodId, date.toString(), meal.name, item.servings, now()))
            }
        }
        refreshDailyState(date)
    }

    suspend fun addWater(amountMl: Int, date: LocalDate) {
        require(amountMl in 1..5_000)
        val old = dao.dailySummary(date.toString()) ?: emptySummary(date)
        dao.upsertDailySummary(old.copy(waterMl = (old.waterMl + amountMl).coerceAtMost(20_000)))
        refreshDailyState(date)
    }

    suspend fun addWeight(weightKg: Double, date: LocalDate, note: String) {
        require(weightKg in 25.0..400.0)
        dao.insertWeight(WeightEntryEntity(id(), weightKg, date.toString(), now(), note.trim()))
        awardXp(XpSourceType.WEIGHT, "weight_check_in", date, AscendConfig.WEIGHT_XP, "Body weight recorded")
        val profile = dao.observeProfile().first()
        if (profile != null) dao.upsertProfile(profile.copy(currentWeightKg = weightKg))
    }

    suspend fun createHabit(name: String, type: HabitType, target: Double, unit: String, difficulty: HabitDifficulty, frequency: HabitFrequency = HabitFrequency.EVERY_DAY) {
        require(name.isNotBlank() && target > 0)
        dao.upsertHabit(
            HabitEntity(
                id(), name.trim(), type.name, target, unit.trim(), difficulty.name, frequency.name,
                weekdays = if (frequency == HabitFrequency.WEEKDAYS) "1,2,3,4,5" else "",
                timesPerWeek = if (frequency == HabitFrequency.THREE_TIMES_WEEKLY) 3 else 7,
                createdAt = now(),
            ),
        )
    }

    suspend fun setHabitCompletion(habit: HabitEntity, completed: Boolean, value: Double, date: LocalDate) {
        if (completed) {
            dao.upsertHabitCompletion(HabitCompletionEntity(id(), habit.id, date.toString(), value.coerceAtLeast(habit.target), true, now()))
            val used = dao.habitXpForDate(date.toString())
            val requested = HabitDifficulty.valueOf(habit.difficulty).xp
            val award = minOf(requested, (AscendConfig.MAX_HABIT_XP_PER_DAY - used).coerceAtLeast(0))
            if (award > 0) awardXp(XpSourceType.HABIT, habit.id, date, award, habit.name)
        } else {
            dao.deleteHabitCompletion(habit.id, date.toString())
            dao.deleteXp(XpSourceType.HABIT.name, habit.id, date.toString())
        }
        refreshDailyState(date)
    }

    suspend fun startTodayWorkout(date: LocalDate): WorkoutLaunch {
        dao.latestWorkout(date.toString())?.takeIf { it.completedAt == null }?.let { existing ->
            return launchForSession(existing.id) ?: error("Unable to restore workout")
        }
        require(dao.completedWorkout(date.toString()) == null) { "Today's primary quest is already complete" }
        val profile = dao.observeProfile().first() ?: error("Complete onboarding first")
        val scheduledDays = profile.workoutDays.split(',').mapNotNull { it.toIntOrNull() }.map { DayOfWeek.of(it) }.toSet()
        val templateIndex = CustomPlanEngine.templateIndexFor(date, LocalDate.parse(profile.programStartDate), scheduledDays)
        val template = dao.templateForIndex(templateIndex) ?: error("Workout program is unavailable")
        val exercises = dao.templateExercises(template.id)
        val session = WorkoutSessionEntity(id(), template.id, date.toString(), now())
        dao.insertWorkoutSession(session)
        val sets = exercises.flatMap { item ->
            (1..item.link.targetSets).map { setNumber ->
                WorkoutSetEntity(id(), session.id, item.exercise.id, setNumber, 0.0, 0, false)
            }
        }
        if (sets.isNotEmpty()) dao.upsertWorkoutSets(sets)
        return WorkoutLaunch(session, template, exercises, sets)
    }

    suspend fun launchForSession(sessionId: String): WorkoutLaunch? {
        val session = dao.workoutSession(sessionId) ?: return null
        val template = dao.observeTemplates().first().firstOrNull { it.id == session.templateId } ?: return null
        return WorkoutLaunch(session, template, dao.templateExercises(template.id), dao.observeWorkoutSets(sessionId).first())
    }

    suspend fun updateSet(set: WorkoutSetEntity, weightKg: Double, reps: Int, completed: Boolean): Boolean {
        require(weightKg in 0.0..1_500.0 && reps in 0..1_000)
        var isPr = false
        if (completed && !set.completed && weightKg > 0 && reps > 0) {
            val history = dao.completedSets(set.exerciseId).map { PerformanceSet(it.weightKg, it.reps) }
            isPr = history.isNotEmpty() && PersonalRecordEngine.detect(PerformanceSet(weightKg, reps), history).isRecord
        }
        dao.updateWorkoutSet(set.copy(weightKg = weightKg, reps = reps, completed = completed, completedAt = if (completed) now() else null))
        val session = dao.workoutSession(set.sessionId) ?: error("Workout session is unavailable")
        val exerciseSets = dao.setsForSessionExercise(set.sessionId, set.exerciseId)
        val exerciseSource = "${session.templateId}:${set.exerciseId}"
        val sourceDate = LocalDate.parse(session.localDate)
        if (exerciseSets.isNotEmpty() && exerciseSets.all { it.completed }) {
            awardXp(XpSourceType.EXERCISE, exerciseSource, sourceDate, 25, "Exercise objective complete")
        } else dao.deleteXp(XpSourceType.EXERCISE.name, exerciseSource, sourceDate.toString())
        if (isPr) awardXp(XpSourceType.PERSONAL_RECORD, set.exerciseId, sourceDate, AscendConfig.PERSONAL_RECORD_XP, "New personal record")
        return isPr
    }

    suspend fun addWorkoutExercise(launch: WorkoutLaunch, name: String, targetSets: Int, minReps: Int, maxReps: Int) {
        require(name.isNotBlank() && targetSets in 1..10 && minReps in 1..100 && maxReps in minReps..100)
        val exerciseId = "custom_${id()}"
        val exercise = ExerciseEntity(exerciseId, name.trim(), "Custom", "CUSTOM")
        dao.upsertExercises(listOf(exercise))
        dao.upsertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(
                    id(), launch.template.id, exerciseId, launch.exercises.size,
                    targetSets, minReps, maxReps,
                ),
            ),
        )
        dao.upsertWorkoutSets((1..targetSets).map { number -> WorkoutSetEntity(id(), launch.session.id, exerciseId, number, 0.0, 0, false) })
    }

    suspend fun removeWorkoutExercise(launch: WorkoutLaunch, detail: WorkoutExerciseDetail) {
        dao.deleteSessionExerciseSets(launch.session.id, detail.exercise.id)
        dao.deleteWorkoutExercise(detail.link.id)
        dao.deleteXp(XpSourceType.EXERCISE.name, "${launch.template.id}:${detail.exercise.id}", LocalDate.parse(launch.session.localDate).toString())
    }

    suspend fun completeWorkout(launch: WorkoutLaunch) {
        val latestSets = dao.observeWorkoutSets(launch.session.id).first()
        require(launch.template.isRecovery || latestSets.isNotEmpty() && latestSets.all { it.completed }) { "Complete every set before finishing the quest" }
        dao.updateWorkoutSession(launch.session.copy(completedAt = now()))
        val date = LocalDate.parse(launch.session.localDate)
        awardXp(
            XpSourceType.WORKOUT, launch.template.id, date, launch.template.rewardXp,
            if (launch.template.isRecovery) "Recovery protocol complete" else "${launch.template.name} complete",
        )
        refreshDailyState(date)
    }

    suspend fun updateNotification(category: String, enabled: Boolean) = preferences.setNotification(category, enabled)

    private suspend fun refreshDailyState(date: LocalDate) {
        val target = dao.observeNutritionTarget().first() ?: return
        val totals = dao.observeFoodLogs(date.toString()).first().toTotals()
        val habits = dao.observeHabits().first().filter { it.scheduledOn(date) }
        val habitDone = dao.observeHabitCompletions(date.toString()).first().count { it.completed }
        val workoutDone = dao.completedWorkout(date.toString()) != null
        val old = dao.dailySummary(date.toString()) ?: emptySummary(date)
        val daily = QuestEngine.daily(
            DailyCompletionInput(
                workoutCompleted = workoutDone, restDayCompleted = workoutDone,
                calorieRatio = totals.calories.toDouble() / target.calories,
                proteinRatio = totals.protein.toDouble() / target.proteinGrams,
                hydrationRatio = old.waterMl.toDouble() / target.waterMl,
                habitsPlanned = habits.size, habitsCompleted = habitDone,
            ),
        )
        syncObjectiveXp(XpSourceType.NUTRITION, "calories", date, daily.calories, AscendConfig.CALORIE_XP, "Calorie objective")
        syncObjectiveXp(XpSourceType.NUTRITION, "protein", date, daily.protein, AscendConfig.PROTEIN_XP, "Protein objective")
        syncObjectiveXp(XpSourceType.HYDRATION, "water", date, daily.hydration, AscendConfig.HYDRATION_XP, "Hydration objective")
        syncObjectiveXp(XpSourceType.QUEST, "discipline", date, daily.discipline, 50, "Discipline quest")
        syncObjectiveXp(XpSourceType.PERFECT_DAY, "perfect_day", date, daily.perfectDay, AscendConfig.PERFECT_DAY_XP, "Perfect day")
        val completion = (daily.completionCount * 20).coerceIn(0, 100)
        val xp = dao.observeXpForDate(date.toString()).first()
        dao.upsertDailySummary(
            old.copy(
                completionPercent = completion, calories = totals.calories, proteinGrams = totals.protein,
                habitsCompleted = habitDone, workoutCompleted = workoutDone, xpEarned = xp,
            ),
        )
        evaluateWeeklyQuests(date)
        evaluateAchievements(date)
        val finalXp = dao.observeXpForDate(date.toString()).first()
        if (finalXp != xp) {
            dao.upsertDailySummary(
                old.copy(
                    completionPercent = completion, calories = totals.calories, proteinGrams = totals.protein,
                    habitsCompleted = habitDone, workoutCompleted = workoutDone, xpEarned = finalXp,
                ),
            )
        }
        val profile = dao.observeProfile().first()
        if (profile != null) {
            cloudProgress.syncProgress(profile, target, dao.observeLifetimeXp().first(), dao.observeDailySummaries().first())
        }
    }

    private suspend fun syncObjectiveXp(type: XpSourceType, sourceId: String, date: LocalDate, complete: Boolean, amount: Int, description: String) {
        if (complete) awardXp(type, sourceId, date, amount, description)
        else dao.deleteXp(type.name, sourceId, date.toString())
    }

    private suspend fun awardXp(type: XpSourceType, sourceId: String, date: LocalDate, amount: Int, description: String): Boolean =
        dao.insertXp(XpTransactionEntity(id(), amount, type.name, sourceId, date.toString(), description, now())) != -1L

    private suspend fun evaluateAchievements(date: LocalDate) {
        val workouts = dao.completedWorkoutCount()
        val xp = dao.observeLifetimeXp().first()
        val summaries = dao.observeDailySummaries().first()
        val streak = StreakEngine.calculate(summaries.filter { it.completionPercent >= 50 }.map { LocalDate.parse(it.localDate) }, date)
        val target = dao.observeNutritionTarget().first()
        val proteinDays = if (target == null) 0 else summaries.count { it.proteinGrams >= target.proteinGrams }
        val eligible = mapOf(
            "first_blood" to (workouts >= 1), "week_one" to (streak.longest >= 7),
            "iron_month" to (workouts >= 20), "centurion" to (workouts >= 100),
            "unbroken" to (streak.longest >= 30), "ascendant" to (LevelEngine.fromLifetimeXp(xp).level >= 50),
            "transcendent" to (LevelEngine.fromLifetimeXp(xp).level >= 100),
            "protein_protocol" to (proteinDays >= 7),
        )
        eligible.filterValues { it }.keys.forEach { achievementId ->
            if (dao.unlockAchievement(UnlockedAchievementEntity(achievementId, now())) != -1L) {
                val achievement = dao.observeAchievements().first().firstOrNull { it.id == achievementId }
                if (achievement != null) awardXp(XpSourceType.ACHIEVEMENT, achievement.id, date, achievement.rewardXp, achievement.title)
            }
        }
    }

    private suspend fun evaluateWeeklyQuests(date: LocalDate) {
        val start = date.with(java.time.DayOfWeek.MONDAY)
        val end = start.plusDays(6)
        val summaries = dao.dailySummariesBetween(start.toString(), end.toString())
        val target = dao.observeNutritionTarget().first() ?: return
        val workoutCount = dao.completedTrainingCountBetween(start.toString(), end.toString())
        val plannedWorkouts = dao.observeProfile().first()?.workoutFrequency ?: 6
        val calorieDays = summaries.count { it.calories.toDouble() / target.calories in .9..1.1 }
        val adherence = summaries.map { it.completionPercent / 100.0 }.average().takeIf { !it.isNaN() } ?: 0.0
        syncObjectiveXp(XpSourceType.QUEST, "weekly_iron", start, workoutCount >= plannedWorkouts, 400, "Scheduled training week")
        syncObjectiveXp(XpSourceType.QUEST, "weekly_nutrition", start, calorieDays >= 5, 250, "Nutrition Control")
        syncObjectiveXp(XpSourceType.QUEST, "weekly_consistency", start, summaries.size >= 5 && adherence >= .8, 250, "Weekly Consistency")
    }

    private suspend fun seedWorkoutProgram() {
        seedPlan(
            CustomPlanEngine.generate(
                frequency = 6,
                workoutDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
                focusAreas = setOf(FocusArea.CHEST, FocusArea.BACK, FocusArea.LEGS),
                injuries = setOf(InjuryArea.NONE),
                equipment = Equipment.FULL_GYM,
                experience = Experience.INTERMEDIATE,
            ),
            Equipment.FULL_GYM,
        )
    }

    private suspend fun seedCustomWorkoutProgram(input: OnboardingProfile) {
        seedPlan(
            CustomPlanEngine.generate(
                input.workoutFrequency, input.workoutDays, input.focusAreas,
                input.injuries, input.equipment, input.experience,
            ),
            input.equipment,
        )
    }

    private suspend fun seedPlan(plan: CustomPlan, equipment: Equipment) {
        plan.workouts.forEachIndexed { index, workout ->
            val templateId = "template_$index"
            dao.upsertTemplates(
                listOf(
                    WorkoutTemplateEntity(
                        templateId, workout.name, index,
                        if (workout.recovery) AscendConfig.RECOVERY_XP else AscendConfig.WORKOUT_XP,
                        workout.estimatedMinutes, workout.recovery,
                    ),
                ),
            )
            val exercises = workout.exercises.map { item ->
                ExerciseEntity(slug(item.name), item.name, item.muscleGroup, equipment.name)
            }
            if (exercises.isNotEmpty()) dao.upsertExercises(exercises)
            val links = workout.exercises.mapIndexed { itemIndex, item ->
                WorkoutExerciseEntity(
                    "${templateId}_${slug(item.name)}", templateId, slug(item.name), itemIndex,
                    item.sets, item.minReps, item.maxReps,
                )
            }
            if (links.isNotEmpty()) dao.upsertWorkoutExercises(links)
        }
    }

    private fun commonFoods(): List<FoodEntity> {
        val timestamp = now()
        fun food(
            name: String, quantity: Double, unit: String, calories: Double,
            protein: Double, carbs: Double, fat: Double, fiber: Double? = null,
            barcode: String? = null,
        ) = FoodEntity(
            id = "food_${slug(name)}", name = name, servingQuantity = quantity, servingUnit = unit,
            calories = calories, proteinGrams = protein, carbohydrateGrams = carbs, fatGrams = fat,
            fiberGrams = fiber, barcode = barcode, createdAt = timestamp,
        )
        return listOf(
            food("Apple", 1.0, "medium", 95.0, .5, 25.0, .3, 4.4),
            food("Banana", 1.0, "medium", 105.0, 1.3, 27.0, .4, 3.1),
            food("Orange", 1.0, "medium", 62.0, 1.2, 15.4, .2, 3.1),
            food("Mango", 1.0, "cup", 99.0, 1.4, 24.7, .6, 2.6),
            food("Papaya", 1.0, "cup", 62.0, .7, 15.7, .4, 2.5),
            food("Mixed Berries", 1.0, "cup", 70.0, 1.0, 17.0, .5, 5.0),
            food("Whole Egg", 1.0, "egg", 72.0, 6.3, .4, 4.8),
            food("Egg Whites", 100.0, "g", 52.0, 10.9, .7, .2),
            food("Chicken Breast Cooked", 100.0, "g", 165.0, 31.0, 0.0, 3.6),
            food("Chicken Thigh Cooked", 100.0, "g", 209.0, 26.0, 0.0, 10.9),
            food("Salmon Cooked", 100.0, "g", 206.0, 22.0, 0.0, 12.0),
            food("Tuna Canned in Water", 100.0, "g", 116.0, 25.5, 0.0, .8),
            food("Paneer", 100.0, "g", 265.0, 18.3, 3.4, 20.8),
            food("Firm Tofu", 100.0, "g", 144.0, 17.3, 2.8, 8.7, 2.3),
            food("Greek Yogurt Plain", 100.0, "g", 97.0, 9.0, 3.9, 5.0),
            food("Milk Whole", 250.0, "ml", 149.0, 7.7, 11.7, 8.0),
            food("Milk Low Fat", 250.0, "ml", 105.0, 8.5, 12.0, 2.5),
            food("Whey Protein", 1.0, "scoop", 120.0, 24.0, 3.0, 2.0),
            food("Cooked White Rice", 100.0, "g", 130.0, 2.7, 28.2, .3, .4),
            food("Cooked Brown Rice", 100.0, "g", 123.0, 2.7, 25.6, 1.0, 1.6),
            food("Roti Chapati", 1.0, "piece", 120.0, 3.5, 22.0, 2.5, 3.0),
            food("Cooked Oats", 1.0, "cup", 154.0, 6.0, 27.0, 3.2, 4.0),
            food("Whole Wheat Bread", 1.0, "slice", 81.0, 4.0, 13.8, 1.1, 1.9),
            food("Cooked Pasta", 100.0, "g", 157.0, 5.8, 30.9, .9, 1.8),
            food("Boiled Potato", 100.0, "g", 87.0, 1.9, 20.1, .1, 1.8),
            food("Sweet Potato Cooked", 100.0, "g", 90.0, 2.0, 20.7, .2, 3.3),
            food("Dal Cooked", 1.0, "cup", 230.0, 17.9, 39.9, .8, 15.6),
            food("Chana Masala", 1.0, "cup", 280.0, 14.0, 45.0, 6.0, 12.0),
            food("Rajma Curry", 1.0, "cup", 290.0, 15.0, 46.0, 6.0, 13.0),
            food("Idli", 1.0, "piece", 58.0, 2.0, 12.0, .4, .5),
            food("Plain Dosa", 1.0, "piece", 168.0, 4.0, 29.0, 4.0, 1.0),
            food("Poha", 1.0, "cup", 250.0, 5.0, 45.0, 6.0, 3.0),
            food("Upma", 1.0, "cup", 220.0, 6.0, 38.0, 5.0, 4.0),
            food("Cooked Mixed Vegetables", 1.0, "cup", 118.0, 5.0, 24.0, 1.0, 8.0),
            food("Broccoli Cooked", 1.0, "cup", 55.0, 3.7, 11.2, .6, 5.1),
            food("Mixed Salad", 2.0, "cups", 50.0, 2.0, 10.0, .5, 4.0),
            food("Avocado", .5, "fruit", 120.0, 1.5, 6.4, 11.0, 5.0),
            food("Almonds", 28.0, "g", 164.0, 6.0, 6.1, 14.2, 3.5),
            food("Peanut Butter", 2.0, "tbsp", 188.0, 8.0, 7.0, 16.0, 1.9),
            food("Olive Oil", 1.0, "tbsp", 119.0, 0.0, 0.0, 13.5),
            food("Ghee", 1.0, "tsp", 45.0, 0.0, 0.0, 5.0),
            food("Nutella", 100.0, "g", 539.0, 6.3, 57.5, 30.9, 3.4, "3017620422003"),
            food("Oreo Original", 100.0, "g", 480.0, 5.0, 70.0, 20.0, 2.5, "7622210449283"),
            food("Coca-Cola", 500.0, "ml", 210.0, 0.0, 53.0, 0.0, null, "5449000000996"),
            food("Coca-Cola Zero", 500.0, "ml", 1.0, 0.0, 0.0, 0.0, null, "5449000131805"),
            food("Red Bull", 250.0, "ml", 112.0, 0.0, 27.0, 0.0, null, "9002490100070"),
        )
    }

    private fun defaultQuests(plannedWorkouts: Int) = listOf(
        QuestEntity("daily_workout", "PRIMARY QUEST", "Complete today's training or recovery protocol", QuestType.DAILY.name, QuestCategory.TRAINING.name, 1.0, 200),
        QuestEntity("daily_calorie", "NUTRITION QUEST", "Reach 90–110% of your calorie target", QuestType.DAILY.name, QuestCategory.NUTRITION.name, 1.0, 40),
        QuestEntity("daily_protein", "PROTEIN QUEST", "Reach your configured protein target", QuestType.DAILY.name, QuestCategory.NUTRITION.name, 1.0, 40),
        QuestEntity("daily_water", "HYDRATION QUEST", "Reach your hydration target", QuestType.DAILY.name, QuestCategory.HYDRATION.name, 1.0, 30),
        QuestEntity("daily_discipline", "DISCIPLINE QUEST", "Complete 80% of planned habits", QuestType.DAILY.name, QuestCategory.DISCIPLINE.name, .8, 50),
        QuestEntity("weekly_iron", "IRON WEEK", "Complete all $plannedWorkouts scheduled training sessions", QuestType.WEEKLY.name, QuestCategory.TRAINING.name, plannedWorkouts.toDouble(), 400),
        QuestEntity("weekly_nutrition", "NUTRITION CONTROL", "Hit calorie goal on five days", QuestType.WEEKLY.name, QuestCategory.NUTRITION.name, 5.0, 250),
        QuestEntity("weekly_consistency", "CONSISTENCY", "Complete at least 80% of scheduled habits", QuestType.WEEKLY.name, QuestCategory.CONSISTENCY.name, .8, 250),
    )

    private fun defaultAchievements() = listOf(
        AchievementEntity("first_blood", "FIRST BLOOD", "Complete your first workout", "TRAINING", 1, 50),
        AchievementEntity("week_one", "WEEK ONE", "Maintain a seven-day streak", "CONSISTENCY", 7, 100),
        AchievementEntity("iron_month", "IRON MONTH", "Complete 20 workouts", "TRAINING", 20, 200),
        AchievementEntity("centurion", "CENTURION", "Complete 100 workouts", "TRAINING", 100, 500),
        AchievementEntity("protein_protocol", "PROTEIN PROTOCOL", "Reach protein target seven days", "NUTRITION", 7, 150),
        AchievementEntity("unbroken", "UNBROKEN", "Reach a 30-day streak", "CONSISTENCY", 30, 300),
        AchievementEntity("ascendant", "ASCENDANT", "Reach level 50", "LEVEL", 50, 500),
        AchievementEntity("transcendent", "TRANSCENDENT", "Reach level 100", "LEVEL", 100, 1_000),
    )

    private fun List<FoodLogWithFood>.toTotals() = NutritionTotals(
        calories = sumOf { it.food.calories * it.log.servings }.toInt(),
        protein = sumOf { it.food.proteinGrams * it.log.servings }.toInt(),
        carbs = sumOf { it.food.carbohydrateGrams * it.log.servings }.toInt(),
        fat = sumOf { it.food.fatGrams * it.log.servings }.toInt(),
        fiber = sumOf { (it.food.fiberGrams ?: 0.0) * it.log.servings }.toInt(),
        sugar = sumOf { (it.food.sugarGrams ?: 0.0) * it.log.servings }.toInt(),
        saturatedFat = sumOf { (it.food.saturatedFatGrams ?: 0.0) * it.log.servings }.toInt(),
        sodiumMg = sumOf { (it.food.sodiumMg ?: 0.0) * it.log.servings }.toInt(),
    )

    private fun validateFood(food: FoodEntity) {
        require(food.name.isNotBlank())
        require(food.servingQuantity > 0 && food.servingUnit.isNotBlank())
        require(food.calories >= 0 && food.proteinGrams >= 0 && food.carbohydrateGrams >= 0 && food.fatGrams >= 0)
        require(listOfNotNull(food.fiberGrams, food.saturatedFatGrams, food.sugarGrams, food.sodiumMg).all { it >= 0 })
        require(food.proteinGrams * 4 + food.carbohydrateGrams * 4 + food.fatGrams * 9 <= food.calories * 1.5 + 20) { "Macros are not plausible for these calories" }
    }

    private fun emptySummary(date: LocalDate) = DailySummaryEntity(date.toString(), 0, 0, 0, 0, 0, false, 0)
    private fun now() = System.currentTimeMillis()
    private fun id() = UUID.randomUUID().toString()
    private fun slug(value: String) = value.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    private fun HabitEntity.scheduledOn(date: LocalDate): Boolean = when (frequency) {
        HabitFrequency.WEEKDAYS.name -> date.dayOfWeek.value in 1..5
        HabitFrequency.CUSTOM.name -> weekdays.split(',').any { it.toIntOrNull() == date.dayOfWeek.value }
        else -> true
    }
}
