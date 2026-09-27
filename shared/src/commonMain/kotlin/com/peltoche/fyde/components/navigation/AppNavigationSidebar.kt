package com.peltoche.fyde.components.navigation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppTypography

/**
 * Persistent side navigation for the expanded breakpoint — a wider, always
 * fully-labeled column (240–360dp, per M3's drawer width tokens) rather than
 * [AppNavigationRail]'s icon-first one, once there's enough width (≥840dp)
 * to spare for it. Shares [AppNavigationItem] with
 * [AppNavigationBar]/[AppNavigationRail]; no screen uses any of the three
 * yet.
 */
@Composable
fun AppNavigationSidebar(
    items: List<AppNavigationItem>,
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null,
) {
    PermanentDrawerSheet(modifier = modifier, drawerContainerColor = AppColors.surfaceElevated) {
        header?.invoke(this)
        items.forEach { item ->
            NavigationDrawerItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = item.icon,
                label = { Text(item.label, style = AppTypography.label) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = AppColors.selectionSurface,
                    selectedIconColor = AppColors.primary,
                    selectedTextColor = AppColors.primary,
                    unselectedIconColor = AppColors.textMuted,
                    unselectedTextColor = AppColors.textMuted,
                ),
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }
    }
}
