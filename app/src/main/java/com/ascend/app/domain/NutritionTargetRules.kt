package com.ascend.app.domain

import java.util.Locale
import kotlin.math.roundToInt

object NutritionTargetRules {
    const val MIN_CALORIES = 1_000
    const val MAX_CALORIES = 8_000
    const val MIN_PROTEIN = 20
    const val MAX_PROTEIN = 400
    const val MIN_CARBS = 0
    const val MAX_CARBS = 1_000
    const val MIN_FAT = 20
    const val MAX_FAT = 250
    const val MIN_WATER_ML = 500
    const val MAX_WATER_ML = 6_000

    fun macroCalories(protein: Int, carbs: Int, fat: Int): Int = protein * 4 + carbs * 4 + fat * 9

    fun isValid(calories: Int, protein: Int, carbs: Int, fat: Int, waterMl: Int): Boolean {
        val macroEnergy = macroCalories(protein, carbs, fat)
        return calories in MIN_CALORIES..MAX_CALORIES &&
            protein in MIN_PROTEIN..MAX_PROTEIN &&
            carbs in MIN_CARBS..MAX_CARBS &&
            fat in MIN_FAT..MAX_FAT &&
            waterMl in MIN_WATER_ML..MAX_WATER_ML &&
            macroEnergy in (calories * .70).roundToInt()..(calories * 1.20).roundToInt()
    }

    fun boundedIntegerInput(raw: String, maximum: Int): String {
        val digits = raw.filter(Char::isDigit).take(6)
        if (digits.isBlank()) return ""
        return (digits.toLongOrNull() ?: maximum.toLong()).coerceAtMost(maximum.toLong()).toString()
    }

    fun waterInputToMl(raw: String): String {
        val normalized = raw.trim().lowercase(Locale.US).replace(" ", "")
        if (normalized.isBlank()) return ""
        val liters = normalized.endsWith("l") || normalized.contains('.')
        val milliliters = if (liters) {
            normalized.removeSuffix("l").toDoubleOrNull()?.times(1_000)?.roundToInt()
        } else {
            normalized.filter(Char::isDigit).toIntOrNull()
        } ?: return ""
        return milliliters.coerceIn(0, MAX_WATER_ML).toString()
    }

    fun waterLabel(waterMl: Int): String = String.format(Locale.US, "%,d ml  /  %.2f L", waterMl, waterMl / 1_000.0)
}
