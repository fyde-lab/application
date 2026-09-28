package com.peltoche.fyde.layout

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.components.navigation.AppNavigationItem
import com.peltoche.fyde.theme.AppColors

/**
 * The top-level shell most screens reach for: adaptive navigation (via
 * [AppNavigationLayout]) wrapping an M3 [Scaffold] for the top bar and
 * snackbar host. A screen that wants adaptive nav without a top bar or
 * snackbar can use [AppNavigationLayout] directly instead.
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    windowSizeClass: AppWindowSizeClass = LocalAppWindowSizeClass.current,
    navigationItems: List<AppNavigationItem> = emptyList(),
    navigationHeader: (@Composable ColumnScope.() -> Unit)? = null,
    navigationFooter: (@Composable ColumnScope.() -> Unit)? = null,
    topBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    AppNavigationLayout(
        navigationItems = navigationItems,
        modifier = modifier,
        windowSizeClass = windowSizeClass,
        header = navigationHeader,
        footer = navigationFooter,
    ) {
        Scaffold(
            topBar = topBar,
            snackbarHost = snackbarHost,
            containerColor = AppColors.background,
            content = content,
        )
    }
}
