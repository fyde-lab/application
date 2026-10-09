package com.peltoche.fyde

import androidx.compose.foundation.background
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
import com.peltoche.fyde.resources.addon_uninstall_button
import com.peltoche.fyde.resources.addons_title
import com.peltoche.fyde.resources.back
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/** Keeps the install/uninstall button a deliberate target, not a word-sized one, when it isn't full width. */
private val ActionButtonMinWidth = 160.dp

/**
 * Drill-down detail view opened by clicking an [AddonsScreen] card: header
 * (icon, name, version line), a form for the script's declared
 * [FydeScript.parameters] while it isn't installed yet, the
 * install/uninstall action with the domains it grants access to right
 * beside it, then the full description.
 *
 * Composed in the same [TopLevelScaffold] as [AddonsScreen], with Add-ons
 * still selected, so navigation stays put; the top bar names the parent
 * ("Add-ons") with a back chevron, and the add-on's own name appears once,
 * in the header. Found via review — this screen used to:
 * - drop the sidebar entirely and skip `AppContentContainer`, so on desktop
 *   the description ran edge-to-edge and the full-width button stretched
 *   across the whole window;
 * - pin the action to the bottom of a non-scrolling column, so a long
 *   description pushed it off-screen with no way to reach it;
 * - not render [banner], so an install/uninstall failure triggered from
 *   here was silent (the spinner just stopped);
 * - repeat the name in both the top bar and the header.
 */
@Composable
fun AddonDetailScreen(
    script: FydeScript,
    isInstalled: Boolean,
    isPending: Boolean,
    onBack: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
    banner: String? = null,
    onDismissBanner: () -> Unit = {},
    onOpenDocuments: () -> Unit = {},
    onOpenScripts: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val windowSizeClass = LocalAppWindowSizeClass.current

    // One text value per parameter (machine name -> entered value), keyed
    // on script.id so switching to a different script's detail view starts
    // from a blank form rather than carrying over the previous script's
    // entries. Booleans are stored as "true"/"false" strings so a single
    // map covers every parameter type.
    val parameterValues = remember(script.id) { mutableStateMapOf<String, String>() }
    // Required-but-empty errors only surface once the user has tried to
    // install with the form incomplete — showing them up front, before any
    // input, would make every parameter look like a validation failure the
    // instant the screen opens.
    var showRequiredErrors by remember(script.id) { mutableStateOf(false) }

    val requiredError = stringResource(Res.string.addon_parameter_required)
    val invalidNumberError = stringResource(Res.string.addon_parameter_invalid_number)

    fun errorFor(parameter: FydeScriptParameter, value: String): String? = when {
        value.isBlank() -> if (parameter.required && showRequiredErrors) requiredError else null
        parameter.type == FydeScriptParameterType.NUMBER && value.toDoubleOrNull() == null -> invalidNumberError
        else -> null
    }

    val parametersValid = script.parameters.all { (key, parameter) ->
        val value = parameterValues[key].orEmpty()
        val blank = value.isBlank()
        (!blank || !parameter.required) &&
            (parameter.type != FydeScriptParameterType.NUMBER || blank || value.toDoubleOrNull() != null)
    }

    fun attemptInstall() {
        if (parametersValid) {
            onInstall()
        } else {
            showRequiredErrors = true
        }
    }

    TopLevelScaffold(
        current = SidebarDestination.Addons,
        onOpenDocuments = onOpenDocuments,
        onOpenScripts = onOpenScripts,
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

                if (script.parameters.isNotEmpty() && !isInstalled) {
                    SectionTitle(stringResource(Res.string.addon_parameters_section))
                    script.parameters.entries.sortedBy { it.value.label }.forEach { (key, parameter) ->
                        val value = parameterValues[key].orEmpty()
                        if (parameter.type == FydeScriptParameterType.BOOLEAN) {
                            AppSwitch(
                                checked = value.toBooleanStrictOrNull() ?: false,
                                onCheckedChange = { checked -> parameterValues[key] = checked.toString() },
                                label = parameter.label,
                                enabled = !isPending,
                            )
                        } else {
                            AppTextField(
                                value = value,
                                onValueChange = { parameterValues[key] = it },
                                label = parameter.label,
                                placeholder = parameter.placeholder.ifBlank { null },
                                errorText = errorFor(parameter, value),
                                isPassword = parameter.secret,
                                enabled = !isPending,
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

                Spacer(Modifier.height(AppSpacing.space6))
                InstallAction(
                    isInstalled = isInstalled,
                    isPending = isPending,
                    onInstall = ::attemptInstall,
                    onUninstall = onUninstall,
                    // Full width only where the column itself is phone-narrow.
                    modifier = if (windowSizeClass == AppWindowSizeClass.Compact) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.widthIn(min = ActionButtonMinWidth)
                    },
                )

                if (script.allowedDomains.isNotEmpty()) {
                    Spacer(Modifier.height(AppSpacing.space5))
                    AllowedDomains(domains = script.allowedDomains, isInstalled = isInstalled)
                }

                if (script.description.isNotBlank()) {
                    SectionTitle(stringResource(Res.string.addon_about_section))
                    Text(text = script.description, style = AppTypography.body, color = AppColors.text)
                }

                Spacer(Modifier.height(AppSpacing.space7))
            }
        }
    }
}

@Composable
private fun InstallAction(
    isInstalled: Boolean,
    isPending: Boolean,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isInstalled) {
        AppButton(
            text = stringResource(Res.string.addon_uninstall_button),
            onClick = onUninstall,
            variant = AppButtonVariant.Outlined,
            enabled = !isPending,
            loading = isPending,
            modifier = modifier,
        )
    } else {
        AppButton(
            text = stringResource(Res.string.addon_install_button),
            onClick = onInstall,
            variant = AppButtonVariant.Filled,
            enabled = !isPending,
            loading = isPending,
            modifier = modifier,
        )
    }
}

/**
 * The domains this add-on's script may reach — a permission disclosure, so
 * it sits directly under the install action and says what it means, rather
 * than trailing the description as an unexplained "Allowed domains" list.
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
