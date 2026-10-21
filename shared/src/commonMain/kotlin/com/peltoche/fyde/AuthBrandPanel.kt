package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peltoche.fyde.components.content.SealHero
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.app_title
import com.peltoche.fyde.resources.auth_password_stays_computer
import com.peltoche.fyde.resources.auth_password_stays_device
import com.peltoche.fyde.resources.welcome_seal_description
import com.peltoche.fyde.resources.welcome_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * The brand panel beside the onboarding and access forms at Expanded
 * (design-system.md § Onboarding and access on Medium and Expanded,
 * `design/assets/mockups/desktop-auth.html`'s `.brand`): a full-height
 * `primary` column with the Fyde mark at the top, [content] centred in
 * between, and the reassurance that the password stays on this device at
 * the bottom. Its content is `onPrimary`.
 */
@Composable
fun AuthBrandPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .width(AppDimensions.authBrandPanel)
            .fillMaxHeight()
            .background(AppColors.primary)
            .safeContentPadding()
            .padding(horizontal = AppSpacing.space9, vertical = AppSpacing.space8),
    ) {
        AuthBrandMark()
        CenteredScrollColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val muted = AppColors.onPrimary.copy(alpha = 0.82f)
            LockGlyph(modifier = Modifier.size(AppIconSize.sm), tint = muted)
            Text(
                text = stringResource(
                    if (getPlatform().isDesktop) Res.string.auth_password_stays_computer else Res.string.auth_password_stays_device,
                ),
                style = AppTypography.caption,
                color = muted,
            )
        }
    }
}

/**
 * [AuthBrandPanel] on the sign-in path (DO1, DO2, DO4, DO5, DO9): the seal on
 * `onPrimary`, "Your documents, under seal." at 38/44, and Welcome's three
 * promises.
 */
@Composable
fun SignInBrandPanel(modifier: Modifier = Modifier) {
    AuthBrandPanel(modifier) {
        SealHero(contentDescription = stringResource(Res.string.welcome_seal_description), onPrimary = true)
        Spacer(Modifier.height(AppSpacing.space7))
        Text(
            text = stringResource(Res.string.welcome_title),
            // The brand panel's title, 38/44 — larger than `display`, which heads the form beside it.
            style = AppTypography.display.copy(fontSize = 38.sp, lineHeight = 44.sp),
            color = AppColors.onPrimary,
        )
        Spacer(Modifier.height(AppSpacing.space7))
        WelcomePromises(textColor = AppColors.onPrimary.copy(alpha = 0.92f), accent = AppColors.onPrimary)
    }
}

/**
 * A sign-in screen's window at Medium and Expanded: [SignInBrandPanel] beside
 * the form pane at Expanded, an [AuthBrandBand] above it at Medium. [form]
 * gets the modifier that sizes its pane. Compact has no split; callers draw
 * their phone layout there instead.
 */
@Composable
fun SignInSplitLayout(modifier: Modifier = Modifier, form: @Composable (Modifier) -> Unit) {
    if (LocalAppWindowSizeClass.current == AppWindowSizeClass.Expanded) {
        Row(modifier = modifier.fillMaxSize().background(AppColors.background)) {
            SignInBrandPanel()
            form(Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(modifier = modifier.fillMaxSize().background(AppColors.background)) {
            AuthBrandBand()
            form(Modifier.weight(1f).fillMaxWidth())
        }
    }
}

/**
 * The brand panel narrowed to a band above the form at Medium, where a
 * 520dp column would leave the form too little room: only the mark.
 */
@Composable
fun AuthBrandBand(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.primary)
            .safeContentPadding()
            .height(AppDimensions.authBrandBand)
            .padding(horizontal = AppSpacing.space9),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AuthBrandMark()
    }
}

/** Fyde's mark on `primary`: the [FydeLogo], sized as in the signed-in top bar, then the product's name. */
@Composable
private fun AuthBrandMark() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FydeLogo(modifier = Modifier.size(28.dp))
        Text(
            text = stringResource(Res.string.app_title),
            style = AppTypography.section,
            color = AppColors.onPrimary,
        )
    }
}

/**
 * A column centred vertically in the space it's given, that scrolls instead
 * once its content is taller — `Arrangement.Center` alone does nothing inside
 * `verticalScroll`, whose height is unbounded.
 */
@Composable
fun CenteredScrollColumn(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}
