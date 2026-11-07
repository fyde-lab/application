package com.peltoche.fyde

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppRetryButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppGroup
import com.peltoche.fyde.components.content.AppGroupRow
import com.peltoche.fyde.components.feedback.AppErrorNote
import com.peltoche.fyde.components.feedback.AppListSkeleton
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.components.overlay.AppDialog
import com.peltoche.fyde.icons.InfoGlyph
import com.peltoche.fyde.icons.LaptopGlyph
import com.peltoche.fyde.icons.PhoneGlyph
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.devices_count_of_plan
import com.peltoche.fyde.resources.devices_intro
import com.peltoche.fyde.resources.devices_lost_note
import com.peltoche.fyde.resources.devices_this_device_since
import com.peltoche.fyde.resources.session_limit_connected_since
import com.peltoche.fyde.resources.sessions_disconnect
import com.peltoche.fyde.resources.sessions_disconnect_confirm_text
import com.peltoche.fyde.resources.sessions_disconnect_confirm_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * What [SettingsScreen]'s devices page shows. The caller (see `App.kt`'s
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
 * M5, Appareils: how many devices the plan allows (from [planUsage], once
 * loaded), then every session open on the account, oldest first. This
 * device's own says so and can't be disconnected from here — logging out is
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
    planUsage: PlanUsageUiState,
    onDisconnect: (FydeOpenSession) -> Unit,
    disconnectingId: String? = null,
    disconnectError: String? = null,
) {
    var confirming by remember { mutableStateOf<FydeOpenSession?>(null) }

    val intro = (planUsage as? PlanUsageUiState.Loaded)?.usage?.let { usage ->
        stringResource(
            Res.string.devices_count_of_plan,
            usage.openSessions.toString(),
            usage.maxSessions.toString(),
            planName(usage.planId),
        ) + " " + stringResource(Res.string.devices_intro)
    } ?: stringResource(Res.string.devices_intro)
    Text(text = intro, style = AppTypography.body, color = AppColors.textMuted)
    Spacer(Modifier.height(AppSpacing.space5))
    disconnectError?.let { message ->
        AppErrorNote(message)
        Spacer(Modifier.height(AppSpacing.space4))
    }
    when (state) {
        SessionsUiState.Loading -> SessionsSkeleton()
        is SessionsUiState.Failed -> {
            AppErrorNote(state.message)
            AppRetryButton(onClick = state.onRetry)
        }
        is SessionsUiState.Loaded -> AppGroup {
            state.sessions.forEachIndexed { index, session ->
                if (index > 0) AppDivider()
                SessionRow(session = session) {
                    if (!session.isCurrent) {
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
                }
            }
        }
    }
    Spacer(Modifier.height(AppSpacing.space4))
    AppNote(
        message = stringResource(Res.string.devices_lost_note),
        kind = AppNoteKind.Info,
        icon = { tint -> InfoGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
    )

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
 * One device in its group: a phone or laptop glyph guessed from its name
 * (see [isPhoneSession]), the name over when it connected — "this device"
 * for the current one — then [trailingContent].
 */
@Composable
private fun SessionRow(session: FydeOpenSession, trailingContent: @Composable () -> Unit) {
    AppGroupRow(minHeight = AppDimensions.rowHeightComfortable) {
        val glyphModifier = Modifier.size(AppIconSize.prominent)
        if (isPhoneSession(session)) PhoneGlyph(modifier = glyphModifier) else LaptopGlyph(modifier = glyphModifier)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.deviceName,
                style = AppTypography.bodyEmphasis,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val since = localizedDate(session.createdAt)
            Text(
                text = if (session.isCurrent) {
                    stringResource(Res.string.devices_this_device_since, since)
                } else {
                    stringResource(Res.string.session_limit_connected_since, since)
                },
                style = AppTypography.caption,
                color = if (session.isCurrent) AppColors.success else AppColors.textMuted,
            )
        }
        trailingContent()
    }
}

/** Shaped like two loaded rows (a glyph, a name over a date), so the page doesn't jump on load. */
@Composable
private fun SessionsSkeleton() {
    AppListSkeleton(
        rows = 2,
        rowHeight = AppDimensions.rowHeightComfortable,
        tileSize = AppIconSize.prominent,
        firstLine = Modifier.width(160.dp).height(18.dp),
        secondLine = Modifier.width(120.dp).height(14.dp),
        lineGap = AppSpacing.space2,
    )
}
