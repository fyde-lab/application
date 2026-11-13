package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceFoldersTest {

    @Test
    fun aNameStartingWithAVowelOrHElidesDe() {
        listOf("EDF", "Ameli · Marie", "impots.gouv", "Électricité", "Harmonie").forEach {
            assertTrue(elidesDe(it), "expected d’$it")
        }
    }

    @Test
    fun aNameStartingWithAConsonantKeepsDe() {
        listOf("Free", "MAIF", "Pajemploi", "", "3F").forEach {
            assertFalse(elidesDe(it), "expected de $it")
        }
    }
}
