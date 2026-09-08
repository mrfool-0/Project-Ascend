package com.ascend.app.domain

import org.junit.Assert.*
import org.junit.Test

class WorkoutInputRulesTest {
    @Test fun bodyweightSetsCanBeCompletedWithoutAddedLoad() {
        assertTrue(WorkoutInputRules.isValidSet(0.0, 12, true))
    }

    @Test fun invalidDraftsCannotBecomeCompletedSets() {
        assertFalse(WorkoutInputRules.isValidSet(null, 12, true))
        assertFalse(WorkoutInputRules.isValidSet(Double.NaN, 12, true))
        assertFalse(WorkoutInputRules.isValidSet(Double.POSITIVE_INFINITY, 12, true))
        assertFalse(WorkoutInputRules.isValidSet(-1.0, 12, true))
        assertFalse(WorkoutInputRules.isValidSet(1500.01, 12, true))
        assertFalse(WorkoutInputRules.isValidSet(30.0, 0, true))
        assertFalse(WorkoutInputRules.isValidSet(30.0, 1001, true))
        assertTrue(WorkoutInputRules.isValidSet(0.0, 0, false))
    }

    @Test fun decimalEditsAllowTransientInputButRejectMultipleSeparators() {
        listOf("", ".", "0.", "12.5", "1500", "2.25").forEach { assertTrue(it, WorkoutInputRules.weightInput(it)) }
        listOf("1..2", "12.555", "-20", "1e3", "10000", "NaN").forEach { assertFalse(it, WorkoutInputRules.weightInput(it)) }
        assertFalse(WorkoutInputRules.isValidSet(".".toDoubleOrNull(), 10, true))
    }

    @Test fun exerciseBoundsMatchWhatTheEditorPromises() {
        assertTrue(WorkoutInputRules.isValidExercise("Cable row", 3, 8, 12))
        assertTrue(WorkoutInputRules.isValidExercise("Squat", 10, 100, 100))
        assertFalse(WorkoutInputRules.isValidExercise(" ", 3, 8, 12))
        assertFalse(WorkoutInputRules.isValidExercise("Row", 11, 8, 12))
        assertFalse(WorkoutInputRules.isValidExercise("Row", 3, 12, 8))
        assertFalse(WorkoutInputRules.isValidExercise("Row", 3, 1, 101))
        assertFalse(WorkoutInputRules.isValidExercise("Row", null, 8, 12))
    }
}
