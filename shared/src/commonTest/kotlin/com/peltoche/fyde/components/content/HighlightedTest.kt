package com.peltoche.fyde.components.content

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class HighlightedTest {

    private fun ranges(text: String, vararg terms: String) =
        highlighted(text, terms.toList(), Color.Yellow).spanStyles.map { it.start until it.end }

    @Test
    fun marksEveryOccurrenceIgnoringCaseAndAccents() {
        assertEquals(listOf(0 until 11, 15 until 26), ranges("Électricité et electricite", "electricite"))
        assertEquals(listOf(0 until 11), ranges("Attestation employeur", "ATTESTATION"))
    }

    @Test
    fun mergesOverlappingTermsAndIgnoresBlankOnes() {
        assertEquals(listOf(0 until 7), ranges("Facture EDF", "fact", "acture", " "))
        assertEquals(emptyList(), ranges("Facture EDF", ""))
    }
}
