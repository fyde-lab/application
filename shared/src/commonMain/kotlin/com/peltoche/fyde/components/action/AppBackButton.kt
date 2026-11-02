package com.peltoche.fyde.components.action

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.peltoche.fyde.ChevronLeftGlyph
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.back
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.appFocusRing
import org.jetbrains.compose.resources.stringResource

/**
 * The circular back button of a pushed screen (design-system.md § Compact:
 * "a 44pt circular back button"), as drawn in O2 and O3: an [AppCircleButton]
 * around a left chevron, labelled "Back" for assistive tech.
 */
@Composable
fun AppBackButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    AppCircleButton(onClick = onClick, contentDescription = stringResource(Res.string.back), modifier = modifier, enabled = enabled) {
        ChevronLeftGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.text)
    }
}

/**
 * A 44dp `surface` circle with a `border` hairline around one glyph: the
 * back button, and a header's lone action where a labelled button would
 * crowd the title (S1's "+").
 */
@Composable
fun AppCircleButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(AppDimensions.backButton)
            .appFocusRing(interactionSource, shape = CircleShape)
            .clip(CircleShape)
            .background(AppColors.surface)
            .border(AppDimensions.borderHairline, AppColors.border, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
