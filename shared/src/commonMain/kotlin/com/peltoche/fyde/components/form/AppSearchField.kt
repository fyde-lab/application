package com.peltoche.fyde.components.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peltoche.fyde.components.action.AppIconButton
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppShapes

/**
 * A search field: a label-less [AppTextField] variant with an optional
 * leading affordance and a trailing clear action once there's something to
 * clear. Generalizes the private `SearchField` already used ad hoc in the
 * document list.
 */
@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    clearContentDescription: String? = null,
    onClear: (() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        placeholder = { Text(placeholder) },
        singleLine = true,
        leadingIcon = leadingIcon,
        trailingIcon = if (onClear != null && value.isNotEmpty() && clearContentDescription != null) {
            {
                AppIconButton(onClick = onClear, contentDescription = clearContentDescription) {
                    Text("×")
                }
            }
        } else null,
        shape = AppShapes.input,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.primary,
            unfocusedBorderColor = AppColors.borderStrong,
            disabledBorderColor = AppColors.border,
            focusedTextColor = AppColors.text,
            unfocusedTextColor = AppColors.text,
            cursorColor = AppColors.primary,
        ),
    )
}
