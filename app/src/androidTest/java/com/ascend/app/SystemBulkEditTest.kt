package com.ascend.app

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ascend.app.cloud.*
import com.ascend.app.core.database.*
import com.ascend.app.core.datastore.UserPreferences
import com.ascend.app.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SystemBulkEditTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun db() = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).build()
    private fun repo(db: AscendDatabase) = AscendRepository(db, UserPreferences(context), GoogleProgressService(context), SystemAiService(context))
    private suspend fun seed(db: AscendDatabase) {
        val dao = db.dao()
        dao.upsertProfile(UserProfileEntity(displayName = "TEST", birthDate = "2000-01-01", heightCm = 175.0, currentWeightKg = 75.0,
            targetWeightKg = 75.0, biologicalSex = "MALE", unitSystem = "METRIC", objective = "GENERAL_HEALTH", activityLevel = "MODERATE",
            experience = "BEGINNER", equipment = "DUMBBELLS", dietPreference = "BALANCED", trainingTime = "EVENING", programStartDate = LocalDate.now().toString(), createdAt = 0L))
        dao.upsertNutritionTarget(NutritionTargetEntity(calories = 2200, proteinGrams = 140, carbohydrateGrams = 260, fatGrams = 65,
            waterMl = 2500, estimatedBmr = 1700, estimatedMaintenance = 2200, manuallyEdited = false))
        dao.upsertTemplates((0..3).map { WorkoutTemplateEntity("p$it", "Protocol $it", it, 100, 30, it == 3) })
        dao.upsertExercises(listOf(ExerciseEntity("row", "Row", "Back", "DUMBBELLS")))
        dao.upsertWorkoutExercises((0..2).map { WorkoutExerciseEntity("l$it", "p$it", "row", 0, 2, 8, 12) })
    }

    @Test fun exactUserRequestPreviewsThenChangesEveryProtocolNotHistory() = runBlocking {
        val db = db()
        try {
            seed(db); val dao = db.dao(); val repo = repo(db)
            dao.insertWorkoutWithSets(WorkoutSessionEntity("history", "p0", "2026-09-01", 0L, 1L), listOf(WorkoutSetEntity("record", "history", "row", 1, 10.0, 8, true)))
            repo.sendSystemMessage("Can you change all my session sets to 3 sets instead of 2 set", SystemTone.DIRECT, LocalDate.now())
            val proposal = requireNotNull(dao.observeProposal().first())
            assertTrue(dao.observeAllWorkoutExercises().first().all { it.targetSets == 2 })
            repo.confirmSystemProposal(proposal.id, true)
            dao.observeTemplates().first().filter { it.active && !it.isRecovery }.forEach { template ->
                assertEquals(3, dao.templateExercises(template.id).single().link.targetSets)
                assertEquals(8, dao.templateExercises(template.id).single().link.minReps)
            }
            assertEquals(2, dao.templateExercises("p0").single().link.targetSets)
            assertEquals(8, dao.observeWorkoutSets("history").first().single().reps)
            assertTrue(dao.templateById("p3")!!.active)
            assertTrue(runCatching { repo.confirmSystemProposal(proposal.id, true) }.isFailure)
        } finally { db.close() }
    }

    @Test fun treadmillClarificationCreatesTimedBlocksAndNeverAHabit() = runBlocking {
        val db = db()
        try {
            seed(db); val dao = db.dao(); val repo = repo(db)
            repo.sendSystemMessage("Add a treadmill session to all my existing sessions", SystemTone.DIRECT, LocalDate.now())
            assertNull(dao.observeProposal().first())
            assertTrue(dao.recentSystemMessages(1).single().message.contains("How many minutes"))
            repo.sendSystemMessage("10 minutes", SystemTone.DIRECT, LocalDate.now())
            val proposal = requireNotNull(dao.observeProposal().first())
            repo.confirmSystemProposal(proposal.id, true)
            dao.observeTemplates().first().filter { it.active && !it.isRecovery }.forEach { template ->
                assertEquals(600, dao.templateExercises(template.id).single { it.exercise.name == "Treadmill" }.link.durationSeconds)
            }
            assertTrue(dao.observeHabits().first().isEmpty())
            assertTrue(dao.templateExercises("p3").isEmpty())
            assertTrue(TrainingStore(db).bulkTargets(SystemAction(SystemActionType.ADD_EXERCISE_ALL, name = "Treadmill", durationSeconds = 600)).isEmpty())
        } finally { db.close() }
    }

    @Test fun timedCardioLoggingDoesNotInventRepsOrWeightAndSurvivesBulkSets() = runBlocking {
        val db = db()
        try {
            seed(db); val dao = db.dao(); val store = TrainingStore(db)
            store.applyBulk(SystemAction(SystemActionType.ADD_EXERCISE_ALL, name = "Treadmill", durationSeconds = 600))
            store.applyBulk(SystemAction(SystemActionType.SET_ALL_STRENGTH_SETS, sets = 3))
            val template = dao.observeTemplates().first().first { it.active && !it.isRecovery }
            val cardio = dao.templateExercises(template.id).single { it.link.durationSeconds > 0 }
            assertEquals(1, cardio.link.targetSets)
            val set = WorkoutSetEntity("timed", "session", cardio.exercise.id, 1, 0.0, 0, false)
            dao.insertWorkoutWithSets(WorkoutSessionEntity("session", template.id, LocalDate.now().toString(), 0L), listOf(set))
            assertFalse(repo(db).updateSet(set, 100.0, 600, true))
            val saved = dao.observeWorkoutSets("session").first().single()
            assertEquals(600, saved.durationSeconds); assertEquals(0, saved.reps); assertEquals(0.0, saved.weightKg, 0.0)
        } finally { db.close() }
    }

    @Test fun removedHabitRetainsHistoryAndDoesNotReseedDefaults() = runBlocking {
        val db = db()
        try {
            seed(db); val repo = repo(db); val dao = db.dao()
            repo.createHabit("Read pages", HabitType.NUMBER, 10.0, "pages", HabitDifficulty.NORMAL)
            val habit = dao.observeHabits().first().single()
            dao.upsertHabitCompletion(HabitCompletionEntity("done", habit.id, "2026-09-01", 10.0, true, 0L))
            repo.sendSystemMessage("Remove the Read pages habit", SystemTone.DIRECT, LocalDate.now())
            val proposal = requireNotNull(dao.observeProposal().first())
            assertEquals(1, dao.observeHabits().first().size)
            repo.confirmSystemProposal(proposal.id, true)
            assertTrue(dao.observeHabits().first().isEmpty())
            assertEquals(1, dao.habitHistory(habit.id).size)
            repo.seedCoreData()
            assertTrue(dao.observeHabits().first().isEmpty())
        } finally { db.close() }
    }

    @Test fun invalidBulkRequestMakesNoPartialChanges() = runBlocking {
        val db = db()
        try {
            seed(db)
            assertTrue(runCatching { TrainingStore(db).applyBulk(SystemAction(SystemActionType.SET_ALL_STRENGTH_SETS, sets = 99)) }.isFailure)
            assertEquals(4, db.dao().observeTemplates().first().size)
            assertTrue(db.dao().observeTemplates().first().all { it.active })
        } finally { db.close() }
    }
}
