package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ScriptParametersTest {

    private fun parameter(type: FydeScriptParameterType, required: Boolean = false, secret: Boolean = false) = FydeScriptParameter(
        label = "label",
        placeholder = "",
        type = type,
        required = required,
        secret = secret,
    )

    private val labels = ParameterValueLabels(hidden = "hidden", yes = "yes", no = "no", unset = "unset")

    @Test
    fun encodesEachValueTypeAsItsJsonType() {
        val json = encodeScriptParametersJson(
            linkedMapOf(
                "username" to FydeScriptParameterValue.Text("alice"),
                "retries" to FydeScriptParameterValue.Number(3.0),
                "ratio" to FydeScriptParameterValue.Number(0.5),
                "verbose" to FydeScriptParameterValue.Bool(true),
            ),
        )

        assertEquals("""{"username":"alice","retries":3,"ratio":0.5,"verbose":true}""", json)
    }

    @Test
    fun escapesQuotesBackslashesAndControlCharactersInStrings() {
        val json = encodeScriptParametersJson(
            mapOf("pass\"word" to FydeScriptParameterValue.Text("a\"b\\c\nd\u0001")),
        )

        assertEquals("""{"pass\"word":"a\"b\\c\nd\u0001"}""", json)
    }

    @Test
    fun encodesNoParametersAsAnEmptyObject() {
        assertEquals("{}", encodeScriptParametersJson(emptyMap()))
    }

    @Test
    fun rejectsNonFiniteNumbers() {
        assertFailsWith<IllegalArgumentException> {
            encodeScriptParametersJson(mapOf("ratio" to FydeScriptParameterValue.Number(Double.NaN)))
        }
    }

    @Test
    fun typedParameterValuesConvertsFormStringsPerDeclaredType() {
        val values = typedParameterValues(
            parameters = mapOf(
                "username" to parameter(FydeScriptParameterType.STRING, required = true),
                "retries" to parameter(FydeScriptParameterType.NUMBER),
                "verbose" to parameter(FydeScriptParameterType.BOOLEAN),
            ),
            values = mapOf("username" to "alice", "retries" to "3", "verbose" to "true"),
        )

        assertEquals(
            mapOf(
                "username" to FydeScriptParameterValue.Text("alice"),
                "retries" to FydeScriptParameterValue.Number(3.0),
                "verbose" to FydeScriptParameterValue.Bool(true),
            ),
            values,
        )
    }

    @Test
    fun typedParameterValuesOmitsBlankOptionalValuesButDefaultsSwitchesToFalse() {
        val values = typedParameterValues(
            parameters = mapOf(
                "nickname" to parameter(FydeScriptParameterType.STRING),
                "retries" to parameter(FydeScriptParameterType.NUMBER),
                "verbose" to parameter(FydeScriptParameterType.BOOLEAN),
            ),
            values = mapOf("nickname" to "  "),
        )

        assertEquals(mapOf("verbose" to FydeScriptParameterValue.Bool(false)), values)
    }

    @Test
    fun ordersRequiredBeforeOptionalAndPlainBeforeSecret() {
        fun parameter(label: String, required: Boolean, secret: Boolean) = FydeScriptParameter(
            label = label,
            placeholder = "",
            type = FydeScriptParameterType.STRING,
            required = required,
            secret = secret,
        )
        val ordered = orderedParameters(
            mapOf(
                "nickname" to parameter("Nickname", required = false, secret = false),
                "password" to parameter("Password", required = true, secret = true),
                "username" to parameter("Username", required = true, secret = false),
                "account" to parameter("Account", required = true, secret = false),
            ),
        )

        assertEquals(listOf("account", "username", "password", "nickname"), ordered.map { it.key })
    }

    @Test
    fun formValuesRoundTripThroughTypedParameterValues() {
        val parameters = mapOf(
            "username" to parameter(FydeScriptParameterType.STRING),
            "retries" to parameter(FydeScriptParameterType.NUMBER),
            "ratio" to parameter(FydeScriptParameterType.NUMBER),
            "verbose" to parameter(FydeScriptParameterType.BOOLEAN),
        )
        val installed = mapOf(
            "username" to FydeScriptParameterValue.Text("alice"),
            "retries" to FydeScriptParameterValue.Number(3.0),
            "ratio" to FydeScriptParameterValue.Number(0.5),
            "verbose" to FydeScriptParameterValue.Bool(true),
        )

        assertEquals(mapOf("username" to "alice", "retries" to "3", "ratio" to "0.5", "verbose" to "true"), formValues(installed))
        assertEquals(installed, typedParameterValues(parameters, formValues(installed)))
    }

    @Test
    fun validatedParameterValuesRejectsABlankRequiredValue() {
        val parameters = mapOf("username" to parameter(FydeScriptParameterType.STRING, required = true))

        assertNull(validatedParameterValues(parameters, mapOf("username" to " ")))
        assertNull(validatedParameterValues(parameters, emptyMap()))
        assertEquals(
            mapOf("username" to FydeScriptParameterValue.Text("alice")),
            validatedParameterValues(parameters, mapOf("username" to "alice")),
        )
    }

    @Test
    fun validatedParameterValuesRejectsANumberFieldHoldingSomethingElse() {
        val parameters = mapOf("retries" to parameter(FydeScriptParameterType.NUMBER))

        assertNull(validatedParameterValues(parameters, mapOf("retries" to "three")))
        assertNull(validatedParameterValues(parameters, mapOf("retries" to "NaN")))
        assertEquals(emptyMap(), validatedParameterValues(parameters, mapOf("retries" to "")))
    }

    @Test
    fun validatedParameterValuesTakesAnUntouchedRequiredSwitchAsFalse() {
        val parameters = mapOf("accept" to parameter(FydeScriptParameterType.BOOLEAN, required = true))

        assertEquals(mapOf("accept" to FydeScriptParameterValue.Bool(false)), validatedParameterValues(parameters, emptyMap()))
    }

    @Test
    fun displayedParameterValueMasksSecretsAndSpellsOutTheRest() {
        assertEquals("alice", displayedParameterValue(parameter(FydeScriptParameterType.STRING), FydeScriptParameterValue.Text("alice"), labels))
        assertEquals(
            "hidden",
            displayedParameterValue(parameter(FydeScriptParameterType.STRING, secret = true), FydeScriptParameterValue.Text("hunter2"), labels),
        )
        assertEquals("3", displayedParameterValue(parameter(FydeScriptParameterType.NUMBER), FydeScriptParameterValue.Number(3.0), labels))
        assertEquals("yes", displayedParameterValue(parameter(FydeScriptParameterType.BOOLEAN), FydeScriptParameterValue.Bool(true), labels))
        assertEquals("no", displayedParameterValue(parameter(FydeScriptParameterType.BOOLEAN), FydeScriptParameterValue.Bool(false), labels))
    }

    private val channel = parameter(FydeScriptParameterType.STRING, required = true).copy(
        choices = listOf(FydeScriptParameterChoice("email", "E-mail"), FydeScriptParameterChoice("sms", "SMS")),
    )

    @Test
    fun validatedParameterValuesOnlyTakesOneOfAParametersChoices() {
        val parameters = mapOf("otp_channel" to channel)

        assertEquals(
            mapOf("otp_channel" to FydeScriptParameterValue.Text("sms")),
            validatedParameterValues(parameters, mapOf("otp_channel" to "sms")),
        )
        assertNull(validatedParameterValues(parameters, mapOf("otp_channel" to "telephone")))
        assertNull(validatedParameterValues(parameters, emptyMap()))
    }

    @Test
    fun displayedParameterValueShowsAChoiceByItsLabel() {
        assertEquals("SMS", displayedParameterValue(channel, FydeScriptParameterValue.Text("sms"), labels))
        // One installed before the script declared its choices reads as typed.
        assertEquals("mail", displayedParameterValue(channel, FydeScriptParameterValue.Text("mail"), labels))
    }

    @Test
    fun onlyAStringParameterIsASelect() {
        assertEquals(true, channel.hasChoices)
        assertEquals(false, parameter(FydeScriptParameterType.STRING).hasChoices)
        assertEquals(false, channel.copy(type = FydeScriptParameterType.NUMBER).hasChoices)
    }

    @Test
    fun displayedParameterValueShowsAMissingValueAsUnsetEvenForASecret() {
        assertEquals("unset", displayedParameterValue(parameter(FydeScriptParameterType.STRING), null, labels))
        assertEquals("unset", displayedParameterValue(parameter(FydeScriptParameterType.STRING, secret = true), null, labels))
    }

    @Test
    fun decodesWhatEncodeProduces() {
        val parameters = linkedMapOf(
            "pass\"word" to FydeScriptParameterValue.Text("a\"b\\c\nd\u0001é"),
            "retries" to FydeScriptParameterValue.Number(3.0),
            "ratio" to FydeScriptParameterValue.Number(-0.5),
            "verbose" to FydeScriptParameterValue.Bool(false),
        )

        assertEquals(parameters, decodeScriptParametersJson(encodeScriptParametersJson(parameters)))
    }

    @Test
    fun decodingSkipsNullsAndToleratesWhitespace() {
        val values = decodeScriptParametersJson(""" { "username" : "alice" , "nickname" : null } """)

        assertEquals(mapOf("username" to FydeScriptParameterValue.Text("alice")), values)
    }

    @Test
    fun decodesAnEmptyObject() {
        assertEquals(emptyMap(), decodeScriptParametersJson("{}"))
    }

    @Test
    fun decodingRejectsNestedValuesAndMalformedJson() {
        assertFailsWith<IllegalArgumentException> { decodeScriptParametersJson("""{"a":{"b":1}}""") }
        assertFailsWith<IllegalArgumentException> { decodeScriptParametersJson("""{"a":1""") }
        assertFailsWith<IllegalArgumentException> { decodeScriptParametersJson("""{"a":1} x""") }
    }

    @Test
    fun keepsAnInstalledSecretLeftBlank() {
        val parameters = mapOf(
            "email" to parameter(FydeScriptParameterType.STRING, required = true),
            "password" to parameter(FydeScriptParameterType.STRING, required = true, secret = true),
        )
        val installed = mapOf(
            "email" to FydeScriptParameterValue.Text("old@example.com"),
            "password" to FydeScriptParameterValue.Text("hunter2"),
        )

        assertEquals(
            mapOf(
                "email" to FydeScriptParameterValue.Text("new@example.com"),
                "password" to FydeScriptParameterValue.Text("hunter2"),
            ),
            validatedCredentials(parameters, mapOf("email" to "new@example.com", "password" to ""), installed),
        )
        assertEquals(
            FydeScriptParameterValue.Text("changed"),
            validatedCredentials(parameters, mapOf("email" to "a@b.c", "password" to "changed"), installed)?.get("password"),
        )
    }

    @Test
    fun stillRequiresASecretNeverInstalledAndPlainRequiredFields() {
        val parameters = mapOf(
            "email" to parameter(FydeScriptParameterType.STRING, required = true),
            "password" to parameter(FydeScriptParameterType.STRING, required = true, secret = true),
        )

        assertNull(validatedCredentials(parameters, mapOf("email" to "a@b.c", "password" to ""), emptyMap()))
        assertNull(
            validatedCredentials(
                parameters,
                mapOf("email" to "", "password" to ""),
                mapOf("password" to FydeScriptParameterValue.Text("hunter2")),
            ),
        )
    }
}
