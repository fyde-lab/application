package com.peltoche.fyde.components.content

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.ChevronRightGlyph
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/**
 * A small bordered filter/facet chip — [AppBadge]'s interactive sibling:
 * where a badge is a static status label, a chip represents a facet a user
 * can act on. [enabled] controls contrast only, not clickability — this
 * component has no `onClick` of its own (not every chip use is a dropdown
 * trigger), so a caller wires its own tap/click handling and passes
 * `enabled = false` for a facet the current data model doesn't support
 * filtering on yet, matching the dimmed-rather-than-full-contrast treatment
 * the rest of the app already uses for a visible-but-not-wired-up control
 * (see [com.peltoche.fyde.components.navigation.AppNavigationItem.enabled]).
 *
 * Promoted out of `DocumentListScreen`'s private `FilterChip`, which had
 * drifted from every other interactive primitive in the app by living
 * outside `components/` instead of alongside [AppBadge].
 */
@Composable
fun AppChip(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showChevron: Boolean = false,
) {
    val contentColor = if (enabled) AppColors.text else AppColors.textMuted
    Row(
        modifier = modifier
            .clip(AppRadius.sm)
            .border(AppDimensions.borderHairline, AppColors.border, AppRadius.sm)
            .padding(horizontal = AppSpacing.space3, vertical = AppSpacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = AppTypography.caption, color = contentColor)
        if (showChevron) {
            Spacer(Modifier.width(AppSpacing.space1))
            ChevronRightGlyph(
                modifier = Modifier.size(10.dp).rotate(90f),
                tint = AppColors.textMuted,
            )
        }
    }
}
