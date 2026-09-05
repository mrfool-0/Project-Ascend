package com.ascend.app.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String,
    val birthDate: String,
    val heightCm: Double,
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    val biologicalSex: String,
    val unitSystem: String,
    val objective: String,
    val activityLevel: String,
    val experience: String,
    val equipment: String,
    val dietPreference: String,
    val trainingTime: String,
    val focusAreas: String = "",
    val injuries: String = "NONE",
    val injuryNotes: String = "",
    val workoutFrequency: Int = 3,
    val workoutDays: String = "1,3,5",
    val futureVision: String = "",
    val coreReason: String = "",
    val minimumPromise: String = "",
    val googleAccountEmail: String? = null,
    val programStartDate: String,
    val createdAt: Long,
)

@Entity(
    tableName = "nutrition_target",
    foreignKeys = [ForeignKey(UserProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class NutritionTargetEntity(
    @PrimaryKey val profileId: Int = 1,
    val calories: Int,
    val proteinGrams: Int,
    val carbohydrateGrams: Int,
    val fatGrams: Int,
    val waterMl: Int,
    val estimatedBmr: Int,
    val estimatedMaintenance: Int,
    val manuallyEdited: Boolean = false,
)

@Entity(tableName = "food", indices = [Index(value = ["name"])])
data class FoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val servingQuantity: Double,
    val servingUnit: String,
    val calories: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double? = null,
    val saturatedFatGrams: Double? = null,
    val sugarGrams: Double? = null,
    val sodiumMg: Double? = null,
    val barcode: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long,
)

@Entity(tableName = "saved_meal")
data class SavedMealEntity(@PrimaryKey val id: String, val name: String, val createdAt: Long)

@Entity(
    tableName = "saved_meal_item",
    primaryKeys = ["savedMealId", "foodId"],
    foreignKeys = [
        ForeignKey(SavedMealEntity::class, ["id"], ["savedMealId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(FoodEntity::class, ["id"], ["foodId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("foodId")],
)
data class SavedMealItemEntity(val savedMealId: String, val foodId: String, val servings: Double)

@Entity(
    tableName = "food_log",
    foreignKeys = [ForeignKey(FoodEntity::class, ["id"], ["foodId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("foodId"), Index("localDate")],
)
data class FoodLogEntity(
    @PrimaryKey val id: String,
    val foodId: String,
    val localDate: String,
    val mealType: String,
    val servings: Double,
    val loggedAt: Long,
)

@Entity(tableName = "weight_entry", indices = [Index(value = ["localDate", "loggedAt"])])
data class WeightEntryEntity(
    @PrimaryKey val id: String,
    val weightKg: Double,
    val localDate: String,
    val loggedAt: Long,
    val note: String = "",
)

@Entity(tableName = "body_measurement", indices = [Index("localDate")])
data class BodyMeasurementEntity(
    @PrimaryKey val id: String,
    val localDate: String,
    val type: String,
    val value: Double,
    val unit: String,
    val note: String = "",
)

@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
)

@Entity(tableName = "workout_template")
data class WorkoutTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val rotationIndex: Int,
    val rewardXp: Int,
    val estimatedMinutes: Int,
    val isRecovery: Boolean = false,
)

@Entity(
    tableName = "workout_exercise",
    foreignKeys = [
        ForeignKey(WorkoutTemplateEntity::class, ["id"], ["templateId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("templateId"), Index("exerciseId")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val targetSets: Int,
    val minReps: Int,
    val maxReps: Int,
)

@Entity(
    tableName = "workout_session",
    foreignKeys = [ForeignKey(WorkoutTemplateEntity::class, ["id"], ["templateId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("templateId"), Index("localDate")],
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val localDate: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val notes: String = "",
)

@Entity(
    tableName = "workout_set",
    foreignKeys = [
        ForeignKey(WorkoutSessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val completed: Boolean,
    val completedAt: Long? = null,
)

@Entity(tableName = "habit")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val target: Double,
    val unit: String,
    val difficulty: String,
    val frequency: String,
    val weekdays: String = "",
    val timesPerWeek: Int = 7,
    val active: Boolean = true,
    val createdAt: Long,
)

@Entity(
    tableName = "habit_completion",
    foreignKeys = [ForeignKey(HabitEntity::class, ["id"], ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["habitId", "localDate"], unique = true), Index("localDate")],
)
data class HabitCompletionEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val localDate: String,
    val value: Double,
    val completed: Boolean,
    val completedAt: Long,
)

@Entity(tableName = "quest", indices = [Index("type")])
data class QuestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val type: String,
    val category: String,
    val target: Double,
    val rewardXp: Int,
    val active: Boolean = true,
)

@Entity(
    tableName = "quest_completion",
    foreignKeys = [ForeignKey(QuestEntity::class, ["id"], ["questId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["questId", "sourceDate"], unique = true), Index("sourceDate")],
)
data class QuestCompletionEntity(
    @PrimaryKey val id: String,
    val questId: String,
    val sourceDate: String,
    val completedAt: Long,
)

@Entity(
    tableName = "xp_transaction",
    indices = [Index(value = ["sourceType", "sourceId", "sourceDate"], unique = true), Index("sourceDate")],
)
data class XpTransactionEntity(
    @PrimaryKey val id: String,
    val amount: Int,
    val sourceType: String,
    val sourceId: String,
    val sourceDate: String,
    val description: String,
    val createdAt: Long,
)

@Entity(tableName = "achievement")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val threshold: Int,
    val rewardXp: Int,
)

@Entity(
    tableName = "unlocked_achievement",
    foreignKeys = [ForeignKey(AchievementEntity::class, ["id"], ["achievementId"], onDelete = ForeignKey.CASCADE)],
)
data class UnlockedAchievementEntity(
    @PrimaryKey val achievementId: String,
    val unlockedAt: Long,
)

@Entity(tableName = "daily_summary")
data class DailySummaryEntity(
    @PrimaryKey val localDate: String,
    val completionPercent: Int,
    val calories: Int,
    val proteinGrams: Int,
    val waterMl: Int,
    val habitsCompleted: Int,
    val workoutCompleted: Boolean,
    val xpEarned: Int,
)

@Entity(tableName = "coach_message", indices = [Index("createdAt")])
data class SystemMessageEntity(
    @PrimaryKey val id: String,
    val role: String,
    val message: String,
    val createdAt: Long,
)

data class FoodLogWithFood(
    @androidx.room.Embedded val log: FoodLogEntity,
    @androidx.room.Relation(parentColumn = "foodId", entityColumn = "id") val food: FoodEntity,
)

data class WorkoutExerciseDetail(
    @androidx.room.Embedded val link: WorkoutExerciseEntity,
    @androidx.room.Relation(parentColumn = "exerciseId", entityColumn = "id") val exercise: ExerciseEntity,
)
