package com.peltoche.fyde.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppTypography

/**
 * One destination in [AppNavigationBar], [AppNavigationRail], or
 * [AppNavigationTopBar] — shared across all three since the shape
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
                icon = { NavigationIndicator(selected = item.selected, icon = item.icon) },
                label = { Text(item.label, style = AppTypography.micro) },
                colors = NavigationBarItemDefaults.colors(
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

/**
 * The selected-destination indicator behind a bar/rail item's icon, at M3's
 * own indicator size but `radius-sm`, in the same `selectionSurface` fill
 * as [AppNavigationTopBar]'s selected tab. M3's `NavigationBarItem`/`NavigationRailItem` draw theirs as
 * a fixed stadium pill with no shape parameter, so their own indicator is
 * made transparent and this one drawn in its place. Found via review: the
 * pill was the one shape design-system.md § Shape forbids ("nothing is a
 * pill except literal circles") still on screen, below 840dp.
 */
@Composable
internal fun NavigationIndicator(selected: Boolean, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 32.dp)
            .clip(AppRadius.sm)
            .background(if (selected) AppColors.selectionSurface else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}
