package com.ascend.app

import androidx.room.withTransaction
import com.ascend.app.core.database.*
import com.ascend.app.domain.*
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

data class TrainingDay(val date: LocalDate, val template: WorkoutTemplateEntity?, val adjusted: Boolean, val completed: Boolean)

object TrainingSchedule {
    fun resolve(profile: UserProfileEntity?, templates: List<WorkoutTemplateEntity>, overrides: List<WorkoutDayOverrideEntity>, history: List<WorkoutSessionEntity>, date: LocalDate): TrainingDay {
        val session = history.firstOrNull { it.localDate == date.toString() }
        val override = overrides.firstOrNull { it.localDate == date.toString() }
        val days = profile?.workoutDays.orEmpty().split(',').mapNotNull(String::toIntOrNull).filter { it in 1..7 }.map(DayOfWeek::of).toSet()
        val index = if (profile == null) -1 else CustomPlanEngine.templateIndexFor(date, LocalDate.parse(profile.programStartDate), days)
        val template = (session?.templateId ?: override?.templateId)?.let { id -> templates.find { it.id == id } }
            ?: templates.find { it.active && it.rotationIndex == index }
        return TrainingDay(date, template, override != null, session?.completedAt != null)
    }
}

data class ProgramSettings(
    val days: Set<DayOfWeek>, val focus: Set<FocusArea>, val split: TrainingSplit,
    val equipment: Equipment, val experience: Experience, val objective: Objective, val minutes: Int,
)

class TrainingStore(private val database: AscendDatabase) {
    private val dao get() = database.dao()
    suspend fun day(date: LocalDate) = TrainingSchedule.resolve(dao.observeProfile().first(), dao.observeTemplates().first(), dao.observeDayOverrides().first(), dao.observeWorkoutHistory().first(), date)
    suspend fun swap(first: LocalDate, second: LocalDate, today: LocalDate = LocalDate.now()) = database.withTransaction {
        WeekRules.validateSwap(first, second, today)
        val a = day(first).template ?: error("The first day's protocol is missing.")
        val b = day(second).template ?: error("The second day's protocol is missing.")
        require(a.id != b.id) { "These days already have the same protocol." }
        listOf(first, second).forEach { date ->
            dao.latestWorkout(date.toString())?.let { session ->
                val sets = dao.observeWorkoutSets(session.id).first()
                require(session.completedAt == null && sets.none { it.completed || it.reps > 0 || it.weightKg > 0 }) {
                    "A session on $date already has recorded work. Its history cannot be moved."
                }
                dao.deleteWorkoutSession(session.id) // Untouched shells only; never recorded work.
            }
        }
        dao.upsertDayOverrides(listOf(WorkoutDayOverrideEntity(first.toString(), b.id, System.currentTimeMillis()), WorkoutDayOverrideEntity(second.toString(), a.id, System.currentTimeMillis())))
    }
    suspend fun rebuild(settings: ProgramSettings) = database.withTransaction {
        val old = dao.observeProfile().first() ?: error("Create your player first.")
        val focus = FocusRules.normalize(settings.focus)
        require(focus.isNotEmpty() && settings.minutes in 30..90)
        val injuries = old.injuries.split(',').mapNotNull { runCatching { InjuryArea.valueOf(it) }.getOrNull() }.toSet()
        val plan = CustomPlanEngine.generate(settings.days.size, settings.days, focus, injuries, settings.equipment, settings.experience, settings.objective, settings.split, settings.minutes)
        dao.archiveTemplates()
        val prefix = UUID.randomUUID().toString()
        plan.workouts.forEachIndexed { index, workout ->
            val templateId = prefix + "_$index"
            dao.upsertTemplates(listOf(WorkoutTemplateEntity(templateId, workout.name, index, if (workout.recovery) AscendConfig.RECOVERY_XP else AscendConfig.WORKOUT_XP, workout.estimatedMinutes, workout.recovery)))
            workout.exercises.forEachIndexed { order, ex ->
                val exerciseId = templateId + "_ex_$order"
                dao.upsertExercises(listOf(ExerciseEntity(exerciseId, ex.name, ex.muscleGroup, settings.equipment.name)))
                dao.upsertWorkoutExercises(listOf(WorkoutExerciseEntity(exerciseId + "_link", templateId, exerciseId, order, ex.sets, ex.minReps, ex.maxReps)))
            }
        }
        dao.upsertProfile(old.copy(workoutFrequency = settings.days.size, workoutDays = settings.days.sortedBy { it.value }.joinToString(",") { it.value.toString() }, focusAreas = focus.joinToString(",") { it.name }, trainingSplit = settings.split.name, equipment = settings.equipment.name, experience = settings.experience.name, objective = settings.objective.name, sessionMinutes = settings.minutes, programStartDate = LocalDate.now().toString()))
        dao.clearFutureOverrides(LocalDate.now().toString())
        dao.supersedeProposals()
    }
    /** Fork a template before editing so historical sessions keep the original prescription. */
    suspend fun editExercise(templateId: String, linkId: String?, name: String, sets: Int, min: Int, max: Int, remove: Boolean = false) = database.withTransaction {
        val template = dao.templateById(templateId) ?: error("Protocol no longer exists.")
        require(template.active && !template.isRecovery) { "Choose an active training protocol." }
        if (!remove) require(WorkoutInputRules.isValidExercise(name, sets, min, max)) { "Use a 2–80 character name, 1–10 sets, and 1–100 reps." }
        val details = dao.templateExercises(templateId)
        require(linkId == null || details.any { it.link.id == linkId }) { "Exercise changed. Reopen the editor." }
        require(!remove || (linkId != null && details.size > 1)) { "Keep at least one exercise in a training protocol." }
        val cloneId = UUID.randomUUID().toString()
        dao.upsertTemplates(listOf(template.copy(active = false), template.copy(id = cloneId)))
        var order = 0
        details.forEach { detail ->
            if (detail.link.id == linkId && remove) return@forEach
            if (detail.link.id == linkId) {
                val exerciseId = UUID.randomUUID().toString()
                dao.upsertExercises(listOf(detail.exercise.copy(id = exerciseId, name = name.trim())))
                dao.upsertWorkoutExercises(listOf(detail.link.copy(id = UUID.randomUUID().toString(), templateId = cloneId, exerciseId = exerciseId, orderIndex = order++, targetSets = sets, minReps = min, maxReps = max)))
            } else dao.upsertWorkoutExercises(listOf(detail.link.copy(id = UUID.randomUUID().toString(), templateId = cloneId, orderIndex = order++)))
        }
        if (linkId == null && !remove) {
            val exId = UUID.randomUUID().toString()
            dao.upsertExercises(listOf(ExerciseEntity(exId, name.trim(), "Custom", "CUSTOM")))
            dao.upsertWorkoutExercises(listOf(WorkoutExerciseEntity(UUID.randomUUID().toString(), cloneId, exId, order, sets, min, max)))
        }
        val updated = dao.observeDayOverrides().first().filter { it.localDate >= LocalDate.now().toString() && it.templateId == templateId }.map { it.copy(templateId = cloneId) }
        dao.upsertDayOverrides(updated)
        val minutes = 12 + dao.templateExercises(cloneId).sumOf { it.link.targetSets } * 2
        dao.upsertTemplates(listOf(template.copy(id = cloneId, estimatedMinutes = minutes)))
        dao.supersedeProposals()
    }
}
