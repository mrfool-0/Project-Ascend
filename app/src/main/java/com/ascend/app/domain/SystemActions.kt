package com.ascend.app.domain

enum class SystemActionType { NONE, CREATE_HABIT, CREATE_QUEST, UPDATE_HABIT, SWAP_DAYS, ADD_EXERCISE, UPDATE_EXERCISE, REMOVE_EXERCISE }

data class SystemAction(
    val type: SystemActionType,
    val name: String = "",
    val target: Double = 1.0,
    val unit: String = "done",
    val frequency: HabitFrequency = HabitFrequency.EVERY_DAY,
    val difficulty: HabitDifficulty = HabitDifficulty.NORMAL,
    val category: QuestCategory = QuestCategory.DISCIPLINE,
    val rewardXp: Int = 25,
    val entityId: String = "",
    val templateId: String = "",
    val fromDate: String = "",
    val toDate: String = "",
    val sets: Int = 3,
    val minReps: Int = 8,
    val maxReps: Int = 12,
)

data class SystemReply(val message: String, val action: SystemAction = SystemAction(SystemActionType.NONE))

/** Conservative offline command parsing for direct create/add requests. */
object SystemCommandParser {
    fun parseHabitUpdate(message: String, habits: List<com.ascend.app.core.database.HabitEntity>): SystemAction? {
        if (!mayPropose(message) || !Regex("\\b(change|update|edit)\\b", RegexOption.IGNORE_CASE).containsMatchIn(message) || !message.contains("habit", true)) return null
        val matching = habits.filter { it.active && Regex("\\b" + Regex.escape(it.name) + "\\b", RegexOption.IGNORE_CASE).containsMatchIn(message) }
        val habit = matching.singleOrNull() ?: return null
        val target = targetPattern.findAll(message).lastOrNull() ?: return null
        // Deliberately narrow: quantity edits only, preserving cadence, difficulty and unit.
        val unit = target.groupValues[2].lowercase().normalizeUnit()
        val requestedFrequency = when {
            message.contains("weekday", true) -> HabitFrequency.WEEKDAYS.name
            message.contains("three times", true) || message.contains("3x", true) -> HabitFrequency.THREE_TIMES_WEEKLY.name
            message.contains("daily", true) || message.contains("every day", true) -> HabitFrequency.EVERY_DAY.name
            else -> habit.frequency
        }
        if (unit != habit.unit.lowercase().normalizeUnit() || requestedFrequency != habit.frequency) return null
        return SystemAction(SystemActionType.UPDATE_HABIT, habit.name, target.groupValues[1].toDoubleOrNull() ?: return null,
            habit.unit, HabitFrequency.valueOf(habit.frequency), HabitDifficulty.valueOf(habit.difficulty), entityId = habit.id)
    }
    fun parseSwap(message: String, today: java.time.LocalDate): SystemAction? {
        val text = message.lowercase()
        if (hypotheticalOpening.containsMatchIn(text)) return null
        if (!Regex("\\b(swap|switch|move|change|shift|instead)\\b").containsMatchIn(text)) return null
        if ("today" in text && Regex("\\b(tomorrow|tom)\\b").containsMatchIn(text) &&
            Regex("\\b(rest|break|workout|train|gym|day)\\b").containsMatchIn(text)) {
            return SystemAction(SystemActionType.SWAP_DAYS, fromDate = today.toString(), toDate = today.plusDays(1).toString())
        }
        return null
    }
    fun mayPropose(message: String) = !hypotheticalOpening.containsMatchIn(message.trim()) && !Regex("\\b(do not|don't|dont|never)\\s+(?:want to\\s+)?(add|create|change|edit|update|swap|move)\\b", RegexOption.IGNORE_CASE).containsMatchIn(message)
    private val createWords = Regex("\\b(add|create|make|set|build|customi[sz]e)\\b", RegexOption.IGNORE_CASE)
    private val targetPattern = Regex("(\\d+(?:\\.\\d+)?)\\s*(steps?|minutes?|mins?|hours?|hrs?|ml|lit(?:er|re)s?|glasses?|reps?|pages?)", RegexOption.IGNORE_CASE)
    private val cadencePattern = Regex("\\b(every day|daily|on weekdays|weekdays|three times (?:a|per) week|3x (?:a|per) week)\\b", RegexOption.IGNORE_CASE)
    private val hypotheticalOpening = Regex("^\\s*(if|what if|suppose|imagine|how|why|should i|would i)\\b", RegexOption.IGNORE_CASE)

