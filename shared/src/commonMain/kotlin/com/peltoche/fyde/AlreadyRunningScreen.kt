package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.icons.AlertGlyph
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.already_running_title
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppIconSize
import org.jetbrains.compose.resources.stringResource

/**
 * Shown in place of every other screen while [FydeSdk.initialize] refuses to
 * start because another instance of the app already uses this device's local
 * data (see [FydeSdkException.isClientAlreadyRunning]). [message] is
 * fyde-sdk's wording for it; [onRetry] tries starting again, e.g. once the
 * user closed the other window.
 */
@Composable
fun AlreadyRunningScreen(message: String, onRetry: () -> Unit) {
    AppErrorState(
        modifier = Modifier.fillMaxSize().background(AppColors.background),
        icon = { AlertGlyph(modifier = Modifier.size(AppIconSize.lg)) },
        title = stringResource(Res.string.already_running_title),
        description = message,
        actionLabel = stringResource(Res.string.retry),
        onAction = onRetry,
    )
}
