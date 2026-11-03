package com.peltoche.fyde

/**
 * The Année filter's choices: one calendar year, or everything before one
 * ("Avant"). Years are UTC calendar years, like [localizedDate] shows them.
 */
sealed interface YearFilter {
    data class In(val year: Long) : YearFilter

    data class Before(val year: Long) : YearFilter

    /** The inclusive `createdAt` range (epoch seconds, `null` when open) this choice covers. */
    fun bounds(): Pair<Long?, Long?> = when (this) {
        is In -> startOfYear(year) to startOfYear(year + 1) - 1
        is Before -> null to startOfYear(year) - 1
    }
}

private fun startOfYear(year: Long): Long = epochSeconds(CivilDate(year, 1, 1))

/**
 * What a vault page asks `fyde-sdk` for: a category page's category,
 * subcategory and year (V3, D1), or the search page's text and facet chips
 * (V4, V5). Held by `App.kt`'s vault container, which runs it through
 * [FydeSdk.searchDocuments]/[FydeSdk.searchDocumentFacets]; the screens only
 * edit it. Each filter is a single choice, `null` for any.
 */
data class VaultQuery(
    val text: String = "",
    val category: FydeCategory? = null,
    val subcategory: FydeSubcategory? = null,
    val type: FydeDocumentType? = null,
    val year: YearFilter? = null,
) {
    val filtersActive: Boolean
        get() = category != null || subcategory != null || type != null || year != null

    /**
     * Picks [category]. Its subcategory is dropped along with it: each
     * subcategory belongs to exactly one category, so the old one can't
     * apply to the new category.
     */
    fun withCategory(category: FydeCategory?): VaultQuery =
        if (category == this.category) this else copy(category = category, subcategory = null)

    /** This query's filters in [FydeSdk.searchDocuments]' terms. */
    fun toSearchFilters(): FydeSearchFilters {
        val (createdFrom, createdUntil) = year?.bounds() ?: (null to null)
        return FydeSearchFilters(
            categories = listOfNotNull(category),
            subcategories = listOfNotNull(subcategory),
            types = listOfNotNull(type),
            createdFrom = createdFrom,
            createdUntil = createdUntil,
        )
    }
}

/**
 * Every filter choice the SDK's [FydeSearchFacets] say still leads to
 * documents, with how many, for a page's chips to offer — plus the selected
 * one even if it doesn't, so a chip never hides the choice it's showing.
 */
data class VaultFilterOptions(
    val categories: List<FydeFacetCount<FydeCategory>> = emptyList(),
    val subcategories: List<FydeFacetCount<FydeSubcategory>> = emptyList(),
    val types: List<FydeFacetCount<FydeDocumentType>> = emptyList(),
    val years: List<YearFilter> = emptyList(),
)

internal fun vaultFilterOptions(facets: FydeSearchFacets, query: VaultQuery): VaultFilterOptions = VaultFilterOptions(
    categories = facets.categories.withSelected(query.category),
    subcategories = facets.subcategories.withSelected(query.subcategory),
    types = facets.types.withSelected(query.type),
    years = yearChoices(facets.earliestCreatedAt, facets.latestCreatedAt, query.year),
)

/**
 * The Année chips for documents created between [earliest] and [latest]
 * (the SDK's span, which ignores the year filter itself): the newest
 * [YEAR_CHOICES] years that have any, newest first, then "Avant" the
 * oldest of them if documents go back further. Nothing to choose from a
 * single year. [selected] is kept even outside the span.
 */
internal fun yearChoices(earliest: Long?, latest: Long?, selected: YearFilter?): List<YearFilter> {
    val choices = if (earliest == null || latest == null) {
        emptyList()
    } else {
        val newest = civilDate(latest).year
        val oldest = civilDate(earliest).year
        val shownOldest = maxOf(oldest, newest - (YEAR_CHOICES - 1))
        val years = (newest downTo shownOldest).map { YearFilter.In(it) }
        val all = if (oldest < shownOldest) years + YearFilter.Before(shownOldest) else years
        if (all.size > 1) all else emptyList()
    }
    return if (selected == null || selected in choices) choices else choices + selected
}

private const val YEAR_CHOICES = 3

private fun <T> List<FydeFacetCount<T>>.withSelected(selected: T?): List<FydeFacetCount<T>> =
    if (selected == null || any { it.value == selected }) this else this + FydeFacetCount(selected, 0)
