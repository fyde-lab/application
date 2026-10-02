package com.peltoche.fyde.components.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppTypography

/**
 * Persistent side navigation for the expanded breakpoint — a wider, always
 * fully-labeled column (240–360dp, per M3's drawer width tokens) rather than
 * [AppNavigationRail]'s icon-first one, once there's enough width (≥840dp)
 * to spare for it. Shares [AppNavigationItem] with
 * [AppNavigationBar]/[AppNavigationRail].
 *
 * [footer] mirrors [header] but pins to the bottom (via a weighted spacer
 * column around [items]) — for chrome that belongs at the foot of the rail
 * (a trust badge, a settings destination) rather than above the nav list.
 */
@Composable
fun AppNavigationSidebar(
    items: List<AppNavigationItem>,
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    PermanentDrawerSheet(modifier = modifier, drawerContainerColor = AppColors.surfaceElevated) {
        header?.invoke(this)
        Column(modifier = Modifier.weight(1f)) {
            items.forEach { item ->
                // NavigationDrawerItem (unlike NavigationBarItem/NavigationRailItem)
                // has no `enabled` param or disabled color slots in this M3
                // version — [AppNavigationItem.enabled] is expressed here as a
                // dimmed unselected tint instead, since onClick is already a
                // no-op for any item the caller marks disabled.
                val unselectedTint = if (item.enabled) AppColors.textMuted else AppColors.textMuted.copy(alpha = 0.5f)
                NavigationDrawerItem(
                    selected = item.selected,
                    onClick = item.onClick,
                    icon = item.icon,
                    label = { Text(item.label, style = AppTypography.label) },
                    // M3's own default is a full stadium pill — design-system.md's
                    // Shape section is explicit that nothing but a circle is a
                    // pill in this app, so the selected-row shape is overridden
                    // to the same small geometric radius every other control uses.
                    shape = AppRadius.sm,
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = AppColors.selectionSurface,
                        selectedIconColor = AppColors.primary,
                        selectedTextColor = AppColors.primary,
                        unselectedIconColor = unselectedTint,
                        unselectedTextColor = unselectedTint,
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        }
        footer?.invoke(this)
    }
}
