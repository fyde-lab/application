package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals

class VaultQueryTest {

    private fun on(year: Long, month: Int = 6, day: Int = 1) = epochSeconds(CivilDate(year, month, day))

    @Test
    fun epochSecondsIsTheInverseOfCivilDate() {
        assertEquals(0L, epochSeconds(CivilDate(1970, 1, 1)))
        assertEquals(1_709_164_800L, epochSeconds(CivilDate(2024, 2, 29)))
        assertEquals(-86_400L, epochSeconds(CivilDate(1969, 12, 31)))
        assertEquals(CivilDate(2026, 10, 11), civilDate(on(2026, 10, 11) + 12 * 3_600))
    }

    @Test
    fun yearBoundsCoverWholeUtcCalendarYears() {
        assertEquals(on(2025, 1, 1) to on(2026, 1, 1) - 1, YearFilter.In(2025).bounds())
        assertEquals(null to on(2024, 1, 1) - 1, YearFilter.Before(2024).bounds())
    }

    @Test
    fun toSearchFiltersMapsEveryPickedFilter() {
        val query = VaultQuery(
            text = "facture",
            folder = FydeFolderFilter.Within("money"),
            type = FydeDocumentType.STATEMENT,
            year = YearFilter.In(2025),
        )

        assertEquals(
            FydeSearchFilters(
                folder = FydeFolderFilter.Within("money"),
                types = listOf(FydeDocumentType.STATEMENT),
                createdFrom = on(2025, 1, 1),
                createdUntil = on(2026, 1, 1) - 1,
            ),
            query.toSearchFilters(),
        )
        assertEquals(FydeSearchFilters(), VaultQuery(text = "facture").toSearchFilters())
    }

    @Test
    fun yearChoicesOfferTheNewestThreeYearsThenBefore() {
        assertEquals(
            listOf(YearFilter.In(2026), YearFilter.In(2025), YearFilter.In(2024), YearFilter.Before(2024)),
            yearChoices(earliest = on(2019), latest = on(2026), selected = null),
        )
        assertEquals(
            listOf(YearFilter.In(2026), YearFilter.In(2025)),
            yearChoices(earliest = on(2025), latest = on(2026), selected = null),
        )
    }

    @Test
    fun yearChoicesOfferNothingForASingleYearOrNoDocuments() {
        assertEquals(emptyList(), yearChoices(earliest = on(2026, 1), latest = on(2026, 9), selected = null))
        assertEquals(emptyList(), yearChoices(earliest = null, latest = null, selected = null))
        assertEquals(listOf(YearFilter.In(2020)), yearChoices(earliest = null, latest = null, selected = YearFilter.In(2020)))
    }

    @Test
    fun filterOptionsKeepThePickedValueEvenWhenTheFacetsNoLongerHaveIt() {
        val facets = FydeSearchFacets(
            folders = listOf(FydeFacetCount("housing", 2)),
            types = listOf(FydeFacetCount(FydeDocumentType.INVOICE, 4)),
        )
        val query = VaultQuery(folder = FydeFolderFilter.Within("money"), type = FydeDocumentType.PAYSLIP)

        val options = vaultFilterOptions(facets, query)

        assertEquals(
            listOf<FydeFacetCount<FydeFolderFilter>>(
                FydeFacetCount(FydeFolderFilter.Within("housing"), 2),
                FydeFacetCount(FydeFolderFilter.Within("money"), 0),
            ),
            options.folders,
        )
        assertEquals(listOf(FydeFacetCount(FydeDocumentType.INVOICE, 4), FydeFacetCount(FydeDocumentType.PAYSLIP, 0)), options.types)
    }

    @Test
    fun folderOptionsPutUnfiledFirstOnlyWhenItHoldsMatches() {
        val folders = listOf(FydeFacetCount("work", 3))

        assertEquals(
            listOf<FydeFacetCount<FydeFolderFilter>>(
                FydeFacetCount(FydeFolderFilter.Unfiled, 2),
                FydeFacetCount(FydeFolderFilter.Within("work"), 3),
            ),
            vaultFilterOptions(FydeSearchFacets(folders = folders, unfiled = 2), VaultQuery()).folders,
        )
        assertEquals(
            listOf<FydeFacetCount<FydeFolderFilter>>(FydeFacetCount(FydeFolderFilter.Within("work"), 3)),
            vaultFilterOptions(FydeSearchFacets(folders = folders), VaultQuery()).folders,
        )
    }

    @Test
    fun termsAreTheTypedWords() {
        assertEquals(listOf("avis", "impôt"), VaultQuery(text = "  avis   impôt ").terms)
        assertEquals(emptyList(), VaultQuery(text = " ").terms)
    }

    @Test
    fun resultsShowOnceTextTypeOrYearIsPicked() {
        assertEquals(false, VaultQuery(text = "  ", folder = FydeFolderFilter.Unfiled).showsResults)
        assertEquals(true, VaultQuery(text = "edf").showsResults)
        assertEquals(true, VaultQuery(type = FydeDocumentType.INVOICE).showsResults)
        assertEquals(true, VaultQuery(year = YearFilter.In(2025)).showsResults)
    }
}
