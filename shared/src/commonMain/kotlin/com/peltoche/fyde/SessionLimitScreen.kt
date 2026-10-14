package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppListItem
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.session_limit_connected_since
import com.peltoche.fyde.resources.session_limit_explanation
import com.peltoche.fyde.resources.session_limit_replace
import com.peltoche.fyde.resources.session_limit_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Shown instead of signing in when the account already has as many open
 * sessions as its plan allows ([maxSessions]): lists them ([sessions],
 * oldest first) so the user picks one to disconnect in favor of this
 * device. Pure presentation, like [LoginScreen] — [App] reruns the login
 * with the picked session's id through [onReplace], or goes back to the
 * login form on [onCancel].
 */
@Composable
fun SessionLimitScreen(
    maxSessions: Int,
    sessions: List<FydeOpenSession>,
    onReplace: (sessionId: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isSubmitting: Boolean = false,
) {
    // Keyed on the list, so a refreshed one (another device connected or
    // left in the meantime) never keeps a selection that's no longer listed.
    var selectedId by remember(sessions) { mutableStateOf<String?>(null) }
    val canSubmit = !isSubmitting && selectedId != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background)
            .safeContentPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppDimensions.maxWidthDetail)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.space7),
            verticalArrangement = Arrangement.Center,
        ) {
            FydeLogo(modifier = Modifier.size(AppDimensions.authLogo))
            Spacer(Modifier.height(AppSpacing.space5))
            Text(
                text = stringResource(Res.string.session_limit_title),
                style = AppTypography.title,
                color = AppColors.text,
            )
            Spacer(Modifier.height(AppSpacing.space3))
            Text(
                text = pluralStringResource(Res.plurals.session_limit_explanation, maxSessions, maxSessions),
                style = AppTypography.body,
                color = AppColors.textMuted,
            )
            Spacer(Modifier.height(AppSpacing.space6))
            Column(modifier = Modifier.selectableGroup()) {
                sessions.forEachIndexed { index, session ->
                    if (index > 0) AppDivider()
                    AppListItem(
                        title = session.deviceName,
                        supportingText = stringResource(
                            Res.string.session_limit_connected_since,
                            localizedDate(session.createdAt),
                        ),
                        selected = session.id == selectedId,
                        onClick = { selectedId = session.id },
                        enabled = !isSubmitting,
                    )
                }
            }
            if (errorMessage != null) {
                Spacer(Modifier.height(AppSpacing.space3))
                Text(
                    text = errorMessage,
                    style = AppTypography.caption,
                    color = AppColors.error,
                )
            }
            Spacer(Modifier.height(AppSpacing.space6))
            AppButton(
                text = stringResource(Res.string.session_limit_replace),
                onClick = { selectedId?.let { if (canSubmit) onReplace(it) } },
                enabled = canSubmit,
                loading = isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AppSpacing.space3))
            AppTextButton(
                text = stringResource(Res.string.cancel),
                onClick = onCancel,
                enabled = !isSubmitting,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
