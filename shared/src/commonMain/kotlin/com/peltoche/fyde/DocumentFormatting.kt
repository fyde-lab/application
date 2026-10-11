package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.date_format
import com.peltoche.fyde.resources.decimal_separator
import com.peltoche.fyde.resources.filter_type_image
import com.peltoche.fyde.resources.filter_type_other
import com.peltoche.fyde.resources.filter_type_pdf
import com.peltoche.fyde.resources.filter_type_spreadsheet
import com.peltoche.fyde.resources.filter_type_text
import com.peltoche.fyde.resources.month_1
import com.peltoche.fyde.resources.month_10
import com.peltoche.fyde.resources.month_11
import com.peltoche.fyde.resources.month_12
import com.peltoche.fyde.resources.month_2
import com.peltoche.fyde.resources.month_3
import com.peltoche.fyde.resources.month_4
import com.peltoche.fyde.resources.month_5
import com.peltoche.fyde.resources.month_6
import com.peltoche.fyde.resources.month_7
import com.peltoche.fyde.resources.month_8
import com.peltoche.fyde.resources.month_9
import com.peltoche.fyde.resources.purpose_attestation
import com.peltoche.fyde.resources.purpose_contract
import com.peltoche.fyde.resources.purpose_description
import com.peltoche.fyde.resources.purpose_employment
import com.peltoche.fyde.resources.purpose_evaluation
import com.peltoche.fyde.resources.purpose_invoice
import com.peltoche.fyde.resources.purpose_report
import com.peltoche.fyde.resources.size_bytes
import com.peltoche.fyde.resources.size_gigabytes
import com.peltoche.fyde.resources.size_kilobytes
import com.peltoche.fyde.resources.size_megabytes
import com.peltoche.fyde.resources.source_category_alimentation
import com.peltoche.fyde.resources.source_category_association
import com.peltoche.fyde.resources.source_category_bank
import com.peltoche.fyde.resources.source_category_building
import com.peltoche.fyde.resources.source_category_education
import com.peltoche.fyde.resources.source_category_employer
import com.peltoche.fyde.resources.source_category_energy
import com.peltoche.fyde.resources.source_category_goods
import com.peltoche.fyde.resources.source_category_gov
import com.peltoche.fyde.resources.source_category_health
import com.peltoche.fyde.resources.source_category_individual
import com.peltoche.fyde.resources.source_category_insurance
import com.peltoche.fyde.resources.source_category_real_estate
import com.peltoche.fyde.resources.source_category_retail
import com.peltoche.fyde.resources.source_category_shopping
import com.peltoche.fyde.resources.source_category_telecom
import com.peltoche.fyde.resources.source_category_transport
import com.peltoche.fyde.resources.source_category_water
import com.peltoche.fyde.resources.source_category_web
import com.peltoche.fyde.resources.source_subcategory_citizen
import com.peltoche.fyde.resources.source_subcategory_civil_registration
import com.peltoche.fyde.resources.source_subcategory_family
import com.peltoche.fyde.resources.source_subcategory_health
import com.peltoche.fyde.resources.source_subcategory_immigration
import com.peltoche.fyde.resources.source_subcategory_internet
import com.peltoche.fyde.resources.source_subcategory_mobile
import com.peltoche.fyde.resources.source_subcategory_real_estate
import com.peltoche.fyde.resources.source_subcategory_sport
import com.peltoche.fyde.resources.source_subcategory_tax
import com.peltoche.fyde.resources.source_subcategory_transport
import com.peltoche.fyde.theme.AppColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Everything here turns a document field into text in the UI's own language.
// Found via review: dates were a fixed YYYY-MM-DD, sizes were truncated
// integers with English-only units ("1 MB" for 1.9 MB, never "Mo"), and
// classification labels were enum names title-cased in English ("Real
// Estate") — all regardless of the app being bilingual.

/** A Gregorian calendar date, as [civilDate] computes it. */
internal data class CivilDate(val year: Long, val month: Int, val day: Int)

/**
 * Converts a Unix epoch-seconds timestamp to its UTC calendar date via
 * Howard Hinnant's `civil_from_days` proleptic-Gregorian algorithm (pure
 * integer math) rather than a platform clock/date API, so it behaves
 * identically on every Kotlin Multiplatform target without an extra
 * dependency.
 */
internal fun civilDate(epochSeconds: Long): CivilDate {
    val days = floorDiv(epochSeconds, 86_400L)
    val z = days + 719_468L
    val era = floorDiv(if (z >= 0) z else z - 146_096L, 146_097L)
    val doe = z - era * 146_097L
    val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
    val year = yoe + era * 400L
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
    return CivilDate(if (month <= 2) year + 1 else year, month, day)
}

