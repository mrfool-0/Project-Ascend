package com.ascend.app.domain

enum class SystemActionType { NONE, CREATE_HABIT, CREATE_QUEST }

data class SystemAction(
    val type: SystemActionType,
    val name: String = "",
    val target: Double = 1.0,
    val unit: String = "done",
    val frequency: HabitFrequency = HabitFrequency.EVERY_DAY,
    val difficulty: HabitDifficulty = HabitDifficulty.NORMAL,
    val category: QuestCategory = QuestCategory.DISCIPLINE,
    val rewardXp: Int = 25,
)

data class SystemReply(val message: String, val action: SystemAction = SystemAction(SystemActionType.NONE))

/** Conservative offline command parsing for direct create/add requests. */
object SystemCommandParser {
    private val createWords = Regex("\\b(add|create|make|set|build|customi[sz]e)\\b", RegexOption.IGNORE_CASE)
    private val targetPattern = Regex("(\\d+(?:\\.\\d+)?)\\s*(steps?|minutes?|mins?|hours?|hrs?|ml|lit(?:er|re)s?|glasses?|reps?|pages?)", RegexOption.IGNORE_CASE)
    private val cadencePattern = Regex("\\b(every day|daily|on weekdays|weekdays|three times (?:a|per) week|3x (?:a|per) week)\\b", RegexOption.IGNORE_CASE)
    private val hypotheticalOpening = Regex("^\\s*(if|what if|suppose|imagine|how|why|should i|would i)\\b", RegexOption.IGNORE_CASE)

    fun parse(message: String): SystemAction? {
        val clean = message.trim()
        if (!createWords.containsMatchIn(clean)) return null
        if (hypotheticalOpening.containsMatchIn(clean)) return null
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
