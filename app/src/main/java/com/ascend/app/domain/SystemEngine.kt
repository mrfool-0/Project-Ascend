package com.ascend.app.domain

enum class SystemTone(val displayName: String, val descriptor: String) {
    SUPPORTIVE("ALLY", "Calm and supportive"),
    DIRECT("COMMAND", "Clear and uncompromising"),
    RUTHLESS("RUTHLESS", "Hard truth without abuse"),
}

data class SystemContext(
    val playerName: String,
    val objective: Objective,
    val calorieTarget: Int,
    val proteinTarget: Int,
    val caloriesLogged: Int,
    val proteinLogged: Int,
    val waterLogged: Int,
    val waterTarget: Int,
    val streak: Int,
    val todayWorkout: String,
    val tomorrowWorkout: String,
    val workoutFrequency: Int,
    val focusAreas: Set<FocusArea>,
    val injuries: Set<InjuryArea>,
    val coreReason: String,
    val futureVision: String,
    val minimumPromise: String,
    val activeDaysLast7: Int = 0,
    val averageCompletionLast7: Int = 0,
    val workoutsLast7: Int = 0,
    val recentPlayerMessages: List<String> = emptyList(),
)

/**
 * ASCEND's private, on-device conversation brain.
 *
 * It classifies fitness-domain intent, resolves short follow-ups from recent player messages,
 * combines multiple requested topics, and changes voice without sending health data off-device.
 */
object SystemEngine {
    private enum class Intent {
        EMERGENCY, PAIN, CALORIES, PROTEIN, HYDRATION, WORKOUT, SCHEDULE, RECOVERY,
        PROGRESS, MOTIVATION, EMOTION, PLATEAU, WEIGHT_GOAL, APP_HELP, IDENTITY, GREETING,
    }

    fun respond(message: String, context: SystemContext, selectedTone: SystemTone = SystemTone.DIRECT): String {
        val raw = message.trim()
        if (raw.isBlank()) return "NO SIGNAL DETECTED. Transmit a question and I will calculate the next useful action."

        val input = raw.normalized()
        val resolvedInput = resolveFollowUp(input, context.recentPlayerMessages)
        val tone = requestedTone(input) ?: selectedTone

        if (intentScore(resolvedInput, Intent.EMERGENCY) > 0) return emergencyResponse(resolvedInput)
        if (intentScore(resolvedInput, Intent.PAIN) > 0) return painResponse(context)

        val intents = Intent.entries
            .filterNot { it == Intent.EMERGENCY || it == Intent.PAIN }
            .map { it to intentScore(resolvedInput, it) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }

        return when {
            isDayStatusQuestion(resolvedInput) -> statusResponse(context, tone)
            intents.isEmpty() -> fallbackResponse(context, tone, raw)
            intents.first() == Intent.GREETING -> greetingResponse(context, tone)
            intents.first() == Intent.IDENTITY -> identityResponse(tone)
            intents.first() == Intent.EMOTION -> emotionalResponse(context, tone, resolvedInput)
            else -> intents.distinct().take(2).joinToString("\n\n") { responseFor(it, context, tone, resolvedInput) }
        }.trim()
    }

    fun requiresImmediateSafetyResponse(message: String): Boolean {
        val input = message.normalized()
        return intentScore(input, Intent.EMERGENCY) > 0 || intentScore(input, Intent.PAIN) > 0
    }

    private fun responseFor(intent: Intent, context: SystemContext, tone: SystemTone, input: String): String = when (intent) {
        Intent.CALORIES -> calorieResponse(context, tone)
        Intent.PROTEIN -> proteinResponse(context, tone)
        Intent.HYDRATION -> hydrationResponse(context, tone)
        Intent.WORKOUT -> workoutResponse(context, tone)
        Intent.SCHEDULE -> scheduleResponse(context, tone, input)
        Intent.RECOVERY -> recoveryResponse(tone, input)
        Intent.PROGRESS -> progressResponse(context, tone)
        Intent.MOTIVATION -> motivationResponse(context, tone)
        Intent.EMOTION -> emotionalResponse(context, tone, input)
        Intent.PLATEAU -> plateauResponse(context, tone)
        Intent.WEIGHT_GOAL -> weightGoalResponse(context, tone)
        Intent.APP_HELP -> appHelpResponse()
        Intent.IDENTITY -> identityResponse(tone)
        Intent.GREETING -> greetingResponse(context, tone)
        Intent.EMERGENCY -> emergencyResponse(input)
        Intent.PAIN -> painResponse(context)
    }

