package com.peltoche.fyde.components.form

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/**
 * When [label] is given, the toggle target is the whole row — via
 * `Modifier.toggleable` on the [Row] itself, with the inner [Checkbox]'s own
 * `onCheckedChange` set to `null` so the click isn't handled twice — rather
 * than just the checkbox glyph. That's the M3-recommended pattern for a
 * labeled checkbox/switch: one tap target, one semantic node, instead of two
 * separately-focusable elements for what's really one control.
 */
@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
) {
    val colors = CheckboxDefaults.colors(
        checkedColor = AppColors.primary,
        uncheckedColor = AppColors.borderStrong,
        checkmarkColor = AppColors.onPrimary,
    )
    if (label == null) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled, colors = colors)
        return
    }
    Row(
        modifier = modifier.toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            enabled = enabled,
            role = Role.Checkbox,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled, colors = colors)
        Spacer(modifier = Modifier.width(AppSpacing.space3))
        Text(
            text = label,
            style = AppTypography.body,
            color = if (enabled) AppColors.text else AppColors.textMuted,
        )
    }
}
