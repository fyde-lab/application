package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DocumentAttributesTest {

    private fun document(type: FydeDocumentType?, vararg attributes: Pair<String, FydeAttribute>) = FydeDocument(
        id = "id",
        name = "Facture d'électricité",
        contentType = "application/pdf",
        createdAt = 0,
        size = 0,
        documentType = type,
        category = FydeCategory.HOUSING,
        subcategory = null,
        attributes = attributes.toMap(),
    )

    private fun eur(cents: Long) = FydeAttribute.Amount(cents, "EUR")
    private fun date(value: String) = FydeAttribute.Date(value)
    private fun text(value: String) = FydeAttribute.Text(value)

    @Test
    fun anInvoiceWithItsTotalGetsTheBillTemplate() {
        val invoice = document(
            FydeDocumentType.INVOICE,
            AttributeNames.ISSUER to text("EDF"),
            AttributeNames.TOTAL_AMOUNT to eur(8420),
            AttributeNames.DUE_ON to date("2026-10-27"),
            AttributeNames.ISSUED_ON to date("2026-10-10"),
            AttributeNames.PERIOD_START to date("2026-09-01"),
            AttributeNames.PERIOD_END to date("2026-10-31"),
            AttributeNames.REFERENCE to text("3001 4471"),
            "contract" to text("CTR 3001 4471 22"),
        )

        val layout = passLayout(invoice)

        assertEquals(PassTemplate.BILL, layout.template)
        assertEquals(PassLine.DueBy(CivilDate(2026, 10, 27)), layout.line)
        assertEquals(listOf(PassCell.PERIOD, PassCell.REFERENCE), layout.cells)
        assertEquals(
            listOf(
                InformationRow.Value(AttributeNames.ISSUED_ON, date("2026-10-10")),
                InformationRow.Value("contract", text("CTR 3001 4471 22")),
            ),
            informationRows(invoice, layout),
        )
    }

    @Test
    fun aTemplateWhoseHeadlineIsUnsetFallsBackToTheNameTemplate() {
        val invoice = document(
            FydeDocumentType.INVOICE,
            AttributeNames.ISSUED_ON to date("2026-10-10"),
            AttributeNames.REFERENCE to text("A-1"),
        )

        val layout = passLayout(invoice)

        assertEquals(PassTemplate.OTHER, layout.template)
        assertEquals(PassLine.IssuedOn(CivilDate(2026, 10, 10)), layout.line)
        assertEquals(listOf(PassCell.REFERENCE, PassCell.FORMAT), layout.cells)
        assertEquals(emptyList(), informationRows(invoice, layout))
    }

    @Test
    fun anAttributeOfTheWrongTypeNeverFillsThePassButStaysInInformations() {
        val invoice = document(FydeDocumentType.INVOICE, AttributeNames.TOTAL_AMOUNT to text("84,20"))

        val layout = passLayout(invoice)

        assertEquals(PassTemplate.OTHER, layout.template)
        assertEquals(listOf(InformationRow.Value(AttributeNames.TOTAL_AMOUNT, text("84,20"))), informationRows(invoice, layout))
    }

    @Test
    fun aPayslipReadsItsMonthFromItsPeriod() {
        val payslip = document(
            FydeDocumentType.PAYSLIP,
            AttributeNames.NET_PAY to eur(191244),
            AttributeNames.GROSS_PAY to eur(245000),
            AttributeNames.PERIOD_START to date("2026-09-01"),
            AttributeNames.PERIOD_END to date("2026-09-30"),
        )

        val layout = passLayout(payslip)

        assertEquals(PassTemplate.PAYSLIP, layout.template)
        assertEquals(PassLine.NetPay(Period.Month(2026, 9)), layout.line)
        assertEquals(listOf(PassCell.GROSS_PAY), layout.cells)
        assertEquals(emptyList(), informationRows(payslip, layout))
    }

    @Test
    fun aNegativeTaxAmountIsARefund() {
        val notice = document(
            FydeDocumentType.TAX_NOTICE,
            AttributeNames.TAX_AMOUNT to eur(-31200),
            AttributeNames.TAX_YEAR to FydeAttribute.Integer(2025),
            AttributeNames.DUE_ON to date("2026-08-29"),
        )

        val layout = passLayout(notice)

        assertEquals(PassTemplate.TAX_NOTICE, layout.template)
        assertEquals(PassLine.Tax(refund = true, date = CivilDate(2026, 8, 29)), layout.line)
        assertEquals(listOf(PassCell.TAX_YEAR), layout.cells)
    }

    @Test
    fun aDueDateOrAnIncomeYearNotOnTheLineFillsTheCells() {
        val statement = document(
            FydeDocumentType.STATEMENT,
            AttributeNames.TOTAL_AMOUNT to eur(4200),
            AttributeNames.REFERENCE to text("R-12"),
            AttributeNames.DUE_ON to date("2026-11-15"),
            AttributeNames.TAX_YEAR to FydeAttribute.Integer(2025),
        )

        val layout = passLayout(statement)

        assertEquals(PassTemplate.STATEMENT, layout.template)
        assertEquals(listOf(PassCell.REFERENCE, PassCell.DUE, PassCell.TAX_YEAR), layout.cells)
        assertEquals(emptyList(), informationRows(statement, layout))
    }

    @Test
    fun aCertificateIsValidUntilItsPeriodEndAndListsItsStartAlone() {
        val certificate = document(
            FydeDocumentType.INSURANCE_CERTIFICATE,
            AttributeNames.PERIOD_START to date("2026-01-01"),
            AttributeNames.PERIOD_END to date("2026-12-31"),
        )

        val layout = passLayout(certificate)

        assertEquals(PassTemplate.CERTIFICATE, layout.template)
        assertNull(layout.line)
        assertEquals(
            listOf(InformationRow.Value(AttributeNames.PERIOD_START, date("2026-01-01"))),
            informationRows(certificate, layout),
        )
    }

    @Test
    fun informationsMergesAPeriodLeftOffThePassAndOrdersDefaultsFirst() {
        val payslip = document(
            FydeDocumentType.PAYSLIP,
            AttributeNames.NET_PAY to eur(100),
            AttributeNames.PERIOD_START to date("2026-09-03"),
            AttributeNames.PERIOD_END to date("2026-09-30"),
            "zeta" to FydeAttribute.Bool(true),
            "alpha" to FydeAttribute.Integer(3),
            AttributeNames.ISSUER to FydeAttribute.Integer(1),
        )

        val rows = informationRows(payslip, passLayout(payslip))

        assertEquals(
            listOf(
                InformationRow.Value(AttributeNames.ISSUER, FydeAttribute.Integer(1)),
                InformationRow.Covers(Period.Days(CivilDate(2026, 9, 3), CivilDate(2026, 9, 30))),
                InformationRow.Value("alpha", FydeAttribute.Integer(3)),
                InformationRow.Value("zeta", FydeAttribute.Bool(true)),
            ),
            rows,
        )
    }

    @Test
    fun periodsReadAsAMonthAYearOrASpan() {
        assertEquals(Period.Month(2026, 2), periodOf(CivilDate(2026, 2, 1), CivilDate(2026, 2, 28)))
        assertEquals(Period.Year(2025), periodOf(CivilDate(2025, 1, 1), CivilDate(2025, 12, 31)))
        assertEquals(
            Period.Months(CivilDate(2026, 9, 1), CivilDate(2026, 10, 31)),
            periodOf(CivilDate(2026, 9, 1), CivilDate(2026, 10, 31)),
        )
        assertEquals(Period.From(CivilDate(2026, 9, 1)), periodOf(CivilDate(2026, 9, 1), null))
        assertNull(periodOf(null, null))
    }

    @Test
    fun parsesOnlyRealCalendarDays() {
        assertEquals(CivilDate(2024, 2, 29), parseIsoDate("2024-02-29"))
        assertNull(parseIsoDate("2026-02-29"))
        assertNull(parseIsoDate("2026-13-01"))
        assertNull(parseIsoDate("12/10/2026"))
    }

    @Test
    fun formatsMinorUnitsExactlyWithTheCurrencysDigits() {
        assertEquals("1 912,44", formatMinorUnits(191244, 2, ",", " "))
        assertEquals("0.05", formatMinorUnits(5, 2, ".", ","))
        assertEquals("-312.00", formatMinorUnits(-31200, 2, ".", ","))
        assertEquals("1,500", formatMinorUnits(1500, currencyDigits("JPY"), ".", ","))
        assertEquals("1.500", formatMinorUnits(1500, currencyDigits("KWD"), ".", ","))
        assertEquals("92,233,720,368,547,758.07", formatMinorUnits(Long.MAX_VALUE, 2, ".", ","))
    }

    @Test
    fun humanizesAnAttributeWithoutWordingOfItsOwn() {
        assertEquals("Contract number", humanizedAttributeName("contract_number"))
        assertEquals("Account", humanizedAttributeName("account"))
    }
}