    private fun statusResponse(context: SystemContext, tone: SystemTone): String {
        val caloriesRemaining = (context.calorieTarget - context.caloriesLogged).coerceAtLeast(0)
        val proteinRemaining = (context.proteinTarget - context.proteinLogged).coerceAtLeast(0)
        val waterRemaining = (context.waterTarget - context.waterLogged).coerceAtLeast(0)
        val lead = voice(
            tone,
            supportive = "STATUS SCAN COMPLETE. You are building the day one decision at a time.",
            direct = "STATUS SCAN COMPLETE. Here is the truth in the data.",
            ruthless = "STATUS SCAN COMPLETE. Numbers do not care about excuses; they show the next move.",
        )
        return "$lead\n" + listOf(
            "• $caloriesRemaining kcal remaining (${context.caloriesLogged}/${context.calorieTarget})",
            "• $proteinRemaining g protein remaining (${context.proteinLogged}/${context.proteinTarget})",
            "• $waterRemaining ml hydration remaining (${context.waterLogged}/${context.waterTarget})",
            "• ${context.streak}-day streak",
            "• 7-day pattern: ${context.activeDaysLast7} active days, ${context.averageCompletionLast7}% average completion, ${context.workoutsLast7} training sessions",
            "• Today's protocol: ${context.todayWorkout}",
        ).joinToString("\n") + "\nNEXT COMMAND: ${nextAction(context)}"
    }

    private fun calorieResponse(context: SystemContext, tone: SystemTone): String {
        val remaining = context.calorieTarget - context.caloriesLogged
        return when {
            remaining > 0 -> voice(
                tone,
                supportive = "You have about $remaining kcal left from today's ${context.calorieTarget} kcal estimate. Build the next meal around a protein source, plants, a useful carbohydrate, and a portion you can repeat.",
                direct = "$remaining kcal remain from today's ${context.calorieTarget} kcal estimate. Stop guessing: log the meal, anchor it with protein, and use the remaining budget deliberately.",
                ruthless = "$remaining kcal remain. Untracked bites are still calories. Choose the meal before hunger chooses it for you, log it honestly, and move on.",
            )
            remaining == 0 -> "CALORIE TARGET REACHED: ${context.caloriesLogged}/${context.calorieTarget} kcal. Eat from hunger and recovery needs now—not for points. The target is an estimate, not a medical prescription."
            else -> "You are ${-remaining} kcal above the planning target. No punishment and no starvation tomorrow. Log accurately, return to the normal plan at the next meal, and judge progress by the weekly pattern."
        }
    }

    private fun proteinResponse(context: SystemContext, tone: SystemTone): String {
        val remaining = (context.proteinTarget - context.proteinLogged).coerceAtLeast(0)
        if (remaining == 0) return "PROTEIN QUEST COMPLETE: ${context.proteinLogged}/${context.proteinTarget} g. Keep the rest of the day balanced; more is not automatically better."
        val foodExamples = "eggs, lentils, tofu, Greek yogurt, fish, paneer, or lean meat"
        return voice(
            tone,
            supportive = "$remaining g remains toward today's ${context.proteinTarget} g estimate. Add one practical serving from $foodExamples, based on your diet.",
            direct = "$remaining g protein remains. Make the next meal protein-first: choose $foodExamples, then build the rest of the plate.",
            ruthless = "$remaining g remains. Hoping dinner fixes it is not a strategy. Put a real protein source in the next meal and log the serving.",
        )
    }

