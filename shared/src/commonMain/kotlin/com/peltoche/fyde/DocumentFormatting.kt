package com.peltoche.fyde

import androidx.compose.runtime.Composable
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.category_housing
import com.peltoche.fyde.resources.category_telecom
import com.peltoche.fyde.resources.category_money
import com.peltoche.fyde.resources.category_taxes
import com.peltoche.fyde.resources.category_health
import com.peltoche.fyde.resources.category_work
import com.peltoche.fyde.resources.category_family
import com.peltoche.fyde.resources.category_transport
import com.peltoche.fyde.resources.category_shopping
import com.peltoche.fyde.resources.category_subscriptions
import com.peltoche.fyde.resources.category_donations
import com.peltoche.fyde.resources.subcategory_energy
import com.peltoche.fyde.resources.subcategory_water
import com.peltoche.fyde.resources.subcategory_internet
import com.peltoche.fyde.resources.subcategory_home_insurance
import com.peltoche.fyde.resources.subcategory_home_services
import com.peltoche.fyde.resources.subcategory_mobile
import com.peltoche.fyde.resources.subcategory_banking
import com.peltoche.fyde.resources.subcategory_savings
import com.peltoche.fyde.resources.subcategory_gaming
import com.peltoche.fyde.resources.subcategory_income_tax
import com.peltoche.fyde.resources.subcategory_tax_certificates
import com.peltoche.fyde.resources.subcategory_health_insurance
import com.peltoche.fyde.resources.subcategory_complementary_health
import com.peltoche.fyde.resources.subcategory_care
import com.peltoche.fyde.resources.subcategory_payslips
import com.peltoche.fyde.resources.subcategory_employment
import com.peltoche.fyde.resources.subcategory_household_employer
import com.peltoche.fyde.resources.subcategory_benefits
import com.peltoche.fyde.resources.subcategory_childcare_school
import com.peltoche.fyde.resources.subcategory_train
import com.peltoche.fyde.resources.subcategory_flight
import com.peltoche.fyde.resources.subcategory_road
import com.peltoche.fyde.resources.subcategory_ride
import com.peltoche.fyde.resources.subcategory_travel
import com.peltoche.fyde.resources.subcategory_online
import com.peltoche.fyde.resources.subcategory_pets
import com.peltoche.fyde.resources.subcategory_groceries
import com.peltoche.fyde.resources.subcategory_food_delivery
import com.peltoche.fyde.resources.subcategory_media
import com.peltoche.fyde.resources.subcategory_cloud_software
import com.peltoche.fyde.resources.date_format
import com.peltoche.fyde.resources.date_format_short
import com.peltoche.fyde.resources.decimal_separator
import com.peltoche.fyde.resources.document_type_invoice
import com.peltoche.fyde.resources.document_type_receipt
import com.peltoche.fyde.resources.document_type_statement
import com.peltoche.fyde.resources.document_type_reimbursement_statement
import com.peltoche.fyde.resources.document_type_payslip
import com.peltoche.fyde.resources.document_type_contract
import com.peltoche.fyde.resources.document_type_insurance_certificate
import com.peltoche.fyde.resources.document_type_insurance_card
import com.peltoche.fyde.resources.document_type_rights_certificate
import com.peltoche.fyde.resources.document_type_proof_of_residence
import com.peltoche.fyde.resources.document_type_rib
import com.peltoche.fyde.resources.document_type_tax_notice
import com.peltoche.fyde.resources.document_type_tax_return
import com.peltoche.fyde.resources.document_type_tax_certificate
import com.peltoche.fyde.resources.document_type_employment_certificate
import com.peltoche.fyde.resources.document_type_fee_summary
import com.peltoche.fyde.resources.document_type_booking
import com.peltoche.fyde.resources.document_type_medical_document
import com.peltoche.fyde.resources.document_type_other
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
import com.peltoche.fyde.resources.month_heading_1
import com.peltoche.fyde.resources.month_heading_2
import com.peltoche.fyde.resources.month_heading_3
import com.peltoche.fyde.resources.month_heading_4
import com.peltoche.fyde.resources.month_heading_5
import com.peltoche.fyde.resources.month_heading_6
import com.peltoche.fyde.resources.month_heading_7
import com.peltoche.fyde.resources.month_heading_8
import com.peltoche.fyde.resources.month_heading_9
import com.peltoche.fyde.resources.month_heading_10
import com.peltoche.fyde.resources.month_heading_11
import com.peltoche.fyde.resources.month_heading_12
import com.peltoche.fyde.resources.month_heading_year
import com.peltoche.fyde.resources.size_bytes
import com.peltoche.fyde.resources.size_gigabytes
import com.peltoche.fyde.resources.size_kilobytes
import com.peltoche.fyde.resources.size_megabytes
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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
internal fun localizedDate(epochSeconds: Long): String = localizedDate(civilDate(epochSeconds))

