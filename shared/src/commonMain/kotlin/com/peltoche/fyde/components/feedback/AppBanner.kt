package com.peltoche.fyde.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/**
 * A persistent, dismissible problem banner pinned above content — for an
 * ongoing condition (a dropped sync stream, a failed background upload),
 * not a one-off transient toast (see [AppSnackbarHost] for that). Every
 * current use is error-class, so this doesn't take a severity variant yet —
 * add one if a non-error use case actually shows up, rather than guessing
 * at the values now.
 */
@Composable
fun AppBanner(message: String, modifier: Modifier = Modifier, onDismiss: (() -> Unit)? = null, dismissLabel: String? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = AppTypography.caption,
            color = AppColors.text,
            modifier = Modifier.weight(1f),
        )
        if (onDismiss != null && dismissLabel != null) {
            AppTextButton(text = dismissLabel, onClick = onDismiss)
        }
    }
}
