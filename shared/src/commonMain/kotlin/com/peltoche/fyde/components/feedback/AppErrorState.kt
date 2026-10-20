package com.peltoche.fyde.components.feedback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.theme.AppColors

/** Same layout as [AppEmptyState], with an error-tinted icon circle and (typically) a retry action. */
@Composable
fun AppErrorState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StateMessage(
        title = title,
        description = description,
        modifier = modifier,
        iconBackground = AppColors.tintOf(AppColors.error),
        icon = icon,
        actionLabel = actionLabel,
        onAction = onAction,
    )
}
