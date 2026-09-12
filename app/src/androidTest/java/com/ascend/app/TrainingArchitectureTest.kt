package com.ascend.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ascend.app.core.database.*
import com.ascend.app.core.datastore.UserPreferences
import com.ascend.app.cloud.*
import com.ascend.app.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TrainingArchitectureTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val monday = LocalDate.of(2026, 9, 7)
    private fun profile() = UserProfileEntity(displayName = "TEST", birthDate = "2000-01-01", heightCm = 175.0,
        currentWeightKg = 75.0, targetWeightKg = 75.0, biologicalSex = "MALE", unitSystem = "METRIC",
        objective = "MUSCLE_GAIN", activityLevel = "MODERATE", experience = "BEGINNER", equipment = "DUMBBELLS",
        dietPreference = "BALANCED", trainingTime = "EVENING", focusAreas = "FULL_BODY", programStartDate = monday.toString(), createdAt = 0L)
    private suspend fun seed(db: AscendDatabase) {
        val dao = db.dao()
        dao.upsertProfile(profile())
        dao.upsertTemplates((0..3).map { WorkoutTemplateEntity("p$it", "Protocol $it", it, 100, 30, it == 3) })
        dao.upsertExercises(listOf(ExerciseEntity("row", "Row", "Back", "DUMBBELLS")))
        dao.upsertWorkoutExercises(listOf(WorkoutExerciseEntity("link", "p0", "row", 0, 3, 8, 12)))
    }
    private fun repository(db: AscendDatabase) = AscendRepository(db, UserPreferences(context), GoogleProgressService(context), SystemAiService(context))

    @Test fun weekSwapPreservesNextWeekAndTrainingFrequency() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).build()
        try {
            seed(db)
            val store = TrainingStore(db)
            store.swap(monday, monday.plusDays(1), monday)
            assertEquals("p3", store.day(monday).template?.id)
            assertEquals("p0", store.day(monday.plusDays(1)).template?.id)
            assertEquals("p0", store.day(monday.plusWeeks(1)).template?.id)
            assertEquals(3, (0L..6L).count { store.day(monday.plusDays(it)).template?.isRecovery == false })
            assertEquals("1,3,5", db.dao().observeProfile().first()?.workoutDays)
        } finally { db.close() }
    }
    @Test fun recordedWorkBlocksSwapAtomically() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).build()
        try {
            seed(db)
            db.dao().insertWorkoutWithSets(WorkoutSessionEntity("s", "p0", monday.toString(), 0L),
                listOf(WorkoutSetEntity("s1", "s", "row", 1, 10.0, 8, false)))
            assertTrue(runCatching { TrainingStore(db).swap(monday, monday.plusDays(1), monday) }.isFailure)
            assertTrue(db.dao().observeDayOverrides().first().isEmpty())
            assertEquals(8, db.dao().observeWorkoutSets("s").first().single().reps)
        } finally { db.close() }
    }
    @Test fun untouchedSessionCanMoveWithoutOrphanedSets() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).build()
        try {
            seed(db)
            db.dao().insertWorkoutWithSets(WorkoutSessionEntity("s", "p0", monday.toString(), 0L),
                listOf(WorkoutSetEntity("s1", "s", "row", 1, 0.0, 0, false)))
            TrainingStore(db).swap(monday, monday.plusDays(1), monday)
            assertNull(db.dao().workoutSession("s"))
            assertTrue(db.dao().observeWorkoutSets("s").first().isEmpty())
        } finally { db.close() }
    }
    @Test fun templateEditPreservesHistoryAndUpdatesEstimate() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).build()
        try {
            seed(db)
            db.dao().insertWorkoutWithSets(WorkoutSessionEntity("s", "p0", monday.toString(), 0L, completedAt = 1L),
                listOf(WorkoutSetEntity("s1", "s", "row", 1, 10.0, 8, true)))
            TrainingStore(db).editExercise("p0", "link", "Supported Row", 5, 10, 15)
            val updated = db.dao().observeTemplates().first().single { it.active && it.rotationIndex == 0 }
            assertNotEquals("p0", updated.id)
            assertEquals(22, updated.estimatedMinutes)
            assertEquals("Row", db.dao().templateExercises("p0").single().exercise.name)
            assertEquals(3, db.dao().templateExercises("p0").single().link.targetSets)
            assertEquals("p0", db.dao().workoutSession("s")?.templateId)
            assertEquals(1, db.dao().completedTrainingCountBetween(monday.toString(), monday.toString()))
        } finally { db.close() }
    }
    @Test fun proposedHabitRequiresConfirmationAndCannotExecuteTwice() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).build()
        try {
            seed(db)
            val repo = repository(db)
            repo.proposeSystemAction(SystemAction(SystemActionType.CREATE_HABIT, "Read ten pages", 10.0, "pages"))
            val proposal = requireNotNull(db.dao().observeProposal().first())
            assertTrue(db.dao().observeHabits().first().isEmpty())
            repo.confirmSystemProposal(proposal.id, true)
            assertEquals("Read ten pages", db.dao().observeHabits().first().single().name)
            assertTrue(runCatching { repo.confirmSystemProposal(proposal.id, true) }.isFailure)
            assertEquals(1, db.dao().observeHabits().first().size)
        } finally { db.close() }
    }
    @Test fun canceledAndStaleProposalsNeverChangeHabits() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).build()
        try {
            seed(db)
            val repo = repository(db)
            val action = SystemAction(SystemActionType.CREATE_HABIT, "Evening reading")
            repo.proposeSystemAction(action)
            repo.confirmSystemProposal(requireNotNull(db.dao().observeProposal().first()).id, false)
            assertTrue(db.dao().observeHabits().first().isEmpty())
            repo.proposeSystemAction(action)
            val proposal = requireNotNull(db.dao().observeProposal().first())
            db.dao().upsertProfile(profile().copy(sessionMinutes = 60))
            assertTrue(runCatching { repo.confirmSystemProposal(proposal.id, true) }.isFailure)
            assertTrue(db.dao().observeHabits().first().isEmpty())
        } finally { db.close() }
    }
    @Test fun remindersNameOnlyScheduledUnfinishedObjectives() {
        val quest = QuestEntity("custom_pushups", "100 PUSH-UPS", "100 reps • every day • tap when complete", "DAILY", "TRAINING", 100.0, 25)
        val weekday = quest.copy(id = "custom_weekday", title = "WEEKDAY QUEST", frequency = "WEEKDAYS")
        val state = DashboardState(profile = profile())
        val saturday = monday.plusDays(5)
        assertEquals(listOf("100 PUSH-UPS · 100 reps • every day"), QuestStatus.pending(state, listOf(quest, weekday), saturday))
        assertTrue(QuestStatus.pending(state.copy(questCompletions = setOf(quest.id)), listOf(quest, weekday), saturday).isEmpty())
        assertFalse(QuestStatus.isScheduled(quest.copy(frequency = "THREE_TIMES_WEEKLY"), monday.plusDays(1)))
        assertTrue(QuestStatus.isScheduled(quest.copy(frequency = "THREE_TIMES_WEEKLY"), monday.plusDays(2)))
    }
    @Test fun rebuildKeepsNutritionAndOldCompletedSessions() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).build()
        try {
            seed(db)
            val target = NutritionTargetEntity(calories = 2400, proteinGrams = 150, carbohydrateGrams = 300, fatGrams = 67, waterMl = 2500, estimatedBmr = 1700, estimatedMaintenance = 2400)
            db.dao().upsertNutritionTarget(target)
            db.dao().insertWorkoutWithSets(WorkoutSessionEntity("s", "p0", monday.toString(), 0L, completedAt = 1L), emptyList())
            TrainingStore(db).rebuild(ProgramSettings(setOf(java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.THURSDAY), setOf(FocusArea.FULL_BODY), TrainingSplit.FULL_BODY, Equipment.BODYWEIGHT, Experience.BEGINNER, Objective.GENERAL_HEALTH, 30))
            assertEquals(target, db.dao().observeNutritionTarget().first())
            assertEquals("p0", db.dao().workoutSession("s")?.templateId)
            assertEquals(3, db.dao().observeTemplates().first().count { it.active })
            assertEquals(2, db.dao().observeProfile().first()?.workoutFrequency)
        } finally { db.close() }
    }
}