/**
 * The inverse of [civilDate]: the Unix epoch-seconds timestamp of [date]'s
 * first second (00:00:00 UTC), via Hinnant's `days_from_civil`.
 */
internal fun epochSeconds(date: CivilDate): Long {
    val year = if (date.month <= 2) date.year - 1 else date.year
    val era = floorDiv(year, 400L)
    val yoe = year - era * 400L
    val mp = if (date.month > 2) date.month - 3 else date.month + 9
    val doy = (153L * mp + 2) / 5 + date.day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return (era * 146_097L + doe - 719_468L) * 86_400L
}

private fun floorDiv(x: Long, y: Long): Long {
    val q = x / y
    return if (x % y != 0L && (x < 0) != (y < 0)) q - 1 else q
}

private val MONTHS = listOf(
    Res.string.month_1, Res.string.month_2, Res.string.month_3, Res.string.month_4,
    Res.string.month_5, Res.string.month_6, Res.string.month_7, Res.string.month_8,
    Res.string.month_9, Res.string.month_10, Res.string.month_11, Res.string.month_12,
)

/** [epochSeconds] as a short date in the UI language: "Oct 12, 2026" / "12 oct. 2026". */
@Composable
internal fun localizedDate(epochSeconds: Long): String {
    val date = civilDate(epochSeconds)
    return stringResource(Res.string.date_format, date.day, stringResource(MONTHS[date.month - 1]), date.year)
}

/**
 * [bytes] in the largest unit it reaches, with one decimal below 10 of that
 * unit ("1.9 MB", "12 MB", "532 KB") — whole numbers alone hid most of a
 * unit, so a 1.9 MB file read as "1 MB".
 */
@Composable
internal fun localizedSize(bytes: Long): String {
    val separator = stringResource(Res.string.decimal_separator)
    val (value, unit) = sizeInUnit(bytes)
    val number = if (unit == SizeUnit.BYTES) value.toLong().toString() else formatOneDecimalBelowTen(value, separator)
    return stringResource(unit.resource, number)
}

internal enum class SizeUnit(val resource: StringResource) {
    BYTES(Res.string.size_bytes),
    KILOBYTES(Res.string.size_kilobytes),
    MEGABYTES(Res.string.size_megabytes),
    GIGABYTES(Res.string.size_gigabytes),
}

internal fun sizeInUnit(bytes: Long): Pair<Double, SizeUnit> {
    var value = bytes.toDouble()
    var unit = SizeUnit.BYTES
    while (value >= 1024 && unit != SizeUnit.GIGABYTES) {
        value /= 1024
        unit = SizeUnit.entries[unit.ordinal + 1]
    }
    return value to unit
}

/** "1.9" below 10 (one decimal, truncated rather than rounded up past what's there), "12" from 10 up. */
internal fun formatOneDecimalBelowTen(value: Double, decimalSeparator: String): String {
    if (value >= 10) return value.toLong().toString()
    val tenths = (value * 10).toLong()
    return "${tenths / 10}$decimalSeparator${tenths % 10}"
}

@Composable
internal fun FydeSourceCategory.label(): String = stringResource(
    when (this) {
        FydeSourceCategory.BANK -> Res.string.source_category_bank
        FydeSourceCategory.INSURANCE -> Res.string.source_category_insurance
        FydeSourceCategory.RETAIL -> Res.string.source_category_retail
        FydeSourceCategory.TELECOM -> Res.string.source_category_telecom
        FydeSourceCategory.ENERGY -> Res.string.source_category_energy
        FydeSourceCategory.WATER -> Res.string.source_category_water
        FydeSourceCategory.HEALTH -> Res.string.source_category_health
        FydeSourceCategory.GOV -> Res.string.source_category_gov
        FydeSourceCategory.ASSOCIATION -> Res.string.source_category_association
        FydeSourceCategory.EDUCATION -> Res.string.source_category_education
        FydeSourceCategory.EMPLOYER -> Res.string.source_category_employer
        FydeSourceCategory.TRANSPORT -> Res.string.source_category_transport
        FydeSourceCategory.GOODS -> Res.string.source_category_goods
        FydeSourceCategory.ALIMENTATION -> Res.string.source_category_alimentation
        FydeSourceCategory.BUILDING -> Res.string.source_category_building
        FydeSourceCategory.REAL_ESTATE -> Res.string.source_category_real_estate
        FydeSourceCategory.WEB -> Res.string.source_category_web
        FydeSourceCategory.INDIVIDUAL -> Res.string.source_category_individual
        FydeSourceCategory.SHOPPING -> Res.string.source_category_shopping
    },
)

