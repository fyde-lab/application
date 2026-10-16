package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals

class PlanUsageTest {

    @Test
    fun usageLevelWarnsNearTheLimitAndFlagsGoingPastIt() {
        assertEquals(UsageLevel.NORMAL, usageLevel(used = 0, limit = 5))
        assertEquals(UsageLevel.NORMAL, usageLevel(used = 89, limit = 100))
        assertEquals(UsageLevel.NEAR, usageLevel(used = 90, limit = 100))
        assertEquals(UsageLevel.FULL, usageLevel(used = 1, limit = 1))
        assertEquals(UsageLevel.OVER, usageLevel(used = 6, limit = 5))
    }

    @Test
    fun usageFractionIsClampedToTheBar() {
        assertEquals(0f, usageFraction(used = 0, limit = 4))
        assertEquals(0.5f, usageFraction(used = 2, limit = 4))
        assertEquals(1f, usageFraction(used = 9, limit = 4))
        assertEquals(1f, usageFraction(used = 0, limit = 0))
    }
}
