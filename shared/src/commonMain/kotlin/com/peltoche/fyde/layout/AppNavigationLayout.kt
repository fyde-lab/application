package com.peltoche.fyde.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.components.navigation.AppNavigationBar
import com.peltoche.fyde.components.navigation.AppNavigationItem
import com.peltoche.fyde.components.navigation.AppNavigationRail
import com.peltoche.fyde.components.navigation.AppNavigationTopBar

/**
 * The adaptive navigation switch: bottom navigation at
 * [AppWindowSizeClass.Compact], an icon rail at Medium, a labeled top bar
 * at Expanded — three structurally different surfaces, not one bar
 * scaled up. Deliberately just this one decision, decoupled from top
 * bars/snackbars, so a screen that only needs adaptive nav (no [AppScaffold]
 * ceremony) can use it directly; it's also what `AppScaffold` itself is
 * built on.
 */
@Composable
fun AppNavigationLayout(
    navigationItems: List<AppNavigationItem>,
    modifier: Modifier = Modifier,
    windowSizeClass: AppWindowSizeClass = LocalAppWindowSizeClass.current,
    header: (@Composable () -> Unit)? = null,
    // Only the Expanded top bar has room for chrome at its far end (a trust
    // indicator, a settings destination, the screen's primary action) — the
    // bottom bar and rail stay icon/label-only, so this is ignored below
    // Expanded rather than forced into a shape neither was designed for.
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    when (windowSizeClass) {
        AppWindowSizeClass.Compact -> {
            Column(modifier = modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) { content() }
                if (navigationItems.isNotEmpty()) {
                    AppNavigationBar(items = navigationItems)
                }
            }
        }
        AppWindowSizeClass.Medium -> {
            Row(modifier = modifier.fillMaxSize()) {
                if (navigationItems.isNotEmpty()) {
                    AppNavigationRail(items = navigationItems, header = header?.let { { it() } })
                }
                Box(modifier = Modifier.weight(1f)) { content() }
            }
        }
        AppWindowSizeClass.Expanded -> {
            Column(modifier = modifier.fillMaxSize()) {
                if (navigationItems.isNotEmpty()) {
                    AppNavigationTopBar(items = navigationItems, leading = header, trailing = trailing)
                }
                Box(modifier = Modifier.weight(1f)) { content() }
            }
        }
    }
}
