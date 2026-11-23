package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppIconTile
import com.peltoche.fyde.components.content.SealHero
import com.peltoche.fyde.icons.BoltGlyph
import com.peltoche.fyde.icons.LockGlyph
import com.peltoche.fyde.icons.SyncGlyph
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.CenteredScrollColumn
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.welcome_create_vault
import com.peltoche.fyde.resources.welcome_have_vault
import com.peltoche.fyde.resources.welcome_promise_devices
import com.peltoche.fyde.resources.welcome_promise_encrypted
import com.peltoche.fyde.resources.welcome_promise_fetched
import com.peltoche.fyde.resources.welcome_seal_description
import com.peltoche.fyde.resources.welcome_server
import com.peltoche.fyde.resources.welcome_title
import com.peltoche.fyde.resources.welcome_wide_subtitle
import com.peltoche.fyde.resources.welcome_wide_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Welcome: the seal, what Fyde promises, and the way in — [onCreateVault] to
 * [RegisterScreen], [onOpenVault] to [LoginScreen]. The first screen on a
 * device with no account open. Pure presentation, like [LoginScreen].
 *
 * At Compact it's O1 (`design/proposals/000-original-design/assets/screens/O1-bienvenue.png`). At Medium
 * and Expanded it's DO1 (`design/proposals/000-original-design/assets/screens/DO1-bienvenue.png`): the
 * seal and promises move to [SignInBrandPanel] (a mark-only [AuthBrandBand]
 * at Medium), and the form pane greets, offers both ways in and names
 * [serverName], the server this build talks to.
 */
@Composable
fun WelcomeScreen(
    serverName: String,
    onCreateVault: () -> Unit,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (LocalAppWindowSizeClass.current) {
        AppWindowSizeClass.Compact -> CompactWelcome(onCreateVault, onOpenVault, modifier)
        AppWindowSizeClass.Medium, AppWindowSizeClass.Expanded -> SignInSplitLayout(modifier) { formModifier ->
            WideWelcomeForm(serverName, onCreateVault, onOpenVault, formModifier)
        }
    }
}

/**
 * O1. The promises and title scroll when the window is too short for them;
 * the two actions stay pinned at the bottom, within the thumb's reach.
 */
@Composable
private fun CompactWelcome(
    onCreateVault: () -> Unit,
    onOpenVault: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background)
            .safeContentPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppDimensions.maxWidthDetail)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.space7),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(AppSpacing.space9))
                SealHero(contentDescription = stringResource(Res.string.welcome_seal_description))
                Spacer(Modifier.height(AppSpacing.space7))
                Text(
                    text = stringResource(Res.string.welcome_title),
                    style = AppTypography.display,
                    color = AppColors.text,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(AppSpacing.space7))
                WelcomePromises(textColor = AppColors.text, accent = AppColors.primary)
                Spacer(Modifier.height(AppSpacing.space7))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space6)
                    .padding(bottom = AppSpacing.space5),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.space3),
            ) {
                AppButton(
                    text = stringResource(Res.string.welcome_create_vault),
                    onClick = onCreateVault,
                    modifier = Modifier.fillMaxWidth(),
                )
                AppTextButton(
                    text = stringResource(Res.string.welcome_have_vault),
                    onClick = onOpenVault,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * DO1's form pane: the greeting and both ways in, centred in an
 * [AppDimensions.authFormColumn] column, then the server the vault lives on.
 */
@Composable
private fun WideWelcomeForm(
    serverName: String,
    onCreateVault: () -> Unit,
    onOpenVault: () -> Unit,
    modifier: Modifier,
) {
    CenteredScrollColumn(
        modifier = modifier.background(AppColors.background).safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppDimensions.authFormColumn)
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.space7, vertical = AppSpacing.space8),
        ) {
            Text(
                text = stringResource(Res.string.welcome_wide_title),
                style = AppTypography.display,
                color = AppColors.text,
            )
            Spacer(Modifier.height(AppSpacing.space3))
            Text(
                text = stringResource(Res.string.welcome_wide_subtitle),
                style = AppTypography.body,
                color = AppColors.textMuted,
            )
            Spacer(Modifier.height(AppSpacing.space8))
            AppButton(
                text = stringResource(Res.string.welcome_create_vault),
                onClick = onCreateVault,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppSpacing.space3))
            AppButton(
                text = stringResource(Res.string.welcome_have_vault),
                onClick = onOpenVault,
                variant = AppButtonVariant.Outlined,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppSpacing.space6))
            Text(
                text = stringResource(Res.string.welcome_server, serverName),
                style = AppTypography.caption,
                color = AppColors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Welcome's three promises, in [textColor] beside glyphs in [accent] on
 * [accent]-tinted tiles — under O1's seal, and in [SignInBrandPanel].
 */
@Composable
internal fun WelcomePromises(textColor: Color, accent: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space6),
    ) {
        WelcomePromise(stringResource(Res.string.welcome_promise_fetched), textColor, accent) {
            BoltGlyph(modifier = Modifier.size(AppIconSize.md), tint = accent)
        }
        WelcomePromise(stringResource(Res.string.welcome_promise_encrypted), textColor, accent) {
            LockGlyph(modifier = Modifier.size(AppIconSize.md), tint = accent)
        }
        WelcomePromise(stringResource(Res.string.welcome_promise_devices), textColor, accent) {
            SyncGlyph(modifier = Modifier.size(AppIconSize.md), tint = accent)
        }
    }
}

/** One of Welcome's promises: its glyph on an [accent]-tinted tile, then its sentence. */
@Composable
private fun WelcomePromise(text: String, textColor: Color, accent: Color, glyph: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
        verticalAlignment = Alignment.Top,
    ) {
        AppIconTile(size = AppDimensions.iconTileSmall, tint = accent) {
            glyph()
        }
        Text(
            text = text,
            style = AppTypography.body,
            color = textColor,
            // Lowers the first line toward the tile's middle; further lines run on below it.
            modifier = Modifier.weight(1f).padding(top = AppSpacing.space2),
        )
    }
}
