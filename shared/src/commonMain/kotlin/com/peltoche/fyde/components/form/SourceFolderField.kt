package com.peltoche.fyde.components.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/**
 * The Ranger dans row of a source's connect form (S3, S3b, S3c, DO7b;
 * design-system.md § A source's folder): [label] above a field-like box —
 * `surface`, `borderStrong` hairline, [AppTextField]'s shape and height —
 * holding the choice's [tile], its [value] (a folder's path, "Rangement
 * automatique", "À classer") and [actionLabel] (Changer, or Créer un
 * dossier with a plus as [actionIcon] in an empty vault), then
 * [supportingText] under it. Only the action is a target: the box itself
 * isn't editable.
 */
@Composable
fun SourceFolderField(
    label: String,
    value: String,
    actionLabel: String,
    onAction: () -> Unit,
    tile: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    actionIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = AppTypography.bodyEmphasis, color = if (enabled) AppColors.text else AppColors.textMuted)
        Spacer(Modifier.height(AppSpacing.space2))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FIELD_HEIGHT)
                .clip(AppShapes.input)
                .background(if (enabled) AppColors.surface else AppColors.background)
                .border(AppDimensions.borderHairline, if (enabled) AppColors.borderStrong else AppColors.border, AppShapes.input)
                .padding(start = AppSpacing.space4, end = AppSpacing.space2),
        ) {
            tile()
            Text(
                text = value,
                style = AppTypography.body,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            AppTextButton(text = actionLabel, onClick = onAction, enabled = enabled, leadingIcon = actionIcon)
        }
        if (supportingText != null) {
            Spacer(Modifier.height(AppSpacing.space2))
            Text(text = supportingText, style = AppTypography.caption, color = AppColors.textMuted)
        }
    }
}

/** [AppTextField]'s height (Material 3's outlined field), so the row lines up with the fields above it. */
private val FIELD_HEIGHT = 56.dp
