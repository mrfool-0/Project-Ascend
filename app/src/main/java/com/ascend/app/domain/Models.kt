package com.ascend.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

enum class BiologicalSex { MALE, FEMALE, UNSPECIFIED }
enum class UnitSystem { METRIC, IMPERIAL }
enum class Objective { FAT_LOSS, MUSCLE_GAIN, RECOMPOSITION, MAINTAIN_FITNESS, GENERAL_HEALTH, BUILD_CONSISTENCY }
enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.20), LIGHT(1.375), MODERATE(1.55), VERY_ACTIVE(1.725)
}
enum class Experience { BEGINNER, INTERMEDIATE, ADVANCED }
enum class Equipment { FULL_GYM, DUMBBELLS, HOME_GYM, BODYWEIGHT }
enum class DietPreference { VEGETARIAN, EGGETARIAN, NON_VEGETARIAN, VEGAN, CUSTOM }
enum class TrainingTime { MORNING, AFTERNOON, EVENING, CUSTOM }
enum class TrainingSplit(val displayName: String, val description: String) {
    AUTO("AUTO", "Frequency-optimized weekly architecture"),
    FULL_BODY("FULL BODY", "Every training day covers the complete body"),
    PUSH_PULL_LEGS("PUSH / PULL / LEGS", "Movement-pattern rotation with balanced volume"),
    UPPER_LOWER("UPPER / LOWER", "Alternating upper and lower body sessions"),
}
enum class FocusArea { FULL_BODY, CHEST, BACK, SHOULDERS, ARMS, CORE, GLUTES, LEGS, ENDURANCE, MOBILITY }
enum class InjuryArea { NONE, LOWER_BACK, KNEE, SHOULDER, HIP, WRIST, ANKLE, CARDIOVASCULAR, OTHER }
enum class HabitType { CHECKBOX, NUMBER, DURATION, AVOIDANCE }
enum class HabitDifficulty(val xp: Int) { NORMAL(10), MEDIUM(15), HARD(25) }
enum class HabitFrequency { EVERY_DAY, WEEKDAYS, THREE_TIMES_WEEKLY, CUSTOM }
enum class MealType { BREAKFAST, LUNCH, DINNER, SNACKS }
enum class QuestType { DAILY, WEEKLY, ACHIEVEMENT }
enum class QuestCategory { TRAINING, NUTRITION, HYDRATION, DISCIPLINE, RECOVERY, CONSISTENCY }
enum class XpSourceType { WORKOUT, EXERCISE, HABIT, NUTRITION, HYDRATION, WEIGHT, QUEST, ACHIEVEMENT, PERSONAL_RECORD, PERFECT_DAY }

data class NutritionCalculation(
    val bmr: Int,
    val maintenanceCalories: Int,
    val calories: Int,
    val proteinGrams: Int,
    val carbohydrateGrams: Int,
    val fatGrams: Int,
    val waterMl: Int,
)

data class LevelProgress(
    val level: Int,
    val rank: PlayerRank,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int,
    val lifetimeXp: Int,
    val isMaxLevel: Boolean,
) {
    val fraction: Float get() = if (isMaxLevel) 1f else (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)
}

enum class PlayerRank(val title: String, val firstLevel: Int) {
    RECRUIT("RECRUIT", 1), STRIKER("STRIKER", 10), VANGUARD("VANGUARD", 20),
    ELITE("ELITE", 30), CHAMPION("CHAMPION", 40), ASCENDANT("ASCENDANT", 50),
    MYTHIC("MYTHIC", 60), PARAGON("PARAGON", 70), APEX("APEX", 80),
    SOVEREIGN("SOVEREIGN", 90), TRANSCENDENT("TRANSCENDENT", 100),
}

data class StreakResult(val current: Int, val longest: Int)

data class PerformanceSet(val weightKg: Double, val reps: Int)
data class PersonalRecordResult(
    val heaviestWeight: Boolean,
    val mostRepsAtWeight: Boolean,
    val estimatedOneRepMax: Double,
    val estimatedOneRepMaxRecord: Boolean,
) {
    val isRecord: Boolean get() = heaviestWeight || mostRepsAtWeight || estimatedOneRepMaxRecord
}

