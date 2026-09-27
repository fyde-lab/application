package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.form.AppDropdown
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.back
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.resources.language_label
import com.peltoche.fyde.resources.language_section_title
import com.peltoche.fyde.resources.server_section_title
import com.peltoche.fyde.resources.server_url_label
import com.peltoche.fyde.resources.settings_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
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
 * Opened from [DocumentListScreen]'s profile dropdown. Mostly read-only —
 * the server it's connected to (see `App.kt`'s `FYDE_SERVER_URL`) — except
 * for [currentLanguage], which [onLanguageChange] lets the user override via
 * `fyde-sdk`'s `set_language` (see `App.kt`'s `Screen.Settings` branch for
 * where that call and [languageError] actually live).
 */
@Composable
fun SettingsScreen(
    serverUrl: String,
    currentLanguage: String?,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit,
    languageError: String? = null,
    onDismissLanguageError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background),
    ) {
        SettingsTopBar(onBack = onBack)
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
                .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space3),
        )
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimensions.topBarHeight)
            .background(AppColors.surfaceElevated),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = AppSpacing.space5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                // "←" stands in for an icon glyph, not language — exempt
                // from the Regular/Medium/SemiBold-only rule real text follows.
                Text(
                    text = "←",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.text,
                )
                Spacer(Modifier.width(AppSpacing.space2))
                Text(
                    text = stringResource(Res.string.back),
                    style = AppTypography.label,
                    color = AppColors.text,
                )
            }
            Spacer(Modifier.width(AppSpacing.space2))
            Text(
                text = stringResource(Res.string.settings_title),
                style = AppTypography.section,
                color = AppColors.text,
            )
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = AppColors.border,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = AppTypography.micro,
        color = AppColors.textMuted,
        modifier = Modifier.padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space4),
    )
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space3)) {
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
