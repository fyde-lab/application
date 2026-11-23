package com.peltoche.fyde

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppIconTile
import com.peltoche.fyde.components.form.AppCheckbox
import com.peltoche.fyde.components.overlay.AppBottomSheet
import com.peltoche.fyde.icons.AlertTriangleGlyph
import com.peltoche.fyde.icons.SyncGlyph
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.allow_insecure_storage_label
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.insecure_storage_continue
import com.peltoche.fyde.resources.insecure_storage_create_anyway
import com.peltoche.fyde.resources.insecure_storage_desktop_message
import com.peltoche.fyde.resources.insecure_storage_desktop_risk
import com.peltoche.fyde.resources.insecure_storage_desktop_title
import com.peltoche.fyde.resources.insecure_storage_message
import com.peltoche.fyde.resources.insecure_storage_title
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Stockage indisponible: shown over [LoginScreen] or [RegisterScreen] when
 * this device has no OS credential store (Keychain, Secret Service, ...) to
 * keep the account's keys in. It says what keeping them in the local
 * database instead exposes, and [onContinue] — the same sign-in or
 * registration ([isRegistration]), now allowed to fall back to it — only
 * unlocks once the user ticks that they accept it. [onDismiss] (Cancel, or
 * dismissing the sheet) leaves the form as it was.
 *
 * Two references, one per platform:
 * - phones, O4 (`design/proposals/000-original-design/assets/screens/O4-stockage-indisponible.png`):
 *   Continue anyway and Cancel stacked under the checkbox.
 * - desktop, DO4b (`design/proposals/000-original-design/assets/screens/DO4b-stockage-indisponible-a-la-creation.png`):
 *   worded for a computer and its keyring daemons, with Cancel, Retry and
 *   Create anyway in a row. [onRetry] looks for the keyring again — one
 *   that was locked or not answering can come back, unlike a phone's.
 *   While [isRetrying], Retry spins.
 *
 * Pure presentation, like the screens it covers: `App.kt` decides when it
 * shows and what continuing or retrying runs.
 */
@Composable
fun InsecureStorageSheet(
    isRegistration: Boolean,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    isRetrying: Boolean = false,
) {
    // Never pre-ticked, even when the sheet comes back after a cancel: the
    // risk is accepted each time, on purpose.
    var accepted by remember { mutableStateOf(false) }
    val desktop = getPlatform().isDesktop

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.space5)) {
            AppIconTile(size = AppDimensions.iconTileLarge, tint = AppColors.warning) {
                // 24, off AppIconSize's scale: design-system.md § Iconography
                // sizes the glyph of a large tile at 24.
                AlertTriangleGlyph(modifier = Modifier.size(AppIconSize.prominent), tint = AppColors.warning)
            }
            Spacer(Modifier.height(AppSpacing.space4))
            Text(
                text = stringResource(if (desktop) Res.string.insecure_storage_desktop_title else Res.string.insecure_storage_title),
                style = AppTypography.title,
                color = AppColors.text,
            )
            Spacer(Modifier.height(AppSpacing.space2))
            Text(
                text = stringResource(if (desktop) Res.string.insecure_storage_desktop_message else Res.string.insecure_storage_message),
                style = AppTypography.body,
                color = AppColors.textMuted,
            )
            if (desktop) {
                Spacer(Modifier.height(AppSpacing.space3))
                Text(
                    text = stringResource(Res.string.insecure_storage_desktop_risk),
                    style = AppTypography.body,
                    color = AppColors.textMuted,
                )
            }
            Spacer(Modifier.height(AppSpacing.space4))
            AppCheckbox(
                checked = accepted,
                onCheckedChange = { accepted = it },
                label = stringResource(Res.string.allow_insecure_storage_label),
                modifier = Modifier.fillMaxWidth(),
            )
            if (desktop) {
                Spacer(Modifier.height(AppSpacing.space6))
                // Wraps rather than overflowing in a narrow window.
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.space2),
                ) {
                    AppTextButton(text = stringResource(Res.string.cancel), onClick = onDismiss)
                    AppButton(
                        text = stringResource(Res.string.retry),
                        onClick = onRetry,
                        variant = AppButtonVariant.Outlined,
                        loading = isRetrying,
                        leadingIcon = { SyncGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.text) },
                    )
                    AppButton(
                        text = stringResource(if (isRegistration) Res.string.insecure_storage_create_anyway else Res.string.insecure_storage_continue),
                        onClick = onContinue,
                        enabled = accepted && !isRetrying,
                    )
                }
            } else {
                Spacer(Modifier.height(AppSpacing.space4))
                AppButton(
                    text = stringResource(Res.string.insecure_storage_continue),
                    onClick = onContinue,
                    enabled = accepted,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(AppSpacing.space2))
                AppTextButton(
                    text = stringResource(Res.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
