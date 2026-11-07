package com.peltoche.fyde.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.icons.ChevronRightGlyph
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.theme.appFocusRing

/**
 * A small bordered filter/facet chip — [AppBadge]'s interactive sibling:
 * where a badge is a static status label, a chip represents a facet a user
 * can act on. With [onClick] it's a real control: focus ring, and a hit
 * area grown to the 48dp touch-target minimum around its compact visual (see
 * design-system.md § Shape, "Touch target minimum is a rule about tap
 * area"). [selected] marks a facet currently narrowing the results —
 * `primary` outline over the `selectionSurface` wash, the same pairing a
 * selected list row uses.
 *
 * [enabled] dims the chip and ignores clicks. Found via review: chips used
 * to be drawn with no click handling at all, as permanently disabled
 * placeholders for filters the data model didn't support — five inert
 * controls on the main screen. Only chips backed by a real filter are shown
 * now.
 */
@Composable
fun AppChip(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showChevron: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val contentColor = when {
        !enabled -> AppColors.textMuted
        selected -> AppColors.primary
        else -> AppColors.text
    }
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        Modifier
            .minimumInteractiveComponentSize()
            .appFocusRing(interactionSource, shape = AppRadius.sm)
            .clip(AppRadius.sm)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
    } else {
        Modifier.clip(AppRadius.sm)
    }
    Row(
        modifier = modifier
            .then(clickableModifier)
            .background(if (selected) AppColors.selectionSurface else AppColors.background)
            .border(AppDimensions.borderHairline, if (selected) AppColors.primary else AppColors.border, AppRadius.sm)
            .padding(horizontal = AppSpacing.space3, vertical = AppSpacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = AppTypography.caption, color = contentColor)
        if (showChevron) {
            // A real icon-to-text gap (design-system.md § Spacing), not the
            // 2dp micro gap it used to be: found via review that the chevron
            // sat glued to the label, reading as one run-together word.
            Spacer(Modifier.width(AppSpacing.space2))
            ChevronRightGlyph(
                modifier = Modifier.size(8.dp).rotate(90f),
                tint = if (selected) AppColors.primary else AppColors.textMuted,
            )
        }
    }
}
