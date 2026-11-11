package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals

class FilingReviewTest {

    @Test
    fun decodingSkipsBlanksAndKeepsTheOrder() {
        assertEquals(listOf("a", "b"), decodeFilingReviewed("a,,b,").toList())
        assertEquals(emptySet(), decodeFilingReviewed(null))
    }

    @Test
    fun aConfirmedDocumentBecomesTheNewest() {
        assertEquals(listOf("b", "a"), withFilingReviewed(setOf("a", "b"), "a").toList())
        assertEquals("a,b,c", encodeFilingReviewed(withFilingReviewed(setOf("a", "b"), "c")))
    }

    @Test
    fun onlyTheMostRecentAreKept() {
        val full = (1..FILING_REVIEWED_MAX).map { "d$it" }.toSet()

        val next = withFilingReviewed(full, "new")

        assertEquals(FILING_REVIEWED_MAX, next.size)
        assertEquals("d2", next.first())
        assertEquals("new", next.last())
    }
}
