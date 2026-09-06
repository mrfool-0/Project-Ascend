package com.ascend.app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SystemCommandParserTest {
    @Test fun `explicit habit command becomes a scheduled habit action`() {
        val action = SystemCommandParser.parse("Add a daily habit to walk 8000 steps")!!
        assertEquals(SystemActionType.CREATE_HABIT, action.type)
        assertEquals(8_000.0, action.target)
        assertEquals("steps", action.unit)
        assertEquals(HabitFrequency.EVERY_DAY, action.frequency)
        assertTrue("walk" in action.name.lowercase())
    }

    @Test fun `explicit custom quest extracts category and frequency`() {
        val action = SystemCommandParser.parse("Create a hydration quest to drink 500 ml on weekdays")!!
        assertEquals(SystemActionType.CREATE_QUEST, action.type)
        assertEquals(QuestCategory.HYDRATION, action.category)
        assertEquals(HabitFrequency.WEEKDAYS, action.frequency)
        assertEquals(500.0, action.target)
    }

    @Test fun `discussion about habits never mutates data`() {
        assertNull(SystemCommandParser.parse("How can I become better at building habits?"))
    }

    @Test fun `missing action name requests clarification instead of creating junk`() {
        assertNull(SystemCommandParser.parse("Add one habit"))
    }

    @Test fun `action name can appear before the habit marker`() {
        val action = SystemCommandParser.parse("Add a daily reading habit")!!
        assertEquals(SystemActionType.CREATE_HABIT, action.type)
        assertEquals("reading", action.name.lowercase())
        assertEquals(HabitFrequency.EVERY_DAY, action.frequency)
    }

    @Test fun `action name can appear before the quest marker`() {
        val action = SystemCommandParser.parse("Create a hydration quest")!!
        assertEquals(SystemActionType.CREATE_QUEST, action.type)
        assertEquals("hydration", action.name.lowercase())
        assertEquals(QuestCategory.HYDRATION, action.category)
    }

    @Test fun `target is removed from the generated title`() {
        val action = SystemCommandParser.parse("Add a habit to walk 8000 steps every day")!!
        assertEquals("walk", action.name.lowercase())
        assertEquals(8_000.0, action.target)
    }

    @Test fun `hypothetical creation language does not authorize an action`() {
        assertNull(SystemCommandParser.parse("If I create a reading habit, will it help me focus?"))
        assertNull(SystemCommandParser.parse("How do I create a useful hydration quest?"))
    }
}
