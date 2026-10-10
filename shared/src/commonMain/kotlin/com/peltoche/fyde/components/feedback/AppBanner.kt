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

/** How serious an [AppBanner]'s condition is. */
enum class AppBannerSeverity {
    /** Something failed (an upload, an install) — the default. */
    Error,

    /** Nothing failed, but something is degraded until it recovers on its own (the server being unreachable). */
    Warning,
}

/**
 * A persistent problem banner pinned above content — for an ongoing
 * condition (a dropped sync stream, a failed background upload, being
 * offline), not a one-off transient toast (see [AppSnackbarHost] for that).
 * Dismissible when [onDismiss] is given; a condition that clears on its own
 * (like being offline) omits it and simply stops showing the banner.
 */
@Composable
fun AppBanner(
    message: String,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    dismissLabel: String? = null,
    severity: AppBannerSeverity = AppBannerSeverity.Error,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                when (severity) {
                    AppBannerSeverity.Error -> MaterialTheme.colorScheme.errorContainer
                    AppBannerSeverity.Warning -> AppColors.warning.copy(alpha = 0.14f)
                },
            )
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
