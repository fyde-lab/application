package com.peltoche.fyde.components.action

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.peltoche.fyde.components.overlay.AppDropdownMenu
import com.peltoche.fyde.components.overlay.AppDropdownMenuItem
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.appFocusRing

/** One action in an [AppMenuButton]'s dropdown. */
data class AppMenuAction(val label: String, val onClick: () -> Unit, val destructive: Boolean = false)

/**
 * A trigger (avatar, icon, anything) that opens a dropdown of actions —
 * generalizes the profile-avatar-with-settings/log-out pattern, which is
 * really just "menu button," not something specific to a profile. Whether
 * the menu is open is kept internal: it's transient interaction state no
 * caller needs to observe, same reasoning as [com.peltoche.fyde.components.form.AppDropdown]'s
 * `expanded`.
 *
 * The tap target is always [AppDimensions.touchTargetMin], regardless of
 * how small [trigger]'s own visual content is — found via review that the
 * original profile-avatar trigger this generalizes had shrunk its hit area
 * to match its 18dp glyph, well under the app's own touch-target minimum.
 */
@Composable
fun AppMenuButton(
    actions: List<AppMenuAction>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    trigger: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(AppDimensions.touchTargetMin)
                .appFocusRing(interactionSource, shape = AppRadius.full)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClickLabel = contentDescription,
                    role = Role.Button,
                ) { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            trigger()
        }
        AppDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                AppDropdownMenuItem(
                    text = action.label,
                    destructive = action.destructive,
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
