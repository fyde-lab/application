package com.peltoche.fyde.components.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.icons.ChevronRightGlyph
import com.peltoche.fyde.components.overlay.AppDropdownMenu
import com.peltoche.fyde.components.overlay.AppDropdownMenuItem
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/**
 * A single-choice select, generic over [T] so callers aren't forced to
 * pre-stringify their data — only [optionLabel] needs to know how to render
 * one. Laid out like [AppTextField] (its [label] above the box, then
 * [supportingText] — or [errorText], in `error` — under it), so a select
 * sits among text fields in one field language. Whether the menu is
 * currently open is kept internal (`remember`): it's transient interaction
 * state with no meaning outside this component, unlike [selectedOption],
 * which is real hoisted state the caller owns.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppDropdown(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val helper = errorText ?: supportingText

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label, style = AppTypography.bodyEmphasis, color = if (enabled) AppColors.text else AppColors.textMuted)
        Spacer(Modifier.height(AppSpacing.space2))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it },
        ) {
            OutlinedTextField(
                value = selectedOption?.let(optionLabel) ?: "",
                onValueChange = {},
                // The label above isn't attached to the field, so the field names itself.
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                    .semantics { contentDescription = label },
                readOnly = true,
                enabled = enabled,
                singleLine = true,
                placeholder = placeholder?.let { { Text(it) } },
                isError = errorText != null,
                // The app's own outline chevron, pointing down (up while open),
                // not M3's filled triangle — found via review that this was the
                // one filled caret beside every outline chevron elsewhere (filter
                // chips, list rows).
                trailingIcon = {
                    ChevronRightGlyph(modifier = Modifier.size(12.dp).rotate(if (expanded) -90f else 90f), tint = AppColors.textMuted)
                },
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
                    focusedPlaceholderColor = AppColors.textMuted,
                    unfocusedPlaceholderColor = AppColors.textMuted,
                ),
            )
            AppDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(),
            ) {
                options.forEach { option ->
                    AppDropdownMenuItem(
                        text = optionLabel(option),
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
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
