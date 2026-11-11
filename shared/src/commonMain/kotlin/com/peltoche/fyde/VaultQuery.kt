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
 * What a vault page asks `fyde-sdk` for: a folder page's folder and year
 * (V3, D1), or the search page's text and facet chips (V4, V5). Held by
 * `App.kt`'s vault container, which runs it through
 * [FydeSdk.searchDocuments]/[FydeSdk.searchDocumentFacets]; the screens only
 * edit it. Each filter is a single choice, `null` for any.
 */
data class VaultQuery(
    val text: String = "",
    /** A folder and everything below it, or À classer. */
    val folder: FydeFolderFilter? = null,
    val type: FydeDocumentType? = null,
    val year: YearFilter? = null,
) {
    val filtersActive: Boolean
        get() = folder != null || type != null || year != null

    /** The typed words, for highlighting them in the results. */
    val terms: List<String>
        get() = text.split(Regex("\\s+")).filter { it.isNotEmpty() }

    /**
     * Whether the search page lists results (V5) rather than its shortcuts
     * (V4): once there's text, a type or a year. A folder alone is the
     * scope a folder page's search opened with, so still V4.
     */
    val showsResults: Boolean
        get() = text.isNotBlank() || type != null || year != null

    /** This query's filters in [FydeSdk.searchDocuments]' terms. */
    fun toSearchFilters(): FydeSearchFilters {
        val (createdFrom, createdUntil) = year?.bounds() ?: (null to null)
        return FydeSearchFilters(
            folder = folder,
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
 * [folders] has À classer first when it holds matches, then each folder
 * (its subfolders' matches counted) in the SDK's order.
 */
data class VaultFilterOptions(
    val folders: List<FydeFacetCount<FydeFolderFilter>> = emptyList(),
    val types: List<FydeFacetCount<FydeDocumentType>> = emptyList(),
    val years: List<YearFilter> = emptyList(),
)

internal fun vaultFilterOptions(facets: FydeSearchFacets, query: VaultQuery): VaultFilterOptions = VaultFilterOptions(
    folders = folderChoices(facets).withSelected(query.folder),
    types = facets.types.withSelected(query.type),
    years = yearChoices(facets.earliestCreatedAt, facets.latestCreatedAt, query.year),
)

private fun folderChoices(facets: FydeSearchFacets): List<FydeFacetCount<FydeFolderFilter>> {
    val unfiled = if (facets.unfiled > 0) listOf(FydeFacetCount<FydeFolderFilter>(FydeFolderFilter.Unfiled, facets.unfiled)) else emptyList()
    return unfiled + facets.folders.map { FydeFacetCount(FydeFolderFilter.Within(it.value), it.count) }
}

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

/** One month's documents in a vault list, under its heading. */
data class MonthGroup(val year: Long, val month: Int, val documents: List<FydeDocument>)

/**
 * [documents] cut into runs of the same calendar month, in the order the SDK
 * returned them (newest or oldest first), for a list's month headings.
 */
internal fun monthGroups(documents: List<FydeDocument>): List<MonthGroup> = buildList {
    for (document in documents) {
        val date = civilDate(document.createdAt)
        val last = lastOrNull()
        if (last != null && last.year == date.year && last.month == date.month) {
            set(lastIndex, last.copy(documents = last.documents + document))
        } else {
            add(MonthGroup(date.year, date.month, listOf(document)))
        }
    }
}
