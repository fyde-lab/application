package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.form.AppSwitch
import com.peltoche.fyde.components.form.AppTextField
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.components.overlay.AppDialog
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.script_about_section
import com.peltoche.fyde.resources.document_detail_value_unset
import com.peltoche.fyde.resources.script_domains_installed
import com.peltoche.fyde.resources.script_domains_not_installed
import com.peltoche.fyde.resources.script_edit_parameters_button
import com.peltoche.fyde.resources.script_edit_parameters_title
import com.peltoche.fyde.resources.script_install_button
import com.peltoche.fyde.resources.script_install_dialog_text
import com.peltoche.fyde.resources.script_install_dialog_title
import com.peltoche.fyde.resources.script_parameter_hidden
import com.peltoche.fyde.resources.script_parameter_invalid_number
import com.peltoche.fyde.resources.script_parameter_no
import com.peltoche.fyde.resources.script_parameter_required
import com.peltoche.fyde.resources.script_parameter_yes
import com.peltoche.fyde.resources.script_parameters_section
import com.peltoche.fyde.resources.script_save_button
import com.peltoche.fyde.resources.script_uninstall_button
import com.peltoche.fyde.resources.script_uninstall_confirm_text
import com.peltoche.fyde.resources.script_uninstall_confirm_title
import com.peltoche.fyde.resources.scripts_title
import com.peltoche.fyde.resources.back
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.input.ImeAction
import com.peltoche.fyde.components.content.AppCard
import com.peltoche.fyde.resources.script_run_button
import com.peltoche.fyde.resources.script_run_starting
import com.peltoche.fyde.resources.scraper_answer_label
import com.peltoche.fyde.resources.scraper_answer_send
import com.peltoche.fyde.resources.scraper_question_title
import org.jetbrains.compose.resources.stringResource

/**
 * Where a run of this script stands (see [ScraperRuns]): [isRunning] while
 * it's this one running, [canRun] unless some other run is in progress (one
 * at a time), the current [step]/[update] for the progress card, and the
 * last run's [failure] message.
 */
data class ScriptRunState(
    val isRunning: Boolean = false,
    val canRun: Boolean = true,
    val step: String? = null,
    val update: FydeScraperProgress.Update? = null,
    val failure: String? = null,
)

/** Keeps an action button a deliberate target, not a word-sized one, when it isn't full width. */
private val ActionButtonMinWidth = 160.dp

/**
 * Drill-down detail view opened by clicking an [ScriptsScreen] card: header
 * (icon, name, version line), the values it's installed with, the sites it
 * may access, the install/run/uninstall actions, then the full description.
 *
 * A script's declared [FydeScript.parameters] are asked for in a form
 * dialog ([ScriptParametersDialog]): Install opens it, and only a form that
 * passes validation installs the script, with those values — the ones every
 * run then uses (see [FydeSdk.runInstalledScript]). A script without
 * parameters (every parser) has nothing to ask, so it installs straight
 * away. Once installed, the values show read-only ([InstalledParameters],
 * secrets masked) and Edit parameters reopens the form pre-filled from
 * [installedParameters]; saving reinstalls with the new values (an
 * installation is keyed by script, so this replaces the previous one).
 *
 * Found via review, this screen used to:
 * - put the "this script can access these sites" disclosure *below* the
 *   Install button, so it was read after agreeing to it;
 * - sort parameters alphabetically by label (see [orderedParameters]);
 * - uninstall on a single tap, with no confirmation.
 */
