package com.ascend.app

import androidx.test.platform.app.InstrumentationRegistry
import com.ascend.app.cloud.SystemAiService
import com.ascend.app.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import org.junit.Test

/** Opt-in probe: fictitious fixture only, no player database or chat writes. */
class SystemAiLiveTest {
    @Test fun liveNaturalLanguageResponse() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveAi") == "true")
        val service = SystemAiService(InstrumentationRegistry.getInstrumentation().targetContext)
        val context = SystemContext("TEST PLAYER", Objective.GENERAL_HEALTH, 2200, 140, 1500, 95, 1500, 2500, 4,
            "UPPER", "RECOVERY", 4, focusAreas = setOf(FocusArea.FULL_BODY), injuries = emptySet(),
            coreReason = "consistency", futureVision = "fitness", minimumPromise = "ten minutes")
        val result = withTimeout(45_000) { service.respond("I had a tough day and need encouragement to do a short workout, not a report of my calories.", context, SystemTone.SUPPORTIVE, emptyList()) }
        // This opt-in test sends a fictitious fixture only. Redact credential-shaped values.
        val diagnostic = result.exceptionOrNull()?.let {
            (it.javaClass.simpleName + ": " + it.message.orEmpty())
                .replace(Regex("AIza[\\w-]+|(?i)bearer\\s+\\S+"), "[REDACTED]")
                .take(1200)
        } ?: "Live response returned"
        assertTrue(diagnostic, result.isSuccess)
        assertTrue(result.getOrThrow().message.isNotBlank())
        assertEquals(SystemActionType.NONE, result.getOrThrow().action.type)
    }
}
