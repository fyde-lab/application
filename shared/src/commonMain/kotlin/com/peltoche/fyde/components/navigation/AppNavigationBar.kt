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
    /** False for a destination that's visually present but not wired up yet — dimmed, not clickable. */
    val enabled: Boolean = true,
)

/**
 * Bottom navigation for the compact breakpoint — the app's top-level
 * destinations (Documents, Scripts, Settings) below 600dp, via
 * `TopLevelScaffold`.
 */
@Composable
fun AppNavigationBar(items: List<AppNavigationItem>, modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier, containerColor = AppColors.surfaceElevated) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                enabled = item.enabled,
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
