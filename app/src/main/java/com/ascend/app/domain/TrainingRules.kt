package com.ascend.app.domain

import java.time.LocalDate

/** Full Body is one canonical choice, never Full Body plus competing priorities. */
object FocusRules {
    val muscles = setOf(FocusArea.CHEST, FocusArea.BACK, FocusArea.SHOULDERS, FocusArea.ARMS, FocusArea.CORE, FocusArea.GLUTES, FocusArea.LEGS)
    fun normalize(values: Set<FocusArea>): Set<FocusArea> =
        if (FocusArea.FULL_BODY in values || values.containsAll(muscles)) setOf(FocusArea.FULL_BODY) else values
    fun toggle(values: Set<FocusArea>, value: FocusArea): Set<FocusArea> {
        if (value == FocusArea.FULL_BODY) return if (value in values) emptySet() else setOf(value)
        val current = if (FocusArea.FULL_BODY in values) muscles else values
        return normalize(if (value in current) current - value else current + value)
    }
    fun includes(values: Set<FocusArea>, value: FocusArea) =
        value in values || (FocusArea.FULL_BODY in values && value in muscles)
}

object WeekRules {
    fun monday(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())
    fun validateSwap(first: LocalDate, second: LocalDate, today: LocalDate) {
        require(first != second) { "Choose two different days." }
        require(first >= today && second >= today) { "Past days cannot be changed." }
        require(monday(first) == monday(today) && monday(second) == monday(today)) { "Swaps apply to this Monday–Sunday week only. Next week stays unchanged." }
    }
}