data class DailyCompletionInput(
    val workoutCompleted: Boolean,
    val restDayCompleted: Boolean,
    val calorieRatio: Double,
    val proteinRatio: Double,
    val hydrationRatio: Double,
    val habitsPlanned: Int,
    val habitsCompleted: Int,
)

data class DailyQuestState(
    val workout: Boolean,
    val calories: Boolean,
    val protein: Boolean,
    val hydration: Boolean,
    val discipline: Boolean,
) {
    val completionCount: Int get() = listOf(workout, calories, protein, hydration, discipline).count { it }
    val perfectDay: Boolean get() = completionCount == 5
}

data class WeeklyQuestProgress(
    val workouts: Int,
    val calorieGoalDays: Int,
    val habitCompletionRatio: Double,
    val progressedExercises: Int,
    val workoutTarget: Int = 6,
) {
    val ironWeekComplete get() = workouts >= workoutTarget.coerceAtLeast(1)
    val nutritionControlComplete get() = calorieGoalDays >= 5
    val consistencyComplete get() = habitCompletionRatio >= .8
    val overloadComplete get() = progressedExercises >= 3
}

data class WeightTrend(
    val current: Double?,
    val sevenDayAverage: Double?,
    val thirtyDayChange: Double?,
)

data class WorkoutDay(val index: Int, val code: String, val title: String, val isRecovery: Boolean = false)

object AscendConfig {
    const val MAX_LEVEL = 100
    const val MAX_HABIT_XP_PER_DAY = 75
    const val WORKOUT_XP = 200
    const val RECOVERY_XP = 100
    const val CALORIE_XP = 40
    const val PROTEIN_XP = 40
    const val HYDRATION_XP = 30
    const val WEIGHT_XP = 15
    const val PERFECT_DAY_XP = 75
    const val PERSONAL_RECORD_XP = 25
    const val CALORIE_LOWER_RATIO = .90
    const val CALORIE_UPPER_RATIO = 1.10
}

object DefaultProgram {
    val rotation = listOf(
        WorkoutDay(0, "PUSH_A", "PUSH A"), WorkoutDay(1, "PULL_A", "PULL A"),
        WorkoutDay(2, "LEGS_A", "LEGS A"), WorkoutDay(3, "RECOVERY", "RECOVERY PROTOCOL", true),
        WorkoutDay(4, "PUSH_B", "PUSH B"), WorkoutDay(5, "PULL_B", "PULL B"),
        WorkoutDay(6, "LEGS_B", "LEGS B"),
    )

    fun dayFor(date: LocalDate, startingDate: LocalDate): WorkoutDay {
        val offset = Math.floorMod(java.time.temporal.ChronoUnit.DAYS.between(startingDate, date).toInt(), rotation.size)
        return rotation[offset]
    }

    fun weekdays(vararg days: DayOfWeek): String = days.joinToString(",") { it.value.toString() }
}

object MotivationLibrary {
    val quotes = listOf(
        "You do not rise to a wish. You rise to a repeated action.",
        "Every honest rep is a vote for the player you are becoming.",
        "Discipline is remembering what you chose when the mood has changed.",
        "Small quests, completed relentlessly, become a different life.",
        "Your future strength is being built by today's ordinary decision.",
        "The system does not demand perfection. It rewards your return.",
        "A difficult day is not a failed campaign; it is part of the map.",
        "Progress becomes inevitable when the next action becomes obvious.",
        "Train the identity first. The results will learn to follow.",
        "The player who keeps promises to themselves becomes hard to stop.",
        "Recovery is not retreat. It is where the next level is forged.",
        "Do the smallest worthy thing now; momentum will meet you there.",
    )

    fun quoteFor(date: LocalDate): String = quotes[Math.floorMod(date.toEpochDay().toInt(), quotes.size)]
}
