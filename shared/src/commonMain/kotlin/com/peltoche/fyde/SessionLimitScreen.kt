package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppBackButton
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.components.form.AppRadio
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.login_submitting
import com.peltoche.fyde.resources.session_limit_connected_since
import com.peltoche.fyde.resources.session_limit_disconnect
import com.peltoche.fyde.resources.session_limit_explanation_computer
import com.peltoche.fyde.resources.session_limit_explanation_device
import com.peltoche.fyde.resources.session_limit_pick
import com.peltoche.fyde.resources.session_limit_reconnect_note
import com.peltoche.fyde.resources.session_limit_see_plans
import com.peltoche.fyde.resources.session_limit_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.theme.appFocusRing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Trop d'appareils: shown instead of signing in when the account already has
 * as many open sessions as its plan allows ([maxSessions]). Lists them
 * ([sessions], oldest first) as a radio group so the user picks one to
 * disconnect in favor of this device, with a note that it can sign back in
 * later. Pure presentation, like [LoginScreen] — [App] reruns the login with
 * the picked session's id through [onReplace], or goes back to the login
 * form on [onCancel].
 *
 * At Compact it's O5 (`design/assets/screens/O5-trop-appareils.png`), the
 * actions pinned at the bottom. At Medium and Expanded it's DO5
 * (`design/assets/screens/DO5-trop-dappareils.png`): [SignInSplitLayout]'s
 * brand panel beside the list centred in an [AppDimensions.authFormColumn]
 * column, back at the pane's top left and the actions side by side. The
 * selection lives here, above both, so resizing across Compact keeps it.
 *
 * "See plans" only shows given [onSeePlans]: nothing in the app presents the
 * plans before signing in yet.
 */
@Composable
fun SessionLimitScreen(
    maxSessions: Int,
    sessions: List<FydeOpenSession>,
    onReplace: (sessionId: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onSeePlans: (() -> Unit)? = null,
    errorMessage: String? = null,
    isSubmitting: Boolean = false,
) {
    // Keyed on the list, so a refreshed one (another device connected or
    // left in the meantime) never keeps a selection that's no longer listed.
    var selectedId by remember(sessions) { mutableStateOf<String?>(null) }
    val state = SessionLimitState(
        maxSessions = maxSessions,
        sessions = sessions,
        selectedId = selectedId,
        onSelect = { selectedId = it },
        errorMessage = errorMessage,
        isSubmitting = isSubmitting,
        onReplace = onReplace,
    )

    when (LocalAppWindowSizeClass.current) {
        AppWindowSizeClass.Compact -> CompactSessionLimit(state, onCancel, onSeePlans, modifier)
        AppWindowSizeClass.Medium, AppWindowSizeClass.Expanded -> SignInSplitLayout(modifier) { paneModifier ->
            WideSessionLimitPane(state, onCancel, onSeePlans, paneModifier)
        }
    }
}

/** What both layouts of [SessionLimitScreen] show, and when the pick can be submitted. */
private class SessionLimitState(
    val maxSessions: Int,
    val sessions: List<FydeOpenSession>,
    val selectedId: String?,
    val onSelect: (String) -> Unit,
    val errorMessage: String?,
    val isSubmitting: Boolean,
    private val onReplace: (sessionId: String) -> Unit,
) {
    val selected: FydeOpenSession? get() = sessions.firstOrNull { it.id == selectedId }

    val canSubmit: Boolean get() = !isSubmitting && selected != null

    fun submit() {
        val session = selected
        if (canSubmit && session != null) onReplace(session.id)
    }
}

/** O5. The list scrolls when the window is too short; the actions stay pinned at the bottom. */
@Composable
private fun CompactSessionLimit(
    state: SessionLimitState,
    onCancel: () -> Unit,
    onSeePlans: (() -> Unit)?,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background)
            .safeContentPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppDimensions.maxWidthDetail)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.space6),
            ) {
                Spacer(Modifier.height(AppSpacing.space2))
                AppBackButton(onClick = onCancel, enabled = !state.isSubmitting)
                Spacer(Modifier.height(AppSpacing.space4))
                SessionLimitContent(state)
                Spacer(Modifier.height(AppSpacing.space6))
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(AppSpacing.space2),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space6)
                    .padding(top = AppSpacing.space4, bottom = AppSpacing.space5),
            ) {
                DisconnectButton(state, modifier = Modifier.fillMaxWidth())
                if (onSeePlans != null) {
                    AppTextButton(
                        text = stringResource(Res.string.session_limit_see_plans),
                        onClick = onSeePlans,
                        enabled = !state.isSubmitting,
                    )
                }
            }
        }
    }
}

/**
 * DO5's pane: back at the top left, hidden while the vault opens (as on
 * DO2b), around the list centred in an [AppDimensions.authFormColumn]
 * column, with "See plans" beside the disconnect button.
 */
