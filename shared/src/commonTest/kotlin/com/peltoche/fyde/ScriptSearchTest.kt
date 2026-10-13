package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals

class ScriptSearchTest {

    private fun script(name: String, shortDescription: String = "", description: String = "") = FydeScript(
        id = name,
        name = name,
        description = description,
        shortDescription = shortDescription,
        isPublic = true,
        icon = ByteArray(0),
        allowedDomains = emptyList(),
        version = 1,
        script = "",
        lastUpdated = 0,
    )

    private val alan = script("Alan", shortDescription = "Décomptes et carte Alan")
    private val aesio = script("Aésio (ex-Eovi MCD)", shortDescription = "Relevés de prestations")
    private val amazon = script("Amazon", description = "Factures Amazon.fr")
    private val all = listOf(alan, aesio, amazon)

    private fun names(query: String) = filterScripts(all, query).map { it.name }

    @Test
    fun blankQueryMatchesEveryScript() {
        assertEquals(all, filterScripts(all, "  "))
    }

    @Test
    fun matchesNameShortDescriptionAndDescription() {
        assertEquals(listOf("Alan"), names("carte"))
        assertEquals(listOf("Amazon"), names("factures"))
        assertEquals(listOf("Alan", "Aésio (ex-Eovi MCD)", "Amazon"), names("a"))
    }

    @Test
    fun ignoresCaseAndAccents() {
        assertEquals(listOf("Aésio (ex-Eovi MCD)"), names("AESIO"))
        assertEquals(listOf("Alan"), names("decomptes"))
        assertEquals(listOf("Aésio (ex-Eovi MCD)"), names("RELEVÉS"))
    }

    @Test
    fun requiresEveryWord() {
        assertEquals(listOf("Alan"), names("alan carte"))
        assertEquals(emptyList(), names("alan factures"))
    }
}