    private fun hydrationResponse(context: SystemContext, tone: SystemTone): String {
        val remaining = (context.waterTarget - context.waterLogged).coerceAtLeast(0)
        if (remaining == 0) return "HYDRATION TARGET REACHED: ${context.waterLogged}/${context.waterTarget} ml. Drink to thirst from here and account for heat or long training; more is not always better."
        return voice(
            tone,
            supportive = "$remaining ml remains toward today's estimate. Take a normal glass now and spread the rest across the day.",
            direct = "$remaining ml remains. Drink a normal serving now, log it, and distribute the rest—do not force a huge volume at once.",
            ruthless = "$remaining ml remains. This is the easiest quest on the board. Drink a glass, log it, continue.",
        )
    }

    private fun workoutResponse(context: SystemContext, tone: SystemTone): String {
        val limitation = context.injuries.activeDescription()
        val focus = context.focusAreas.readableList().ifBlank { "your selected objective" }
        val base = "TODAY'S PROTOCOL: ${context.todayWorkout}. It supports $focus. Warm up gradually, keep 2–3 reps in reserve on early sets, and use controlled form."
        val safety = if (limitation == null) "Stop if a movement causes sharp or unusual pain." else "Your reported limitation is $limitation; use the programmed substitution and stop any painful movement."
        return "$base $safety " + voice(
            tone,
            supportive = "A shortened, honest session still counts when energy is low.",
            direct = "Start the warm-up before your mind opens another negotiation.",
            ruthless = "You do not need another speech. Put the phone down, begin the warm-up, and earn the feeling you are waiting for.",
        )
    }

    private fun scheduleResponse(context: SystemContext, tone: SystemTone, input: String): String {
        val requestedDay = when {
            "tomorrow" in input -> "TOMORROW: ${context.tomorrowWorkout}."
            "today" in input -> "TODAY: ${context.todayWorkout}."
            else -> "TODAY: ${context.todayWorkout}. TOMORROW: ${context.tomorrowWorkout}."
        }
        return "$requestedDay Your program uses ${context.workoutFrequency} training day${if (context.workoutFrequency == 1) "" else "s"} per week and automatically places recovery on the other days. " +
            voice(tone, "Follow the rhythm and adjust when recovery genuinely needs it.", "Protect the scheduled time like an appointment.", "A plan you keep beats a perfect plan you repeatedly abandon.")
    }

    private fun recoveryResponse(tone: SystemTone, input: String): String {
        if (input.containsAny("fever", "sick", "ill", "vomit", "flu")) {
            return "RECOVERY OVERRIDE: skip hard training while acutely ill, feverish, vomiting, or unusually weak. Rest, hydrate normally, and seek medical advice if symptoms are severe, worsening, or persistent."
        }
        if (input.containsAny("no sleep", "didn't sleep", "hardly slept", "exhausted")) {
            return "LOW-RECOVERY SIGNAL DETECTED. Do mobility, walking, or a reduced session instead of chasing intensity. One smart recovery day protects the campaign; repeated sleep loss is not a toughness test."
        }
        return voice(
            tone,
            supportive = "Recovery is part of the program. Aim for a consistent sleep window, a normal meal pattern, hydration, light movement, and a lower-effort day when fatigue stays elevated.",
            direct = "Recovery protocol: consistent sleep time, enough food and protein, normal hydration, and light movement. If performance and energy keep falling, reduce training load before adding more work.",
            ruthless = "Stop calling poor recovery dedication. If you train hard and sleep carelessly, you are sabotaging your own work. Protect tonight's sleep window.",
        )
    }

    private fun progressResponse(context: SystemContext, tone: SystemTone): String {
        val streakText = "Current streak: ${context.streak} day${if (context.streak == 1) "" else "s"}."
        return "$streakText " + voice(
            tone,
            supportive = "Progress is a trend, not one reading. Compare four-week consistency, training performance, energy, and recovery.",
            direct = "Judge the campaign by four-week consistency, workout performance, food logging, and recovery—not one weigh-in or one bad day.",
            ruthless = "You do not need a dramatic result today. You need enough ordinary days stacked together that the result has nowhere else to go.",
        )
    }

