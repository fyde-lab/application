package com.peltoche.fyde

import androidx.compose.runtime.Composable
import com.peltoche.fyde.components.content.DocumentAttributeRow
import com.peltoche.fyde.components.content.DocumentPassCell
import com.peltoche.fyde.components.content.DocumentPassHeadline
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.amount_format
import com.peltoche.fyde.resources.attribute_due_on
import com.peltoche.fyde.resources.attribute_gross_pay
import com.peltoche.fyde.resources.attribute_issued_on
import com.peltoche.fyde.resources.attribute_issued_on_f
import com.peltoche.fyde.resources.attribute_issuer
import com.peltoche.fyde.resources.attribute_net_pay
import com.peltoche.fyde.resources.attribute_no
import com.peltoche.fyde.resources.attribute_period
import com.peltoche.fyde.resources.attribute_period_end
import com.peltoche.fyde.resources.attribute_period_start
import com.peltoche.fyde.resources.attribute_reference
import com.peltoche.fyde.resources.attribute_tax_amount
import com.peltoche.fyde.resources.attribute_tax_year
import com.peltoche.fyde.resources.attribute_total_amount
import com.peltoche.fyde.resources.attribute_yes
import com.peltoche.fyde.resources.date_format_long
import com.peltoche.fyde.resources.date_format_long_no_year
import com.peltoche.fyde.resources.decimal_separator
import com.peltoche.fyde.resources.grouping_separator
import com.peltoche.fyde.resources.month_long_1
import com.peltoche.fyde.resources.month_long_10
import com.peltoche.fyde.resources.month_long_11
import com.peltoche.fyde.resources.month_long_12
import com.peltoche.fyde.resources.month_long_2
import com.peltoche.fyde.resources.month_long_3
import com.peltoche.fyde.resources.month_long_4
import com.peltoche.fyde.resources.month_long_5
import com.peltoche.fyde.resources.month_long_6
import com.peltoche.fyde.resources.month_long_7
import com.peltoche.fyde.resources.month_long_8
import com.peltoche.fyde.resources.month_long_9
import com.peltoche.fyde.resources.month_year_format
import com.peltoche.fyde.resources.period_from
import com.peltoche.fyde.resources.period_range
import com.peltoche.fyde.resources.period_until
import com.peltoche.fyde.resources.vault_pass_amount_due
import com.peltoche.fyde.resources.vault_pass_amount_due_by
import com.peltoche.fyde.resources.vault_pass_due_by
import com.peltoche.fyde.resources.vault_pass_format
import com.peltoche.fyde.resources.vault_pass_issued
import com.peltoche.fyde.resources.vault_pass_issued_f
import com.peltoche.fyde.resources.vault_pass_issued_on
import com.peltoche.fyde.resources.vault_pass_issued_on_f
import com.peltoche.fyde.resources.vault_pass_net_pay
import com.peltoche.fyde.resources.vault_pass_net_pay_month
import com.peltoche.fyde.resources.vault_pass_refund
import com.peltoche.fyde.resources.vault_pass_refund_amount
import com.peltoche.fyde.resources.vault_pass_refund_on
import com.peltoche.fyde.resources.vault_pass_valid_until
import org.jetbrains.compose.resources.stringResource

// The words for DocumentAttributes.kt's layout, in the UI's language.

private val LONG_MONTHS = listOf(
    Res.string.month_long_1, Res.string.month_long_2, Res.string.month_long_3, Res.string.month_long_4,
    Res.string.month_long_5, Res.string.month_long_6, Res.string.month_long_7, Res.string.month_long_8,
    Res.string.month_long_9, Res.string.month_long_10, Res.string.month_long_11, Res.string.month_long_12,
)

/** "10 octobre 2026" / "October 10, 2026"; without the year when [withYear] is false and it's this year's. */
@Composable
internal fun localizedLongDate(date: CivilDate, withYear: Boolean = true): String {
    val month = stringResource(LONG_MONTHS[date.month - 1])
    return if (!withYear && date.year == currentYear()) {
        stringResource(Res.string.date_format_long_no_year, date.day, month)
    } else {
        stringResource(Res.string.date_format_long, date.day, month, date.year)
    }
}

/** "septembre 2026", "2025", "sept.–oct.", "12 sept.–11 oct.", "depuis le 1 sept.". */
@Composable
internal fun localizedPeriod(period: Period): String = when (period) {
    is Period.Month -> stringResource(Res.string.month_year_format, stringResource(LONG_MONTHS[period.month - 1]), period.year)
    is Period.Year -> period.year.toString()
    is Period.Months -> {
        val sameYear = period.start.year == period.end.year
        val start = localizedShortMonth(period.start.month).let {
            if (sameYear) it else stringResource(Res.string.month_year_format, it, period.start.year)
        }
        val end = localizedShortMonth(period.end.month).let {
            if (sameYear && period.end.year == currentYear()) it else stringResource(Res.string.month_year_format, it, period.end.year)
        }
        stringResource(Res.string.period_range, start, end)
    }
    is Period.Days -> stringResource(Res.string.period_range, localizedShortDate(period.start), localizedShortDate(period.end))
    is Period.From -> stringResource(Res.string.period_from, localizedShortDate(period.start))
    is Period.Until -> stringResource(Res.string.period_until, localizedShortDate(period.end))
}

