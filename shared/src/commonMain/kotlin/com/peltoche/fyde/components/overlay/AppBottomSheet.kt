package com.peltoche.fyde.components.overlay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing

/**
 * A sheet for a short, contextual task (design-system.md § Patterns): the
 * caller shows it while it's composed and removes it in [onDismissRequest].
 *
 * At Compact it's a modal bottom sheet, opened fully (its content is one
 * short task, never worth a half-open state), with its grab handle. At
 * Medium and Expanded it's a centred panel instead — "phone sheets become
 * inline panels" (§ Layout) — dismissed by its own actions, Esc or a click
 * outside, never by dragging: sliding a sheet away is a touch gesture, out
 * of place with a mouse.
 *
 * [content] brings its own horizontal padding; this adds the space above
 * and below it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalAppWindowSizeClass.current != AppWindowSizeClass.Compact) {
        Dialog(onDismissRequest = onDismissRequest) {
            Surface(
                modifier = modifier.widthIn(max = AppDimensions.sheetPanelMaxWidth).fillMaxWidth(),
                shape = AppShapes.dialog,
                color = AppColors.surfaceElevated,
                contentColor = AppColors.text,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = AppSpacing.space6),
                    content = content,
                )
            }
        }
        return
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // Top corners only, at AppRadius.lg's value (10dp) — RoundedCornerShape
        // can't express "one radius token, only some corners" any other way.
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        containerColor = AppColors.surfaceElevated,
        contentColor = AppColors.text,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), content = content)
        Spacer(Modifier.height(AppSpacing.space6))
    }
}
