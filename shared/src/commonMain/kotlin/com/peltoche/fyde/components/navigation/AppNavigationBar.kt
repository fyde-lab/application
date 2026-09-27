package com.peltoche.fyde.components.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppTypography

/**
 * One destination in [AppNavigationBar], [AppNavigationRail], or
 * [AppNavigationSidebar] — shared across all three since the shape
 * (label/icon/selected/click) is identical; only the M3 host layout differs
 * by breakpoint.
 */
data class AppNavigationItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
)

/**
 * Bottom navigation for the compact breakpoint. design-system.md is explicit
 * that this has no current use — no screen has 3+ peer top-level
 * destinations yet — so this exists as a ready primitive, not something
 * wired into navigation today.
 */
@Composable
fun AppNavigationBar(items: List<AppNavigationItem>, modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier, containerColor = AppColors.surfaceElevated) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = item.icon,
                label = { Text(item.label, style = AppTypography.micro) },
                colors = NavigationBarItemDefaults.colors(
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
