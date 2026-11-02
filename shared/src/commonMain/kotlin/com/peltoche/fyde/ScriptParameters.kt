package com.peltoche.fyde

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.peltoche.fyde.components.form.AppSwitch
import com.peltoche.fyde.components.form.AppTextField
import com.peltoche.fyde.components.overlay.AppDialog
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.scraper_answer_label
import com.peltoche.fyde.resources.scraper_answer_send
import com.peltoche.fyde.resources.scraper_question_title
import org.jetbrains.compose.resources.stringResource

/**
 * The control one of a script's [parameter]s is entered with: a switch for
 * a boolean ([value] "true"/"false"), a text field otherwise — masked for a
 * secret, numeric keyboard for a number. Shared by every form a source's
 * credentials are typed in, so a parameter reads the same wherever it's asked.
 */
@Composable
internal fun ScriptParameterField(
    parameter: FydeScriptParameter,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
) {
    if (parameter.type == FydeScriptParameterType.BOOLEAN) {
        AppSwitch(
            checked = value.toBooleanStrictOrNull() ?: false,
            onCheckedChange = { checked -> onValueChange(checked.toString()) },
            label = parameter.label,
            enabled = enabled,
            modifier = modifier,
        )
    } else {
        AppTextField(
            value = value,
            onValueChange = onValueChange,
            label = parameter.label,
            placeholder = parameter.placeholder.ifBlank { null },
            supportingText = supportingText,
            errorText = errorText,
            enabled = enabled,
            isPassword = parameter.secret,
            modifier = modifier,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (parameter.type == FydeScriptParameterType.NUMBER) KeyboardType.Number else KeyboardType.Unspecified,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        )
    }
}

/** The localized words [displayedParameterValue] reads a value as. */
internal data class ParameterValueLabels(val hidden: String, val yes: String, val no: String, val unset: String)

/**
 * How an installed [value] reads: a secret never shows, only that it's set;
 * a boolean as yes/no; nothing (an optional parameter left blank) as the
 * standard unset value.
 */
internal fun displayedParameterValue(
    parameter: FydeScriptParameter,
    value: FydeScriptParameterValue?,
    labels: ParameterValueLabels,
): String = when {
    value == null -> labels.unset
    parameter.secret -> labels.hidden
    value is FydeScriptParameterValue.Bool -> if (value.value) labels.yes else labels.no
    else -> formText(value).ifBlank { labels.unset }
}

/**
 * A question a running script asks the user (a one-time code, a security
 * answer), shown above whatever screen is open. Cancel (or dismissing)
 * answers with an empty string, which the script treats as no answer.
 */
@Composable
internal fun ScraperQuestionDialog(scriptName: String, question: String, onAnswer: (String) -> Unit) {
    var answer by remember(question) { mutableStateOf("") }
    AppDialog(
        onDismissRequest = { onAnswer("") },
        title = stringResource(Res.string.scraper_question_title, scriptName),
        text = question,
        confirmLabel = stringResource(Res.string.scraper_answer_send),
        onConfirm = { onAnswer(answer) },
        confirmEnabled = answer.isNotBlank(),
        dismissLabel = stringResource(Res.string.cancel),
        onDismiss = { onAnswer("") },
    ) {
        AppTextField(
            value = answer,
            onValueChange = { answer = it },
            label = stringResource(Res.string.scraper_answer_label),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (answer.isNotBlank()) onAnswer(answer) }),
        )
    }
}

/**
 * The order a script's parameters are asked in: required before optional,
 * plain before secret (so a username comes before its password), then by
 * label. The order the script's author declared them in would be better
 * still, but it doesn't survive the trip here — parameters cross fyde-sdk's
 * FFI as a hash map, which has no order — so this is the most useful order
 * available from the fields themselves.
 */
internal fun orderedParameters(parameters: Map<String, FydeScriptParameter>): List<Map.Entry<String, FydeScriptParameter>> =
    parameters.entries.sortedWith(
        compareByDescending<Map.Entry<String, FydeScriptParameter>> { it.value.required }
            .thenBy { it.value.secret }
            .thenBy { it.value.label.lowercase() },
    )

