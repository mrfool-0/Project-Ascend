package com.ascend.app.healthconnect

import java.time.LocalDate

data class ImportedHealthSnapshot(
    val date: LocalDate,
    val steps: Long? = null,
    val weightKg: Double? = null,
    val activeCalories: Double? = null,
    val exerciseSessionCount: Int? = null,
)

/** Explicit opt-in boundary for a future Health Connect SDK implementation. */
interface HealthConnectGateway {
    val isAvailable: Boolean
    suspend fun grantedPermissions(): Set<String>
    suspend fun readDailySnapshot(date: LocalDate): ImportedHealthSnapshot?
}

class DisabledHealthConnectGateway : HealthConnectGateway {
    override val isAvailable = false
    override suspend fun grantedPermissions(): Set<String> = emptySet()
    override suspend fun readDailySnapshot(date: LocalDate): ImportedHealthSnapshot? = null
}
