package com.ascend.app.cloud

import android.content.Context
import com.ascend.app.core.database.SystemMessageEntity
import com.ascend.app.domain.SystemContext
import com.ascend.app.domain.SystemTone
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig

class SystemAiService(private val context: Context) {
    val isConfigured: Boolean
        get() = FirebaseApp.initializeApp(context) != null

    suspend fun respond(
        message: String,
        systemContext: SystemContext,
        tone: SystemTone,
        conversation: List<SystemMessageEntity>,
    ): Result<String> = runCatching {
        check(isConfigured) { "Firebase AI Logic is not configured" }
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.7-flash",
            generationConfig = generationConfig { maxOutputTokens = 650 },
            systemInstruction = content(role = "system") {
                text(systemInstruction(systemContext, tone))
            },
        )
        val history = conversation
            .sortedBy { it.createdAt }
            .takeLast(10)
            .map {
                content(role = if (it.role == "PLAYER") "user" else "model") { text(it.message.take(1200)) }
            }
        val answer = model.startChat(history = history).sendMessage(message).text?.trim()
        check(!answer.isNullOrBlank()) { "SYSTEM returned an empty response" }
        answer.take(2400)
    }

    private fun systemInstruction(context: SystemContext, tone: SystemTone): String {
        val voice = when (tone) {
            SystemTone.SUPPORTIVE -> "Be warm, calm, validating, and hopeful. Still give one concrete action."
            SystemTone.DIRECT -> "Be concise, candid, firm, and action-oriented. Do not over-comfort or lecture."
            SystemTone.RUTHLESS -> "Use disciplined tough love and hard truths. Challenge excuses strongly, but never insult, humiliate, threaten, body-shame, encourage punishment, or dismiss genuine fatigue, pain, illness, or distress."
        }
        return """
            You are ASCEND SYSTEM, a fitness RPG application's intelligent health, nutrition,
            training, recovery, and discipline interface. Your name is SYSTEM, never Coach.
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

            Write in a clean robotic game-system voice. Prefer 2–5 short paragraphs or compact bullets.
            End with a practical NEXT COMMAND when appropriate. Ask at most one clarifying question and only
            when the missing detail materially changes the answer.

            PLAYER DATA
            Name: ${context.playerName}
            Objective: ${context.objective.name}
            Focus areas: ${context.focusAreas.joinToString { it.name }}
            Workout frequency: ${context.workoutFrequency} days/week
            Today's workout: ${context.todayWorkout}
            Tomorrow's workout: ${context.tomorrowWorkout}
            Calories: ${context.caloriesLogged}/${context.calorieTarget} kcal
            Protein: ${context.proteinLogged}/${context.proteinTarget} g
            Hydration: ${context.waterLogged}/${context.waterTarget} ml
            Current streak: ${context.streak} days
            Reported limitations: ${context.injuries.joinToString { it.name }}
            Core reason: ${context.coreReason.ifBlank { "not supplied" }}
            Future vision: ${context.futureVision.ifBlank { "not supplied" }}
            Minimum promise: ${context.minimumPromise.ifBlank { "not supplied" }}
        """.trimIndent()
    }
}
