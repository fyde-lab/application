package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceAccountsTest {

    private fun script(
        id: String,
        name: String = id,
        parameters: Map<String, FydeScriptParameter> = emptyMap(),
    ) = FydeScript(
        id = id,
        name = name,
        description = "",
        shortDescription = "",
        isPublic = true,
        icon = ByteArray(0),
        allowedDomains = emptyList(),
        version = 1,
        lastUpdated = 0,
        parameters = parameters,
        type = FydeScriptType.SCRAPER,
    )

    private fun parameter(secret: Boolean = false, type: FydeScriptParameterType = FydeScriptParameterType.STRING) =
        FydeScriptParameter(label = "", placeholder = "", type = type, required = true, secret = secret)

    private fun installation(id: String, scriptId: String, label: String) =
        FydeInstallation(id = id, scriptId = scriptId, label = label, parameters = emptyMap())

    private val ameli = script("ameli", "Ameli")
    private val edf = script("edf", "EDF")

    @Test
    fun aSourceWithOneAccountIsNamedAfterTheSourceAlone() {
        val accounts = sourceAccountsOf(listOf(edf), listOf(installation("1", "edf", "pierre@exemple.fr")))

        assertFalse(accounts.single().shared)
        assertEquals("EDF", accounts.single().name)
    }

    @Test
    fun aSourceWithSeveralAccountsNamesEachAfterItsLabel() {
        val accounts = sourceAccountsOf(
            listOf(ameli, edf),
            listOf(installation("1", "ameli", "Pierre"), installation("2", "edf", "x"), installation("3", "ameli", "Marie")),
        )

        assertEquals(listOf("Ameli · Marie", "Ameli · Pierre", "EDF"), accounts.map { it.name })
        assertTrue(accounts.filter { it.script == ameli }.all { it.shared })
    }

    @Test
    fun anAccountKeepsItsInstallationsFolder() {
        val accounts = sourceAccountsOf(
            listOf(edf),
            listOf(installation("1", "edf", "x").copy(folderId = "energie"), installation("2", "edf", "y")),
        )

        assertEquals(listOf("energie", null), accounts.map { it.folderId })
    }

    @Test
    fun anInstallationOfAScriptNoLongerOfferedIsLeftOut() {
        val accounts = sourceAccountsOf(listOf(edf), listOf(installation("1", "gone", "x"), installation("2", "edf", "y")))

        assertEquals(listOf("2"), accounts.map { it.installationId })
    }

    @Test
    fun provisionalLabelPrefersTheUsername() {
        val lucca = script(
            "lucca",
            parameters = mapOf("loginUrl" to parameter(), "username" to parameter(), "password" to parameter(secret = true)),
        )
        val values = mapOf(
            "loginUrl" to FydeScriptParameterValue.Text("https://acme.example.com"),
            "username" to FydeScriptParameterValue.Text(" marie "),
            "password" to FydeScriptParameterValue.Text("hunter2"),
        )

        assertEquals("marie", provisionalLabel(lucca, values))
    }

    @Test
    fun provisionalLabelFallsBackToOtherTextParametersThenTheSourceName() {
        val ameliWithFields = script(
            "ameli",
            "Ameli",
            parameters = mapOf("identifiant" to parameter(), "password" to parameter(secret = true)),
        )

        assertEquals(
            "2840775112048",
            provisionalLabel(ameliWithFields, mapOf("identifiant" to FydeScriptParameterValue.Text("2840775112048"))),
        )
        assertEquals("Ameli", provisionalLabel(ameliWithFields, mapOf("password" to FydeScriptParameterValue.Text("x"))))
    }
}
