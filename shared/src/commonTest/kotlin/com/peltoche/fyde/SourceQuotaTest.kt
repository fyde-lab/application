package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SourceQuotaTest {

    private fun usage(maxScripts: Int, enabledScripts: Int) = FydePlanUsage(
        planId = "free",
        maxScripts = maxScripts,
        maxSessions = 2,
        maxSize = 0,
        enabledScripts = enabledScripts,
        openSessions = 1,
        totalSize = 0,
    )

    @Test
    fun countsTheActiveSourcesWithTheSelection() {
        val quota = sourceQuotaOf(usage(maxScripts = 5, enabledScripts = 1))!!
        assertEquals(1, quota.used(selected = 0))
        assertEquals(4, quota.used(selected = 3))
    }

    @Test
    fun allowsAnotherSourceOnlyBelowTheLimit() {
        val quota = sourceQuotaOf(usage(maxScripts = 5, enabledScripts = 0))!!
        assertTrue(quota.allowsAnother(selected = 4))
        assertFalse(quota.allowsAnother(selected = 5))
        // A plan moved below what's already active is full from the start.
        assertFalse(sourceQuotaOf(usage(maxScripts = 2, enabledScripts = 3))!!.allowsAnother(selected = 0))
    }

    @Test
    fun aPlanWithoutALimitHasNoQuota() {
        assertNull(sourceQuotaOf(usage(maxScripts = 0, enabledScripts = 4)))
    }
}
