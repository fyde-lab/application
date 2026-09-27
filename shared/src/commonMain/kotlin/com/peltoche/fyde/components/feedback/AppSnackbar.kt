package com.peltoche.fyde.components.feedback

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppShapes

/**
 * Transient toast host. State is hoisted via [SnackbarHostState] — M3's own
 * mechanism, not a new one of ours — which the caller creates/remembers and
 * drives with `showSnackbar(...)`; this component only renders it, styled
 * to the theme.
 */
@Composable
fun AppSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = AppShapes.dialog,
            containerColor = AppColors.surfaceElevated,
            contentColor = AppColors.text,
            actionColor = AppColors.primary,
            actionContentColor = AppColors.primary,
            dismissActionContentColor = AppColors.textMuted,
        )
    }
}