@Composable
internal fun localizedDate(date: CivilDate): String =
    stringResource(Res.string.date_format, date.day, stringResource(MONTHS[date.month - 1]), date.year)

/** [localizedDate] without the year when it's this year's: "Oct 12" / "12 oct.", as the vault's lists show dates. */
@Composable
internal fun localizedShortDate(epochSeconds: Long): String = localizedShortDate(civilDate(epochSeconds))

@Composable
internal fun localizedShortDate(date: CivilDate): String {
    if (date.year != currentYear()) return localizedDate(date)
    return stringResource(Res.string.date_format_short, date.day, stringResource(MONTHS[date.month - 1]))
}

/** A short month name ("oct."), as [localizedShortDate] writes it. */
@Composable
internal fun localizedShortMonth(month: Int): String = stringResource(MONTHS[month - 1])

private val MONTH_HEADINGS = listOf(
    Res.string.month_heading_1, Res.string.month_heading_2, Res.string.month_heading_3, Res.string.month_heading_4,
    Res.string.month_heading_5, Res.string.month_heading_6, Res.string.month_heading_7, Res.string.month_heading_8,
    Res.string.month_heading_9, Res.string.month_heading_10, Res.string.month_heading_11, Res.string.month_heading_12,
)

/** A month heading a list's documents ("October"), with its year unless it's this year ("October 2025"). */
@Composable
internal fun localizedMonthHeading(year: Long, month: Int): String {
    val name = stringResource(MONTH_HEADINGS[month - 1])
    return if (year == currentYear()) name else stringResource(Res.string.month_heading_year, name, year)
}

/** This year, in the UTC calendar [civilDate] uses. */
@OptIn(ExperimentalTime::class)
internal fun currentYear(): Long = civilDate(Clock.System.now().epochSeconds).year

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
internal fun FydeCategory.label(): String = stringResource(
    when (this) {
        FydeCategory.HOUSING -> Res.string.category_housing
        FydeCategory.TELECOM -> Res.string.category_telecom
        FydeCategory.MONEY -> Res.string.category_money
        FydeCategory.TAXES -> Res.string.category_taxes
        FydeCategory.HEALTH -> Res.string.category_health
        FydeCategory.WORK -> Res.string.category_work
        FydeCategory.FAMILY -> Res.string.category_family
        FydeCategory.TRANSPORT -> Res.string.category_transport
        FydeCategory.SHOPPING -> Res.string.category_shopping
        FydeCategory.SUBSCRIPTIONS -> Res.string.category_subscriptions
        FydeCategory.DONATIONS -> Res.string.category_donations
    },
)

