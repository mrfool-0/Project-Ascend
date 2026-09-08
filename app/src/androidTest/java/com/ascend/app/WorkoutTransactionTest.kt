package com.ascend.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ascend.app.core.database.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutTransactionTest {
    @Test fun failedSetInsertionRollsBackTheSession() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AscendDatabase::class.java).build()
        try {
            val dao = db.dao()
            dao.upsertTemplates(listOf(WorkoutTemplateEntity("template", "Test", 0, 100, 30)))
            val session = WorkoutSessionEntity("session", "template", "2026-09-08", 0L)
            val invalid = WorkoutSetEntity("set", "session", "missing_exercise", 1, 0.0, 0, false)
            val result = runCatching { dao.insertWorkoutWithSets(session, listOf(invalid)) }
            assertTrue(result.isFailure)
            assertNull(dao.workoutSession("session"))
        } finally { db.close() }
    }

    @Test fun successfulSessionIncludesAllItsSets() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AscendDatabase::class.java).build()
        try {
            val dao = db.dao()
            dao.upsertTemplates(listOf(WorkoutTemplateEntity("template", "Test", 0, 100, 30)))
            dao.upsertExercises(listOf(ExerciseEntity("row", "Row", "Back", "GYM")))
            val session = WorkoutSessionEntity("session", "template", "2026-09-08", 0L)
            val sets = (1..3).map { WorkoutSetEntity("set_$it", "session", "row", it, 0.0, 0, false) }
            dao.insertWorkoutWithSets(session, sets)
            assertNotNull(dao.workoutSession("session"))
            assertEquals(3, dao.observeWorkoutSets("session").first().size)
        } finally { db.close() }
    }
}
