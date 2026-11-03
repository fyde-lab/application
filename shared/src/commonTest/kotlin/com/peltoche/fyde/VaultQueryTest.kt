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
            category = FydeCategory.MONEY,
            subcategory = FydeSubcategory.BANKING,
            type = FydeDocumentType.STATEMENT,
            year = YearFilter.In(2025),
        )

        assertEquals(
            FydeSearchFilters(
                categories = listOf(FydeCategory.MONEY),
                subcategories = listOf(FydeSubcategory.BANKING),
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
            categories = listOf(FydeFacetCount(FydeCategory.HOUSING, 2)),
            subcategories = listOf(FydeFacetCount(FydeSubcategory.BANKING, 1)),
            types = listOf(FydeFacetCount(FydeDocumentType.INVOICE, 4)),
        )
        val query = VaultQuery(category = FydeCategory.MONEY, subcategory = FydeSubcategory.BANKING, type = FydeDocumentType.PAYSLIP)

        val options = vaultFilterOptions(facets, query)

        assertEquals(listOf(FydeFacetCount(FydeCategory.HOUSING, 2), FydeFacetCount(FydeCategory.MONEY, 0)), options.categories)
        assertEquals(listOf(FydeFacetCount(FydeSubcategory.BANKING, 1)), options.subcategories)
        assertEquals(listOf(FydeFacetCount(FydeDocumentType.INVOICE, 4), FydeFacetCount(FydeDocumentType.PAYSLIP, 0)), options.types)
    }

    @Test
    fun withCategoryDropsTheSubcategoryOnlyWhenTheCategoryChanges() {
        val query = VaultQuery(category = FydeCategory.TAXES, subcategory = FydeSubcategory.INCOME_TAX)

        assertEquals(query, query.withCategory(FydeCategory.TAXES))
        assertEquals(VaultQuery(category = FydeCategory.MONEY), query.withCategory(FydeCategory.MONEY))
        assertEquals(VaultQuery(), query.withCategory(null))
    }
}
