package com.peltoche.fyde.components.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.peltoche.fyde.EyeGlyph
import com.peltoche.fyde.components.action.AppIconButton
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.hide_password
import com.peltoche.fyde.resources.show_password
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * The general-purpose bordered field, as drawn in O2/O3
 * (`design/assets/mockups/tokens.css`'s `.field`): its [label] above a
 * `surface` box with a `borderStrong` hairline (2dp `primary` when
 * focused), then [supportingText] — or [errorText], in `error` — flush
 * under it. Used everywhere text input is needed, so the app has one field
 * language from first launch onward.
 *
 * An [isPassword] field masks its value and carries an eye button to show
 * it (design-system.md O2: "Mot de passe (eye)"), in place of any
 * [trailingIcon] — unless [revealable] is false, as on O3's confirmation
 * field, which shows its [trailingIcon] (a check once it matches) instead.
 *
 * No hardcoded copy: every user-visible string is a required parameter, so
 * callers always supply their own localized string. [textStyle] overrides
 * the entered text's style only (not the label's) — e.g. the Data role for
 * a field holding code, where a proportional face misaligns it.
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
    revealable: Boolean = true,
    singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textStyle: TextStyle? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val passwordToggle: (@Composable () -> Unit)? = if (isPassword && revealable) {
        {
            AppIconButton(
                onClick = { passwordVisible = !passwordVisible },
                contentDescription = stringResource(if (passwordVisible) Res.string.hide_password else Res.string.show_password),
                enabled = enabled,
            ) {
                EyeGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.textMuted, crossed = passwordVisible)
            }
        }
    } else {
        null
    }
    val helper = errorText ?: supportingText

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = AppTypography.bodyEmphasis, color = if (enabled) AppColors.text else AppColors.textMuted)
        Spacer(Modifier.height(AppSpacing.space2))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            // The label above isn't attached to the field, so the field names itself.
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            enabled = enabled,
            placeholder = placeholder?.let { { Text(it) } },
            isError = errorText != null,
            singleLine = singleLine,
            leadingIcon = leadingIcon,
            trailingIcon = passwordToggle ?: trailingIcon,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = if (isPassword && keyboardOptions.keyboardType == KeyboardType.Unspecified) {
                keyboardOptions.copy(keyboardType = KeyboardType.Password)
            } else {
                keyboardOptions
            },
            keyboardActions = keyboardActions,
            textStyle = textStyle ?: LocalTextStyle.current,
            shape = AppShapes.input,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppColors.surface,
                unfocusedContainerColor = AppColors.surface,
                disabledContainerColor = AppColors.background,
                errorContainerColor = AppColors.surface,
                focusedBorderColor = AppColors.primary,
                unfocusedBorderColor = AppColors.borderStrong,
                disabledBorderColor = AppColors.border,
                errorBorderColor = AppColors.error,
                focusedTextColor = AppColors.text,
                unfocusedTextColor = AppColors.text,
                cursorColor = AppColors.primary,
                errorCursorColor = AppColors.error,
                focusedPlaceholderColor = AppColors.textMuted,
                unfocusedPlaceholderColor = AppColors.textMuted,
            ),
        )
        if (helper != null) {
            Spacer(Modifier.height(AppSpacing.space2))
            Text(
                text = helper,
                style = AppTypography.caption,
                color = if (errorText != null) AppColors.error else AppColors.textMuted,
            )
        }
    }
}
