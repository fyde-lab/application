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
import androidx.compose.ui.text.style.TextAlign
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppIconTile
import com.peltoche.fyde.components.content.SealHero
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.welcome_create_vault
import com.peltoche.fyde.resources.welcome_have_vault
import com.peltoche.fyde.resources.welcome_promise_devices
import com.peltoche.fyde.resources.welcome_promise_encrypted
import com.peltoche.fyde.resources.welcome_promise_fetched
import com.peltoche.fyde.resources.welcome_seal_description
import com.peltoche.fyde.resources.welcome_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Welcome (O1, `design/assets/screens/O1-bienvenue.png`): the seal, what
 * Fyde promises, and the way in — [onCreateVault] to [RegisterScreen],
 * [onOpenVault] to [LoginScreen]. The first screen on a device with no
 * account open. Pure presentation, like [LoginScreen].
 *
 * The promises and title scroll when the window is too short for them; the
 * two actions stay pinned at the bottom, within the thumb's reach.
 */
@Composable
fun WelcomeScreen(
    onCreateVault: () -> Unit,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier,
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.space6),
                ) {
                    WelcomePromise(stringResource(Res.string.welcome_promise_fetched)) {
                        BoltGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.primary)
                    }
                    WelcomePromise(stringResource(Res.string.welcome_promise_encrypted)) {
                        LockGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.primary)
                    }
                    WelcomePromise(stringResource(Res.string.welcome_promise_devices)) {
                        SyncGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.primary)
                    }
                }
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

/** One of Welcome's promises: its glyph on a `primary`-tinted tile, then its sentence. */
@Composable
private fun WelcomePromise(text: String, glyph: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
        verticalAlignment = Alignment.Top,
    ) {
        AppIconTile(size = AppDimensions.iconTileSmall, tint = AppColors.primary) {
            glyph()
        }
        Text(
            text = text,
            style = AppTypography.body,
            color = AppColors.text,
            // Lowers the first line toward the tile's middle; further lines run on below it.
            modifier = Modifier.weight(1f).padding(top = AppSpacing.space2),
        )
    }
}
