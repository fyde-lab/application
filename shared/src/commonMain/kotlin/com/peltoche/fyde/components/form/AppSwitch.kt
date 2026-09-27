package com.peltoche.fyde.components.form

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/** Same labeled-row a11y pattern as [AppCheckbox] — see its doc comment. */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
) {
    val colors = SwitchDefaults.colors(
        checkedThumbColor = AppColors.onPrimary,
        checkedTrackColor = AppColors.primary,
        checkedBorderColor = AppColors.primary,
        uncheckedThumbColor = AppColors.surface,
        uncheckedTrackColor = AppColors.surfaceElevated,
        uncheckedBorderColor = AppColors.borderStrong,
    )
    if (label == null) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled, colors = colors)
        return
    }
    Row(
        modifier = modifier.toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            enabled = enabled,
            role = Role.Switch,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, colors = colors)
        Spacer(modifier = Modifier.width(AppSpacing.space3))
        Text(
            text = label,
            style = AppTypography.body,
            color = if (enabled) AppColors.text else AppColors.textMuted,
        )
    }
}
