package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.content.AppBadge
import com.peltoche.fyde.components.content.AppBadgeVariant
import com.peltoche.fyde.components.feedback.AppErrorNote
import com.peltoche.fyde.components.form.AppRadio
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.CenteredScrollColumn
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.folder_layout_blank
import com.peltoche.fyde.resources.folder_layout_blank_text
import com.peltoche.fyde.resources.folder_layout_continue
import com.peltoche.fyde.resources.folder_layout_housing
import com.peltoche.fyde.resources.folder_layout_money
import com.peltoche.fyde.resources.folder_layout_more
import com.peltoche.fyde.resources.folder_layout_proposed
import com.peltoche.fyde.resources.folder_layout_proposed_text
import com.peltoche.fyde.resources.folder_layout_recommended
import com.peltoche.fyde.resources.folder_layout_subtitle
import com.peltoche.fyde.resources.folder_layout_taxes
import com.peltoche.fyde.resources.folder_layout_title
import com.peltoche.fyde.resources.folder_layout_work
import com.peltoche.fyde.resources.folder_layout_your_folders
import com.peltoche.fyde.resources.vault_unclassified
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** O11's two choices: fyde-sdk's proposed layout, or no folder at all. */
enum class FolderLayoutChoice { Proposed, Blank }

/** How many top-level folders the proposed layout creates (fyde-sdk's `PROPOSED_LAYOUT`). */
private const val PROPOSED_TOP_LEVEL_FOLDERS = 16

/** The proposed layout's first folders, drawn on the card as it will create them. */
private val ProposedExamples: List<Pair<StringResource, FydeFolderAppearance>> = listOf(
    Res.string.folder_layout_housing to FydeFolderAppearance(FydeFolderColor.SAND, FydeFolderIcon.HOME),
    Res.string.folder_layout_money to FydeFolderAppearance(FydeFolderColor.GREEN, FydeFolderIcon.WALLET),
    Res.string.folder_layout_work to FydeFolderAppearance(FydeFolderColor.TEAL, FydeFolderIcon.BRIEFCASE),
    Res.string.folder_layout_taxes to FydeFolderAppearance(FydeFolderColor.BRICK, FydeFolderIcon.LANDMARK),
)

/**
 * O11 / DO11, vault creation's folders step right after the account: the
 * proposed folders (pre-selected) or an empty vault, then Continuer.
 * Presentation only — `App.kt` creates the folders. No back button (the
 * account behind it is already created) and no skipping: a choice is
 * always made.
 */
@Composable
fun FolderLayoutScreen(
    choice: FolderLayoutChoice,
    onChoiceChange: (FolderLayoutChoice) -> Unit,
    busy: Boolean,
    error: String?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (LocalAppWindowSizeClass.current) {
        AppWindowSizeClass.Compact -> Box(
            modifier = modifier.fillMaxSize().background(AppColors.background).safeContentPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            CenteredScrollColumn(modifier = Modifier.widthIn(max = AppDimensions.maxWidthDetail).fillMaxSize()) {
                Column(modifier = Modifier.padding(horizontal = AppSpacing.space6, vertical = AppSpacing.space4)) {
                    VaultStepDots(VaultCreationStep.Folders, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(AppSpacing.space6))
                    FolderLayoutForm(choice, onChoiceChange, busy, error, onContinue)
                }
            }
        }
        AppWindowSizeClass.Medium, AppWindowSizeClass.Expanded -> VaultCreationSplitLayout(
            currentStep = VaultCreationStep.Folders,
            modifier = modifier,
        ) { paneModifier ->
            Box(modifier = paneModifier.background(AppColors.background).safeContentPadding()) {
                CenteredScrollColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.space7, vertical = AppSpacing.space10),
                    ) {
                        FolderLayoutForm(choice, onChoiceChange, busy, error, onContinue)
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderLayoutForm(
    choice: FolderLayoutChoice,
    onChoiceChange: (FolderLayoutChoice) -> Unit,
    busy: Boolean,
    error: String?,
    onContinue: () -> Unit,
) {
    Text(
        text = stringResource(Res.string.folder_layout_title),
        style = AppTypography.display,
        color = AppColors.text,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(AppSpacing.space3))
    Text(text = stringResource(Res.string.folder_layout_subtitle), style = AppTypography.body, color = AppColors.textMuted)
    Spacer(Modifier.height(AppSpacing.space6))
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space4)) {
        ChoiceCard(
            selected = choice == FolderLayoutChoice.Proposed,
            enabled = !busy,
            onClick = { onChoiceChange(FolderLayoutChoice.Proposed) },
            title = stringResource(Res.string.folder_layout_proposed),
            badge = stringResource(Res.string.folder_layout_recommended),
            text = stringResource(Res.string.folder_layout_proposed_text),
        ) {
            ProposedExamples.forEach { (name, appearance) ->
                FolderChip(stringResource(name), appearance)
            }
            val more = PROPOSED_TOP_LEVEL_FOLDERS - ProposedExamples.size
            Text(
                text = stringResource(Res.string.folder_layout_more, more),
                style = AppTypography.caption,
                color = AppColors.textMuted,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        ChoiceCard(
            selected = choice == FolderLayoutChoice.Blank,
            enabled = !busy,
            onClick = { onChoiceChange(FolderLayoutChoice.Blank) },
            title = stringResource(Res.string.folder_layout_blank),
            badge = null,
            text = stringResource(Res.string.folder_layout_blank_text),
        ) {
            FolderChip(stringResource(Res.string.vault_unclassified), null)
            FolderChip(stringResource(Res.string.folder_layout_your_folders), null, dashed = true)
        }
    }
    error?.let {
        Spacer(Modifier.height(AppSpacing.space5))
        AppErrorNote(it)
    }
    Spacer(Modifier.height(AppSpacing.space6))
    AppButton(
        text = stringResource(Res.string.folder_layout_continue),
        onClick = onContinue,
        loading = busy,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A radio card: its radio, title and badge, what it does, then a strip of folder chips. */
@Composable
private fun ChoiceCard(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    title: String,
    badge: String?,
    text: String,
    chips: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(if (selected) AppColors.selectionSurface else AppColors.surface)
            .border(
                width = if (selected) 2.dp else AppDimensions.borderHairline,
                color = if (selected) AppColors.primary else AppColors.border,
                shape = AppShapes.card,
            )
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(AppSpacing.space5),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
            AppRadio(selected = selected, enabled = enabled)
            Text(text = title, style = AppTypography.bodyEmphasis, color = AppColors.text, modifier = Modifier.weight(1f))
            if (badge != null) AppBadge(text = badge, variant = AppBadgeVariant.Primary)
        }
        Text(text = text, style = AppTypography.body, color = AppColors.textMuted)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.space2),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.space2),
            content = chips,
        )
    }
}

/** A folder as a chip: its icon on its fill, its name; À classer's for none; [dashed] for one still to make. */
@Composable
private fun FolderChip(name: String, appearance: FydeFolderAppearance?, dashed: Boolean = false) {
    val colors = appearance.colors()
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (dashed) AppColors.surface else colors.fill)
            .then(if (dashed) Modifier.border(AppDimensions.borderHairline, AppColors.borderStrong, shape) else Modifier)
            .padding(horizontal = AppSpacing.space3, vertical = AppSpacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space2),
    ) {
        if (!dashed) FolderIconGlyph(appearance?.icon, tint = colors.ink, modifier = Modifier.size(AppIconSize.sm))
        BasicText(text = name, style = AppTypography.caption.copy(color = if (dashed) AppColors.textMuted else colors.ink))
    }
}
