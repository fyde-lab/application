package com.peltoche.fyde.components.action

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.appFocusRing

/** Emphasis level for [AppButton] — see design-system.md § Shape/Buttons. */
enum class AppButtonVariant { Filled, Tonal, Outlined }

/**
 * The app's "regular" button shape. [Filled][AppButtonVariant.Filled],
 * [Tonal][AppButtonVariant.Tonal], and [Outlined][AppButtonVariant.Outlined]
 * share identical size/shape and differ only in color emphasis, so one
 * component with a variant covers all three — [AppTextButton] and
 * [AppIconButton] are different enough shapes to be their own components.
 *
 * `loading` disables the button and swaps its content for a spinner rather
 * than adding a second boolean the caller has to reason about alongside
 * `enabled`.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val isEnabled = enabled && !loading
    val interactionSource = remember { MutableInteractionSource() }
    val buttonModifier = modifier
        .height(AppDimensions.touchTargetMin)
        .appFocusRing(interactionSource, shape = AppShapes.button)
        .semantics { if (loading) stateDescription = "Loading" }

    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        } else {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(AppSpacing.space2))
            }
            Text(text)
        }
    }

    when (variant) {
        AppButtonVariant.Filled -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = isEnabled,
            shape = AppShapes.button,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.primary,
                contentColor = AppColors.onPrimary,
            ),
            content = content,
        )
        AppButtonVariant.Tonal -> FilledTonalButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = isEnabled,
            shape = AppShapes.button,
            interactionSource = interactionSource,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            content = content,
        )
        AppButtonVariant.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = isEnabled,
            shape = AppShapes.button,
            interactionSource = interactionSource,
            border = BorderStroke(1.dp, if (isEnabled) AppColors.borderStrong else AppColors.border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.text),
            content = content,
        )
    }
}
