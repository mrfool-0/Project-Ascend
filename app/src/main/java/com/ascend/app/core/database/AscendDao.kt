package com.ascend.app.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AscendDao {
    @Query("SELECT * FROM user_profile WHERE id = 1") fun observeProfile(): Flow<UserProfileEntity?>
    @Query("SELECT * FROM nutrition_target WHERE profileId = 1") fun observeNutritionTarget(): Flow<NutritionTargetEntity?>
    // REPLACE deletes the existing parent row before inserting it. Because nutrition_target
    // cascades on profile deletion, a weight update would otherwise erase nutrition targets.
    @Upsert suspend fun upsertProfile(value: UserProfileEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertNutritionTarget(value: NutritionTargetEntity)

    @Query("SELECT * FROM food ORDER BY isFavorite DESC, name") fun observeFoods(): Flow<List<FoodEntity>>
    @Query("SELECT * FROM food WHERE id = :id") suspend fun foodById(id: String): FoodEntity?
    @Query("SELECT * FROM food WHERE barcode = :barcode LIMIT 1") suspend fun foodByBarcode(barcode: String): FoodEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertFood(value: FoodEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertFoodIfAbsent(value: FoodEntity): Long
    @Update suspend fun updateFood(value: FoodEntity)
    @Insert suspend fun insertFoodLog(value: FoodLogEntity)
    @Delete suspend fun deleteFoodLog(value: FoodLogEntity)
    @Transaction @Query("SELECT * FROM food_log WHERE localDate = :date ORDER BY loggedAt")
    fun observeFoodLogs(date: String): Flow<List<FoodLogWithFood>>
    @Transaction @Query("SELECT * FROM food_log WHERE localDate BETWEEN :start AND :end ORDER BY localDate")
    suspend fun foodLogsBetween(start: String, end: String): List<FoodLogWithFood>
    @Query("SELECT * FROM food_log WHERE localDate = :date ORDER BY loggedAt") suspend fun foodLogsForDate(date: String): List<FoodLogEntity>

    @Query("SELECT * FROM saved_meal ORDER BY name") fun observeSavedMeals(): Flow<List<SavedMealEntity>>
    @Query("SELECT * FROM saved_meal_item WHERE savedMealId = :mealId") suspend fun savedMealItems(mealId: String): List<SavedMealItemEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSavedMeal(value: SavedMealEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSavedMealItems(values: List<SavedMealItemEntity>)

    @Query("SELECT * FROM weight_entry ORDER BY localDate, loggedAt") fun observeWeights(): Flow<List<WeightEntryEntity>>
    @Insert suspend fun insertWeight(value: WeightEntryEntity)
    @Delete suspend fun deleteWeight(value: WeightEntryEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertBodyMeasurement(value: BodyMeasurementEntity)
    @Query("SELECT * FROM body_measurement ORDER BY localDate") fun observeMeasurements(): Flow<List<BodyMeasurementEntity>>

    @Upsert suspend fun upsertExercises(values: List<ExerciseEntity>)
    @Upsert suspend fun upsertTemplates(values: List<WorkoutTemplateEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertWorkoutExercises(values: List<WorkoutExerciseEntity>)
    @Query("DELETE FROM workout_exercise WHERE id = :id") suspend fun deleteWorkoutExercise(id: String)
    @Query("SELECT * FROM workout_template WHERE active = 1 AND rotationIndex = :index LIMIT 1") suspend fun templateForIndex(index: Int): WorkoutTemplateEntity?
    @Query("SELECT * FROM workout_template ORDER BY rotationIndex") fun observeTemplates(): Flow<List<WorkoutTemplateEntity>>
    @Query("SELECT * FROM workout_template WHERE id = :id") suspend fun templateById(id: String): WorkoutTemplateEntity?
    @Query("UPDATE workout_template SET active = 0") suspend fun archiveTemplates()
    @Query("SELECT * FROM workout_exercise ORDER BY templateId, orderIndex") fun observeAllWorkoutExercises(): Flow<List<WorkoutExerciseEntity>>
    @Query("SELECT * FROM exercise ORDER BY name") fun observeExercises(): Flow<List<ExerciseEntity>>
    @Query("SELECT * FROM workout_day_override ORDER BY localDate") fun observeDayOverrides(): Flow<List<WorkoutDayOverrideEntity>>
    @Upsert suspend fun upsertDayOverrides(values: List<WorkoutDayOverrideEntity>)
    @Query("DELETE FROM workout_day_override WHERE localDate >= :date") suspend fun clearFutureOverrides(date: String)
    @Query("DELETE FROM workout_session WHERE id = :id") suspend fun deleteWorkoutSession(id: String)
    @Query("SELECT * FROM system_proposal WHERE status = 'PENDING' ORDER BY createdAt DESC LIMIT 1") fun observeProposal(): Flow<SystemProposalEntity?>
    @Query("SELECT * FROM system_proposal WHERE id = :id") suspend fun proposalById(id: String): SystemProposalEntity?
    @Upsert suspend fun upsertProposal(value: SystemProposalEntity)
    @Query("UPDATE system_proposal SET status = 'SUPERSEDED' WHERE status = 'PENDING'") suspend fun supersedeProposals()
    @Transaction @Query("SELECT * FROM workout_exercise WHERE templateId = :templateId ORDER BY orderIndex")
    suspend fun templateExercises(templateId: String): List<WorkoutExerciseDetail>
    @Insert suspend fun insertWorkoutSession(value: WorkoutSessionEntity)
    @Transaction
    suspend fun insertWorkoutWithSets(session: WorkoutSessionEntity, sets: List<WorkoutSetEntity>) {
        insertWorkoutSession(session)
        if (sets.isNotEmpty()) upsertWorkoutSets(sets)
    }
    @Update suspend fun updateWorkoutSession(value: WorkoutSessionEntity)
    @Query("SELECT * FROM workout_session WHERE id = :id") suspend fun workoutSession(id: String): WorkoutSessionEntity?
    @Query("SELECT * FROM workout_session WHERE localDate = :date AND completedAt IS NOT NULL LIMIT 1") suspend fun completedWorkout(date: String): WorkoutSessionEntity?
    @Query("SELECT * FROM workout_session WHERE localDate = :date ORDER BY startedAt DESC LIMIT 1") suspend fun latestWorkout(date: String): WorkoutSessionEntity?
    @Query("SELECT * FROM workout_session ORDER BY startedAt DESC") fun observeWorkoutHistory(): Flow<List<WorkoutSessionEntity>>
    @Query("SELECT COUNT(*) FROM workout_session AS session INNER JOIN workout_template AS template ON session.templateId = template.id WHERE session.completedAt IS NOT NULL AND template.isRecovery = 0")
    suspend fun completedWorkoutCount(): Int
    @Query("SELECT COUNT(*) FROM workout_session AS session INNER JOIN workout_template AS template ON session.templateId = template.id WHERE session.completedAt IS NOT NULL AND template.isRecovery = 0 AND session.localDate BETWEEN :start AND :end")
    suspend fun completedTrainingCountBetween(start: String, end: String): Int
    @Query("SELECT COUNT(*) FROM workout_session WHERE completedAt IS NOT NULL AND templateId != :restTemplateId AND localDate BETWEEN :start AND :end")
    suspend fun completedScheduledProtocolCountBetween(start: String, end: String, restTemplateId: String): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertWorkoutSets(values: List<WorkoutSetEntity>)
    @Update suspend fun updateWorkoutSet(value: WorkoutSetEntity)
    @Query("SELECT * FROM workout_set WHERE sessionId = :sessionId ORDER BY exerciseId, setNumber") fun observeWorkoutSets(sessionId: String): Flow<List<WorkoutSetEntity>>
    @Query("SELECT * FROM workout_set WHERE exerciseId = :exerciseId AND completed = 1 ORDER BY completedAt DESC") suspend fun completedSets(exerciseId: String): List<WorkoutSetEntity>
    @Query("SELECT * FROM workout_set WHERE sessionId = :sessionId AND exerciseId = :exerciseId ORDER BY setNumber") suspend fun setsForSessionExercise(sessionId: String, exerciseId: String): List<WorkoutSetEntity>
    @Query("DELETE FROM workout_set WHERE sessionId = :sessionId AND exerciseId = :exerciseId") suspend fun deleteSessionExerciseSets(sessionId: String, exerciseId: String)
    @Query("SELECT SUM(weightKg * reps) FROM workout_set WHERE completed = 1") fun observeTrainingVolume(): Flow<Double?>

    @Query("SELECT * FROM habit WHERE active = 1 ORDER BY createdAt") fun observeHabits(): Flow<List<HabitEntity>>
    @Upsert suspend fun upsertHabit(value: HabitEntity)
    @Query("SELECT * FROM habit_completion WHERE localDate = :date") fun observeHabitCompletions(date: String): Flow<List<HabitCompletionEntity>>
    @Query("SELECT * FROM habit_completion WHERE habitId = :habitId ORDER BY localDate") suspend fun habitHistory(habitId: String): List<HabitCompletionEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertHabitCompletion(value: HabitCompletionEntity)
    @Query("DELETE FROM habit_completion WHERE habitId = :habitId AND localDate = :date") suspend fun deleteHabitCompletion(habitId: String, date: String)

    // Seeded quests are refreshed on every launch. A REPLACE would delete each parent row and
    // cascade-delete the player's quest_completion rows, so use an in-place upsert instead.
    @Upsert suspend fun upsertQuests(values: List<QuestEntity>)
    @Query("SELECT * FROM quest WHERE active = 1 ORDER BY type, category") fun observeQuests(): Flow<List<QuestEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertQuestCompletion(value: QuestCompletionEntity): Long
    @Query("DELETE FROM quest_completion WHERE questId = :questId AND sourceDate = :date")
    suspend fun deleteQuestCompletion(questId: String, date: String)
    @Query("SELECT * FROM quest_completion WHERE sourceDate = :date") fun observeQuestCompletions(date: String): Flow<List<QuestCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertXp(value: XpTransactionEntity): Long
    @Query("DELETE FROM xp_transaction WHERE sourceType = :sourceType AND sourceId = :sourceId AND sourceDate = :date")
    suspend fun deleteXp(sourceType: String, sourceId: String, date: String)
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_transaction") fun observeLifetimeXp(): Flow<Int>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_transaction WHERE sourceDate = :date") fun observeXpForDate(date: String): Flow<Int>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_transaction WHERE sourceType = 'HABIT' AND sourceDate = :date") suspend fun habitXpForDate(date: String): Int
    @Query("SELECT * FROM xp_transaction ORDER BY createdAt DESC LIMIT :limit") fun observeXpLedger(limit: Int = 50): Flow<List<XpTransactionEntity>>

    // Default achievements are immutable seed rows. Ignoring existing IDs preserves their
    // unlocked_achievement children instead of delete/reinserting the parent rows.
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAchievementsIfAbsent(values: List<AchievementEntity>)
    @Query("SELECT * FROM achievement ORDER BY category, threshold") fun observeAchievements(): Flow<List<AchievementEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun unlockAchievement(value: UnlockedAchievementEntity): Long
    @Query("SELECT * FROM unlocked_achievement") fun observeUnlockedAchievements(): Flow<List<UnlockedAchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertDailySummary(value: DailySummaryEntity)
    @Query("SELECT * FROM daily_summary WHERE localDate = :date") fun observeDailySummary(date: String): Flow<DailySummaryEntity?>
    @Query("SELECT * FROM daily_summary WHERE localDate = :date") suspend fun dailySummary(date: String): DailySummaryEntity?
    @Query("SELECT * FROM daily_summary ORDER BY localDate") fun observeDailySummaries(): Flow<List<DailySummaryEntity>>
    @Query("SELECT * FROM daily_summary WHERE localDate BETWEEN :start AND :end ORDER BY localDate") suspend fun dailySummariesBetween(start: String, end: String): List<DailySummaryEntity>

    @Query("SELECT * FROM coach_message ORDER BY createdAt") fun observeSystemMessages(): Flow<List<SystemMessageEntity>>
    @Query("SELECT * FROM coach_message ORDER BY createdAt DESC LIMIT :limit") suspend fun recentSystemMessages(limit: Int = 8): List<SystemMessageEntity>
    @Insert suspend fun insertSystemMessage(value: SystemMessageEntity)
    @Query("DELETE FROM coach_message") suspend fun clearSystemMessages()
}
