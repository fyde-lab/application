package com.peltoche.fyde.components.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/** Below this a dialog's title and actions wrap awkwardly — M3's own dialog minimum. */
private val DialogMinWidth = 280.dp

/**
 * A confirm/cancel prompt (design-system.md § Patterns: a dialog only to
 * confirm something irreversible, its confirm button filled `error`). No
 * hardcoded copy: [confirmLabel] is required, [text]/[dismissLabel] are
 * opt-in but still caller-supplied strings, never defaulted to English.
 * [destructive] fills the confirm in `error` (uninstalling, logging out);
 * otherwise it's the dialog's one filled `primary` action. [content] goes
 * below [text] — e.g. the field a question is answered in — and scrolls
 * with the dialog's body when it's taller than the window. [confirmLoading]
 * keeps the dialog up, its confirm spinning and cancel disabled, while the
 * confirmed action runs.
 *
 * At Compact the actions stack full width, confirm first (M8); wider, they
 * sit right-aligned on one row, cancel before confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    confirmLoading: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    val stacked = LocalAppWindowSizeClass.current == AppWindowSizeClass.Compact
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier) {
        Column(
            modifier = Modifier
                .widthIn(min = DialogMinWidth, max = AppDimensions.sheetPanelMaxWidth)
                .clip(AppShapes.dialog)
                .background(AppColors.surfaceElevated)
                .padding(AppSpacing.space7),
        ) {
            Text(title, style = AppTypography.section, color = AppColors.text)
            if (text != null || content != null) {
                Spacer(Modifier.height(AppSpacing.space4))
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.space4),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    text?.let { Text(it, style = AppTypography.body, color = AppColors.textMuted) }
                    content?.invoke()
                }
            }
            Spacer(Modifier.height(AppSpacing.space7))
            val dismiss: (@Composable (Modifier) -> Unit)? = if (dismissLabel != null && onDismiss != null) {
                { dismissModifier -> AppTextButton(text = dismissLabel, onClick = onDismiss, enabled = !confirmLoading, modifier = dismissModifier) }
            } else {
                null
            }
            if (stacked) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.space2),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AppButton(
                        text = confirmLabel,
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        loading = confirmLoading,
                        destructive = destructive,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    dismiss?.invoke(Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    dismiss?.invoke(Modifier)
                    AppButton(text = confirmLabel, onClick = onConfirm, enabled = confirmEnabled, loading = confirmLoading, destructive = destructive)
                }
            }
        }
    }
}