/** The inverse of [typedParameterValues]: each installed value as the text its form field holds. */
internal fun formValues(values: Map<String, FydeScriptParameterValue>): Map<String, String> =
    values.mapValues { (_, value) -> formText(value) }

/** One value as the text its form field holds; a whole number without a trailing ".0". */
internal fun formText(value: FydeScriptParameterValue): String = when (value) {
    is FydeScriptParameterValue.Text -> value.value
    is FydeScriptParameterValue.Bool -> value.value.toString()
    is FydeScriptParameterValue.Number -> {
        val asLong = value.value.toLong()
        if (asLong.toDouble() == value.value) asLong.toString() else value.value.toString()
    }
}

/**
 * The form's raw [values] as typed values per [parameters]' declared types
 * (see [typedParameterValues]), or `null` while the form isn't valid: a
 * required parameter is blank, or a number field holds something that isn't
 * a finite number.
 */
internal fun validatedParameterValues(
    parameters: Map<String, FydeScriptParameter>,
    values: Map<String, String>,
): Map<String, FydeScriptParameterValue>? {
    val valid = parameters.all { (key, parameter) ->
        val value = values[key].orEmpty()
        val blank = value.isBlank()
        // A boolean is never blank in effect: an untouched switch is `false`.
        (!blank || !parameter.required || parameter.type == FydeScriptParameterType.BOOLEAN) &&
            (parameter.type != FydeScriptParameterType.NUMBER || blank || value.toFiniteDoubleOrNull() != null)
    }
    return if (valid) typedParameterValues(parameters, values) else null
}

/** The secret parameters a source is already installed with: their field may stay blank to keep them. */
internal fun keptSecretKeys(
    parameters: Map<String, FydeScriptParameter>,
    installed: Map<String, FydeScriptParameterValue>,
): Set<String> = parameters.filter { (key, parameter) -> parameter.secret && key in installed }.keys

/**
 * Like [validatedParameterValues], for a source being reinstalled: a secret
 * left blank keeps its [installed] value instead of failing as required.
 */
internal fun validatedCredentials(
    parameters: Map<String, FydeScriptParameter>,
    values: Map<String, String>,
    installed: Map<String, FydeScriptParameterValue>,
): Map<String, FydeScriptParameterValue>? {
    val kept = keptSecretKeys(parameters, installed)
    val relaxed = parameters.mapValues { (key, parameter) -> if (key in kept) parameter.copy(required = false) else parameter }
    val typed = validatedParameterValues(relaxed, values) ?: return null
    return typed + kept.filter { values[it].isNullOrBlank() }.associateWith { installed.getValue(it) }
}

/** Like [String.toDoubleOrNull], but also rejects `NaN`/`Infinity`, which JSON can't carry. */
internal fun String.toFiniteDoubleOrNull(): Double? = toDoubleOrNull()?.takeIf { it.isFinite() }

/**
 * Converts the form's raw [values] (one string per parameter machine name,
 * booleans as "true"/"false") into typed values per [parameters]' declared
 * types, for [FydeSdk.installScript]. A boolean is always included (an
 * untouched switch is `false`); a blank text/number value is left out, so
 * an optional parameter the user skipped is absent rather than empty.
 * Assumes [values] already passed the form's validation.
 */
internal fun typedParameterValues(
    parameters: Map<String, FydeScriptParameter>,
    values: Map<String, String>,
): Map<String, FydeScriptParameterValue> = buildMap {
    parameters.forEach { (key, parameter) ->
        val value = values[key].orEmpty()
        when (parameter.type) {
            FydeScriptParameterType.BOOLEAN ->
                put(key, FydeScriptParameterValue.Bool(value.toBooleanStrictOrNull() ?: false))
            FydeScriptParameterType.NUMBER ->
                value.toFiniteDoubleOrNull()?.let { put(key, FydeScriptParameterValue.Number(it)) }
            FydeScriptParameterType.STRING ->
                if (value.isNotBlank()) put(key, FydeScriptParameterValue.Text(value))
        }
    }
}
