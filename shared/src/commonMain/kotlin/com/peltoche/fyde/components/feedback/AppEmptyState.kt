package com.peltoche.fyde.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/**
 * Icon-in-circle / title / description / optional-action — generalizes the
 * layout already proven for the document list's empty state. No hardcoded
 * copy: title/description/actionLabel are all caller-supplied strings.
 *
 * [actionVariant] is Filled by default, for a state that *is* the whole
 * screen; pass Outlined when the screen already has its own filled primary
 * action (e.g. Upload in the top bar), so two filled buttons don't compete
 * — design-system.md § Shape/Buttons allows one per screen.
 */
@Composable
fun AppEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionVariant: AppButtonVariant = AppButtonVariant.Filled,
) {
    StateMessage(
        title = title,
        description = description,
        modifier = modifier,
        iconBackground = AppColors.surfaceElevated,
        icon = icon,
        actionLabel = actionLabel,
        onAction = onAction,
        actionVariant = actionVariant,
    )
}

/** Shared by [AppEmptyState] and [AppErrorState] — they differ only in icon-circle tint. */
@Composable
internal fun StateMessage(
    title: String,
    description: String,
    iconBackground: Color,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionVariant: AppButtonVariant = AppButtonVariant.Filled,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background)
            .padding(horizontal = AppSpacing.space8),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape).background(iconBackground),
                    contentAlignment = Alignment.Center,
                ) { icon() }
                Spacer(Modifier.height(AppSpacing.space5))
            }
            Text(text = title, style = AppTypography.section, color = AppColors.text)
            Spacer(Modifier.height(AppSpacing.space2))
            Text(
                text = description,
                style = AppTypography.body,
                color = AppColors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(AppSpacing.space6))
                AppButton(text = actionLabel, onClick = onAction, variant = actionVariant)
            }
        }
    }
}
