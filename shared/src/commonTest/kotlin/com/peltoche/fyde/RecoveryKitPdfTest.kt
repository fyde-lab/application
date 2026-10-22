package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecoveryKitPdfTest {
    private val words = List(24) { "word$it" }

    private fun pdfText(): String = recoveryKitPdf(
        title = "Kit de récupération Fyde",
        account = "Compte : pierre",
        intro = "Si vous oubliez votre mot de passe, ces 24 mots sont la seule façon de rouvrir votre coffre.",
        words = words,
        warning = "Gardez cette feuille (hors ligne) en lieu sûr.",
    ).decodeToString()

    @Test
    fun isAWellFormedPdf() {
        val pdf = pdfText()
        assertTrue(pdf.startsWith("%PDF-1.4\n"))
        assertTrue(pdf.endsWith("%%EOF\n"))
        val startXref = pdf.substringAfter("startxref\n").substringBefore('\n').toInt()
        assertTrue(pdf.startsWith("xref\n", startIndex = startXref))
    }

    @Test
    fun crossReferencePointsAtEachObject() {
        val pdf = pdfText()
        val xref = pdf.substring(pdf.indexOf("xref\n0 "))
        val offsets = xref.lines().drop(3).takeWhile { it.endsWith(" n ") }.map { it.take(10).toInt() }
        assertEquals(7, offsets.size)
        offsets.forEachIndexed { index, offset ->
            assertTrue(pdf.startsWith("${index + 1} 0 obj\n", startIndex = offset), "object ${index + 1} at $offset")
        }
    }

    @Test
    fun contentStreamLengthMatchesItsBytes() {
        val pdf = pdfText()
        val declared = pdf.substringAfter("<< /Length ").substringBefore(" >>").toInt()
        val stream = pdf.substringAfter(">>\nstream\n").substringBefore("endstream")
        assertEquals(declared, stream.length)
    }

    @Test
    fun listsEveryWordNumbered() {
        val pdf = pdfText()
        words.forEachIndexed { index, word ->
            assertTrue(pdf.contains("(${(index + 1).toString().padStart(2, ' ')}. $word) Tj"), word)
        }
    }

    @Test
    fun escapesPdfStringDelimitersAndEncodesAccentsAsWinAnsi() {
        assertEquals("a\\(b\\)c\\\\", pdfString("a(b)c\\"))
        assertEquals("r\\351cup\\351ration", pdfString("récupération"))
        assertEquals("l\\222appareil", pdfString("l’appareil"))
        assertEquals("Compte : x", pdfString("Compte : x"))
        assertEquals("?", pdfString("鍵"))
    }

    @Test
    fun wrapsAtSpacesWithinTheColumnCount() {
        assertEquals(listOf("one two", "three"), wrap("one two three", columns = 8))
        assertEquals(listOf("averyverylongword", "x"), wrap("averyverylongword x", columns = 5))
        assertEquals(emptyList(), wrap("", columns = 10))
    }
}
