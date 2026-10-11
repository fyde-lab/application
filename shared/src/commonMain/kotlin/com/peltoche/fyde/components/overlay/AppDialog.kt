package com.peltoche.fyde.components.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.peltoche.fyde.theme.AppSpacing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppTypography

/**
 * A confirm/cancel prompt. No hardcoded copy: [confirmLabel] is required,
 * [text]/[dismissLabel] are opt-in but still caller-supplied strings, never
 * defaulted to English. [destructive] marks the confirm action as one that
 * can't be undone (uninstalling, logging out), see [AppTextButton].
 * [content] goes below [text] — e.g. the field a question is answered in.
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    content: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = { Text(title, style = AppTypography.section, color = AppColors.text) },
        text = if (text != null || content != null) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space4)) {
                    text?.let { Text(it, style = AppTypography.body, color = AppColors.textMuted) }
                    content?.invoke()
                }
            }
        } else {
            null
        },
        confirmButton = { AppTextButton(text = confirmLabel, onClick = onConfirm, enabled = confirmEnabled, destructive = destructive) },
        dismissButton = if (dismissLabel != null && onDismiss != null) {
            { AppTextButton(text = dismissLabel, onClick = onDismiss) }
        } else {
            null
        },
        shape = AppShapes.dialog,
        containerColor = AppColors.surfaceElevated,
        titleContentColor = AppColors.text,
        textContentColor = AppColors.textMuted,
    )
}
