package com.ascend.app.cloud

import android.content.Context
import com.ascend.app.core.database.SystemMessageEntity
import com.ascend.app.domain.SystemContext
import com.ascend.app.domain.SystemAction
import com.ascend.app.domain.SystemActionType
import com.ascend.app.domain.SystemReply
import com.ascend.app.domain.SystemTone
import com.ascend.app.domain.HabitDifficulty
import com.ascend.app.domain.HabitFrequency
import com.ascend.app.domain.QuestCategory
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import org.json.JSONObject

class SystemAiService(private val context: Context) {
    val isConfigured: Boolean
        get() = FirebaseApp.initializeApp(context) != null

    suspend fun respond(
        message: String,
        systemContext: SystemContext,
        tone: SystemTone,
        conversation: List<SystemMessageEntity>,
    ): Result<SystemReply> = runCatching {
        check(isConfigured) { "Firebase AI Logic is not configured" }
        val schema = Schema.obj(
            mapOf(
                "reply" to Schema.string("The complete user-facing coaching response"),
                "action_type" to Schema.string("Exactly NONE, CREATE_HABIT, or CREATE_QUEST"),
                "action_name" to Schema.string("Short habit or quest name; empty when action_type is NONE"),
                "action_target" to Schema.double("Numeric target; use 1 when not applicable", minimum = 0.1, maximum = 100000.0),
                "action_unit" to Schema.string("Short unit such as done, steps, min, ml, pages, or reps"),
                "action_frequency" to Schema.string("Exactly EVERY_DAY, WEEKDAYS, or THREE_TIMES_WEEKLY"),
                "action_difficulty" to Schema.string("Exactly NORMAL, MEDIUM, or HARD"),
                "action_category" to Schema.string("Exactly TRAINING, NUTRITION, HYDRATION, DISCIPLINE, RECOVERY, or CONSISTENCY"),
                "action_reward_xp" to Schema.double("XP reward from 10 through 50", minimum = 10.0, maximum = 50.0),
            ),
        )
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.7-flash",
            generationConfig = generationConfig {
                maxOutputTokens = 520
                responseMimeType = "application/json"
                responseSchema = schema
            },
            systemInstruction = content(role = "system") {
                text(systemInstruction(systemContext, tone))
            },
        )
        val history = conversation
            .sortedBy { it.createdAt }
            .takeLast(10)
            .dropWhile { it.role != "PLAYER" }
            .map {
                content(role = if (it.role == "PLAYER") "user" else "model") { text(it.message.take(1200)) }
            }
        val answer = model.startChat(history = history).sendMessage(message).text?.trim()
        check(!answer.isNullOrBlank()) { "SYSTEM returned an empty response" }
        parseReply(answer)
    }

    private fun parseReply(json: String): SystemReply {
        val value = JSONObject(json)
        val reply = value.getString("reply").trim().take(2400)
        check(reply.isNotBlank()) { "SYSTEM returned an empty response" }
        val type = enumValueOr(value.optString("action_type"), SystemActionType.NONE)
        if (type == SystemActionType.NONE) return SystemReply(reply)
        val name = value.optString("action_name").trim().take(80)
        check(name.length >= 3) { "SYSTEM action needs a clear name" }
        return SystemReply(
            message = reply,
            action = SystemAction(
                type = type,
                name = name,
                target = value.optDouble("action_target", 1.0).coerceIn(.1, 100_000.0),
                unit = value.optString("action_unit", "done").trim().ifBlank { "done" }.take(20),
                frequency = enumValueOr(value.optString("action_frequency"), HabitFrequency.EVERY_DAY),
                difficulty = enumValueOr(value.optString("action_difficulty"), HabitDifficulty.NORMAL),
                category = enumValueOr(value.optString("action_category"), QuestCategory.DISCIPLINE),
                rewardXp = value.optDouble("action_reward_xp", 25.0).toInt().coerceIn(10, 50),
            ),
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOr(value: String, fallback: T): T =
        runCatching { enumValueOf<T>(value.trim().uppercase()) }.getOrDefault(fallback)

    private fun systemInstruction(context: SystemContext, tone: SystemTone): String {
        val voice = when (tone) {
            SystemTone.SUPPORTIVE -> "ACTIVE MODE: ALLY. Be grounded and steady; reinforce consistency, cognitive clarity, and one concrete next action."
            SystemTone.DIRECT -> "ACTIVE MODE: COMMAND. Be terse, directive, and zero-fluff; give precise sets, reps, timing, hydration, or nutrient targets whenever the supplied data supports them."
            SystemTone.RUTHLESS -> "ACTIVE MODE: RUTHLESS. Audit missed streaks, weak adherence, and poor nutrition execution immediately, then demand a precise corrective action, sound technique, and recovery discipline. Use hard truths, but never insult, humiliate, threaten, body-shame, encourage punishment, or dismiss genuine fatigue, pain, illness, or distress."
        }
        return """
            You are the ASCEND SYSTEM — a tactical performance OS and strategic discipline partner.
            You are not a generic polite support bot. Your name is SYSTEM, never Coach.
            Understand natural language, misspellings, emotions, follow-up questions, and compound requests.
            $voice

            Give specific, useful answers grounded only in the supplied player data. Do not invent logs,
            diagnoses, research citations, or exact outcomes. Treat calorie and nutrient numbers as estimates.
            Never prescribe medication or diagnose an injury. For sharp, worsening, sudden, or unexplained
            pain, advise stopping the provoking movement and seeking a qualified clinician. For chest pain,
            severe breathing difficulty, fainting, immediate self-harm risk, or another emergency, instruct
            the player to contact local emergency services and a nearby trusted person immediately.
            Do not make moral judgments about food or suggest starvation, purging, dangerous dehydration,
            extreme exercise, or using pain as proof of discipline.

            Write in a clean robotic game-system voice. Keep ordinary answers under 3–4 punchy sentences.
            Only exceed that limit when the player explicitly requests an in-depth breakdown.
            End with a practical NEXT COMMAND when appropriate. Ask at most one clarifying question and only
            when the missing detail materially changes the answer. Act as a knowledgeable fitness coach:
            explain form, progression, recovery, and sustainable nutrition clearly, while staying inside the
            medical boundaries above. Adapt the size and intensity of the next action to the player's logged
            behavior. If adherence is low, reduce friction and ask what repeatedly blocks them. If adherence
            and streak are strong, reinforce the identity and offer a measured progression.

            Use transparent motivational interviewing, implementation intentions, identity cues, and positive
            reinforcement. Never use deception, fear, dependency, humiliation, or covert psychological
            manipulation. Preserve the player's autonomy and say why a behavior-change tactic may help.

            ACTION PROTOCOL
            If and only if the player explicitly asks to add/create a habit, return CREATE_HABIT. If and only if
            they explicitly ask to add/create/customize a quest, return CREATE_QUEST. Extract a useful short
            name, target, unit, schedule, difficulty, category, and bounded XP reward. Never perform an action
            from a hypothetical question. If essential action details are missing, use NONE and ask one question.
            For all ordinary coaching responses, use NONE and neutral defaults for the remaining action fields.

            PLAYER DATA
            Name: ${context.playerName}
            Objective: ${context.objective.name}
            Focus areas: ${context.focusAreas.joinToString { it.name }}
            Calibrated BMR: ${context.bmr} kcal
            Workout frequency: ${context.workoutFrequency} days/week
            Weekly split: ${context.weeklySplit.ifBlank { "frequency optimized" }}
            Today's workout: ${context.todayWorkout}
            Tomorrow's workout: ${context.tomorrowWorkout}
            Calories: ${context.caloriesLogged}/${context.calorieTarget} kcal
            Protein: ${context.proteinLogged}/${context.proteinTarget} g
            Carbohydrate target: ${context.carbohydrateTarget} g
            Fat target: ${context.fatTarget} g
            Hydration: ${context.waterLogged}/${context.waterTarget} ml
            Current streak: ${context.streak} days
            Last 7 days with logged progress: ${context.activeDaysLast7}
            Last 7 day average completion: ${context.averageCompletionLast7}%
            Training sessions in last 7 days: ${context.workoutsLast7}
            Reported limitations: ${context.injuries.joinToString { it.name }}
            Core reason: ${context.coreReason.ifBlank { "not supplied" }}
            Future vision: ${context.futureVision.ifBlank { "not supplied" }}
            Minimum promise: ${context.minimumPromise.ifBlank { "not supplied" }}
        """.trimIndent()
    }
}
