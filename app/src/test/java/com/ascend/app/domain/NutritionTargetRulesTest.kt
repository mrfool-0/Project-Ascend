package com.ascend.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NutritionTargetRulesTest {
    @Test fun `accepts a coherent everyday target`() {
        assertTrue(NutritionTargetRules.isValid(2_200, 160, 245, 65, 3_000))
    }

    @Test fun `rejects unsafe bounds and incoherent macro energy`() {
        assertFalse(NutritionTargetRules.isValid(9_000, 160, 245, 65, 3_000))
        assertFalse(NutritionTargetRules.isValid(2_200, 500, 245, 65, 3_000))
        assertFalse(NutritionTargetRules.isValid(2_200, 160, 245, 65, 7_000))
        assertFalse(NutritionTargetRules.isValid(2_200, 20, 0, 20, 3_000))
    }

    @Test fun `bounds typed numbers instead of allowing overflow`() {
        assertEquals("8000", NutritionTargetRules.boundedIntegerInput("999999999", NutritionTargetRules.MAX_CALORIES))
        assertEquals("250", NutritionTargetRules.boundedIntegerInput("25g0", NutritionTargetRules.MAX_FAT))
        assertEquals("", NutritionTargetRules.boundedIntegerInput("", NutritionTargetRules.MAX_WATER_ML))
    }

    @Test fun `converts liters and caps water`() {
        assertEquals("3500", NutritionTargetRules.waterInputToMl("3.5 L"))
        assertEquals("6000", NutritionTargetRules.waterInputToMl("9 L"))
        assertEquals("2750", NutritionTargetRules.waterInputToMl("2750"))
    }
}
