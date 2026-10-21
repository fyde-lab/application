package com.peltoche.fyde

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.content.AppIconTile
import com.peltoche.fyde.components.form.AppCheckbox
import com.peltoche.fyde.components.overlay.AppBottomSheet
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.allow_insecure_storage_label
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.insecure_storage_continue
import com.peltoche.fyde.resources.insecure_storage_message
import com.peltoche.fyde.resources.insecure_storage_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Stockage indisponible (O4, `design/assets/screens/O4-stockage-indisponible.png`):
 * shown over [LoginScreen] or [RegisterScreen] when this device has no OS
 * credential store (Keychain, Secret Service, ...) to keep the account's
 * keys in. It says what keeping them in the local database instead exposes,
 * and [onContinue] — the same sign-in or registration, now allowed to fall
 * back to it — only unlocks once the user ticks that they accept it.
 * [onDismiss] (Annuler, or dismissing the sheet) leaves the form as it was.
 *
 * Pure presentation, like the screens it covers: `App.kt` decides when it
 * shows and what continuing runs.
 */
@Composable
fun InsecureStorageSheet(onContinue: () -> Unit, onDismiss: () -> Unit) {
    // Never pre-ticked, even when the sheet comes back after a cancel: the
    // risk is accepted each time, on purpose.
    var accepted by remember { mutableStateOf(false) }

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.space5)) {
            AppIconTile(size = AppDimensions.iconTileLarge, tint = AppColors.warning) {
                // 24, off AppIconSize's scale: design-system.md § Iconography
                // sizes the glyph of a large tile at 24.
                AlertTriangleGlyph(modifier = Modifier.size(24.dp), tint = AppColors.warning)
            }
            Spacer(Modifier.height(AppSpacing.space4))
            Text(
                text = stringResource(Res.string.insecure_storage_title),
                style = AppTypography.title,
                color = AppColors.text,
            )
            Spacer(Modifier.height(AppSpacing.space2))
            Text(
                text = stringResource(Res.string.insecure_storage_message),
                style = AppTypography.body,
                color = AppColors.textMuted,
            )
            Spacer(Modifier.height(AppSpacing.space4))
            AppCheckbox(
                checked = accepted,
                onCheckedChange = { accepted = it },
                label = stringResource(Res.string.allow_insecure_storage_label),
                modifier = Modifier.fillMaxWidth(),
            )
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