@Composable
private fun WideSessionLimitPane(
    state: SessionLimitState,
    onCancel: () -> Unit,
    onSeePlans: (() -> Unit)?,
    modifier: Modifier,
) {
    Box(modifier = modifier.background(AppColors.background).safeContentPadding()) {
        CenteredScrollColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = AppDimensions.authFormColumn)
                    .fillMaxWidth()
                    // Clears the back button above the form when the window is short.
                    .padding(horizontal = AppSpacing.space7, vertical = AppSpacing.space10 + AppSpacing.space8),
            ) {
                SessionLimitContent(state)
                Spacer(Modifier.height(AppSpacing.space6))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (onSeePlans != null) {
                        AppTextButton(
                            text = stringResource(Res.string.session_limit_see_plans),
                            onClick = onSeePlans,
                            enabled = !state.isSubmitting,
                        )
                    }
                    // The rest of the row, not the mockup's fixed 2:1 split:
                    // its label carries a device name of any length.
                    DisconnectButton(
                        state,
                        loadingText = stringResource(Res.string.login_submitting),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (!state.isSubmitting) {
            AppBackButton(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = AppSpacing.space9, top = AppSpacing.space8),
            )
        }
    }
}

/** The title, the plan's limit, the sessions to pick from and the notes under them — the same at every size. */
@Composable
private fun SessionLimitContent(state: SessionLimitState) {
    Text(
        text = stringResource(Res.string.session_limit_title),
        style = AppTypography.display,
        color = AppColors.text,
    )
    Spacer(Modifier.height(AppSpacing.space3))
    Text(
        text = pluralStringResource(
            if (getPlatform().isDesktop) Res.plurals.session_limit_explanation_computer else Res.plurals.session_limit_explanation_device,
            state.maxSessions,
            state.maxSessions,
        ),
        style = AppTypography.body,
        color = AppColors.textMuted,
    )
    Spacer(Modifier.height(AppSpacing.space6))
    SessionPicker(state)
    Spacer(Modifier.height(AppSpacing.space4))
    AppNote(
        message = stringResource(Res.string.session_limit_reconnect_note),
        kind = AppNoteKind.Info,
        icon = { tint -> InfoGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
    )
    if (state.errorMessage != null) {
        Spacer(Modifier.height(AppSpacing.space4))
        AppNote(message = state.errorMessage, kind = AppNoteKind.Error, icon = { tint ->
            AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint)
        })
    }
}

/**
 * The open sessions as one `surface` group with hairline separators
 * (`tokens.css`'s `.group`), each row a radio choice: the mark, the
 * device's kind, its name and since when it's connected.
 */
@Composable
private fun SessionPicker(state: SessionLimitState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppRadius.lg)
            .background(AppColors.surface)
            .border(AppDimensions.borderHairline, AppColors.border, AppRadius.lg)
            .selectableGroup(),
    ) {
        state.sessions.forEachIndexed { index, session ->
            if (index > 0) AppDivider()
            SessionChoice(
                session = session,
                selected = session.id == state.selectedId,
                enabled = !state.isSubmitting,
                onClick = { state.onSelect(session.id) },
            )
        }
    }
}

/** One row of [SessionPicker]; the whole row is the radio button. */
@Composable
private fun SessionChoice(session: FydeOpenSession, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .appFocusRing(interactionSource, shape = AppRadius.none)
            .heightIn(min = AppDimensions.rowHeightStandard)
            .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space3),
    ) {
        AppRadio(selected = selected, enabled = enabled)
        // 24, off AppIconSize's scale: the mockup's `.ic.lg`, sized to the
        // two-line row as a leading illustration rather than an inline icon.
        val glyphModifier = Modifier.size(24.dp)
        if (isPhoneSession(session)) {
            PhoneGlyph(modifier = glyphModifier)
        } else {
            LaptopGlyph(modifier = glyphModifier)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.deviceName,
                style = AppTypography.bodyEmphasis,
                color = if (enabled) AppColors.text else AppColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(Res.string.session_limit_connected_since, localizedDate(session.createdAt)),
                style = AppTypography.caption,
                color = AppColors.textMuted,
            )
        }
    }
}

/**
 * "Disconnect <device>" once one is picked, "Choose a device" (disabled)
 * until then. While the login reruns it spins, with [loadingText] if given.
 */
@Composable
private fun DisconnectButton(state: SessionLimitState, modifier: Modifier = Modifier, loadingText: String? = null) {
    val selected = state.selected
    AppButton(
        text = if (selected != null) {
            stringResource(Res.string.session_limit_disconnect, selected.deviceName)
        } else {
            stringResource(Res.string.session_limit_pick)
        },
        onClick = state::submit,
        enabled = state.canSubmit,
        loading = state.isSubmitting,
        loadingText = loadingText,
        modifier = modifier,
    )
}

/**
 * Whether [session] runs on a phone, for its glyph. Sessions carry no device
 * kind, only the name each platform registers them under
 * ([Platform.name]: "Android 34", "iOS 18.1", "Desktop (Linux)"), so the
 * mobile ones are told apart by that name; anything else is a computer.
 */
internal fun isPhoneSession(session: FydeOpenSession): Boolean =
    MOBILE_DEVICE_PREFIXES.any { session.deviceName.startsWith(it, ignoreCase = true) }

private val MOBILE_DEVICE_PREFIXES = listOf("Android", "iOS", "iPadOS")
