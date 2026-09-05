package com.ascend.app.domain

data class CoachContext(
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
    val injuries: Set<InjuryArea>,
    val coreReason: String,
)

object CoachEngine {
    fun respond(message: String, context: CoachContext): String {
        val input = message.trim().lowercase()
        if (input.isBlank()) return "Transmit a question and I will calculate your next useful action."
        if (input.containsAny("chest pain", "faint", "can't breathe", "cannot breathe", "suicid", "emergency")) {
            return "This may need urgent human help, not coaching. Stop training. Contact local emergency services now, and tell someone nearby."
        }
        if (input.containsAny("injury", "pain", "hurt", "medical")) {
            return "Do not train through sharp, worsening, or unexplained pain. Use a pain-free range and stop the provoking movement. Because you reported ${context.injuries.humanize()}, a qualified clinician or physiotherapist should clear persistent symptoms. I can adapt a plan, but I cannot diagnose an injury."
        }
        if (input.containsAny("calorie", "food", "eat", "meal", "hungry")) {
            val remaining = (context.calorieTarget - context.caloriesLogged).coerceAtLeast(0)
            return if (remaining == 0) {
                "Energy objective reached. Choose your next meal for hunger, recovery, and food quality—not extra XP. Logged: ${context.caloriesLogged} kcal."
            } else {
                "You have about $remaining kcal remaining from your ${context.calorieTarget} kcal planning target. Build the next meal around protein, vegetables or fruit, a useful carbohydrate, and a portion you can repeat. Targets are estimates, not medical prescriptions."
            }
        }
        if (input.containsAny("protein", "macro")) {
            val remaining = (context.proteinTarget - context.proteinLogged).coerceAtLeast(0)
            return if (remaining == 0) "Protein quest complete at ${context.proteinLogged} g. Spread future servings across the day and keep total food balanced."
            else "$remaining g remains toward today's ${context.proteinTarget} g protein estimate. A practical next choice could be eggs, lentils, tofu, Greek yogurt, fish, or lean meat according to your diet."
        }
        if (input.containsAny("water", "hydrate")) {
            val remaining = (context.waterTarget - context.waterLogged).coerceAtLeast(0)
            return if (remaining == 0) "Hydration target reached. Drink to thirst from here and account for heat and long training; more is not always better."
            else "$remaining ml remains toward the hydration estimate. Take a normal serving now and spread the rest across the day. Do not force large volumes quickly."
        }
        if (input.containsAny("workout", "train", "exercise", "today")) {
            return "Today's generated protocol is ${context.todayWorkout}. Start with a gradual warm-up, keep 2–3 reps in reserve on early sets, and stop any movement that causes pain. The win condition is controlled work, not exhaustion."
        }
        if (input.containsAny("motivat", "quit", "lazy", "tired", "discipline")) {
            return "Your reason was: “${context.coreReason.ifBlank { "become someone who keeps promises" }}.” Do not negotiate with the whole campaign. Complete the smallest honest version of today's quest, then decide the next move. ${MotivationLibrary.quotes[(context.streak + 3) % MotivationLibrary.quotes.size]}"
        }
        if (input.containsAny("progress", "streak", "score")) {
            return "Current streak: ${context.streak} day${if (context.streak == 1) "" else "s"}. Progress is a trend, not one reading. Compare four-week consistency, training performance, energy, and recovery instead of reacting to a single day."
        }
        return "${context.playerName}, focus the next turn: complete ${context.todayWorkout}, log the meal you actually ate, and protect recovery. ${MotivationLibrary.quotes[(message.hashCode() and Int.MAX_VALUE) % MotivationLibrary.quotes.size]} Ask me about training, calories, protein, hydration, pain, or motivation for a more specific protocol."
    }

    private fun String.containsAny(vararg values: String) = values.any(::contains)
    private fun Set<InjuryArea>.humanize() = filter { it != InjuryArea.NONE }.joinToString { it.name.lowercase().replace('_', ' ') }.ifBlank { "no specific injury" }
}
