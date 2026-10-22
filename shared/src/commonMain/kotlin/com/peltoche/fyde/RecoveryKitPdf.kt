package com.peltoche.fyde

/**
 * The recovery kit as a one-page A4 PDF, for "Save the PDF" on O6/DO6 and
 * for printing it: [title], the [account] it recovers, [intro], the [words]
 * numbered in three columns, then [warning]. Written by hand rather than
 * through a PDF library, since none runs on every target and the page is
 * only text: the standard Helvetica and Courier fonts (no font embedded),
 * WinAnsi-encoded, so French accents print but characters outside that
 * encoding come out as `?`. Paragraphs wrap by character count, an
 * approximation that stays inside the margins for Helvetica at these sizes.
 */
internal fun recoveryKitPdf(
    title: String,
    account: String,
    intro: String,
    words: List<String>,
    warning: String,
): ByteArray {
    val page = PdfPage()
    page.text(title, PdfFont.HelveticaBold, size = 20f, y = TOP)
    var y = TOP - 26f
    page.text(account, PdfFont.Helvetica, size = 11f, y = y)
    y -= 30f
    y = page.paragraph(intro, y)
    y -= 18f

    val rows = (words.size + WORD_COLUMNS - 1) / WORD_COLUMNS
    val columnWidth = (PAGE_WIDTH - 2 * MARGIN) / WORD_COLUMNS
    words.forEachIndexed { index, word ->
        // Down each column, then across (1-8, 9-16, 17-24), as a numbered
        // list reads on paper.
        val column = index / rows
        val row = index % rows
        val number = (index + 1).toString().padStart(2, ' ')
        page.text("$number. $word", PdfFont.Courier, size = 14f, y = y - row * 24f, x = MARGIN + column * columnWidth)
    }
    y -= rows * 24f + 18f
    page.paragraph(warning, y)
    return page.toPdf()
}

private const val PAGE_WIDTH = 595f
private const val PAGE_HEIGHT = 842f
private const val MARGIN = 56f
private const val TOP = PAGE_HEIGHT - 72f
private const val WORD_COLUMNS = 3

/** Characters per line for 11pt Helvetica across the margins — a rough average width, on the safe side. */
private const val WRAP_COLUMNS = 88

private enum class PdfFont(val resource: String, val baseFont: String) {
    Helvetica("F1", "Helvetica"),
    HelveticaBold("F2", "Helvetica-Bold"),
    Courier("F3", "Courier"),
}

/** One page's content stream, built line by line. */
private class PdfPage {
    private val content = StringBuilder()

    fun text(text: String, font: PdfFont, size: Float, y: Float, x: Float = MARGIN) {
        content.append("BT /${font.resource} ${size.pdf()} Tf ${x.pdf()} ${y.pdf()} Td (")
            .append(pdfString(text))
            .append(") Tj ET\n")
    }

    /** [text] wrapped at word boundaries, 11pt with 16pt leading; returns the baseline below its last line. */
    fun paragraph(text: String, top: Float): Float {
        var y = top
        wrap(text, WRAP_COLUMNS).forEach { line ->
            text(line, PdfFont.Helvetica, size = 11f, y = y)
            y -= 16f
        }
        return y
    }

    /**
     * The whole file: catalog, page tree, the page, its content stream and
     * the three standard fonts, then the cross-reference table pointing at
     * each object's byte offset. Everything is ASCII (see [pdfString]), so
     * a character is a byte.
     */
    fun toPdf(): ByteArray {
        val stream = content.toString()
        val objects = buildList {
            add("<< /Type /Catalog /Pages 2 0 R >>")
            add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
            add(
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${PAGE_WIDTH.pdf()} ${PAGE_HEIGHT.pdf()}] " +
                    "/Contents 4 0 R /Resources << /Font << " +
                    PdfFont.entries.mapIndexed { i, font -> "/${font.resource} ${5 + i} 0 R" }.joinToString(" ") +
                    " >> >> >>",
            )
            add("<< /Length ${stream.length} >>\nstream\n${stream}endstream")
            PdfFont.entries.forEach { font ->
                add("<< /Type /Font /Subtype /Type1 /BaseFont /${font.baseFont} /Encoding /WinAnsiEncoding >>")
            }
        }
        val out = StringBuilder("%PDF-1.4\n")
        val offsets = objects.mapIndexed { index, body ->
            val offset = out.length
            out.append("${index + 1} 0 obj\n").append(body).append("\nendobj\n")
            offset
        }
        val xref = out.length
        out.append("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { out.append(it.toString().padStart(10, '0')).append(" 00000 n \n") }
        out.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return out.toString().encodeToByteArray()
    }
}

/** [this] with at most two decimals and no trailing zeros, as PDF numbers are written. */
private fun Float.pdf(): String {
    val hundredths = kotlin.math.round(this * 100).toLong()
    val whole = hundredths / 100
    val fraction = kotlin.math.abs(hundredths % 100)
    return if (fraction == 0L) whole.toString() else "$whole.${fraction.toString().padStart(2, '0').trimEnd('0')}"
}

/**
 * [text] as the inside of a PDF literal string in WinAnsiEncoding: ASCII as
 * is (parentheses and backslashes escaped), anything else as an octal
 * escape of its WinAnsi code, or `?` without one.
 */
internal fun pdfString(text: String): String = buildString {
    text.forEach { char ->
        when {
            char == '(' || char == ')' || char == '\\' -> append('\\').append(char)
            char.code in 0x20..0x7E -> append(char)
            else -> {
                when (val code = winAnsiCode(char)) {
                    null -> append('?')
                    in 0x20..0x7E -> append(code.toChar())
                    else -> append('\\').append(code.toString(8).padStart(3, '0'))
                }
            }
        }
    }
}

private fun winAnsiCode(char: Char): Int? = when (char) {
    // Spaces French typography puts before ":" "!" "?" and inside quotes.
    ' ', ' ' -> 0x20
    '’' -> 0x92
    '‘' -> 0x91
    '“' -> 0x93
    '”' -> 0x94
    '…' -> 0x85
    '–' -> 0x96
    '—' -> 0x97
    '€' -> 0x80
    'Œ' -> 0x8C
    'œ' -> 0x9C
    else -> char.code.takeIf { it in 0xA1..0xFF }
}

/** [text] split into lines of at most [columns] characters at spaces; a longer word gets a line of its own. */
internal fun wrap(text: String, columns: Int): List<String> {
    val lines = mutableListOf<String>()
    var line = StringBuilder()
    text.split(' ').filter { it.isNotEmpty() }.forEach { word ->
        if (line.isNotEmpty() && line.length + 1 + word.length > columns) {
            lines += line.toString()
            line = StringBuilder()
        }
        if (line.isNotEmpty()) line.append(' ')
        line.append(word)
    }
    if (line.isNotEmpty()) lines += line.toString()
    return lines
}
