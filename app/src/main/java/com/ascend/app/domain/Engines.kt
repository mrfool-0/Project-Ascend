package com.ascend.app.domain

import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.roundToInt

object NutritionEngine {
    fun calculate(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        sex: BiologicalSex,
        activity: ActivityLevel,
        objective: Objective,
    ): NutritionCalculation {
        require(weightKg in 25.0..400.0) { "Weight must be between 25 and 400 kg" }
        require(heightCm in 100.0..250.0) { "Height must be between 100 and 250 cm" }
        require(age in 18..100) { "ASCEND currently supports adult players aged 18 to 100" }
        val sexOffset = when (sex) {
            BiologicalSex.MALE -> 5
            BiologicalSex.FEMALE -> -161
            BiologicalSex.UNSPECIFIED -> -78
        }
        val bmr = (10 * weightKg + 6.25 * heightCm - 5 * age + sexOffset).roundToInt()
        val maintenance = (bmr * activity.multiplier).roundToInt()
        val objectiveMultiplier = when (objective) {
            Objective.FAT_LOSS -> .85
            Objective.MUSCLE_GAIN -> 1.08
            Objective.RECOMPOSITION -> .95
            Objective.MAINTAIN_FITNESS, Objective.GENERAL_HEALTH, Objective.BUILD_CONSISTENCY -> 1.0
        }
        val calories = (maintenance * objectiveMultiplier).roundToInt().coerceIn(1_200, NutritionTargetRules.MAX_CALORIES)
        val protein = (weightKg * 2.0).roundToInt().coerceIn(NutritionTargetRules.MIN_PROTEIN, NutritionTargetRules.MAX_PROTEIN)
        val fat = (weightKg * .8).roundToInt().coerceIn(NutritionTargetRules.MIN_FAT, NutritionTargetRules.MAX_FAT)
        val carbs = ((calories - protein * 4 - fat * 9) / 4.0).roundToInt()
            .coerceIn(NutritionTargetRules.MIN_CARBS, NutritionTargetRules.MAX_CARBS)
        val water = ((weightKg * 35 / 250).roundToInt() * 250).coerceIn(1_500, 5_000)
        return NutritionCalculation(bmr, maintenance, calories, protein, carbs, fat, water)
    }

    fun adherence(actual: Int, target: Int, lower: Double = .9, upper: Double = 1.1): Double {
        if (target <= 0) return 0.0
        val ratio = actual.toDouble() / target
        return when {
            ratio in lower..upper -> 1.0
            ratio < lower -> (ratio / lower).coerceIn(0.0, 1.0)
            else -> (upper / ratio).coerceIn(0.0, 1.0)
        }
    }
}

object LevelEngine {
    fun xpForNextLevel(level: Int): Int {
        if (level >= AscendConfig.MAX_LEVEL) return 0
        require(level >= 1)
        return 100 + (10 * level.toDouble().pow(1.2)).roundToInt()
    }

    fun fromLifetimeXp(xp: Int): LevelProgress {
        var remaining = xp.coerceAtLeast(0)
        var level = 1
        while (level < AscendConfig.MAX_LEVEL) {
            val needed = xpForNextLevel(level)
            if (remaining < needed) break
            remaining -= needed
            level++
        }
        return LevelProgress(
            level = level,
            rank = rankFor(level),
            xpIntoLevel = if (level == AscendConfig.MAX_LEVEL) 0 else remaining,
            xpForNextLevel = xpForNextLevel(level),
            lifetimeXp = xp.coerceAtLeast(0),
            isMaxLevel = level == AscendConfig.MAX_LEVEL,
        )
    }

    fun rankFor(level: Int): PlayerRank = PlayerRank.entries.last { level.coerceIn(1, 100) >= it.firstLevel }

    fun lifetimeXpAtLevel(level: Int): Int = (1 until level.coerceIn(1, AscendConfig.MAX_LEVEL)).sumOf(::xpForNextLevel)
}

object StreakEngine {
    fun calculate(completedDates: Collection<LocalDate>, today: LocalDate): StreakResult {
        val dates = completedDates.filterNot { it.isAfter(today) }.distinct().sorted()
        if (dates.isEmpty()) return StreakResult(0, 0)
        var longest = 1
        var run = 1
        for (index in 1 until dates.size) {
            if (dates[index - 1].plusDays(1) == dates[index]) {
                run++
                longest = maxOf(longest, run)
            } else run = 1
        }
        val set = dates.toSet()
        var cursor = if (today in set) today else today.minusDays(1)
        var current = 0
        while (cursor in set) {
            current++
            cursor = cursor.minusDays(1)
        }
        return StreakResult(current, longest)
    }
}

object QuestEngine {
    fun daily(input: DailyCompletionInput): DailyQuestState {
        val planned = input.habitsPlanned
        return DailyQuestState(
            workout = input.workoutCompleted || input.restDayCompleted,
            calories = input.calorieRatio in AscendConfig.CALORIE_LOWER_RATIO..AscendConfig.CALORIE_UPPER_RATIO,
            protein = input.proteinRatio >= 1.0,
            hydration = input.hydrationRatio >= 1.0,
            discipline = planned > 0 && input.habitsCompleted.toDouble() / planned >= .8,
        )
    }

    fun weekly(progress: WeeklyQuestProgress): Map<String, Boolean> = linkedMapOf(
        "IRON WEEK" to progress.ironWeekComplete,
        "NUTRITION CONTROL" to progress.nutritionControlComplete,
        "CONSISTENCY" to progress.consistencyComplete,
        "PROGRESSIVE OVERLOAD" to progress.overloadComplete,
    )
}

object PersonalRecordEngine {
    fun detect(candidate: PerformanceSet, history: List<PerformanceSet>): PersonalRecordResult {
        require(candidate.weightKg >= 0 && candidate.reps > 0)
        val oneRm = epley(candidate.weightKg, candidate.reps)
        return PersonalRecordResult(
            heaviestWeight = history.none { it.weightKg >= candidate.weightKg },
            mostRepsAtWeight = history.filter { it.weightKg == candidate.weightKg }.none { it.reps >= candidate.reps },
            estimatedOneRepMax = oneRm,
            estimatedOneRepMaxRecord = history.none { epley(it.weightKg, it.reps) >= oneRm },
        )
    }

    fun epley(weightKg: Double, reps: Int): Double = weightKg * (1 + reps.coerceAtMost(15) / 30.0)
}

object WeightTrendEngine {
    fun calculate(entries: List<Pair<LocalDate, Double>>, today: LocalDate): WeightTrend {
        val sorted = entries.filterNot { it.first.isAfter(today) }.sortedBy { it.first }
        val current = sorted.lastOrNull()?.second
        val average = sorted.filter { !it.first.isBefore(today.minusDays(6)) }.map { it.second }.takeIf { it.isNotEmpty() }?.average()
        val baseline = sorted.lastOrNull { !it.first.isAfter(today.minusDays(30)) }?.second ?: sorted.firstOrNull()?.second
        return WeightTrend(current, average, if (current != null && baseline != null) current - baseline else null)
    }
}

class InMemoryXpLedger {
    private val transactions = linkedMapOf<Triple<XpSourceType, String, LocalDate>, Int>()
    val total: Int get() = transactions.values.sum()

    fun award(type: XpSourceType, sourceId: String, date: LocalDate, amount: Int): Boolean {
        require(amount >= 0)
        val key = Triple(type, sourceId, date)
        if (key in transactions) return false
        transactions[key] = amount
        return true
    }

    fun revert(type: XpSourceType, sourceId: String, date: LocalDate): Boolean = transactions.remove(Triple(type, sourceId, date)) != null
}