@Composable
fun ScriptDetailScreen(
    script: FydeScript,
    isInstalled: Boolean,
    isPending: Boolean,
    onBack: () -> Unit,
    onInstall: (parameters: Map<String, FydeScriptParameterValue>) -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
    installedParameters: Map<String, FydeScriptParameterValue>? = null,
    run: ScriptRunState = ScriptRunState(),
    onRun: () -> Unit = {},
    onDismissRunFailure: () -> Unit = {},
    banner: String? = null,
    onDismissBanner: () -> Unit = {},
    onOpenDocuments: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val windowSizeClass = LocalAppWindowSizeClass.current
    // Nothing about the installation changes while it's running.
    val isBusy = isPending || run.isRunning

    // The form's values, one string per parameter machine name (booleans as
    // "true"/"false", so a single map covers every parameter type). Held
    // here rather than in the dialog so a failed install — the dialog has
    // closed by then — can be retried without typing everything again; keyed
    // on script.id so another script's detail view starts from its own.
    val parameterValues = remember(script.id) { mutableStateMapOf<String, String>() }
    var editingParameters by remember(script.id) { mutableStateOf(false) }
    var confirmingUninstall by remember(script.id) { mutableStateOf(false) }

    // A required parameter the installation has no value for (the script
    // gained one since, or the installation hasn't synced here yet) would
    // fail the run, so filling it in comes first.
    val missingRequired = isInstalled && script.parameters.any { (key, parameter) ->
        parameter.required && installedParameters?.containsKey(key) != true
    }

    fun install() {
        if (script.parameters.isEmpty()) {
            onInstall(emptyMap())
            return
        }
        // Reinstalling after an uninstall starts from the values it had.
        if (parameterValues.isEmpty()) installedParameters?.let { parameterValues.putAll(formValues(it)) }
        editingParameters = true
    }

    fun editParameters() {
        parameterValues.clear()
        parameterValues.putAll(formValues(installedParameters.orEmpty()))
        editingParameters = true
    }

    TopLevelScaffold(
        current = SidebarDestination.Scripts,
        onOpenDocuments = onOpenDocuments,
        // Re-selecting Scripts from its own drill-down returns to the grid.
        onOpenScripts = onBack,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.scripts_title),
                onBack = onBack,
                backContentDescription = stringResource(Res.string.back),
                windowSizeClass = windowSizeClass,
                width = AppContentWidth.Detail,
                sidebarAdjacent = windowSizeClass == AppWindowSizeClass.Expanded,
            )
        },
    ) { contentPadding ->
        AppContentContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(AppColors.background),
            width = AppContentWidth.Detail,
            windowSizeClass = windowSizeClass,
            sidebarAdjacent = windowSizeClass == AppWindowSizeClass.Expanded,
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                banner?.let { message ->
                    AppBanner(message = message, onDismiss = onDismissBanner, dismissLabel = stringResource(Res.string.dismiss))
                }
                run.failure?.let { message ->
                    AppBanner(message = message, onDismiss = onDismissRunFailure, dismissLabel = stringResource(Res.string.dismiss))
                }

                Spacer(Modifier.height(AppSpacing.space7))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScriptIcon(icon = script.icon, size = 64.dp)
                    Spacer(Modifier.width(AppSpacing.space4))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = script.name, style = AppTypography.title, color = AppColors.text)
                        Spacer(Modifier.height(AppSpacing.space1))
                        Text(text = scriptVersionLine(script), style = AppTypography.data, color = AppColors.textMuted)
                    }
                }

                if (run.isRunning) {
                    Spacer(Modifier.height(AppSpacing.space5))
                    RunProgressCard(step = run.step, update = run.update)
                }

                if (isInstalled && script.parameters.isNotEmpty()) {
                    SectionTitle(stringResource(Res.string.script_parameters_section))
                    InstalledParameters(parameters = script.parameters, values = installedParameters.orEmpty())
                }

                // A permission disclosure, so it comes before the action it
                // applies to, never after.
                if (script.allowedDomains.isNotEmpty()) {
                    Spacer(Modifier.height(AppSpacing.space5))
                    AllowedDomains(domains = script.allowedDomains, isInstalled = isInstalled)
                }

                Spacer(Modifier.height(AppSpacing.space6))
                // Full width only where the column itself is phone-narrow.
                val buttonModifier = if (windowSizeClass == AppWindowSizeClass.Compact) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.widthIn(min = ActionButtonMinWidth)
                }
                if (isInstalled) {
                    val actions: @Composable (Modifier) -> Unit = { itemModifier ->
                        // Only scrapers are run on demand; a parser runs by
                        // itself on every upload. A run always uses the
                        // installed values, so with a required one missing,
                        // Edit parameters takes Run's place as the primary
                        // action.
                        if (script.type == FydeScriptType.SCRAPER && !missingRequired) {
                            AppButton(
                                text = stringResource(Res.string.script_run_button),
                                onClick = onRun,
                                enabled = run.canRun && !isPending,
                                loading = run.isRunning,
                                leadingIcon = { PlayGlyph(modifier = Modifier.size(12.dp), tint = AppColors.onPrimary) },
                                modifier = itemModifier,
                            )
                        }
                        if (script.parameters.isNotEmpty()) {
                            AppButton(
                                text = stringResource(Res.string.script_edit_parameters_button),
                                onClick = ::editParameters,
                                variant = if (missingRequired) AppButtonVariant.Filled else AppButtonVariant.Outlined,
                                enabled = !run.isRunning,
                                loading = isPending,
                                modifier = itemModifier,
                            )
                        }
                        AppButton(
                            text = stringResource(Res.string.script_uninstall_button),
                            onClick = { confirmingUninstall = true },
                            variant = AppButtonVariant.Outlined,
                            enabled = !isBusy,
                            destructive = true,
                            modifier = itemModifier,
                        )
                    }
                    if (windowSizeClass == AppWindowSizeClass.Compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) { actions(buttonModifier) }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) { actions(buttonModifier) }
                    }
                } else {
                    AppButton(
                        text = stringResource(Res.string.script_install_button),
                        onClick = ::install,
                        enabled = !isPending,
                        loading = isPending,
                        modifier = buttonModifier,
                    )
                }

                if (script.description.isNotBlank()) {
                    SectionTitle(stringResource(Res.string.script_about_section))
                    Text(text = script.description, style = AppTypography.body, color = AppColors.text)
                }

                Spacer(Modifier.height(AppSpacing.space7))
            }
        }
    }

    if (editingParameters) {
        ScriptParametersDialog(
            title = stringResource(
                if (isInstalled) Res.string.script_edit_parameters_title else Res.string.script_install_dialog_title,
                script.name,
            ),
            confirmLabel = stringResource(if (isInstalled) Res.string.script_save_button else Res.string.script_install_button),
            parameters = script.parameters,
            values = parameterValues,
            onSubmit = { values ->
                editingParameters = false
                // Saving the values it's already installed with has nothing to publish.
                if (!isInstalled || values != installedParameters) onInstall(values)
            },
            onDismiss = { editingParameters = false },
        )
    }

    if (confirmingUninstall) {
        AppDialog(
            onDismissRequest = { confirmingUninstall = false },
            title = stringResource(Res.string.script_uninstall_confirm_title, script.name),
            text = stringResource(Res.string.script_uninstall_confirm_text),
            confirmLabel = stringResource(Res.string.script_uninstall_button),
            onConfirm = {
                confirmingUninstall = false
                onUninstall()
            },
            dismissLabel = stringResource(Res.string.cancel),
            onDismiss = { confirmingUninstall = false },
            destructive = true,
        )
    }
}

