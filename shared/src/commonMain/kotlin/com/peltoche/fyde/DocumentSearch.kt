package com.peltoche.fyde

import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.filter_period_30_days
import com.peltoche.fyde.resources.filter_period_any
import com.peltoche.fyde.resources.filter_period_last_year
import com.peltoche.fyde.resources.filter_period_this_year
import org.jetbrains.compose.resources.StringResource

/** The Period filter's choices, each a range of document creation dates. */
enum class PeriodFilter(val label: StringResource) {
    ANY(Res.string.filter_period_any),
    LAST_30_DAYS(Res.string.filter_period_30_days),
    THIS_YEAR(Res.string.filter_period_this_year),
    LAST_YEAR(Res.string.filter_period_last_year),
    ;

    /**
     * The inclusive `createdAt` range (epoch seconds, either end `null` when
     * open) this choice covers as of [now], calendar years in UTC like
     * [localizedDate] shows them.
     */
    internal fun bounds(now: Long): Pair<Long?, Long?> {
        val year = civilDate(now).year
        fun startOf(year: Long) = epochSeconds(CivilDate(year, 1, 1))
        return when (this) {
            ANY -> null to null
            LAST_30_DAYS -> now - 30L * 86_400L to null
            THIS_YEAR -> startOf(year) to startOf(year + 1) - 1
            LAST_YEAR -> startOf(year - 1) to startOf(year) - 1
        }
    }
}

/**
 * What the Documents screen's search field and filter chips currently ask
 * for. Hoisted out of [DocumentListScreen] to `DocumentsScreen` in `App.kt`,
 * which runs it through [FydeSdk.searchDocuments]/[FydeSdk.searchDocumentFacets]
 * — the screen only edits it. Each filter is a single choice (`null`/
 * [PeriodFilter.ANY] for any), the way its chip's menu offers them.
 */
data class DocumentSearch(
    val text: String = "",
    val kind: DocumentKind? = null,
    val period: PeriodFilter = PeriodFilter.ANY,
    val sourceCategory: FydeSourceCategory? = null,
    val sourceSubCategory: FydeSourceSubCategory? = null,
    val purpose: FydePurpose? = null,
) {
    val filtersActive: Boolean
        get() = kind != null || period != PeriodFilter.ANY || sourceCategory != null ||
            sourceSubCategory != null || purpose != null

    /**
     * Picks [category] as the source. Its sub-category is dropped along with
     * it: sub-categories are offered per source (see [FydeSearchFacets]), so
     * the old one may not exist under the new source at all.
     */
    fun withSourceCategory(category: FydeSourceCategory?): DocumentSearch =
        if (category == sourceCategory) this else copy(sourceCategory = category, sourceSubCategory = null)
}

/**
 * This search's filters in [FydeSdk.searchDocuments]' terms, as of [now],
 * except [DocumentSearch.kind]: the SDK filters on exact content types, while
 * a [DocumentKind] groups many ("image/png", "image/jpeg"...), so the caller
 * resolves it with [contentTypesOf] against the SDK's own content-type facet
 * and passes the result as [contentTypes].
 */
internal fun DocumentSearch.toSearchFilters(now: Long, contentTypes: List<String> = emptyList()): FydeSearchFilters {
    val (createdFrom, createdUntil) = period.bounds(now)
    return FydeSearchFilters(
        sourceCategories = listOfNotNull(sourceCategory),
        sourceSubCategories = listOfNotNull(sourceSubCategory),
        purposes = listOfNotNull(purpose),
        contentTypes = contentTypes,
        createdFrom = createdFrom,
        createdUntil = createdUntil,
    )
}

/** The content types among [facets]' that are documents of [kind]. */
internal fun contentTypesOf(kind: DocumentKind, facets: FydeSearchFacets): List<String> =
    facets.contentTypes.map { it.value }.filter { documentKindOf(it) == kind }

/**
 * Every filter choice [facets] (computed by `fyde-sdk`, see
 * [FydeSearchFacets]) says still leads to documents, with how many, for the
 * chips to offer — plus the currently selected one even if it doesn't, so a
 * chip never hides the choice it's showing.
 */
data class DocumentFilterOptions(
    val kinds: List<FydeFacetCount<DocumentKind>> = emptyList(),
    val periods: List<PeriodFilter> = listOf(PeriodFilter.ANY),
    val sourceCategories: List<FydeFacetCount<FydeSourceCategory>> = emptyList(),
    val sourceSubCategories: List<FydeFacetCount<FydeSourceSubCategory>> = emptyList(),
    val purposes: List<FydeFacetCount<FydePurpose>> = emptyList(),
)

internal fun filterOptions(facets: FydeSearchFacets, search: DocumentSearch, now: Long): DocumentFilterOptions {
    val earliest = facets.earliestCreatedAt
    val latest = facets.latestCreatedAt
    return DocumentFilterOptions(
        kinds = facets.contentTypes
            .groupBy { documentKindOf(it.value) }
            .map { (kind, counts) -> FydeFacetCount(kind, counts.sumOf { it.count }) }
            .sortedByDescending { it.count }
            .withSelected(search.kind),
        // A period is worth offering if it overlaps the span of creation
        // dates the SDK found among the matches.
        periods = PeriodFilter.entries.filter { period ->
            val (from, until) = period.bounds(now)
            period == PeriodFilter.ANY || period == search.period || (
                earliest != null && latest != null &&
                    (from == null || latest >= from) && (until == null || earliest <= until)
                )
        },
        sourceCategories = facets.sourceCategories.withSelected(search.sourceCategory),
        sourceSubCategories = facets.sourceSubCategories.withSelected(search.sourceSubCategory),
        purposes = facets.purposes.withSelected(search.purpose),
    )
}

private fun <T> List<FydeFacetCount<T>>.withSelected(selected: T?): List<FydeFacetCount<T>> =
    if (selected == null || any { it.value == selected }) this else this + FydeFacetCount(selected, 0)
