package com.ascend.app.domain

import kotlin.test.Test
import kotlin.test.assertTrue

class SystemEngineTest {
    private val context = SystemContext(
        playerName = "Nova",
        objective = Objective.GENERAL_HEALTH,
        calorieTarget = 2200,
        proteinTarget = 140,
        caloriesLogged = 1500,
        proteinLogged = 95,
        waterLogged = 1500,
        waterTarget = 2500,
        streak = 4,
        todayWorkout = "UPPER",
        tomorrowWorkout = "RECOVERY PROTOCOL",
        workoutFrequency = 4,
        bmr = 1_710,
        carbohydrateTarget = 245,
        fatTarget = 65,
        weeklySplit = "UPPER A → LOWER A → UPPER B → LOWER B",
        focusAreas = setOf(FocusArea.CHEST),
        injuries = setOf(InjuryArea.SHOULDER),
        coreReason = "have lasting energy",
        futureVision = "be dependable and strong",
        minimumPromise = "do a ten-minute session",
    )

    @Test
    fun `nutrition answers use live remaining targets`() {
        assertTrue("700" in SystemEngine.respond("How many calories are left?", context))
        assertTrue("45" in SystemEngine.respond("protein status", context))
    }

    @Test
    fun `status scan references exact calibrated metrics`() {
        val answer = SystemEngine.respond("Scan my day", context)
        assertTrue("BMR 1710" in answer)
        assertTrue("P 140 g" in answer)
        assertTrue("C 245 g" in answer)
        assertTrue("F 65 g" in answer)
    }

    @Test
    fun `compound nutrition question answers both intents`() {
        val answer = SystemEngine.respond("How many calories and protein do I have left?", context)
        assertTrue("700" in answer)
        assertTrue("45" in answer)
    }

    @Test
    fun `ruthless voice stays challenging without shame`() {
        val answer = SystemEngine.respond("Be harsh and motivate me", context, SystemTone.SUPPORTIVE)
        assertTrue("Enough bargaining" in answer)
        assertTrue("now" in answer.lowercase())
    }

    @Test
    fun `short follow-up uses conversation history`() {
        val withHistory = context.copy(recentPlayerMessages = listOf("What is my next workout?"))
        val answer = SystemEngine.respond("What about tomorrow?", withHistory)
        assertTrue("RECOVERY PROTOCOL" in answer)
    }

    @Test
    fun `pain answer remains within system boundary`() {
        val answer = SystemEngine.respond("My shoulder hurts", context)
        assertTrue("cannot diagnose" in answer)
        assertTrue("clinician" in answer)
    }

    @Test
    fun `emergency language escalates immediately`() {
        assertTrue("emergency services" in SystemEngine.respond("I have chest pain and can't breathe", context))
    }
}
