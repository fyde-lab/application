package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.form.AppDropdown
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.components.overlay.AppDialog
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.account_section_title
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.resources.language_label
import com.peltoche.fyde.resources.language_section_title
import com.peltoche.fyde.resources.log_out
import com.peltoche.fyde.resources.log_out_confirm_text
import com.peltoche.fyde.resources.log_out_confirm_title
import com.peltoche.fyde.resources.settings_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * The app's two supported UI languages, each showing its own name in its own
 * language (an "endonym") rather than a translation of it — the standard
 * convention for a language picker, so a reader can find their language
 * regardless of whichever one currently happens to be active. [code] must
 * match one of the resource sets under `commonMain/composeResources`
 * (`values`/`values-fr`).
 */
private enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    FRENCH("fr", "Français"),
    ;

    companion object {
        fun fromCode(code: String?): AppLanguage? = entries.find { it.code == code }
    }
}

/**
 * The Settings destination (sidebar footer on desktop, a regular nav item
 * below that — see [TopLevelScaffold]). Mostly read-only, except for
 * [currentLanguage], which [onLanguageChange] lets the user override via
 * `fyde-sdk`'s `set_language` (see `App.kt`'s `Screen.Settings` branch for
 * where that call and [languageError] actually live), and [onLogout], which
 * ends the session. [planUsage] shows the account's plan and how much of
 * each of its limits it uses (see [PlanUsageSection]), and [sessions] the
 * devices connected to it, any other than this one disconnectable through
 * [onDisconnectSession] (see [SessionsSection]).
 *
 * Logging out wipes this device's copy of the account (see
 * [FydeSdk.logout]), so it asks first; [isLoggingOut] keeps the action
 * visibly busy until that finishes, and [logoutError] reports a failure to
 * clear local state, the one case that keeps the user here.
 *
 * A peer of the other top-level destinations, so its top bar matches theirs:
 * title only, no back chevron. Found via review: it used to carry a back
 * button (redundant beside the sidebar, and one that shrank the title from
 * Title to Section size, so moving between peer destinations made the
 * heading jump).
 */
@Composable
fun SettingsScreen(
    currentLanguage: String?,
    onLanguageChange: (String) -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenScripts: () -> Unit = {},
    onLogout: () -> Unit = {},
    isLoggingOut: Boolean = false,
    logoutError: String? = null,
    onDismissLogoutError: () -> Unit = {},
    languageError: String? = null,
    onDismissLanguageError: () -> Unit = {},
    planUsage: PlanUsageUiState = PlanUsageUiState.Loading,
    sessions: SessionsUiState = SessionsUiState.Loading,
    onDisconnectSession: (FydeOpenSession) -> Unit = {},
    disconnectingSessionId: String? = null,
    disconnectSessionError: String? = null,
    onDismissDisconnectSessionError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = LocalAppWindowSizeClass.current
    var confirmingLogout by remember { mutableStateOf(false) }

    TopLevelScaffold(
        current = TopLevelDestination.Settings,
        onOpenDocuments = onOpenDocuments,
        onOpenScripts = onOpenScripts,
        onOpenSettings = {},
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.settings_title),
                windowSizeClass = windowSizeClass,
                width = AppContentWidth.Detail,
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
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                listOfNotNull(
                    languageError?.let { it to onDismissLanguageError },
                    logoutError?.let { it to onDismissLogoutError },
                ).forEach { (message, onDismiss) ->
                    AppBanner(message = message, onDismiss = onDismiss, dismissLabel = stringResource(Res.string.dismiss))
                }
                SectionTitle(stringResource(Res.string.language_section_title))
                AppDropdown(
                    label = stringResource(Res.string.language_label),
                    options = AppLanguage.entries,
                    // With no saved preference the app follows the platform
                    // locale, so show that rather than an empty field — found
                    // via review that the dropdown read as unset even though
                    // the UI was already in a definite language.
                    selectedOption = AppLanguage.fromCode(currentLanguage)
                        ?: AppLanguage.fromCode(Locale.current.language)
                        ?: AppLanguage.ENGLISH,
                    onOptionSelected = { onLanguageChange(it.code) },
                    optionLabel = AppLanguage::displayName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.space3),
                )
                PlanUsageSection(planUsage)
                SessionsSection(
                    state = sessions,
                    onDisconnect = onDisconnectSession,
                    disconnectingId = disconnectingSessionId,
                    disconnectError = disconnectSessionError,
                    onDismissDisconnectError = onDismissDisconnectSessionError,
                )
                SectionTitle(stringResource(Res.string.account_section_title))
                EncryptionBadge()
                Spacer(Modifier.height(AppSpacing.space3))
                // An outlined button, not a text button: its label starts on
                // the content edge like everything above it, where a text
                // button's inner padding indented it ~12dp. Destructive, since
                // it leads to logging out (see design-system.md § Feedback).
                AppButton(
                    text = stringResource(Res.string.log_out),
                    onClick = { confirmingLogout = true },
                    variant = AppButtonVariant.Outlined,
                    loading = isLoggingOut,
                    destructive = true,
                )
            }
        }
    }

    if (confirmingLogout) {
        AppDialog(
            onDismissRequest = { confirmingLogout = false },
            title = stringResource(Res.string.log_out_confirm_title),
            text = stringResource(Res.string.log_out_confirm_text),
            confirmLabel = stringResource(Res.string.log_out),
            onConfirm = {
                confirmingLogout = false
                onLogout()
            },
            dismissLabel = stringResource(Res.string.cancel),
            onDismiss = { confirmingLogout = false },
            destructive = true,
        )
    }
}

// Unlike DocumentListScreen's rows, these two have no leading icon/visual
// weight of their own to balance against a margin, and sit entirely inside
// SettingsScreen's single AppContentContainer — so they take only vertical
// padding and let the container supply the one horizontal margin for the
// whole screen, rather than adding a second, redundant one of their own.
//
// Section role (not Micro): design-system.md's Section role is for
// group/section headers, matching the document detail pane's headings and
// the reference layout's "Informations extraites"/"Mots-clés".
@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTypography.section,
        color = AppColors.text,
        modifier = modifier.padding(top = AppSpacing.space7, bottom = AppSpacing.space3),
    )
}

/**
 * Retries a Settings section that failed to load, below its error banner.
 * Outlined, not a text button, so its label starts on the content edge
 * like the rest of the section (design-system.md § Layout, "The top bar
 * shares the content's edges").
 */
@Composable
internal fun RetryButton(onClick: () -> Unit) {
    AppButton(
        text = stringResource(Res.string.retry),
        onClick = onClick,
        variant = AppButtonVariant.Outlined,
        modifier = Modifier.padding(top = AppSpacing.space3),
    )
}

@Composable
internal fun SettingsRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.space3)) {
        Text(
            text = label,
            style = AppTypography.caption,
            color = AppColors.textMuted,
        )
        Text(
            text = value,
            style = AppTypography.body,
            color = AppColors.text,
        )
    }
}
