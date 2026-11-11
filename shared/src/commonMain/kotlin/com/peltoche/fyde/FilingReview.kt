package com.peltoche.fyde

/**
 * The documents whose filing the user confirmed (the filing card's "C'est
 * bon", or a move of their own), kept as an app setting on this device:
 * comma-separated ids, oldest first, the most recent [FILING_REVIEWED_MAX]
 * only, so the setting never grows past a few kilobytes. Once one is
 * confirmed, its card keeps Modifier but no longer asks.
 */
internal const val FILING_REVIEWED_SETTING = "filing_reviewed"

internal const val FILING_REVIEWED_MAX = 500

internal fun decodeFilingReviewed(value: String?): Set<String> =
    value.orEmpty().split(',').filter { it.isNotBlank() }.toCollection(LinkedHashSet())

/** [reviewed] with [id] added as the newest, the oldest dropped past [FILING_REVIEWED_MAX]. */
internal fun withFilingReviewed(reviewed: Set<String>, id: String): Set<String> =
    (reviewed - id + id).toList().takeLast(FILING_REVIEWED_MAX).toCollection(LinkedHashSet())

internal fun encodeFilingReviewed(reviewed: Set<String>): String = reviewed.joinToString(",")
