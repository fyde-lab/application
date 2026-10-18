package com.peltoche.fyde.components.action

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.appFocusRing

/**
 * Low-emphasis action or inline link — see design-system.md § Buttons
 * ("Text/ghost"). `loading` mirrors [AppButton]'s same-named parameter —
 * swaps the content for a spinner and disables the button, rather than
 * making every caller with a text-button-triggered async action hand-roll
 * that swap itself. [destructive] colors the label with `error` instead of
 * `primary`, for the confirm action of a prompt about something that can't
 * be undone (design-system.md § Color: `error` is for destructive actions).
 *
 * Shaped and focus-ringed like [AppButton]. Found via review: it kept M3's
 * own text-button shape, a full pill — visible as its pressed/hovered state
 * layer — the one shape design-system.md § Shape forbids for buttons, and
 * drew no focus ring when reached from the keyboard.
 */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    destructive: Boolean = false,
) {
    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        } else {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(AppSpacing.space2))
            }
            Text(text)
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        modifier = modifier.appFocusRing(interactionSource, shape = AppShapes.button),
        enabled = enabled && !loading,
        shape = AppShapes.button,
        interactionSource = interactionSource,
        colors = ButtonDefaults.textButtonColors(contentColor = if (destructive) AppColors.error else AppColors.primary),
        content = content,
    )
}
