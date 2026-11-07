package com.peltoche.fyde.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.appFocusRing

/**
 * One row of an [AppGroup]: [content] in a row at least [minHeight] tall,
 * `space5` from the group's edges (or [horizontalPadding]) and `space3`
 * above and below, its children [horizontalArrangement] apart.
 *
 * With [onClick] the whole row is the control, with a ripple and the focus
 * ring: a button, or — when [selected] isn't null — one choice of a
 * master–detail list column, filled with `selectionSurface` while it's the
 * one open. [background] overrides that fill.
 *
 * Unlike [AppListItem] (M3's `ListItem`, the previous design language) it
 * imposes no text: callers lay out their own label, value and trailing.
 */
@Composable
fun AppGroupRow(
    modifier: Modifier = Modifier,
    minHeight: Dp = AppDimensions.rowHeightStandard,
    onClick: (() -> Unit)? = null,
    selected: Boolean? = null,
    enabled: Boolean = true,
    background: Color = if (selected == true) AppColors.selectionSurface else Color.Transparent,
    horizontalPadding: Dp = AppSpacing.space5,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(AppSpacing.space4),
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val control = when {
        onClick == null -> Modifier
        selected != null -> Modifier
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .appFocusRing(interactionSource, shape = AppRadius.none)
        else -> Modifier
            .clickable(interactionSource = interactionSource, indication = ripple(), enabled = enabled, role = Role.Button, onClick = onClick)
            .appFocusRing(interactionSource, shape = AppRadius.none)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        modifier = modifier
            .fillMaxWidth()
            .then(if (background != Color.Transparent) Modifier.background(background) else Modifier)
            .then(control)
            .heightIn(min = minHeight)
            .padding(horizontal = horizontalPadding, vertical = AppSpacing.space3),
        content = content,
    )
}
