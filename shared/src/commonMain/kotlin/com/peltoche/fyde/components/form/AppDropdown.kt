package com.peltoche.fyde.components.form

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
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.icons.ChevronRightGlyph
import com.peltoche.fyde.components.overlay.AppDropdownMenu
import com.peltoche.fyde.components.overlay.AppDropdownMenuItem
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppShapes

/**
 * A single-choice select, generic over [T] so callers aren't forced to
 * pre-stringify their data — only [optionLabel] needs to know how to render
 * one. Whether the menu is currently open is kept internal (`remember`):
 * it's transient interaction state with no meaning outside this component,
 * unlike [selectedOption], which is real hoisted state the caller owns.
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
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedOption?.let(optionLabel) ?: "",
            onValueChange = {},
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            // The app's own outline chevron, pointing down (up while open),
            // not M3's filled triangle — found via review that this was the
            // one filled caret beside every outline chevron elsewhere (filter
            // chips, list rows).
            trailingIcon = {
                ChevronRightGlyph(modifier = Modifier.size(12.dp).rotate(if (expanded) -90f else 90f), tint = AppColors.textMuted)
            },
            shape = AppShapes.input,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.primary,
                unfocusedBorderColor = AppColors.borderStrong,
                disabledBorderColor = AppColors.border,
                focusedTextColor = AppColors.text,
                unfocusedTextColor = AppColors.text,
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
}