/** "84,20 €" / "€84.20": exact, in the currency's own minor unit. */
@Composable
internal fun localizedAmount(amount: FydeAttribute.Amount): String {
    val number = formatMinorUnits(
        amount.minorUnits,
        currencyDigits(amount.currency),
        stringResource(Res.string.decimal_separator),
        stringResource(Res.string.grouping_separator),
    )
    return stringResource(Res.string.amount_format, number, currencySymbol(amount.currency))
}

/** A tax amount as the pass reads it: a refund (negative) as "+ 312,00 €", an amount due as is. */
@Composable
internal fun localizedTaxAmount(amount: FydeAttribute.Amount): String =
    if (amount.minorUnits < 0) {
        stringResource(Res.string.vault_pass_refund_amount, localizedAmount(amount.copy(minorUnits = -amount.minorUnits)))
    } else {
        localizedAmount(amount)
    }

/**
 * Whether French words a document of [type] in the feminine ("une facture":
 * "Émise", "Émise le"). English has one form, so its strings repeat it.
 */
internal fun isFeminine(type: FydeDocumentType?): Boolean = when (type) {
    FydeDocumentType.INVOICE, FydeDocumentType.INSURANCE_CERTIFICATE, FydeDocumentType.INSURANCE_CARD,
    FydeDocumentType.RIGHTS_CERTIFICATE, FydeDocumentType.TAX_RETURN, FydeDocumentType.TAX_CERTIFICATE,
    FydeDocumentType.EMPLOYMENT_CERTIFICATE, FydeDocumentType.BOOKING, FydeDocumentType.ID_DOCUMENT,
    FydeDocumentType.BROCHURE, FydeDocumentType.ATTESTATION, FydeDocumentType.PAYMENT_REMINDER,
    FydeDocumentType.FINE, FydeDocumentType.WARRANTY,
    -> true
    else -> false
}

/** An attribute's label in Informations: its own wording for a default attribute, else its name. */
@Composable
internal fun attributeLabel(name: String, feminine: Boolean): String = when (name) {
    AttributeNames.ISSUED_ON -> stringResource(if (feminine) Res.string.attribute_issued_on_f else Res.string.attribute_issued_on)
    AttributeNames.ISSUER -> stringResource(Res.string.attribute_issuer)
    AttributeNames.REFERENCE -> stringResource(Res.string.attribute_reference)
    AttributeNames.PERIOD_START -> stringResource(Res.string.attribute_period_start)
    AttributeNames.PERIOD_END -> stringResource(Res.string.attribute_period_end)
    AttributeNames.TOTAL_AMOUNT -> stringResource(Res.string.attribute_total_amount)
    AttributeNames.DUE_ON -> stringResource(Res.string.attribute_due_on)
    AttributeNames.GROSS_PAY -> stringResource(Res.string.attribute_gross_pay)
    AttributeNames.NET_PAY -> stringResource(Res.string.attribute_net_pay)
    AttributeNames.TAX_YEAR -> stringResource(Res.string.attribute_tax_year)
    AttributeNames.TAX_AMOUNT -> stringResource(Res.string.attribute_tax_amount)
    else -> humanizedAttributeName(name)
}

/** An attribute's value, formatted by its type. */
@Composable
internal fun attributeValue(name: String, attribute: FydeAttribute): String = when (attribute) {
    is FydeAttribute.Text -> attribute.value
    is FydeAttribute.Integer -> if (name == AttributeNames.TAX_YEAR) {
        attribute.value.toString()
    } else {
        val grouped = groupThousands(kotlin.math.abs(attribute.value).toString(), stringResource(Res.string.grouping_separator))
        if (attribute.value < 0) "-$grouped" else grouped
    }
    is FydeAttribute.Decimal -> formatDecimal(
        attribute.value,
        stringResource(Res.string.decimal_separator),
        stringResource(Res.string.grouping_separator),
    )
    is FydeAttribute.Bool -> stringResource(if (attribute.value) Res.string.attribute_yes else Res.string.attribute_no)
    is FydeAttribute.Date -> parseIsoDate(attribute.value)?.let { localizedLongDate(it) } ?: attribute.value
    is FydeAttribute.Amount -> localizedAmount(attribute)
    is FydeAttribute.Select -> attribute.selected
}

/** One Informations row, worded. */
@Composable
internal fun informationText(row: InformationRow, feminine: Boolean): DocumentAttributeRow = when (row) {
    is InformationRow.Covers -> DocumentAttributeRow(stringResource(Res.string.attribute_period), localizedPeriod(row.period))
    is InformationRow.Value -> DocumentAttributeRow(
        attributeLabel(row.name, feminine),
        attributeValue(row.name, row.attribute),
        isAmount = row.attribute is FydeAttribute.Amount,
    )
}

