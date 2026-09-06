package com.ascend.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class XpAndProgressTest {
    @Test fun `duplicate XP source cannot be farmed and revert is clean`() {
        val ledger = InMemoryXpLedger()
        val date = LocalDate.of(2026, 9, 5)
        assertTrue(ledger.award(XpSourceType.HABIT, "meditate", date, 10))
        assertFalse(ledger.award(XpSourceType.HABIT, "meditate", date, 10))
        assertEquals(10, ledger.total)
        assertTrue(ledger.revert(XpSourceType.HABIT, "meditate", date))
        assertEquals(0, ledger.total)
    }

    @Test fun `personal record detects weight reps and estimated one rep max`() {
        val history = listOf(PerformanceSet(70.0, 8), PerformanceSet(75.0, 5))
        val result = PersonalRecordEngine.detect(PerformanceSet(80.0, 6), history)
        assertTrue(result.heaviestWeight)
        assertTrue(result.estimatedOneRepMaxRecord)
        assertTrue(result.isRecord)
    }

    @Test fun `weight trend uses averages and thirty day baseline`() {
        val today = LocalDate.of(2026, 9, 5)
        val trend = WeightTrendEngine.calculate(listOf(today.minusDays(31) to 80.0, today.minusDays(5) to 78.0, today to 77.0), today)
        assertEquals(77.0, trend.current!!, .001)
        assertEquals(77.5, trend.sevenDayAverage!!, .001)
        assertEquals(-3.0, trend.thirtyDayChange!!, .001)
    }

    @Test fun `weight trend excludes future dated entries`() {
        val today = LocalDate.of(2026, 9, 5)
        val trend = WeightTrendEngine.calculate(listOf(today to 77.0, today.plusDays(1) to 120.0), today)
        assertEquals(77.0, trend.current!!, .001)
        assertEquals(77.0, trend.sevenDayAverage!!, .001)
        assertEquals(0.0, trend.thirtyDayChange!!, .001)
    }
}
