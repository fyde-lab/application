package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentFormattingTest {

    @Test
    fun civilDateConvertsEpochSecondsToTheUtcCalendarDate() {
        assertEquals(CivilDate(1970, 1, 1), civilDate(0))
        assertEquals(CivilDate(2024, 2, 29), civilDate(1_709_164_800))
        assertEquals(CivilDate(1969, 12, 31), civilDate(-1))
    }

    @Test
    fun sizeInUnitPicksTheLargestUnitReached() {
        assertEquals(512.0 to SizeUnit.BYTES, sizeInUnit(512))
        assertEquals(1.5 to SizeUnit.KILOBYTES, sizeInUnit(1536))
        assertEquals(2.0 to SizeUnit.MEGABYTES, sizeInUnit(2L * 1024 * 1024))
        assertEquals(3.0 to SizeUnit.GIGABYTES, sizeInUnit(3L * 1024 * 1024 * 1024))
    }

    @Test
    fun keepsOneTruncatedDecimalBelowTenOnly() {
        assertEquals("1.9", formatOneDecimalBelowTen(1.99, "."))
        assertEquals("1,0", formatOneDecimalBelowTen(1.0, ","))
        assertEquals("12", formatOneDecimalBelowTen(12.7, "."))
    }

    @Test
    fun parsesAllowedDomainsFromLinesCommasAndSpaces() {
        assertEquals(
            listOf("example.com", "api.example.com", "other.org"),
            parseAllowedDomains(" Example.com\napi.example.com, other.org  example.com\n\n"),
        )
        assertEquals(emptyList(), parseAllowedDomains("  \n "))
    }
}