    private fun motivationResponse(context: SystemContext, tone: SystemTone): String {
        val reason = context.coreReason.ifBlank { "become someone who keeps promises to themselves" }
        val minimum = context.minimumPromise.ifBlank { "complete the smallest honest version of today's quest" }
        val quote = MotivationLibrary.quotes[Math.floorMod(context.streak + reason.hashCode(), MotivationLibrary.quotes.size)]
        return behaviorCue(context, tone) + " " + voice(
            tone,
            supportive = "You chose this because you want to $reason. You do not need to feel powerful first. $minimum, then let momentum help. $quote",
            direct = "Your reason: $reason. Motivation is optional; the next action is not. $minimum. $quote",
            ruthless = "Enough bargaining. The version of you that you described is built while the current version wants comfort. $minimum—now. $quote",
        )
    }

    private fun behaviorCue(context: SystemContext, tone: SystemTone): String = when {
        context.activeDaysLast7 == 0 -> voice(
            tone,
            "Your recent log is quiet. That is not a verdict; it means we restart with one action small enough to repeat.",
            "No progress was logged in the last seven days. Reset the pattern with one action now, not a heroic plan tomorrow.",
            "The last seven days show no logged follow-through. Stop designing the comeback and complete one small command now.",
        )
        context.averageCompletionLast7 < 45 || context.activeDaysLast7 <= 2 -> voice(
            tone,
            "Your recent pattern says the plan needs less friction. Choose a smaller minimum and identify the moment that usually breaks the chain.",
            "Recent adherence is inconsistent. Reduce the daily minimum, anchor it to a fixed cue, and name the obstacle that keeps repeating.",
            "Your recent behavior is below the standard you chose. Do not add complexity—remove one recurring excuse and lock the minimum action to a fixed time.",
        )
        context.averageCompletionLast7 >= 80 || context.streak >= 7 -> voice(
            tone,
            "Your recent consistency is strong. Protect the routine and progress only one variable at a time.",
            "The pattern is working. Keep the routine stable and progress one measurable variable this week.",
            "You have earned momentum. Do not waste it chasing novelty—raise one standard and keep every other variable stable.",
        )
        else -> "Your recent behavior is building, but not yet automatic. Keep the same cue, lower avoidable friction, and complete today's minimum before expanding it."
    }

    private fun emotionalResponse(context: SystemContext, tone: SystemTone, input: String): String {
        val overwhelmed = input.containsAny("overwhelmed", "too much", "stressed", "anxious", "terrible day")
        if (overwhelmed) {
            return "SIGNAL RECEIVED. Shrink the battlefield: drink water, take ten slow breaths, and complete only this minimum—${context.minimumPromise.ifBlank { "five minutes of useful movement" }}. If distress feels unsafe or unmanageable, contact someone you trust or a qualified mental-health professional."
        }
        return voice(
            tone,
            supportive = "A low mood is real, but it is not a verdict on you. Make the next action tiny: ${context.minimumPromise.ifBlank { "five minutes of movement" }}. Then reassess without judging yourself.",
            direct = "Your feeling is data, not an order. Complete ${context.minimumPromise.ifBlank { "five minutes of movement" }}, then decide whether recovery or the full session is the honest next move.",
            ruthless = "Your mood can ride with you, but it does not get the steering wheel. Complete ${context.minimumPromise.ifBlank { "five minutes of movement" }} before making another deal with yourself.",
        )
    }

    private fun plateauResponse(context: SystemContext, tone: SystemTone): String = voice(
        tone,
        supportive = "A plateau is a request for better evidence, not panic. Review three to four weeks of body-weight trend, workout performance, intake accuracy, sleep, and adherence before changing the plan.",
        direct = "Do not change five variables. Audit three to four weeks, then change one lever: training progression, average intake, daily movement, or recovery. Hold it long enough to measure.",
        ruthless = "A three-day stall is not a plateau. Stop chasing novelty. Prove three to four weeks of honest adherence, then adjust one variable with evidence.",
    ) + " Your current objective is ${context.objective.readable()}."

