package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals

class ReplayPercentTest {

    @Test
    fun roundsDown() {
        assertEquals(66, replayPercent(142, 214))
        assertEquals(0, replayPercent(1, 214))
    }

    @Test
    fun reachesHundredOnlyWhenCaughtUp() {
        assertEquals(99, replayPercent(999, 1000))
        assertEquals(100, replayPercent(1000, 1000))
    }

    @Test
    fun emptyHistoryIsComplete() {
        assertEquals(100, replayPercent(0, 0))
    }

    @Test
    fun staysWithinBounds() {
        assertEquals(100, replayPercent(300, 214))
        assertEquals(0, replayPercent(-1, 214))
    }
}
