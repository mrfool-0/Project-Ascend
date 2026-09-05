package com.ascend.app.domain

import org.junit.Assert.*
import org.junit.Test

class LevelEngineTest {
    @Test fun `level starts at one and advances at threshold`() {
        assertEquals(1, LevelEngine.fromLifetimeXp(0).level)
        val threshold = LevelEngine.xpForNextLevel(1)
        assertEquals(1, LevelEngine.fromLifetimeXp(threshold - 1).level)
        assertEquals(2, LevelEngine.fromLifetimeXp(threshold).level)
    }

    @Test fun `level is capped at one hundred`() {
        val progress = LevelEngine.fromLifetimeXp(Int.MAX_VALUE)
        assertEquals(100, progress.level)
        assertTrue(progress.isMaxLevel)
        assertEquals(0, progress.xpForNextLevel)
    }

    @Test fun `rank boundaries are exact`() {
        assertEquals(PlayerRank.RECRUIT, LevelEngine.rankFor(9))
        assertEquals(PlayerRank.STRIKER, LevelEngine.rankFor(10))
        assertEquals(PlayerRank.SOVEREIGN, LevelEngine.rankFor(99))
        assertEquals(PlayerRank.TRANSCENDENT, LevelEngine.rankFor(100))
    }
}