/**
 * The form a script's [parameters] are entered in: one field per parameter,
 * in [orderedParameters] order — a switch for a boolean, a text field
 * otherwise (masked for a secret, numeric keyboard for a number). Confirming
 * hands [onSubmit] the typed values only once the form is valid (see
 * [validatedParameterValues]); until then it flags each field that's missing
 * or malformed. [values] belongs to the caller and is edited in place, see
 * [ScriptDetailScreen].
 */
@Composable
private fun ScriptParametersDialog(
    title: String,
    confirmLabel: String,
    parameters: Map<String, FydeScriptParameter>,
    values: SnapshotStateMap<String, String>,
    onSubmit: (Map<String, FydeScriptParameterValue>) -> Unit,
    onDismiss: () -> Unit,
) {
    // Required-but-empty errors only surface once the user has tried to
    // submit with the form incomplete — showing them up front would make
    // every parameter look like a validation failure the instant it opens.
    var showRequiredErrors by remember { mutableStateOf(false) }
    val requiredError = stringResource(Res.string.script_parameter_required)
    val invalidNumberError = stringResource(Res.string.script_parameter_invalid_number)

    fun errorFor(parameter: FydeScriptParameter, value: String): String? = when {
        value.isBlank() -> if (parameter.required && showRequiredErrors) requiredError else null
        parameter.type == FydeScriptParameterType.NUMBER && value.toFiniteDoubleOrNull() == null -> invalidNumberError
        else -> null
    }

    fun submit() {
        val entered = validatedParameterValues(parameters, values)
        if (entered != null) onSubmit(entered) else showRequiredErrors = true
    }

    AppDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = stringResource(Res.string.script_install_dialog_text),
        confirmLabel = confirmLabel,
        onConfirm = ::submit,
        dismissLabel = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
    ) {
        // Scrolls by itself: a script can declare more fields than a phone
        // screen holds, and the dialog's title and buttons must stay put.
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.space3),
        ) {
            orderedParameters(parameters).forEach { (key, parameter) ->
                val value = values[key].orEmpty()
                if (parameter.type == FydeScriptParameterType.BOOLEAN) {
                    AppSwitch(
                        checked = value.toBooleanStrictOrNull() ?: false,
                        onCheckedChange = { checked -> values[key] = checked.toString() },
                        label = parameter.label,
                    )
                } else {
                    AppTextField(
                        value = value,
                        onValueChange = { values[key] = it },
                        label = parameter.label,
                        placeholder = parameter.placeholder.ifBlank { null },
                        errorText = errorFor(parameter, value),
                        isPassword = parameter.secret,
                        keyboardOptions = if (parameter.type == FydeScriptParameterType.NUMBER) {
                            KeyboardOptions(keyboardType = KeyboardType.Number)
                        } else {
                            KeyboardOptions.Default
                        },
                    )
                }
            }
        }
    }
}

