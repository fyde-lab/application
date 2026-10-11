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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.peltoche.fyde.resources.addon_about_section
import com.peltoche.fyde.resources.addon_domains_installed
import com.peltoche.fyde.resources.addon_domains_not_installed
import com.peltoche.fyde.resources.addon_install_button
import com.peltoche.fyde.resources.addon_parameter_invalid_number
import com.peltoche.fyde.resources.addon_parameter_required
import com.peltoche.fyde.resources.addon_parameters_section
import com.peltoche.fyde.resources.addon_save_button
import com.peltoche.fyde.resources.addon_uninstall_button
import com.peltoche.fyde.resources.addon_uninstall_confirm_text
import com.peltoche.fyde.resources.addon_uninstall_confirm_title
import com.peltoche.fyde.resources.addons_title
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
import com.peltoche.fyde.resources.addon_run_button
import com.peltoche.fyde.resources.addon_run_starting
import com.peltoche.fyde.resources.scraper_answer_label
import com.peltoche.fyde.resources.scraper_answer_send
import com.peltoche.fyde.resources.scraper_question_title
import org.jetbrains.compose.resources.stringResource

/**
 * Where a run of this add-on stands (see [ScraperRuns]): [isRunning] while
 * it's this one running, [canRun] unless some other run is in progress (one
 * at a time), the current [step]/[update] for the progress card, and the
 * last run's [failure] message.
 */
data class AddonRunState(
    val isRunning: Boolean = false,
    val canRun: Boolean = true,
    val step: String? = null,
    val update: FydeScraperProgress.Update? = null,
    val failure: String? = null,
)

/** Keeps an action button a deliberate target, not a word-sized one, when it isn't full width. */
private val ActionButtonMinWidth = 160.dp

/**
 * Drill-down detail view opened by clicking an [AddonsScreen] card: header
 * (icon, name, version line), a form for the script's declared
 * [FydeScript.parameters], the sites it may access, the install/save/
 * uninstall actions, then the full description.
 *
 * Found via review, this screen used to:
 * - put the "this add-on can access these sites" disclosure *below* the
 *   Install button, so it was read after agreeing to it;
 * - drop the parameter form once installed, so credentials could only be
 *   changed by uninstalling and installing again — now the form stays,
 *   pre-filled from [installedParameters], and Save changes reinstalls with
 *   the new values (an installation is keyed by script, so this replaces
 *   the previous one);
 * - sort parameters alphabetically by label (see [orderedParameters]);
 * - uninstall on a single tap, with no confirmation.
 */
