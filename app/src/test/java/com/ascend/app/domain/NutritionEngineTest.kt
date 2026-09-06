package com.ascend.app.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class NutritionEngineTest {
    @Test fun `mifflin st jeor male calculation and macros are coherent`() {
        val result = NutritionEngine.calculate(80.0, 180.0, 30, BiologicalSex.MALE, ActivityLevel.MODERATE, Objective.MAINTAIN_FITNESS)
        assertEquals(1_780, result.bmr)
        assertEquals(2_759, result.maintenanceCalories)
        val macroCalories = result.proteinGrams * 4 + result.carbohydrateGrams * 4 + result.fatGrams * 9
        assertTrue(abs(result.calories - macroCalories) <= 4)
    }

    @Test fun `objective adjusts maintenance without rewarding restriction`() {
        val maintain = NutritionEngine.calculate(70.0, 170.0, 30, BiologicalSex.FEMALE, ActivityLevel.LIGHT, Objective.MAINTAIN_FITNESS)
        val loss = NutritionEngine.calculate(70.0, 170.0, 30, BiologicalSex.FEMALE, ActivityLevel.LIGHT, Objective.FAT_LOSS)
        assertTrue(loss.calories < maintain.calories)
        assertEquals(1.0, NutritionEngine.adherence(2_000, 2_000), 0.0)
        assertTrue(NutritionEngine.adherence(800, 2_000) < NutritionEngine.adherence(1_900, 2_000))
    }

    @Test fun `generated targets remain editable-rule safe at supported extremes`() {
        listOf(
            NutritionEngine.calculate(25.0, 100.0, 100, BiologicalSex.FEMALE, ActivityLevel.SEDENTARY, Objective.FAT_LOSS),
            NutritionEngine.calculate(400.0, 250.0, 18, BiologicalSex.MALE, ActivityLevel.VERY_ACTIVE, Objective.MUSCLE_GAIN),
        ).forEach { target ->
            assertTrue(
                NutritionTargetRules.isValid(
                    target.calories,
                    target.proteinGrams,
                    target.carbohydrateGrams,
                    target.fatGrams,
                    target.waterMl,
                ),
            )
        }
    }
}
