package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScriptParametersTest {

    private fun parameter(type: FydeScriptParameterType, required: Boolean = false) = FydeScriptParameter(
        label = "label",
        placeholder = "",
        type = type,
        required = required,
        secret = false,
    )

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
}
