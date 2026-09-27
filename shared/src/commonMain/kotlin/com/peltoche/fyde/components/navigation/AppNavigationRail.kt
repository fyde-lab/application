package com.peltoche.fyde.components.navigation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppTypography

/**
 * Side navigation for the medium/expanded breakpoint — design-system.md's
 * Responsive section names a rail as where master-detail navigation should
 * live once a screen needs it. Shares [AppNavigationItem] with
 * [AppNavigationBar]; no screen uses either yet.
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
                icon = item.icon,
                label = { Text(item.label, style = AppTypography.micro) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = AppColors.primary,
                    selectedTextColor = AppColors.primary,
                    indicatorColor = AppColors.selectionSurface,
                    unselectedIconColor = AppColors.textMuted,
                    unselectedTextColor = AppColors.textMuted,
                ),
            )
        }
    }
}