/**
 * The values an installed script runs with, as an information grid (see
 * `design/design-system.md`'s Detail panes) in [orderedParameters] order —
 * see [displayedParameterValue] for how each value reads.
 */
@Composable
private fun InstalledParameters(parameters: Map<String, FydeScriptParameter>, values: Map<String, FydeScriptParameterValue>) {
    val labels = ParameterValueLabels(
        hidden = stringResource(Res.string.script_parameter_hidden),
        yes = stringResource(Res.string.script_parameter_yes),
        no = stringResource(Res.string.script_parameter_no),
        unset = stringResource(Res.string.document_detail_value_unset),
    )
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space4)) {
        orderedParameters(parameters).chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (key, parameter) ->
                    DetailCell(
                        label = parameter.label,
                        value = displayedParameterValue(parameter, values[key], labels),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** The localized words [displayedParameterValue] reads a value as. */
internal data class ParameterValueLabels(val hidden: String, val yes: String, val no: String, val unset: String)

/**
 * How an installed [value] reads on the script's page: a secret never
 * shows, only that it's set; a boolean as yes/no; nothing (an optional
 * parameter left blank) as the standard unset value.
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
 * A running script's progress: the current step's localized wording (or
 * "Starting…" before the script announces one) and a bar —
 * determinate when the script says how many items there are, indeterminate
 * otherwise.
 */
@Composable
private fun RunProgressCard(step: String?, update: FydeScraperProgress.Update?) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = step ?: stringResource(Res.string.script_run_starting),
                style = AppTypography.bodyEmphasis,
                color = AppColors.text,
            )
            Spacer(Modifier.height(AppSpacing.space3))
            if (update != null && update.total > 0) {
                LinearProgressIndicator(
                    progress = { (update.current.toFloat() / update.total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.primary,
                    trackColor = AppColors.border,
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AppColors.primary, trackColor = AppColors.border)
            }
        }
    }
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
 * The domains this script may reach — a permission disclosure,
 * worded for whether installing grants it or it's already granted.
 * Hostnames are exact identifiers, so they're set in the Data role.
 */
@Composable
private fun AllowedDomains(domains: List<String>, isInstalled: Boolean) {
    Column {
        Text(
            text = stringResource(if (isInstalled) Res.string.script_domains_installed else Res.string.script_domains_not_installed),
            style = AppTypography.caption,
            color = AppColors.textMuted,
        )
        Spacer(Modifier.height(AppSpacing.space2))
        domains.forEach { domain ->
            Text(
                text = domain,
                style = AppTypography.data,
                color = AppColors.text,
                modifier = Modifier.padding(vertical = AppSpacing.space1),
            )
        }
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

/** Like [String.toDoubleOrNull], but also rejects `NaN`/`Infinity`, which JSON can't carry. */
private fun String.toFiniteDoubleOrNull(): Double? = toDoubleOrNull()?.takeIf { it.isFinite() }

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