    private fun weightGoalResponse(context: SystemContext, tone: SystemTone): String {
        val objective = context.objective.readable()
        return "OBJECTIVE: $objective. " + when (context.objective) {
            Objective.FAT_LOSS -> "Use a moderate calorie deficit, keep protein and resistance training consistent, and judge the weekly weight trend—not daily noise."
            Objective.MUSCLE_GAIN -> "Use progressive resistance training, sufficient protein, and a modest energy surplus. Faster scale gain is not automatically better muscle gain."
            Objective.RECOMPOSITION -> "Prioritize progressive training, protein, and high consistency near maintenance calories. Expect slower scale change and watch performance and measurements."
            else -> voice(tone, "Build sustainable meals, training, sleep, and daily movement.", "Consistency across training, nutrition, sleep, and movement is the main target.", "Master the basics before demanding advanced results.")
        }
    }

    private fun appHelpResponse(): String =
        "SYSTEM MAP: use HOME for today's status, WORKOUT for the generated protocol, NUTRITION to log food or run the Food Vision photo scan, HABITS for repeatable quests, and PROGRESS for trends. Tell me what you are trying to log or change and I will point to the exact module."

    private fun identityResponse(tone: SystemTone): String =
        "I am ASCEND SYSTEM: your private on-device training, nutrition, recovery, and discipline interface. I read the progress you log in ASCEND and turn it into a useful next action. " +
            if (tone == SystemTone.RUTHLESS) "I can be ruthless, but I will not shame you, invent medical advice, or confuse punishment with progress." else "I can support you or challenge you, but I will not diagnose medical conditions."

    private fun greetingResponse(context: SystemContext, tone: SystemTone): String = voice(
        tone,
        supportive = "SYSTEM ONLINE. Good to see you, ${context.playerName}. Tell me what feels difficult or ask me to scan your day.",
        direct = "SYSTEM ONLINE, ${context.playerName}. State the objective, obstacle, or question.",
        ruthless = "SYSTEM ONLINE, ${context.playerName}. Skip the performance. Tell me the real obstacle and we will attack it.",
    )

    private fun painResponse(context: SystemContext): String {
        val reported = context.injuries.activeDescription() ?: "no specific prior limitation"
        return "SAFETY OVERRIDE. Do not train through sharp, worsening, sudden, or unexplained pain. Stop the provoking movement and use a pain-free range. Your profile lists $reported. Persistent symptoms need assessment by a qualified clinician or physiotherapist; I can adapt training guidance, but I cannot diagnose an injury."
    }

    private fun emergencyResponse(input: String): String = when {
        input.containsAny("suicid", "kill myself", "end my life", "self harm") ->
            "URGENT HUMAN SUPPORT NEEDED. Pause the app and contact local emergency services or a crisis line now. Move near another person, tell someone you trust exactly what is happening, and do not stay alone with immediate danger."
        else -> "URGENT MEDICAL SIGNAL. Stop training and contact local emergency services now. If possible, tell someone nearby and do not drive yourself. This needs human medical help, not coaching."
    }

    private fun fallbackResponse(context: SystemContext, tone: SystemTone, original: String): String {
        val acknowledged = original.take(120).trim().trimEnd('.', '?', '!')
        return voice(
            tone,
            supportive = "I understand that you are asking about “$acknowledged.” I am strongest on your ASCEND plan, workouts, food, calories, protein, hydration, recovery, progress, pain boundaries, and motivation. Give me the outcome you want and the obstacle in one sentence.",
            direct = "REQUEST PARSED: “$acknowledged.” I need a clearer fitness target to calculate a useful command. Tell me: desired outcome, current obstacle, and what you can do today. Current next action: ${nextAction(context)}",
            ruthless = "“$acknowledged” is not yet an actionable target. Replace the fog with facts: what do you want, what is stopping you, and what will you do today? Until then: ${nextAction(context)}",
        )
    }

    private fun nextAction(context: SystemContext): String = when {
        context.proteinLogged < context.proteinTarget * .65 -> "plan and log a protein-first meal"
        context.waterLogged < context.waterTarget * .60 -> "drink and log a normal glass of water"
        context.todayWorkout != "RECOVERY PROTOCOL" -> "begin the ${context.todayWorkout} warm-up"
        else -> "complete ten minutes of recovery movement"
    }

