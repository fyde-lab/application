package com.peltoche.fyde.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.theme.appFocusRing

/**
 * Top navigation for the expanded breakpoint: a full-width bar above the
 * screen, with [leading] (the product's identity), the [items] as labeled
 * tabs, and [trailing] chrome pinned to the far end (secondary
 * destinations, the screen's primary action). Replaced a 240dp+ sidebar
 * that spent a whole column of a desktop window on two destinations.
 * Shares [AppNavigationItem] with [AppNavigationBar]/[AppNavigationRail].
 */
@Composable
fun AppNavigationTopBar(
    items: List<AppNavigationItem>,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimensions.topBarHeightExpanded)
            .background(AppColors.surface),
    ) {
        // Drawn before the tabs, so a selected tab's underline covers it.
        AppDivider(modifier = Modifier.align(Alignment.BottomCenter))
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.space7),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(AppSpacing.space8))
            }
            Row(modifier = Modifier.fillMaxHeight()) {
                items.forEach { item -> AppNavigationTopBarItem(item) }
            }
            Spacer(Modifier.weight(1f))
            if (trailing != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.space2),
                    verticalAlignment = Alignment.CenterVertically,
                    content = trailing,
                )
            }
        }
    }
}

/**
 * One [AppNavigationTopBar] tab: icon and label over the bar's full height,
 * filled with `selectionSurface` and underlined in `primary` along the bar's
 * bottom edge when selected — the same selected fill the bottom bar's
 * indicator uses, so selection reads alike at every breakpoint.
 */
@Composable
private fun AppNavigationTopBarItem(item: AppNavigationItem) {
    val interactionSource = remember { MutableInteractionSource() }
    val tint = when {
        item.selected -> AppColors.primary
        item.enabled -> AppColors.textMuted
        else -> AppColors.textMuted.copy(alpha = 0.5f)
    }
    val underline = AppColors.primary
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .appFocusRing(interactionSource, shape = RectangleShape)
            .background(if (item.selected) AppColors.selectionSurface else Color.Transparent)
            .drawBehind {
                if (item.selected) {
                    val thickness = UnderlineThickness.toPx()
                    drawRect(underline, topLeft = Offset(0f, size.height - thickness), size = Size(size.width, thickness))
                }
            }
            .selectable(
                selected = item.selected,
                onClick = item.onClick,
                enabled = item.enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = ripple(),
            )
            .padding(horizontal = AppSpacing.space7),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            item.icon()
            Spacer(Modifier.width(AppSpacing.space3))
            Text(item.label, style = AppTypography.label, color = tint)
        }
    }
}

private val UnderlineThickness = 2.dp
