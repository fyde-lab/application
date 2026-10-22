package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peltoche.fyde.components.content.SealHero
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.app_title
import com.peltoche.fyde.resources.auth_password_stays_computer
import com.peltoche.fyde.resources.auth_password_stays_device
import com.peltoche.fyde.resources.register_account_title
import com.peltoche.fyde.resources.register_brand_title
import com.peltoche.fyde.resources.vault_step_account_subtitle
import com.peltoche.fyde.resources.vault_step_progress
import com.peltoche.fyde.resources.vault_step_recovery_subtitle
import com.peltoche.fyde.resources.vault_step_recovery_title
import com.peltoche.fyde.resources.vault_steps_summary
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
    AuthSplitLayout(brandPanel = { SignInBrandPanel() }, modifier = modifier, form = form)
}

/**
 * The steps of creating a vault, in order: what [VaultCreationBrandPanel]'s
 * stepper and [VaultStepDots] count. The design has four (DO3, DO6–DO8);
 * only those the app has screens for are listed, so the stepper never
 * promises a step that doesn't come — sources and notifications join here
 * once they exist.
 */
enum class VaultCreationStep { Account, RecoveryKit }

/**
 * [AuthBrandPanel] on the vault-creation path (DO3, DO6–DO8): "Create your
 * vault" at 38/44, how long it takes, and a vertical stepper of the
 * [VaultCreationStep]s with [currentStep] lit.
 */
@Composable
fun VaultCreationBrandPanel(currentStep: VaultCreationStep, modifier: Modifier = Modifier) {
    AuthBrandPanel(modifier) {
        Text(
            text = stringResource(Res.string.register_brand_title),
            style = AppTypography.display.copy(fontSize = 38.sp, lineHeight = 44.sp),
            color = AppColors.onPrimary,
        )
        Spacer(Modifier.height(AppSpacing.space3))
        Text(
            text = stringResource(Res.string.vault_steps_summary),
            style = AppTypography.bodyEmphasis.copy(fontWeight = FontWeight.Normal),
            color = AppColors.onPrimary.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(AppSpacing.space8))
        VaultCreationStep.entries.forEach { step ->
            VaultStepRow(step, currentStep, isLast = step == VaultCreationStep.entries.last())
        }
    }
}

/**
 * A vault-creation screen's window at Medium and Expanded:
 * [VaultCreationBrandPanel] beside the form pane at Expanded, an
 * [AuthBrandBand] naming [currentStep] above it at Medium.
 */
@Composable
fun VaultCreationSplitLayout(
    currentStep: VaultCreationStep,
    modifier: Modifier = Modifier,
    form: @Composable (Modifier) -> Unit,
) {
    AuthSplitLayout(
        brandPanel = { VaultCreationBrandPanel(currentStep) },
        band = { AuthBrandBand { VaultStepBadge(currentStep) } },
        modifier = modifier,
        form = form,
    )
}

/**
 * An onboarding or access screen's window at Medium and Expanded: [brandPanel]
 * beside the form pane at Expanded, [band] above it at Medium.
 * [form] gets the modifier that sizes its pane.
 */
