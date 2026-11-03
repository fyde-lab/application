package com.peltoche.fyde

// How a document's attributes fill its pass and its Informations section
// (design-system.md § Document pass, § Informations). Everything here is
// pure, so the layout rules are tested without Compose; the wording lives in
// DocumentAttributesText.kt.

/** The server's default attribute names, in the order Informations lists them. */
internal object AttributeNames {
    const val ISSUED_ON = "issued_on"
    const val ISSUER = "issuer"
    const val REFERENCE = "reference"
    const val PERIOD_START = "period_start"
    const val PERIOD_END = "period_end"
    const val TOTAL_AMOUNT = "total_amount"
    const val DUE_ON = "due_on"
    const val GROSS_PAY = "gross_pay"
    const val NET_PAY = "net_pay"
    const val TAX_YEAR = "tax_year"
    const val TAX_AMOUNT = "tax_amount"

    val DEFAULTS = listOf(
        ISSUED_ON, ISSUER, REFERENCE, PERIOD_START, PERIOD_END, TOTAL_AMOUNT, DUE_ON, GROSS_PAY, NET_PAY, TAX_YEAR, TAX_AMOUNT,
    )
}

internal fun FydeDocument.text(name: String): String? =
    (attributes[name] as? FydeAttribute.Text)?.value?.takeIf { it.isNotBlank() }

internal fun FydeDocument.amount(name: String): FydeAttribute.Amount? = attributes[name] as? FydeAttribute.Amount

internal fun FydeDocument.date(name: String): CivilDate? = (attributes[name] as? FydeAttribute.Date)?.let { parseIsoDate(it.value) }

internal fun FydeDocument.integer(name: String): Long? = (attributes[name] as? FydeAttribute.Integer)?.value

/** The document's issuer ("EDF"), shown atop its pass and in its rows. */
internal val FydeDocument.issuer: String?
    get() = text(AttributeNames.ISSUER)

/** A `YYYY-MM-DD` string as a [CivilDate], or null if it isn't a real calendar day. */
internal fun parseIsoDate(value: String): CivilDate? {
    val match = Regex("""^(\d{4})-(\d{2})-(\d{2})$""").matchEntire(value) ?: return null
    val (year, month, day) = match.destructured
    val date = CivilDate(year.toLong(), month.toInt(), day.toInt())
    if (date.month !in 1..12 || date.day !in 1..lastDayOfMonth(date.year, date.month)) return null
    return date
}

