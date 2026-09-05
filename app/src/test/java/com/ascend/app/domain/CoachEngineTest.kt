package com.ascend.app.domain

import kotlin.test.Test
import kotlin.test.assertTrue

class CoachEngineTest {
    private val context = CoachContext(
        playerName = "Nova", objective = Objective.GENERAL_HEALTH,
        calorieTarget = 2200, proteinTarget = 140, caloriesLogged = 1500,
        proteinLogged = 95, waterLogged = 1500, waterTarget = 2500,
        streak = 4, todayWorkout = "UPPER", injuries = setOf(InjuryArea.SHOULDER),
        coreReason = "I want lasting energy",
    )

    @Test fun `nutrition answer uses remaining target`() {
        assertTrue("700" in CoachEngine.respond("How many calories are left?", context))
        assertTrue("45" in CoachEngine.respond("protein status", context))
    }

    @Test fun `pain answer remains within coaching boundary`() {
        val answer = CoachEngine.respond("My shoulder hurts", context)
        assertTrue("cannot diagnose" in answer)
        assertTrue("clinician" in answer)
    }

    @Test fun `emergency language escalates`() {
        assertTrue("emergency services" in CoachEngine.respond("I have chest pain and can't breathe", context))
    }
}