@Composable
internal fun FydeSourceSubCategory.label(): String = stringResource(
    when (this) {
        FydeSourceSubCategory.CIVIL_REGISTRATION -> Res.string.source_subcategory_civil_registration
        FydeSourceSubCategory.IMMIGRATION -> Res.string.source_subcategory_immigration
        FydeSourceSubCategory.TRANSPORT -> Res.string.source_subcategory_transport
        FydeSourceSubCategory.FAMILY -> Res.string.source_subcategory_family
        FydeSourceSubCategory.TAX -> Res.string.source_subcategory_tax
        FydeSourceSubCategory.HEALTH -> Res.string.source_subcategory_health
        FydeSourceSubCategory.REAL_ESTATE -> Res.string.source_subcategory_real_estate
        FydeSourceSubCategory.MOBILE -> Res.string.source_subcategory_mobile
        FydeSourceSubCategory.INTERNET -> Res.string.source_subcategory_internet
        FydeSourceSubCategory.CITIZEN -> Res.string.source_subcategory_citizen
        FydeSourceSubCategory.SPORT -> Res.string.source_subcategory_sport
    },
)

@Composable
internal fun FydePurpose.label(): String = stringResource(
    when (this) {
        FydePurpose.ATTESTATION -> Res.string.purpose_attestation
        FydePurpose.CONTRACT -> Res.string.purpose_contract
        FydePurpose.INVOICE -> Res.string.purpose_invoice
        FydePurpose.REPORT -> Res.string.purpose_report
        FydePurpose.DESCRIPTION -> Res.string.purpose_description
        FydePurpose.EVALUATION -> Res.string.purpose_evaluation
        FydePurpose.EMPLOYMENT -> Res.string.purpose_employment
    },
)

/**
 * What kind of file a document is, derived from its content type: what the
 * Type filter groups by, what picks [FileTypeIcon]'s tint, and what decides
 * how (or whether) it can be previewed in the app.
 */
enum class DocumentKind(val label: StringResource) {
    PDF(Res.string.filter_type_pdf),
    IMAGE(Res.string.filter_type_image),
    SPREADSHEET(Res.string.filter_type_spreadsheet),
    TEXT(Res.string.filter_type_text),
    OTHER(Res.string.filter_type_other),
}

internal val FydeDocument.kind: DocumentKind
    get() = documentKindOf(contentType)

/** The [DocumentKind] a document of [contentType] is. */
internal fun documentKindOf(contentType: String): DocumentKind {
    val type = contentType.lowercase()
    return when {
        type.contains("pdf") -> DocumentKind.PDF
        type.startsWith("image/") -> DocumentKind.IMAGE
        type.contains("spreadsheet") || type.contains("excel") || type.contains("csv") -> DocumentKind.SPREADSHEET
        type.startsWith("text/") || type.contains("word") || type.contains("document") -> DocumentKind.TEXT
        else -> DocumentKind.OTHER
    }
}

/** The short type code shown for a document ("PDF", "PNG", "XLSX"): its file extension, or a fallback per kind. */
internal val FydeDocument.typeCode: String
    get() = name.substringAfterLast('.', "").uppercase().take(4).ifEmpty {
        when (kind) {
            DocumentKind.PDF -> "PDF"
            DocumentKind.IMAGE -> "IMG"
            DocumentKind.SPREADSHEET -> "XLS"
            DocumentKind.TEXT -> "DOC"
            DocumentKind.OTHER -> "FILE"
        }
    }

/**
 * [FileTypeIcon]'s accent per [DocumentKind], drawn only from the app's two
 * brand accents — `primary` and `secondary`, the latter already scoped by
 * design-system.md's Color table to "folder/document-type accents" — plus
 * two blends between them, so the kinds stay visually distinct without
 * reaching for a semantic-state color (`success`/`warning`/`error` mean
 * "this succeeded/needs caution/failed" everywhere else) or inventing a
 * fifth hue the "one disciplined accent" rule doesn't allow for.
 */
@Composable
internal fun DocumentKind.tint(): Color = when (this) {
    DocumentKind.PDF -> AppColors.primary
    DocumentKind.SPREADSHEET -> AppColors.secondary
    DocumentKind.IMAGE -> lerp(AppColors.primary, AppColors.secondary, 0.5f)
    DocumentKind.TEXT -> lerp(AppColors.secondary, AppColors.text, 0.3f)
    DocumentKind.OTHER -> AppColors.textMuted
}