@Composable
internal fun FydeSubcategory.label(): String = stringResource(
    when (this) {
        FydeSubcategory.ENERGY -> Res.string.subcategory_energy
        FydeSubcategory.WATER -> Res.string.subcategory_water
        FydeSubcategory.INTERNET -> Res.string.subcategory_internet
        FydeSubcategory.HOME_INSURANCE -> Res.string.subcategory_home_insurance
        FydeSubcategory.HOME_SERVICES -> Res.string.subcategory_home_services
        FydeSubcategory.MOBILE -> Res.string.subcategory_mobile
        FydeSubcategory.BANKING -> Res.string.subcategory_banking
        FydeSubcategory.SAVINGS -> Res.string.subcategory_savings
        FydeSubcategory.GAMING -> Res.string.subcategory_gaming
        FydeSubcategory.INCOME_TAX -> Res.string.subcategory_income_tax
        FydeSubcategory.TAX_CERTIFICATES -> Res.string.subcategory_tax_certificates
        FydeSubcategory.HEALTH_INSURANCE -> Res.string.subcategory_health_insurance
        FydeSubcategory.COMPLEMENTARY_HEALTH -> Res.string.subcategory_complementary_health
        FydeSubcategory.CARE -> Res.string.subcategory_care
        FydeSubcategory.PAYSLIPS -> Res.string.subcategory_payslips
        FydeSubcategory.EMPLOYMENT -> Res.string.subcategory_employment
        FydeSubcategory.HOUSEHOLD_EMPLOYER -> Res.string.subcategory_household_employer
        FydeSubcategory.BENEFITS -> Res.string.subcategory_benefits
        FydeSubcategory.CHILDCARE_SCHOOL -> Res.string.subcategory_childcare_school
        FydeSubcategory.TRAIN -> Res.string.subcategory_train
        FydeSubcategory.FLIGHT -> Res.string.subcategory_flight
        FydeSubcategory.ROAD -> Res.string.subcategory_road
        FydeSubcategory.RIDE -> Res.string.subcategory_ride
        FydeSubcategory.TRAVEL -> Res.string.subcategory_travel
        FydeSubcategory.ONLINE -> Res.string.subcategory_online
        FydeSubcategory.PETS -> Res.string.subcategory_pets
        FydeSubcategory.GROCERIES -> Res.string.subcategory_groceries
        FydeSubcategory.FOOD_DELIVERY -> Res.string.subcategory_food_delivery
        FydeSubcategory.MEDIA -> Res.string.subcategory_media
        FydeSubcategory.CLOUD_SOFTWARE -> Res.string.subcategory_cloud_software
    },
)

@Composable
internal fun FydeDocumentType.label(): String = stringResource(
    when (this) {
        FydeDocumentType.INVOICE -> Res.string.document_type_invoice
        FydeDocumentType.RECEIPT -> Res.string.document_type_receipt
        FydeDocumentType.STATEMENT -> Res.string.document_type_statement
        FydeDocumentType.REIMBURSEMENT_STATEMENT -> Res.string.document_type_reimbursement_statement
        FydeDocumentType.PAYSLIP -> Res.string.document_type_payslip
        FydeDocumentType.CONTRACT -> Res.string.document_type_contract
        FydeDocumentType.INSURANCE_CERTIFICATE -> Res.string.document_type_insurance_certificate
        FydeDocumentType.INSURANCE_CARD -> Res.string.document_type_insurance_card
        FydeDocumentType.RIGHTS_CERTIFICATE -> Res.string.document_type_rights_certificate
        FydeDocumentType.PROOF_OF_RESIDENCE -> Res.string.document_type_proof_of_residence
        FydeDocumentType.RIB -> Res.string.document_type_rib
        FydeDocumentType.TAX_NOTICE -> Res.string.document_type_tax_notice
        FydeDocumentType.TAX_RETURN -> Res.string.document_type_tax_return
        FydeDocumentType.TAX_CERTIFICATE -> Res.string.document_type_tax_certificate
        FydeDocumentType.EMPLOYMENT_CERTIFICATE -> Res.string.document_type_employment_certificate
        FydeDocumentType.FEE_SUMMARY -> Res.string.document_type_fee_summary
        FydeDocumentType.BOOKING -> Res.string.document_type_booking
        FydeDocumentType.MEDICAL_DOCUMENT -> Res.string.document_type_medical_document
        FydeDocumentType.OTHER -> Res.string.document_type_other
    },
)

/**
 * What kind of file a document is, derived from its content type: what
 * decides how (or whether) it can be previewed in the app, and its type
 * code's fallback.
 */
enum class DocumentKind { PDF, IMAGE, SPREADSHEET, TEXT, OTHER }

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
 * The file name offered when downloading a document: its name, plus the
 * extension of its [FydeDocument.contentType] when the name has none.
 * Found via testing: an uploaded `invoice.pdf` is listed as "invoice", and
 * the file it downloaded to had no extension, so nothing opened it.
 */
internal val FydeDocument.downloadFileName: String
    get() {
        if (name.substringAfterLast('.', "").isNotEmpty()) return name
        val extension = when (contentType.lowercase()) {
            "application/pdf" -> "pdf"
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            else -> return name
        }
        return "$name.$extension"
    }