    private fun isDayStatusQuestion(input: String): Boolean =
        input.containsAny("scan my day", "how am i doing", "my status", "daily status", "check my day", "what do i need today")

    private fun requestedTone(input: String): SystemTone? = when {
        input.containsAny("be ruthless", "be harsh", "tough love", "no excuses", "roast me") -> SystemTone.RUTHLESS
        input.containsAny("be gentle", "be kind", "go easy", "support me") -> SystemTone.SUPPORTIVE
        input.containsAny("be direct", "straight answer", "just tell me") -> SystemTone.DIRECT
        else -> null
    }

    private fun resolveFollowUp(input: String, history: List<String>): String {
        if (input.length > 32 || !input.containsAny("that", "it", "tomorrow", "more", "why", "what about")) return input
        val previous = history.lastOrNull()?.normalized().orEmpty()
        return if (previous.isBlank()) input else "$input $previous"
    }

    private fun intentScore(input: String, intent: Intent): Int {
        val phrases = when (intent) {
            Intent.EMERGENCY -> listOf("chest pain", "cannot breathe", "can't breathe", "fainting", "passed out", "suicid", "kill myself", "end my life", "self harm", "emergency")
            Intent.PAIN -> listOf("injury", "injured", "pain", "hurts", "hurt", "sprain", "swollen", "medical", "physio")
            Intent.CALORIES -> listOf("calorie", "kcal", "food budget", "how much can i eat", "ate today", "meal")
            Intent.PROTEIN -> listOf("protein", "macro", "muscle food")
            Intent.HYDRATION -> listOf("water", "hydrate", "hydration", "thirst")
            Intent.WORKOUT -> listOf("workout", "train", "training", "exercise", "gym", "sets", "reps", "protocol")
            Intent.SCHEDULE -> listOf("schedule", "tomorrow", "rest day", "which day", "next workout", "when do i train")
            Intent.RECOVERY -> listOf("recover", "recovery", "sleep", "sore", "fatigue", "exhausted", "rest", "sick", "fever", "deload")
            Intent.PROGRESS -> listOf("progress", "streak", "score", "results", "doing well", "improving")
            Intent.MOTIVATION -> listOf("motivat", "lazy", "discipline", "procrastinat", "quit", "no drive", "no energy", "excuse", "push me")
            Intent.EMOTION -> listOf("sad", "depressed", "anxious", "overwhelmed", "stressed", "hate myself", "bad day", "feel terrible", "frustrated")
            Intent.PLATEAU -> listOf("plateau", "stuck", "not losing", "not gaining", "no progress")
            Intent.WEIGHT_GOAL -> listOf("lose weight", "gain weight", "gain muscle", "fat loss", "bulk", "cut", "recomposition", "body fat")
            Intent.APP_HELP -> listOf("how do i log", "where is", "how to use", "scan food", "food photo", "app help", "delete log")
            Intent.IDENTITY -> listOf("who are you", "what are you", "your name", "are you a coach", "system name")
            Intent.GREETING -> listOf("hello", "hey", "hi system", "good morning", "good evening", "what's up")
        }
        return phrases.sumOf { phrase ->
            when {
                input == phrase -> 4
                " $input ".contains(" $phrase ") -> 3
                input.contains(phrase) -> 2
                else -> 0
            }
        }
    }

    private fun voice(tone: SystemTone, supportive: String, direct: String, ruthless: String): String = when (tone) {
        SystemTone.SUPPORTIVE -> supportive
        SystemTone.DIRECT -> direct
        SystemTone.RUTHLESS -> ruthless
    }

    private fun String.normalized(): String = lowercase()
        .replace(Regex("[^a-z0-9' ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun String.containsAny(vararg values: String) = values.any(::contains)
    private fun Set<InjuryArea>.activeDescription(): String? = filter { it != InjuryArea.NONE }
        .joinToString { it.name.lowercase().replace('_', ' ') }
        .ifBlank { null }
    private fun Set<FocusArea>.readableList(): String = joinToString { it.name.lowercase().replace('_', ' ') }
    private fun Objective.readable(): String = name.lowercase().replace('_', ' ')
}