/** Everything a document's `DocumentPass` shows, worded. */
internal data class PassText(
    val issuer: String?,
    val headline: String,
    val headlineKind: DocumentPassHeadline,
    val line: String?,
    val cells: List<DocumentPassCell>,
)

/** [layout] worded for [document]; [fallbackIssuer] heads the pass when the document has no issuer. */
@Composable
internal fun passText(document: FydeDocument, layout: PassLayout, fallbackIssuer: String): PassText {
    val feminine = isFeminine(document.documentType)
    val headline = when (layout.template) {
        PassTemplate.BILL, PassTemplate.STATEMENT -> localizedAmount(document.amount(AttributeNames.TOTAL_AMOUNT)!!)
        PassTemplate.PAYSLIP -> localizedAmount(document.amount(AttributeNames.NET_PAY)!!)
        PassTemplate.TAX_NOTICE -> localizedTaxAmount(document.amount(AttributeNames.TAX_AMOUNT)!!)
        PassTemplate.CERTIFICATE -> stringResource(
            Res.string.vault_pass_valid_until,
            localizedLongDate(document.date(AttributeNames.PERIOD_END)!!),
        )
        PassTemplate.OTHER -> document.name
    }
    val line = when (val passLine = layout.line) {
        null -> null
        is PassLine.DueBy -> stringResource(Res.string.vault_pass_due_by, localizedLongDate(passLine.date, withYear = false))
        is PassLine.IssuedOn -> stringResource(
            if (feminine) Res.string.vault_pass_issued_on_f else Res.string.vault_pass_issued_on,
            localizedLongDate(passLine.date, withYear = false),
        )
        is PassLine.NetPay -> passLine.month?.let { stringResource(Res.string.vault_pass_net_pay_month, localizedPeriod(it)) }
            ?: stringResource(Res.string.vault_pass_net_pay)
        is PassLine.Tax -> when {
            passLine.date == null -> stringResource(if (passLine.refund) Res.string.vault_pass_refund else Res.string.vault_pass_amount_due)
            passLine.refund -> stringResource(Res.string.vault_pass_refund_on, localizedLongDate(passLine.date, withYear = false))
            else -> stringResource(Res.string.vault_pass_amount_due_by, localizedLongDate(passLine.date, withYear = false))
        }
        is PassLine.Covers -> localizedPeriod(passLine.period)
    }
    val cells = layout.cells.map { cell ->
        when (cell) {
            PassCell.PERIOD -> DocumentPassCell(stringResource(Res.string.attribute_period), localizedPeriod(document.period!!))
            PassCell.REFERENCE -> DocumentPassCell(
                stringResource(Res.string.attribute_reference),
                document.text(AttributeNames.REFERENCE)!!,
            )
            PassCell.DUE -> DocumentPassCell(
                stringResource(Res.string.attribute_due_on),
                localizedShortDate(document.date(AttributeNames.DUE_ON)!!),
            )
            PassCell.GROSS_PAY -> DocumentPassCell(
                stringResource(Res.string.attribute_gross_pay),
                localizedAmount(document.amount(AttributeNames.GROSS_PAY)!!),
            )
            PassCell.TAX_YEAR -> DocumentPassCell(
                stringResource(Res.string.attribute_tax_year),
                document.integer(AttributeNames.TAX_YEAR)!!.toString(),
            )
            PassCell.ISSUED -> DocumentPassCell(
                stringResource(if (feminine) Res.string.vault_pass_issued_f else Res.string.vault_pass_issued),
                localizedShortDate(document.date(AttributeNames.ISSUED_ON)!!),
            )
            PassCell.FORMAT -> DocumentPassCell(stringResource(Res.string.vault_pass_format), document.typeCode)
        }
    }
    return PassText(
        issuer = document.issuer ?: fallbackIssuer,
        headline = headline,
        headlineKind = if (layout.template == PassTemplate.OTHER) DocumentPassHeadline.Name else DocumentPassHeadline.Value,
        line = line,
        cells = cells,
    )
}

/** The amount a document's rows show at their end: its pass's headline amount, if its template has one. */
@Composable
internal fun rowAmount(document: FydeDocument): String? = when (passLayout(document).template) {
    PassTemplate.BILL, PassTemplate.STATEMENT -> localizedAmount(document.amount(AttributeNames.TOTAL_AMOUNT)!!)
    PassTemplate.PAYSLIP -> localizedAmount(document.amount(AttributeNames.NET_PAY)!!)
    PassTemplate.TAX_NOTICE -> localizedTaxAmount(document.amount(AttributeNames.TAX_AMOUNT)!!)
    PassTemplate.CERTIFICATE, PassTemplate.OTHER -> null
}
