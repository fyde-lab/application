package com.peltoche.fyde.components.overlay

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors

/**
 * No screen has a bottom-sheet pattern yet, but it's a broadly generic
 * overlay worth having ready (e.g. mobile filter/actions sheets). State is
 * hoisted via [SheetState] — M3's own convention, the same shape as
 * [com.peltoche.fyde.components.feedback.AppSnackbarHost]'s use of
 * `SnackbarHostState` — so the caller owns visibility, not this component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        // Top corners only, at AppRadius.lg's value (10dp) — RoundedCornerShape
        // can't express "one radius token, only some corners" any other way.
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        containerColor = AppColors.surfaceElevated,
        contentColor = AppColors.text,
        content = content,
    )
}
