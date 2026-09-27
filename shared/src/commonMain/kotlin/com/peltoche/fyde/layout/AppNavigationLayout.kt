package com.peltoche.fyde.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.components.navigation.AppNavigationBar
import com.peltoche.fyde.components.navigation.AppNavigationItem
import com.peltoche.fyde.components.navigation.AppNavigationRail

/**
 * The adaptive navigation switch: bottom navigation at
 * [AppWindowSizeClass.Compact], a persistent side rail at Medium/Expanded —
 * a structural change, not the same bar scaled up. Deliberately just this
 * one decision, decoupled from top bars/snackbars, so a screen that only
 * needs adaptive nav (no [AppScaffold] ceremony) can use it directly; it's
 * also what `AppScaffold` itself is built on.
 */
@Composable
fun AppNavigationLayout(
    navigationItems: List<AppNavigationItem>,
    modifier: Modifier = Modifier,
    windowSizeClass: AppWindowSizeClass = LocalAppWindowSizeClass.current,
    railHeader: (@Composable ColumnScope.() -> Unit)? = null,
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
        AppWindowSizeClass.Medium, AppWindowSizeClass.Expanded -> {
            Row(modifier = modifier.fillMaxSize()) {
                if (navigationItems.isNotEmpty()) {
                    AppNavigationRail(items = navigationItems, header = railHeader)
                }
                Box(modifier = Modifier.weight(1f)) { content() }
            }
        }
    }
}
