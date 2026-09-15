package com.ascend.app.domain

import com.ascend.app.core.database.HabitEntity

/** Deterministic high-impact edits take precedence over model suggestions and coaching keywords. */
object SystemEditRouter {
    private val edit = Regex("\\b(add|include|change|update|edit|set|remove|delete)\\b", RegexOption.IGNORE_CASE)
    private val workout = Regex("\\b(workouts?|sessions?|exercises?|protocols?|treadmill|sets?)\\b", RegexOption.IGNORE_CASE)
    private val all = Regex("\\b(all|every|each|entire)\\b", RegexOption.IGNORE_CASE)
    private val programScope = Regex("\\b(?:all|every|each)\\s+(?:(?:of|my|the|existing|active|training|workout)\\s+)*(?:sessions?|workouts?|protocols?|exercises?)\\b|\\b(?:entire|whole)\\s+(?:training\\s+)?(?:plan|program)\\b", RegexOption.IGNORE_CASE)
    private val restrictedScope = Regex("\\b(?:only|except|excluding)\\b|\\b(?:in|of|for)\\s+(?:(?:my|the)\\s+)?(?:push|pull|legs?|upper|lower|bench|squats?)\\b", RegexOption.IGNORE_CASE)
    private val compoundEdit = Regex("\\b(?:and|then|also)\\s+(?:please\\s+)?(?:add|include|change|update|edit|set|remove|delete)\\b", RegexOption.IGNORE_CASE)
    private val minutes = Regex("(\\d+)\\s*(?:minutes?|mins?)\\b", RegexOption.IGNORE_CASE)

    fun isWorkoutEdit(message: String) = edit.containsMatchIn(message) && workout.containsMatchIn(message) && !message.contains("habit", true) && !message.contains("quest", true)

    fun resolve(message: String, habits: List<HabitEntity>, priorRequest: String? = null): SystemReply? {
        val answer = message.trim().trimEnd('.', '!')
        val followUp = (minutes.matches(answer) || answer.all(Char::isDigit) && answer.isNotEmpty()) && priorRequest != null && priorRequest.contains("treadmill", true) && isWorkoutEdit(priorRequest)
        val text = if (followUp) "$priorRequest ${if (answer.all(Char::isDigit)) "$answer minutes" else answer}" else message
        if (!SystemCommandParser.mayPropose(text)) return null
        if (edit.containsMatchIn(text) && compoundEdit.containsMatchIn(text)) return SystemReply("Let's make one confirmed change at a time so nothing is missed. Which change should I apply first?")
        if (Regex("\\b(remove|delete)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) && text.contains("habit", true)) {
            val candidates = habits.filter { text.contains(it.name, true) }
            val match = candidates.singleOrNull()
                ?: return SystemReply("Which habit should I remove? Give its exact name from Habits; I will show a confirmation before removing it.")
            return SystemReply("Remove ${match.name} from active habits? Past completions and earned XP will stay intact.", SystemAction(SystemActionType.REMOVE_HABIT, entityId = match.id))
        }
        if (!isWorkoutEdit(text)) return null
        if (all.containsMatchIn(text) && (!programScope.containsMatchIn(text) || restrictedScope.containsMatchIn(text))) return SystemReply("Should this change apply to every active training protocol, or just a specific exercise or protocol? Tell me the scope before I prepare a change.")
        if (all.containsMatchIn(text) && Regex("\\bsets?\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) && !Regex("\\b(add|include|remove|delete)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            // 'to 3 sets instead of 2' must choose the destination, never the old count.
            val count = Regex("\\bto\\s+(\\d+)\\s*sets?", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
                ?: Regex("\\b(\\d+)\\s*sets?", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
            val sets = count?.toIntOrNull()
                ?: return SystemReply("How many working sets should each strength exercise use across your active training sessions?")
            if (sets !in 1..10) return SystemReply("Use 1–10 working sets per strength exercise. What set count do you want?")
            return SystemReply("I can set every strength exercise to $sets working sets across your active protocols. Reps, cardio duration, recovery days and recorded sessions stay unchanged; review the preview first.", SystemAction(SystemActionType.SET_ALL_STRENGTH_SETS, sets = sets))
        }
        if (Regex("\\b(add|include)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) && text.contains("treadmill", true)) {
            if (!all.containsMatchIn(text)) return SystemReply("Which training protocol should receive the treadmill block—or should it go in every active training session?")
            val duration = minutes.find(text)?.groupValues?.get(1)?.toIntOrNull()
                ?: return SystemReply("How many minutes of treadmill should I add to each active training session? I will add a timed workout block—not a habit—and leave recovery days unchanged.")
            if (duration !in 1..120) return SystemReply("Choose a treadmill duration from 1 to 120 minutes per session. How many minutes do you want?")
            return SystemReply("Add a $duration-minute treadmill block to every active training protocol? Existing treadmill blocks will not be duplicated; pace and incline remain your choice.", SystemAction(SystemActionType.ADD_EXERCISE_ALL, name = "Treadmill", sets = 1, minReps = 1, maxReps = 1, durationSeconds = duration * 60))
        }
        return null
    }

    /** Reject cross-domain/bulk-to-single suggestions even when the model reports high confidence. */
    fun permits(message: String, action: SystemAction): Boolean {
        if (action.type != SystemActionType.NONE && compoundEdit.containsMatchIn(message)) return false
        if (action.type in setOf(SystemActionType.SET_ALL_STRENGTH_SETS, SystemActionType.ADD_EXERCISE_ALL) && (!programScope.containsMatchIn(message) || restrictedScope.containsMatchIn(message))) return false
        if (isWorkoutEdit(message) && action.type in setOf(SystemActionType.CREATE_HABIT, SystemActionType.UPDATE_HABIT, SystemActionType.REMOVE_HABIT, SystemActionType.CREATE_QUEST)) return false
        if (isWorkoutEdit(message) && all.containsMatchIn(message) && action.type !in setOf(SystemActionType.SET_ALL_STRENGTH_SETS, SystemActionType.ADD_EXERCISE_ALL, SystemActionType.NONE)) return false
        if (action.type == SystemActionType.REMOVE_HABIT && !Regex("\\b(remove|delete)\\b", RegexOption.IGNORE_CASE).containsMatchIn(message)) return false
        return true
    }
}