internal fun lastDayOfMonth(year: Long, month: Int): Int = when (month) {
    2 -> if ((year % 4 == 0L && year % 100 != 0L) || year % 400 == 0L) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

/** The period a document covers, as `period_start`–`period_end` read together. */
internal sealed interface Period {
    /** A whole calendar month: "septembre 2026". */
    data class Month(val year: Long, val month: Int) : Period

    /** A whole calendar year: "2025". */
    data class Year(val year: Long) : Period

    /** From the first of [start]'s month to the last of [end]'s: "sept.–oct.". */
    data class Months(val start: CivilDate, val end: CivilDate) : Period

    /** Any other span: "12 sept.–11 oct.". */
    data class Days(val start: CivilDate, val end: CivilDate) : Period

    data class From(val start: CivilDate) : Period

    data class Until(val end: CivilDate) : Period
}

internal fun periodOf(start: CivilDate?, end: CivilDate?): Period? = when {
    start == null && end == null -> null
    start == null -> Period.Until(end!!)
    end == null -> Period.From(start)
    start.day != 1 || end.day != lastDayOfMonth(end.year, end.month) -> Period.Days(start, end)
    start.year == end.year && start.month == 1 && end.month == 12 -> Period.Year(start.year)
    start.year == end.year && start.month == end.month -> Period.Month(start.year, start.month)
    else -> Period.Months(start, end)
}

internal val FydeDocument.period: Period?
    get() = periodOf(date(AttributeNames.PERIOD_START), date(AttributeNames.PERIOD_END))

/** Which of design-system.md's pass templates a document gets. */
internal enum class PassTemplate { BILL, PAYSLIP, TAX_NOTICE, STATEMENT, CERTIFICATE, OTHER }

/** A meta cell under the pass's perforation. */
internal enum class PassCell { PERIOD, REFERENCE, DUE, GROSS_PAY, TAX_YEAR, ISSUED, FORMAT }

/** What a pass's line under the headline says. */
internal sealed interface PassLine {
    data class DueBy(val date: CivilDate) : PassLine
    data class IssuedOn(val date: CivilDate) : PassLine
    data class NetPay(val month: Period.Month?) : PassLine

    /** A tax notice's [refund] or amount due, with its [date] when known. */
    data class Tax(val refund: Boolean, val date: CivilDate?) : PassLine
    data class Covers(val period: Period) : PassLine
}

/**
 * A document's pass, decided from its type and attributes: its [template],
 * its [line], its [cells] (at most three, each one set), and which
 * attributes it shows (Informations lists every attribute regardless).
 */
internal data class PassLayout(
    val template: PassTemplate,
    val line: PassLine?,
    val cells: List<PassCell>,
    val shown: Set<String>,
)

/** The template [type] asks for, before checking its headline attribute is set. */
private fun templateFor(type: FydeDocumentType?): PassTemplate = when (type) {
    FydeDocumentType.INVOICE, FydeDocumentType.RECEIPT, FydeDocumentType.FEE_SUMMARY -> PassTemplate.BILL
    FydeDocumentType.PAYSLIP -> PassTemplate.PAYSLIP
    FydeDocumentType.TAX_NOTICE -> PassTemplate.TAX_NOTICE
    FydeDocumentType.STATEMENT, FydeDocumentType.REIMBURSEMENT_STATEMENT -> PassTemplate.STATEMENT
    FydeDocumentType.INSURANCE_CERTIFICATE, FydeDocumentType.INSURANCE_CARD, FydeDocumentType.RIGHTS_CERTIFICATE,
    FydeDocumentType.TAX_CERTIFICATE, FydeDocumentType.EMPLOYMENT_CERTIFICATE,
    -> PassTemplate.CERTIFICATE
    else -> PassTemplate.OTHER
}

/** The attribute a template's headline shows; the template applies only when it's set. */
private fun headlineAttribute(template: PassTemplate): String? = when (template) {
    PassTemplate.BILL, PassTemplate.STATEMENT -> AttributeNames.TOTAL_AMOUNT
    PassTemplate.PAYSLIP -> AttributeNames.NET_PAY
    PassTemplate.TAX_NOTICE -> AttributeNames.TAX_AMOUNT
    PassTemplate.CERTIFICATE -> AttributeNames.PERIOD_END
    PassTemplate.OTHER -> null
}

private fun FydeDocument.hasHeadline(template: PassTemplate): Boolean = when (template) {
    PassTemplate.CERTIFICATE -> date(AttributeNames.PERIOD_END) != null
    PassTemplate.OTHER -> true
    else -> amount(headlineAttribute(template)!!) != null
}

/**
 * Each template's cells in order of preference; the first three set are
 * shown. The date the document reached the vault is never one: the facts
 * already show it, and it says nothing about the document itself.
 */
private fun candidateCells(template: PassTemplate): List<PassCell> = when (template) {
    PassTemplate.BILL -> listOf(PassCell.PERIOD, PassCell.REFERENCE, PassCell.DUE, PassCell.TAX_YEAR)
    PassTemplate.PAYSLIP -> listOf(PassCell.GROSS_PAY, PassCell.REFERENCE, PassCell.TAX_YEAR, PassCell.DUE)
    PassTemplate.TAX_NOTICE -> listOf(PassCell.TAX_YEAR, PassCell.REFERENCE, PassCell.DUE)
    PassTemplate.STATEMENT -> listOf(PassCell.REFERENCE, PassCell.ISSUED, PassCell.DUE, PassCell.TAX_YEAR)
    PassTemplate.CERTIFICATE -> listOf(PassCell.REFERENCE, PassCell.ISSUED, PassCell.TAX_YEAR)
    PassTemplate.OTHER -> listOf(PassCell.REFERENCE, PassCell.DUE, PassCell.TAX_YEAR, PassCell.FORMAT)
}

private val PERIOD_ATTRIBUTES = setOf(AttributeNames.PERIOD_START, AttributeNames.PERIOD_END)

internal fun passLayout(document: FydeDocument): PassLayout {
    val template = templateFor(document.documentType).takeIf { document.hasHeadline(it) } ?: PassTemplate.OTHER
    val shown = mutableSetOf<String>()
    if (document.issuer != null) shown += AttributeNames.ISSUER
    headlineAttribute(template)?.let { shown += it }

    val period = document.period
    val line: PassLine? = when (template) {
        PassTemplate.BILL -> document.date(AttributeNames.DUE_ON)?.let { shown += AttributeNames.DUE_ON; PassLine.DueBy(it) }
            ?: document.date(AttributeNames.ISSUED_ON)?.let { shown += AttributeNames.ISSUED_ON; PassLine.IssuedOn(it) }
        PassTemplate.PAYSLIP -> {
            val month = period as? Period.Month
            if (month != null) shown += PERIOD_ATTRIBUTES
            PassLine.NetPay(month)
        }
        PassTemplate.TAX_NOTICE -> {
            val due = document.date(AttributeNames.DUE_ON)
            if (due != null) shown += AttributeNames.DUE_ON
            PassLine.Tax(refund = document.amount(AttributeNames.TAX_AMOUNT)!!.minorUnits < 0, date = due)
        }
        PassTemplate.STATEMENT -> period?.let { shown += PERIOD_ATTRIBUTES; PassLine.Covers(it) }
        PassTemplate.CERTIFICATE -> null
        PassTemplate.OTHER -> period?.let { shown += PERIOD_ATTRIBUTES; PassLine.Covers(it) }
            ?: document.date(AttributeNames.ISSUED_ON)?.let { shown += AttributeNames.ISSUED_ON; PassLine.IssuedOn(it) }
    }

    val cells = candidateCells(template).filter { cell ->
        when (cell) {
            PassCell.PERIOD -> period != null && PERIOD_ATTRIBUTES.none { it in shown }
            PassCell.REFERENCE -> document.text(AttributeNames.REFERENCE) != null
            PassCell.GROSS_PAY -> document.amount(AttributeNames.GROSS_PAY) != null
            PassCell.TAX_YEAR -> document.integer(AttributeNames.TAX_YEAR) != null
            PassCell.ISSUED -> document.date(AttributeNames.ISSUED_ON) != null && AttributeNames.ISSUED_ON !in shown
            PassCell.DUE -> document.date(AttributeNames.DUE_ON) != null && AttributeNames.DUE_ON !in shown
            PassCell.FORMAT -> true
        }
    }.take(3)
    cells.forEach { cell ->
        when (cell) {
            PassCell.PERIOD -> shown += PERIOD_ATTRIBUTES
            PassCell.REFERENCE -> shown += AttributeNames.REFERENCE
            PassCell.GROSS_PAY -> shown += AttributeNames.GROSS_PAY
            PassCell.TAX_YEAR -> shown += AttributeNames.TAX_YEAR
            PassCell.ISSUED -> shown += AttributeNames.ISSUED_ON
            PassCell.DUE -> shown += AttributeNames.DUE_ON
            PassCell.FORMAT -> Unit
        }
    }
    return PassLayout(template, line, cells, shown)
}

/** One row of the Informations section. */
internal sealed interface InformationRow {
    /** `period_start` and `period_end`, both set, read as one "Période" row. */
    data class Covers(val period: Period) : InformationRow

    data class Value(val name: String, val attribute: FydeAttribute) : InformationRow
}

/**
 * Every attribute of [document], in Informations' order: the default
 * attributes first, in [AttributeNames.DEFAULTS]' order, then the others by
 * name. The pass repeats some of them; Informations still lists them all.
 */
internal fun informationRows(document: FydeDocument): List<InformationRow> {
    val attributes = document.attributes
    val ordered = attributes.keys.sortedWith(
        compareBy<String> { AttributeNames.DEFAULTS.indexOf(it).let { index -> if (index < 0) Int.MAX_VALUE else index } }.thenBy { it },
    )
    val start = document.date(AttributeNames.PERIOD_START)
    val end = document.date(AttributeNames.PERIOD_END)
    val period = if (start != null && end != null) periodOf(start, end) else null
    return buildList {
        for (name in ordered) {
            if (period != null && name in PERIOD_ATTRIBUTES) {
                if (name == AttributeNames.PERIOD_START) add(InformationRow.Covers(period))
                continue
            }
            add(InformationRow.Value(name, attributes.getValue(name)))
        }
    }
}

/** An attribute without wording of its own, by name: `contract_number` → "Contract number". */
internal fun humanizedAttributeName(name: String): String =
    name.replace('_', ' ').trim().replaceFirstChar { it.uppercase() }

/** How many digits [currency]'s minor unit has (ISO 4217): 2 for most, 0 for JPY, 3 for KWD... */
internal fun currencyDigits(currency: String): Int = when (currency.uppercase()) {
    "BIF", "CLP", "DJF", "GNF", "ISK", "JPY", "KMF", "KRW", "PYG", "RWF", "UGX", "UYI", "VND", "VUV", "XAF", "XOF", "XPF" -> 0
    "BHD", "IQD", "JOD", "KWD", "LYD", "OMR", "TND" -> 3
    else -> 2
}

/** The sign written for [currency] ("€", "$"), or its code when it has no common one. */
internal fun currencySymbol(currency: String): String = when (currency.uppercase()) {
    "EUR" -> "€"
    "USD" -> "$"
    "GBP" -> "£"
    "JPY" -> "¥"
    "CHF" -> "CHF"
    else -> currency.uppercase()
}

/**
 * [minorUnits] as a number with [digits] decimals, grouped by thousands:
 * `191244, 2` → "1 912,44" with French separators. Exact: never through a
 * floating-point value, never rounded.
 */
internal fun formatMinorUnits(minorUnits: Long, digits: Int, decimalSeparator: String, groupingSeparator: String): String {
    val negative = minorUnits < 0
    val absolute = minorUnits.toString().removePrefix("-").padStart(digits + 1, '0')
    val whole = absolute.dropLast(digits)
    val fraction = absolute.takeLast(digits)
    val grouped = groupThousands(whole, groupingSeparator)
    val number = if (digits == 0) grouped else "$grouped$decimalSeparator$fraction"
    return if (negative) "-$number" else number
}

/** "1234567" → "1 234 567" with [separator] between each group of three digits. */
internal fun groupThousands(digits: String, separator: String): String {
    val head = digits.length % 3
    val groups = buildList {
        if (head > 0) add(digits.take(head))
        addAll(digits.drop(head).chunked(3))
    }
    return groups.joinToString(separator)
}

/** A float with [decimalSeparator], without an exponent or a trailing ".0". */
internal fun formatDecimal(value: Double, decimalSeparator: String, groupingSeparator: String): String {
    if (value.isNaN() || value.isInfinite()) return value.toString()
    if (value == kotlin.math.floor(value) && kotlin.math.abs(value) < 1e15) {
        val whole = value.toLong()
        val grouped = groupThousands(kotlin.math.abs(whole).toString(), groupingSeparator)
        return if (whole < 0) "-$grouped" else grouped
    }
    return value.toString().replace(".", decimalSeparator)
}
