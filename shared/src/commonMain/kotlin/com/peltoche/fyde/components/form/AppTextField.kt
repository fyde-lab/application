package com.peltoche.fyde.components.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppShapes

/**
 * The general-purpose bordered field — design-system.md § Shape/Inputs
 * calls for a bordered field, superseding the app's original underline
 * style (still used by the pre-existing `AuthTextField`, untouched here;
 * migrating its call sites is follow-up work, not this component).
 *
 * No hardcoded copy: every user-visible string is a required parameter, so
 * callers always supply their own localized string.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = (errorText ?: supportingText)?.let { { Text(it) } },
        isError = errorText != null,
        singleLine = singleLine,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        shape = AppShapes.input,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.primary,
            unfocusedBorderColor = AppColors.borderStrong,
            disabledBorderColor = AppColors.border,
            errorBorderColor = AppColors.error,
            focusedLabelColor = AppColors.primary,
            unfocusedLabelColor = AppColors.textMuted,
            errorLabelColor = AppColors.error,
            focusedTextColor = AppColors.text,
            unfocusedTextColor = AppColors.text,
            cursorColor = AppColors.primary,
            errorCursorColor = AppColors.error,
            focusedSupportingTextColor = AppColors.textMuted,
            unfocusedSupportingTextColor = AppColors.textMuted,
            errorSupportingTextColor = AppColors.error,
        ),
    )
}
