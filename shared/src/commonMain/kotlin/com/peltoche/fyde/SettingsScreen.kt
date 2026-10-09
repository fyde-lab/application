package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.form.AppDropdown
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.account_section_title
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.resources.language_label
import com.peltoche.fyde.resources.language_section_title
import com.peltoche.fyde.resources.log_out
import com.peltoche.fyde.resources.server_section_title
import com.peltoche.fyde.resources.server_url_label
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
 * below that — see [TopLevelScaffold]). Mostly read-only — the server it's
 * connected to (see `App.kt`'s `FYDE_SERVER_URL`) — except for
 * [currentLanguage], which [onLanguageChange] lets the user override via
 * `fyde-sdk`'s `set_language` (see `App.kt`'s `Screen.Settings` branch for
 * where that call and [languageError] actually live), and [onLogout], which
 * ends the session.
 *
 * A peer of the other top-level destinations, so its top bar matches theirs:
 * title only, no back chevron. Found via review: it used to carry a back
 * button (redundant beside the sidebar, and one that shrank the title from
 * Title to Section size, so moving between peer destinations made the
 * heading jump).
 */
@Composable
fun SettingsScreen(
    serverUrl: String,
    currentLanguage: String?,
    onLanguageChange: (String) -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenScripts: () -> Unit = {},
    onOpenAddons: () -> Unit = {},
    onLogout: () -> Unit = {},
    languageError: String? = null,
    onDismissLanguageError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = LocalAppWindowSizeClass.current

    TopLevelScaffold(
        current = SidebarDestination.Settings,
        onOpenDocuments = onOpenDocuments,
        onOpenScripts = onOpenScripts,
        onOpenAddons = onOpenAddons,
        onOpenSettings = {},
        modifier = modifier,
        topBar = { AppTopBar(title = stringResource(Res.string.settings_title), windowSizeClass = windowSizeClass) },
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
                if (languageError != null) {
                    AppBanner(
                        message = languageError,
                        onDismiss = onDismissLanguageError,
                        dismissLabel = stringResource(Res.string.dismiss),
                    )
                }
                SectionTitle(stringResource(Res.string.server_section_title))
                SettingsRow(label = stringResource(Res.string.server_url_label), value = serverUrl)
                SectionTitle(stringResource(Res.string.language_section_title))
                AppDropdown(
                    label = stringResource(Res.string.language_label),
                    options = AppLanguage.entries,
                    selectedOption = AppLanguage.fromCode(currentLanguage),
                    onOptionSelected = { onLanguageChange(it.code) },
                    optionLabel = AppLanguage::displayName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppSpacing.space3),
                )
                SectionTitle(stringResource(Res.string.account_section_title))
                AppTextButton(
                    text = stringResource(Res.string.log_out),
                    onClick = onLogout,
                    modifier = Modifier.padding(vertical = AppSpacing.space1),
                )
            }
        }
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

@Composable
private fun SettingsRow(label: String, value: String) {
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
