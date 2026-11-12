package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals

class SimilarityTest {
    @Test
    fun similarityPercent_roundsDown() {
        assertEquals(93, similarityPercent(0.938f))
        assertEquals(99, similarityPercent(0.9999f))
    }

    @Test
    fun similarityPercent_keepsWholeValuesStoredJustBelow() {
        // 0.94f is 0.93999…: still 94, not 93.
        assertEquals(94, similarityPercent(0.94f))
        assertEquals(29, similarityPercent(0.29f))
    }

    @Test
    fun similarityPercent_readsHundredOnlyForIdenticalText() {
        assertEquals(100, similarityPercent(1f))
        assertEquals(0, similarityPercent(0f))
    }

    @Test
    fun similarityPercent_staysWithinZeroAndHundred() {
        assertEquals(100, similarityPercent(1.2f))
        assertEquals(0, similarityPercent(-0.1f))
    }
}
