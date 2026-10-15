package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentSearchTest {

    /** 2026-10-11T12:00:00Z. */
    private val now = epochSeconds(CivilDate(2026, 10, 11)) + 12 * 3_600

    @Test
    fun epochSecondsIsTheInverseOfCivilDate() {
        assertEquals(0L, epochSeconds(CivilDate(1970, 1, 1)))
        assertEquals(1_709_164_800L, epochSeconds(CivilDate(2024, 2, 29)))
        assertEquals(-86_400L, epochSeconds(CivilDate(1969, 12, 31)))
        assertEquals(CivilDate(2026, 10, 11), civilDate(now))
    }

    @Test
    fun periodBoundsCoverWholeUtcCalendarYears() {
        val start2025 = epochSeconds(CivilDate(2025, 1, 1))
        val start2026 = epochSeconds(CivilDate(2026, 1, 1))
        val start2027 = epochSeconds(CivilDate(2027, 1, 1))

        assertEquals(null to null, PeriodFilter.ANY.bounds(now))
        assertEquals(now - 30L * 86_400L to null, PeriodFilter.LAST_30_DAYS.bounds(now))
        assertEquals(start2026 to start2027 - 1, PeriodFilter.THIS_YEAR.bounds(now))
        assertEquals(start2025 to start2026 - 1, PeriodFilter.LAST_YEAR.bounds(now))
    }

    @Test
    fun toSearchFiltersMapsEveryPickedFilter() {
        val search = DocumentSearch(
            text = "facture",
            kind = DocumentKind.PDF,
            period = PeriodFilter.LAST_YEAR,
            category = FydeCategory.MONEY,
            subcategory = FydeSubcategory.BANKING,
        )

        assertEquals(
            FydeSearchFilters(
                categories = listOf(FydeCategory.MONEY),
                subcategories = listOf(FydeSubcategory.BANKING),
                contentTypes = listOf("application/pdf"),
                createdFrom = epochSeconds(CivilDate(2025, 1, 1)),
                createdUntil = epochSeconds(CivilDate(2026, 1, 1)) - 1,
            ),
            search.toSearchFilters(now, contentTypes = listOf("application/pdf")),
        )
        assertEquals(FydeSearchFilters(), DocumentSearch(text = "facture").toSearchFilters(now))
    }

    @Test
    fun contentTypesOfKeepsOnlyTheFacetsContentTypesOfThatKind() {
        val facets = FydeSearchFacets(
            contentTypes = listOf(
                FydeFacetCount("application/pdf", 3),
                FydeFacetCount("image/png", 2),
                FydeFacetCount("image/jpeg", 1),
            ),
        )

        assertEquals(listOf("image/png", "image/jpeg"), contentTypesOf(DocumentKind.IMAGE, facets))
        assertEquals(emptyList(), contentTypesOf(DocumentKind.SPREADSHEET, facets))
    }

    @Test
    fun filterOptionsGroupsContentTypesIntoKindsMostCommonFirst() {
        val facets = FydeSearchFacets(
            contentTypes = listOf(
                FydeFacetCount("application/pdf", 3),
                FydeFacetCount("image/png", 2),
                FydeFacetCount("image/jpeg", 2),
            ),
        )

        assertEquals(
            listOf(FydeFacetCount(DocumentKind.IMAGE, 4), FydeFacetCount(DocumentKind.PDF, 3)),
            filterOptions(facets, DocumentSearch(), now).kinds,
        )
    }

    @Test
    fun filterOptionsOnlyOffersPeriodsOverlappingTheMatchingDocuments() {
        val lastYear = epochSeconds(CivilDate(2025, 6, 1))
        val facets = FydeSearchFacets(earliestCreatedAt = lastYear, latestCreatedAt = lastYear)

        assertEquals(
            listOf(PeriodFilter.ANY, PeriodFilter.LAST_YEAR),
            filterOptions(facets, DocumentSearch(), now).periods,
        )
        // Nothing matches: only "any time", plus whatever is already picked.
        assertEquals(
            listOf(PeriodFilter.ANY, PeriodFilter.THIS_YEAR),
            filterOptions(FydeSearchFacets(), DocumentSearch(period = PeriodFilter.THIS_YEAR), now).periods,
        )
    }

    @Test
    fun filterOptionsKeepsThePickedValueEvenWhenTheFacetsNoLongerHaveIt() {
        val facets = FydeSearchFacets(
            categories = listOf(FydeFacetCount(FydeCategory.HOUSING, 2)),
            subcategories = listOf(FydeFacetCount(FydeSubcategory.BANKING, 1)),
        )
        val search = DocumentSearch(category = FydeCategory.MONEY, subcategory = FydeSubcategory.BANKING)

        val options = filterOptions(facets, search, now)

        assertEquals(
            listOf(FydeFacetCount(FydeCategory.HOUSING, 2), FydeFacetCount(FydeCategory.MONEY, 0)),
            options.categories,
        )
        assertEquals(listOf(FydeFacetCount(FydeSubcategory.BANKING, 1)), options.subcategories)
    }

    @Test
    fun withCategoryDropsTheSubcategoryOnlyWhenTheCategoryChanges() {
        val search = DocumentSearch(
            category = FydeCategory.TAXES,
            subcategory = FydeSubcategory.INCOME_TAX,
        )

        assertEquals(search, search.withCategory(FydeCategory.TAXES))
        assertEquals(
            DocumentSearch(category = FydeCategory.MONEY),
            search.withCategory(FydeCategory.MONEY),
        )
        assertEquals(DocumentSearch(), search.withCategory(null))
    }
}
