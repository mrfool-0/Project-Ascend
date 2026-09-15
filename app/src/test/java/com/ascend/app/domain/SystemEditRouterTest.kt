package com.ascend.app.domain

import org.junit.Assert.*
import org.junit.Test

class SystemEditRouterTest {
    @Test fun restrictedOrCompoundRequestsNeverSilentlyChangeWholeProgram() {
        for (text in listOf("Change all sets in push to 3 sets", "Change all my sessions except legs to 3 sets", "Change all sessions to 3 sets and add 10 minutes of treadmill")) {
            assertEquals(SystemActionType.NONE, SystemEditRouter.resolve(text, emptyList())!!.action.type)
            assertFalse(SystemEditRouter.permits(text, SystemAction(SystemActionType.SET_ALL_STRENGTH_SETS, sets = 3)))
        }
    }
    @Test fun screenshotRequestChoosesNewCountNotOldCount() {
        val reply = SystemEditRouter.resolve("Can you change all my session sets to 3 sets instead of 2 set", emptyList())!!
        assertEquals(SystemActionType.SET_ALL_STRENGTH_SETS, reply.action.type)
        assertEquals(3, reply.action.sets)
    }
    @Test fun treadmillWithoutDurationAsksQuestionInsteadOfCreatingHabit() {
        val reply = SystemEditRouter.resolve("Add a treadmill session to all my existing sessions", emptyList())!!
        assertEquals(SystemActionType.NONE, reply.action.type)
        assertTrue(reply.message.contains("How many minutes"))
    }
    @Test fun treadmillFollowUpRetainsWorkoutAndAllSessionsScope() {
        for (answer in listOf("10", "10 minutes")) {
            val reply = SystemEditRouter.resolve(answer, emptyList(), "Add treadmill to all my sessions")!!
            assertEquals(SystemActionType.ADD_EXERCISE_ALL, reply.action.type)
            assertEquals(600, reply.action.durationSeconds)
        }
    }
    @Test fun crossDomainAndPartialScopeModelSuggestionsAreRejected() {
        val text = "Add treadmill to all my sessions"
        assertFalse(SystemEditRouter.permits(text, SystemAction(SystemActionType.CREATE_HABIT)))
        assertFalse(SystemEditRouter.permits(text, SystemAction(SystemActionType.ADD_EXERCISE)))
    }
    @Test fun explicitTreadmillHabitIsNotReclassifiedAsWorkout() {
        assertNull(SystemEditRouter.resolve("Add a treadmill habit for 10 minutes daily", emptyList()))
        assertEquals(SystemActionType.CREATE_HABIT, SystemCommandParser.parse("Add a treadmill habit for 10 minutes daily")?.type)
    }
    @Test fun negatedOrHypotheticalRequestsDoNotChangeAnything() {
        assertNull(SystemEditRouter.resolve("Don't change all sessions to 3 sets", emptyList()))
        assertNull(SystemEditRouter.resolve("What if I change all sessions to 3 sets", emptyList()))
        assertFalse(SystemCommandParser.mayPropose("Don't delete my reading habit"))
    }
    @Test fun excessiveDurationAndCountsAreNotSilentlyClamped() {
        assertEquals(SystemActionType.NONE, SystemEditRouter.resolve("Add 900 minutes of treadmill to all sessions", emptyList())!!.action.type)
        assertEquals(SystemActionType.NONE, SystemEditRouter.resolve("Set all sessions to 99 sets", emptyList())!!.action.type)
    }
}