@Composable
fun AuthSplitLayout(
    brandPanel: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    band: @Composable () -> Unit = { AuthBrandBand() },
    form: @Composable (Modifier) -> Unit,
) {
    if (LocalAppWindowSizeClass.current == AppWindowSizeClass.Expanded) {
        Row(modifier = modifier.fillMaxSize().background(AppColors.background)) {
            brandPanel()
            form(Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(modifier = modifier.fillMaxSize().background(AppColors.background)) {
            band()
            form(Modifier.weight(1f).fillMaxWidth())
        }
    }
}

/**
 * The brand panel narrowed to a band above the form at Medium, where a
 * 520dp column would leave the form too little room: the mark, and
 * [trailing] at the far end (the vault-creation path's current step).
 */
@Composable
fun AuthBrandBand(modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
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
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

/**
 * A done step's disc: the dark theme's `success`, legible on `primary` in
 * both themes as the mockup draws it (`desktop-auth.html`'s `.stp.done`),
 * with a near-black check.
 */
private val StepDoneFill = Color(0xFF8FD1AE)
private val StepDoneInk = Color(0xFF0B2418)

private enum class StepState { Done, Current, ToDo }

/** One step of [VaultCreationBrandPanel]'s stepper: its disc, joined to the next by a 2dp line, then its name and what it asks. */
@Composable
private fun VaultStepRow(step: VaultCreationStep, currentStep: VaultCreationStep, isLast: Boolean) {
    val state = when {
        step.ordinal < currentStep.ordinal -> StepState.Done
        step == currentStep -> StepState.Current
        else -> StepState.ToDo
    }
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp).fillMaxHeight()) {
            StepDisc(step, state, size = 32.dp)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(vertical = AppSpacing.space1)
                        .width(2.dp)
                        .weight(1f)
                        .background(AppColors.onPrimary.copy(alpha = 0.18f)),
                )
            }
        }
        Spacer(Modifier.width(AppSpacing.space4))
        Column(modifier = Modifier.padding(top = AppSpacing.space1, bottom = if (isLast) 0.dp else AppSpacing.space7)) {
            Text(
                text = step.title(),
                style = AppTypography.bodyEmphasis.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.onPrimary.copy(alpha = if (state == StepState.ToDo) 0.7f else 1f),
            )
            Text(
                text = step.subtitle(),
                style = AppTypography.body,
                color = AppColors.onPrimary.copy(alpha = 0.72f),
            )
        }
    }
}

/** A step's number in a disc: [StepDoneFill] with a check once done, white while current, an outline still to do. */
@Composable
private fun StepDisc(step: VaultCreationStep, state: StepState, size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                when (state) {
                    StepState.Done -> Modifier.background(StepDoneFill)
                    StepState.Current -> Modifier.background(AppColors.onPrimary)
                    StepState.ToDo -> Modifier.border(2.dp, AppColors.onPrimary.copy(alpha = 0.35f), CircleShape)
                },
            ),
    ) {
        if (state == StepState.Done) {
            CheckGlyph(modifier = Modifier.size(AppIconSize.sm), tint = StepDoneInk, strokeWidth = 2.dp)
        } else {
            Text(
                text = (step.ordinal + 1).toString(),
                style = AppTypography.label.copy(fontWeight = FontWeight.SemiBold),
                color = if (state == StepState.Current) AppColors.primary else AppColors.onPrimary.copy(alpha = 0.75f),
            )
        }
    }
}

/** The current step at the end of the Medium band: its lit disc and its name. */
@Composable
private fun VaultStepBadge(step: VaultCreationStep) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepDisc(step, StepState.Current, size = 28.dp)
        Text(text = step.title(), style = AppTypography.label, color = AppColors.onPrimary)
    }
}

/**
 * The phone's step indicator (`tokens.css`'s `.dots3`): an 8dp dot per
 * [VaultCreationStep], the current one stretched to 22dp in `primary`.
 * Read out as "Step n of m".
 */
@Composable
fun VaultStepDots(currentStep: VaultCreationStep, modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.vault_step_progress, currentStep.ordinal + 1, VaultCreationStep.entries.size)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.semantics { contentDescription = description },
    ) {
        VaultCreationStep.entries.forEach { step ->
            Box(
                modifier = Modifier
                    .size(width = if (step == currentStep) 22.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (step == currentStep) AppColors.primary else AppColors.border),
            )
        }
    }
}

@Composable
private fun VaultCreationStep.title(): String = stringResource(
    when (this) {
        VaultCreationStep.Account -> Res.string.register_account_title
        VaultCreationStep.RecoveryKit -> Res.string.vault_step_recovery_title
    },
)

@Composable
private fun VaultCreationStep.subtitle(): String = stringResource(
    when (this) {
        VaultCreationStep.Account -> Res.string.vault_step_account_subtitle
        VaultCreationStep.RecoveryKit -> Res.string.vault_step_recovery_subtitle
    },
)

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