    fun parse(message: String): SystemAction? {
        val clean = message.trim()
        if (!createWords.containsMatchIn(clean)) return null
        if (!mayPropose(clean)) return null
        val lower = clean.lowercase()
        val type = when {
            "habit" in lower -> SystemActionType.CREATE_HABIT
            "quest" in lower -> SystemActionType.CREATE_QUEST
            else -> return null
        }
        val marker = if (type == SystemActionType.CREATE_HABIT) "habit" else "quest"
        val markerIndex = clean.indexOf(marker, ignoreCase = true)
        val afterMarker = clean.substring(markerIndex + marker.length)
            .replace(Regex("^(\\s*(called|named|to|for|of|:)\\s*)", RegexOption.IGNORE_CASE), "")
        val createMatch = createWords.find(clean.substring(0, markerIndex))
        val beforeMarker = createMatch?.let { clean.substring(it.range.last + 1, markerIndex) }.orEmpty()
            .replace(Regex("^\\s*(a|an|one|new|my)\\b", RegexOption.IGNORE_CASE), "")
        val candidate = afterMarker.takeIf { it.trim().length >= 3 } ?: beforeMarker
        val targetMatch = targetPattern.find(clean)
        val actionName = candidate
            .replace(cadencePattern, "")
            .replace(targetPattern, "")
            .replace(Regex("\\b(?:each|per)\\s+day\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\b(?:for|to|of|on|a|an|one|new|my)\\s*$", RegexOption.IGNORE_CASE), "")
            .trim(' ', '.', ',', ':', '-', '"')
        if (actionName.length < 3) return null

        val target = targetMatch?.groupValues?.get(1)?.toDoubleOrNull()?.coerceIn(.1, 100_000.0) ?: 1.0
        val unit = targetMatch?.groupValues?.get(2)?.lowercase()?.normalizeUnit() ?: "done"
        val frequency = when {
            "weekday" in lower -> HabitFrequency.WEEKDAYS
            "three times" in lower || "3x" in lower -> HabitFrequency.THREE_TIMES_WEEKLY
            else -> HabitFrequency.EVERY_DAY
        }
        val difficulty = when {
            listOf("hard", "advanced", "intense").any(lower::contains) -> HabitDifficulty.HARD
            listOf("medium", "moderate").any(lower::contains) -> HabitDifficulty.MEDIUM
            else -> HabitDifficulty.NORMAL
        }
        val category = when {
            Regex("\\b(workout|train(?:ing)?|exercise|walk(?:ing)?|run(?:ning)?|steps?)\\b").containsMatchIn(lower) -> QuestCategory.TRAINING
            Regex("\\b(food|meal|protein|calories?|eat(?:ing)?)\\b").containsMatchIn(lower) -> QuestCategory.NUTRITION
            Regex("\\b(water|drink|hydrat\\w*)\\b").containsMatchIn(lower) -> QuestCategory.HYDRATION
            Regex("\\b(sleep|rest|recover\\w*|mobility)\\b").containsMatchIn(lower) -> QuestCategory.RECOVERY
            else -> QuestCategory.DISCIPLINE
        }
        return SystemAction(
            type = type,
            name = actionName.take(80),
            target = target,
            unit = unit,
            frequency = frequency,
            difficulty = difficulty,
            category = category,
            rewardXp = when (difficulty) {
                HabitDifficulty.NORMAL -> 15
                HabitDifficulty.MEDIUM -> 25
                HabitDifficulty.HARD -> 40
            },
        )
    }

    private fun String.normalizeUnit(): String = when {
        startsWith("min") -> "min"
        startsWith("hr") || startsWith("hour") -> "hours"
        startsWith("lit") -> "liters"
        startsWith("glass") -> "glasses"
        startsWith("step") -> "steps"
        startsWith("rep") -> "reps"
        startsWith("page") -> "pages"
        else -> this
    }
}
