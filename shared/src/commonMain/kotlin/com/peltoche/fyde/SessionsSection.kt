package com.peltoche.fyde

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppBadge
import com.peltoche.fyde.components.content.AppBadgeVariant
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.feedback.AppSkeleton
import com.peltoche.fyde.components.overlay.AppDialog
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.resources.session_limit_connected_since
import com.peltoche.fyde.resources.sessions_current_device
import com.peltoche.fyde.resources.sessions_disconnect
import com.peltoche.fyde.resources.sessions_disconnect_confirm_text
import com.peltoche.fyde.resources.sessions_disconnect_confirm_title
import com.peltoche.fyde.resources.sessions_section_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * What [SettingsScreen]'s devices section shows. The caller (see `App.kt`'s
 * `Screen.Settings` branch) owns the [FydeSdk.listSessions] call and maps its
 * result into this state, same split as [PlanUsageUiState] — and, like it,
 * re-fetches every time Settings opens, since the account's other devices
 * connect and disconnect too.
 */
sealed interface SessionsUiState {
    data object Loading : SessionsUiState

    data class Failed(val message: String, val onRetry: () -> Unit) : SessionsUiState

    data class Loaded(val sessions: List<FydeOpenSession>) : SessionsUiState
}

/**
 * The devices section of [SettingsScreen]: every session open on the account,
 * oldest first, each with when it connected. This device's own is badged and
 * can't be disconnected from here — logging out (below, under Account) is
 * what ends it, since that also clears this device's local copy. Any other
 * can be disconnected through [onDisconnect], after confirming: that device
 * is signed out on its next call and needs the password to come back.
 *
 * [disconnectingId] keeps the matching row's button busy while [App]'s
 * [FydeSdk.revokeSession] call runs; [disconnectError] reports its failure
 * above the list.
 */
@Composable
internal fun SessionsSection(
    state: SessionsUiState,
    onDisconnect: (FydeOpenSession) -> Unit,
    disconnectingId: String? = null,
    disconnectError: String? = null,
    onDismissDisconnectError: () -> Unit = {},
) {
    var confirming by remember { mutableStateOf<FydeOpenSession?>(null) }

    SectionTitle(stringResource(Res.string.sessions_section_title))
    if (disconnectError != null) {
        AppBanner(
            message = disconnectError,
            onDismiss = onDismissDisconnectError,
            dismissLabel = stringResource(Res.string.dismiss),
        )
    }
    when (state) {
        SessionsUiState.Loading -> SessionsSkeleton()
        is SessionsUiState.Failed -> {
            AppBanner(message = state.message)
            RetryButton(onClick = state.onRetry)
        }
        is SessionsUiState.Loaded -> Column {
            state.sessions.forEachIndexed { index, session ->
                if (index > 0) AppDivider()
                SessionRow(
                    session = session,
                    trailingContent = {
                        if (session.isCurrent) {
                            AppBadge(
                                text = stringResource(Res.string.sessions_current_device),
                                variant = AppBadgeVariant.Primary,
                            )
                        } else {
                            // Destructive, like every action that leads to
                            // something that can't be undone (see
                            // design-system.md § Feedback & confirmation).
                            AppTextButton(
                                text = stringResource(Res.string.sessions_disconnect),
                                onClick = { confirming = session },
                                enabled = disconnectingId == null,
                                loading = session.id == disconnectingId,
                                destructive = true,
                            )
                        }
                    },
                )
            }
        }
    }

    confirming?.let { session ->
        AppDialog(
            onDismissRequest = { confirming = null },
            title = stringResource(Res.string.sessions_disconnect_confirm_title, session.deviceName),
            text = stringResource(Res.string.sessions_disconnect_confirm_text),
            confirmLabel = stringResource(Res.string.sessions_disconnect),
            onConfirm = {
                confirming = null
                onDisconnect(session)
            },
            dismissLabel = stringResource(Res.string.cancel),
            onDismiss = { confirming = null },
            destructive = true,
        )
    }
}

/**
 * One device: its name over when it connected, then [trailingContent].
 * Not an [com.peltoche.fyde.components.content.AppListItem]: that one brings
 * M3's own horizontal inset, where every other Settings row (see
 * [SettingsRow]) takes only vertical padding and leaves the one horizontal
 * margin to the screen's container — so its rows would sit indented.
 */
@Composable
private fun SessionRow(session: FydeOpenSession, trailingContent: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.deviceName,
                style = AppTypography.bodyEmphasis,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(Res.string.session_limit_connected_since, localizedDate(session.createdAt)),
                style = AppTypography.caption,
                color = AppColors.textMuted,
            )
        }
        Spacer(Modifier.width(AppSpacing.space3))
        trailingContent()
    }
}

/** Shaped like two loaded rows (a name over a date), so the section doesn't jump on load. */
@Composable
private fun SessionsSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(2) {
            Column(modifier = Modifier.padding(vertical = AppSpacing.space3)) {
                AppSkeleton(Modifier.width(160.dp).height(18.dp))
                Spacer(Modifier.height(AppSpacing.space2))
                AppSkeleton(Modifier.width(120.dp).height(14.dp))
            }
        }
    }
}
