package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper

/** Reads [recoveryKitPdf]'s output back with a real PDF parser, which the hand-written file has to satisfy. */
class RecoveryKitPdfJvmTest {
    @Test
    fun parsesAsOnePageWithEveryWordAndTheAccents() {
        val words = List(24) { "word$it" }
        val pdf = recoveryKitPdf(
            title = "Kit de récupération Fyde",
            account = "Compte : pierre",
            intro = "Si vous oubliez votre mot de passe, ces 24 mots sont la seule façon de rouvrir votre coffre. Gardez-les hors ligne, sur papier.",
            words = words,
            warning = "Quiconque détient ces mots et votre identifiant peut définir un nouveau mot de passe et ouvrir votre coffre.",
        )

        Loader.loadPDF(pdf).use { document ->
            assertEquals(1, document.numberOfPages)
            val text = PDFTextStripper().getText(document)
            assertTrue("Kit de récupération Fyde" in text, text)
            assertTrue("Compte : pierre" in text, text)
            assertTrue("façon" in text, text)
            words.forEach { assertTrue(it in text, it) }
        }
    }
}
