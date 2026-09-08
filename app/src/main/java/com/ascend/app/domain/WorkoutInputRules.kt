package com.ascend.app.domain

/** Input validation is shared by the set editor and persistence boundary. */
object WorkoutInputRules {
    fun isValidSet(weight: Double?, reps: Int?, completed: Boolean): Boolean =
        weight != null && weight.isFinite() && weight in 0.0..1_500.0 &&
            reps != null && reps in (if (completed) 1 else 0)..1_000

    fun isValidExercise(name: String, sets: Int?, min: Int?, max: Int?): Boolean =
        name.trim().length in 2..80 && sets != null && sets in 1..10 &&
            min != null && max != null && min in 1..100 && max in min..100

    fun weightInput(value: String): Boolean = value.matches(Regex("[0-9]{0,4}(\\.[0-9]{0,2})?"))
}
