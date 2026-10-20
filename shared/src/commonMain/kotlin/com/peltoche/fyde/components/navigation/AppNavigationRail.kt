package com.peltoche.fyde.components.navigation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppTypography

/**
 * Side navigation for the medium breakpoint — design-system.md's Responsive
 * section names a rail as where master-detail navigation should live once a
 * screen needs it. Yields to [AppNavigationTopBar] at Expanded.
 * Shares [AppNavigationItem] with [AppNavigationBar]/[AppNavigationTopBar].
 * Not used by `TopLevelScaffold`, which keeps the bottom bar at Medium so
 * the document two-pane layout keeps its width (see its doc comment).
 */
@Composable
fun AppNavigationRail(
    items: List<AppNavigationItem>,
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null,
) {
    NavigationRail(modifier = modifier, containerColor = AppColors.surfaceElevated, header = header) {
        items.forEach { item ->
            NavigationRailItem(
                selected = item.selected,
                onClick = item.onClick,
                enabled = item.enabled,
                icon = { NavigationIndicator(selected = item.selected, icon = item.icon) },
                label = { Text(item.label, style = AppTypography.micro) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = AppColors.primary,
                    selectedTextColor = AppColors.primary,
                    // Drawn by NavigationIndicator instead, see its doc.
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = AppColors.textMuted,
                    unselectedTextColor = AppColors.textMuted,
                ),
            )
        }
    }
}