@Composable
fun AddonDetailScreen(
    script: FydeScript,
    isInstalled: Boolean,
    isPending: Boolean,
    onBack: () -> Unit,
    onInstall: (parameters: Map<String, FydeScriptParameterValue>) -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
    installedParameters: Map<String, FydeScriptParameterValue>? = null,
    run: AddonRunState = AddonRunState(),
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

    // One text value per parameter (machine name -> entered value), keyed
    // on script.id so switching to a different script's detail view starts
    // from its own values rather than carrying over the previous script's.
    // Booleans are stored as "true"/"false" strings so a single map covers
    // every parameter type.
    val parameterValues = remember(script.id) { mutableStateMapOf<String, String>() }
    // Pre-fills the form with the values the script is installed with, once
    // they're known — and again after a save, so the form matches them.
    LaunchedEffect(script.id, installedParameters) {
        if (installedParameters != null) {
            parameterValues.clear()
            parameterValues.putAll(formValues(installedParameters))
        }
    }
    // Required-but-empty errors only surface once the user has tried to
    // submit with the form incomplete — showing them up front would make
    // every parameter look like a validation failure the instant the screen
    // opens.
    var showRequiredErrors by remember(script.id) { mutableStateOf(false) }
    var confirmingUninstall by remember(script.id) { mutableStateOf(false) }

    val requiredError = stringResource(Res.string.addon_parameter_required)
    val invalidNumberError = stringResource(Res.string.addon_parameter_invalid_number)

    fun errorFor(parameter: FydeScriptParameter, value: String): String? = when {
        value.isBlank() -> if (parameter.required && showRequiredErrors) requiredError else null
        parameter.type == FydeScriptParameterType.NUMBER && value.toFiniteDoubleOrNull() == null -> invalidNumberError
        else -> null
    }

    val parametersValid = script.parameters.all { (key, parameter) ->
        val value = parameterValues[key].orEmpty()
        val blank = value.isBlank()
        (!blank || !parameter.required) &&
            (parameter.type != FydeScriptParameterType.NUMBER || blank || value.toFiniteDoubleOrNull() != null)
    }
    val enteredValues = if (parametersValid) typedParameterValues(script.parameters, parameterValues) else null
    // Nothing to save for a script without parameters (every parser), and
    // no installation yet reads as "nothing entered" rather than a change.
    val hasChanges = script.parameters.isNotEmpty() && enteredValues != (installedParameters ?: emptyMap<String, FydeScriptParameterValue>())

    fun submit() {
        if (enteredValues != null) onInstall(enteredValues) else showRequiredErrors = true
    }

    TopLevelScaffold(
        current = SidebarDestination.Addons,
        onOpenDocuments = onOpenDocuments,
        // Re-selecting Add-ons from its own drill-down returns to the grid.
        onOpenAddons = onBack,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.addons_title),
                onBack = onBack,
                backContentDescription = stringResource(Res.string.back),
                windowSizeClass = windowSizeClass,
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
                    AddonIcon(icon = script.icon, size = 64.dp)
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

                if (script.parameters.isNotEmpty()) {
                    SectionTitle(stringResource(Res.string.addon_parameters_section))
                    orderedParameters(script.parameters).forEach { (key, parameter) ->
                        val value = parameterValues[key].orEmpty()
                        if (parameter.type == FydeScriptParameterType.BOOLEAN) {
                            AppSwitch(
                                checked = value.toBooleanStrictOrNull() ?: false,
                                onCheckedChange = { checked -> parameterValues[key] = checked.toString() },
                                label = parameter.label,
                                enabled = !isBusy,
                            )
                        } else {
                            AppTextField(
                                value = value,
                                onValueChange = { parameterValues[key] = it },
                                label = parameter.label,
                                placeholder = parameter.placeholder.ifBlank { null },
                                errorText = errorFor(parameter, value),
                                isPassword = parameter.secret,
                                enabled = !isBusy,
                                keyboardOptions = if (parameter.type == FydeScriptParameterType.NUMBER) {
                                    KeyboardOptions(keyboardType = KeyboardType.Number)
                                } else {
                                    KeyboardOptions.Default
                                },
                            )
                        }
                        Spacer(Modifier.height(AppSpacing.space3))
                    }
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
                        // Edited values are saved before anything else: a
                        // run always uses what's installed, so offering Run
                        // next to unsaved changes would run the old ones.
                        // Only scrapers are run on demand; a parser runs by
                        // itself on every upload.
                        if (hasChanges || (script.parameters.isNotEmpty() && !parametersValid)) {
                            AppButton(
                                text = stringResource(Res.string.addon_save_button),
                                onClick = ::submit,
                                enabled = !run.isRunning,
                                loading = isPending,
                                modifier = itemModifier,
                            )
                        } else if (script.type == FydeScriptType.SCRAPER) {
                            AppButton(
                                text = stringResource(Res.string.addon_run_button),
                                onClick = onRun,
                                enabled = run.canRun && !isPending,
                                loading = run.isRunning,
                                leadingIcon = { PlayGlyph(modifier = Modifier.size(12.dp), tint = AppColors.onPrimary) },
                                modifier = itemModifier,
                            )
                        }
                        AppButton(
                            text = stringResource(Res.string.addon_uninstall_button),
                            onClick = { confirmingUninstall = true },
                            variant = AppButtonVariant.Outlined,
                            enabled = !isBusy,
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
                        text = stringResource(Res.string.addon_install_button),
                        onClick = ::submit,
                        enabled = !isPending,
                        loading = isPending,
                        modifier = buttonModifier,
                    )
                }

                if (script.description.isNotBlank()) {
                    SectionTitle(stringResource(Res.string.addon_about_section))
                    Text(text = script.description, style = AppTypography.body, color = AppColors.text)
                }

                Spacer(Modifier.height(AppSpacing.space7))
            }
        }
    }

    if (confirmingUninstall) {
        AppDialog(
            onDismissRequest = { confirmingUninstall = false },
            title = stringResource(Res.string.addon_uninstall_confirm_title, script.name),
            text = stringResource(Res.string.addon_uninstall_confirm_text),
            confirmLabel = stringResource(Res.string.addon_uninstall_button),
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
 * A running add-on's progress: the current step's name (or "Starting…"
 * before the script names one), its latest message, and a bar —
 * determinate when the script says how many items there are, indeterminate
 * otherwise.
 */
@Composable
private fun RunProgressCard(step: String?, update: FydeScraperProgress.Update?) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = step ?: stringResource(Res.string.addon_run_starting),
                style = AppTypography.bodyEmphasis,
                color = AppColors.text,
            )
            update?.message?.takeIf { it.isNotBlank() }?.let { message ->
                Spacer(Modifier.height(AppSpacing.space1))
                Text(text = message, style = AppTypography.caption, color = AppColors.textMuted)
            }
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
 * The domains this add-on's script may reach — a permission disclosure,
 * worded for whether installing grants it or it's already granted.
 * Hostnames are exact identifiers, so they're set in the Data role.
 */
@Composable
private fun AllowedDomains(domains: List<String>, isInstalled: Boolean) {
    Column {
        Text(
            text = stringResource(if (isInstalled) Res.string.addon_domains_installed else Res.string.addon_domains_not_installed),
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
    values.mapValues { (_, value) ->
        when (value) {
            is FydeScriptParameterValue.Text -> value.value
            is FydeScriptParameterValue.Bool -> value.value.toString()
            is FydeScriptParameterValue.Number -> {
                val asLong = value.value.toLong()
                if (asLong.toDouble() == value.value) asLong.toString() else value.value.toString()
            }
        }
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
