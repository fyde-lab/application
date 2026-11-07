package com.peltoche.fyde.components.action

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.theme.AppSpacing
import org.jetbrains.compose.resources.stringResource

/** Retries what failed to load, below its [com.peltoche.fyde.components.feedback.AppErrorNote]. */
@Composable
fun AppRetryButton(onClick: () -> Unit) {
    AppButton(
        text = stringResource(Res.string.retry),
        onClick = onClick,
        variant = AppButtonVariant.Outlined,
        modifier = Modifier.padding(top = AppSpacing.space4),
    )
}
