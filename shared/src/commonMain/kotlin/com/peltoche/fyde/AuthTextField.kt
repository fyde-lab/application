package com.peltoche.fyde

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppShapes

/**
 * Transparent fill, floating label, bottom indicator only — shared by
 * [LoginScreen] and [RegisterScreen]. Kept separate from the document
 * list's own search field since the two have different label/placeholder
 * needs, but the token values match.
 *
 * `design/design-system.md` § Shape actually specifies a fully bordered
 * field (M3 `OutlinedTextField`), superseding this underline style — that's
 * a component-level rewrite, not a token change, so it's deliberately not
 * done here; only the color/shape tokens below are wired to the new theme.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = AppShapes.input,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = AppColors.primary,
            unfocusedIndicatorColor = AppColors.borderStrong,
            focusedLabelColor = AppColors.primary,
            unfocusedLabelColor = AppColors.textMuted,
            focusedTextColor = AppColors.text,
            unfocusedTextColor = AppColors.text,
            cursorColor = AppColors.text,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
