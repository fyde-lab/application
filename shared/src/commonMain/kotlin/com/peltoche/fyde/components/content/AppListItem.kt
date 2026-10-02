package com.peltoche.fyde.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppTypography

/**
 * Generalizes the icon/title/caption row already used for documents, but
 * with no coupling to that domain — pure slots. Deliberately doesn't draw
 * its own divider: composing an [AppDivider] between items is the caller's
 * call, since not every list wants one between every row.
 *
 * [selected] is for master-detail lists (pick one item, preview it in a
 * second pane) — it tints the row with `selectionSurface` and, when
 * [onClick] is given, uses `Modifier.selectable` with `Role.RadioButton`
 * (not plain `clickable`, and not `Role.Tab` — a list row picked from a set
 * is a radio-button selection semantically, not a tab strip) so assistive
 * tech gets accurate "this is the selected item in a set" semantics.
 *
 * The title always truncates to one line: a list row is meant to be
 * scanned, and an unbounded title wrapping to two or three lines makes row
 * height inconsistent across a list and across breakpoints for the exact
 * same content. Callers needing full text have the full string regardless
 * (e.g. in a detail pane) — this component's job is the scannable row.
 */
@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    supportingTextStyle: TextStyle = AppTypography.caption,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val rowModifier = if (onClick != null) {
        modifier.selectable(selected = selected, enabled = enabled, onClick = onClick, role = Role.RadioButton)
    } else {
        modifier
    }
    val textColor = if (enabled) AppColors.text else AppColors.textMuted
    val mutedColor = AppColors.textMuted

    Box(modifier = rowModifier) {
        ListItem(
            headlineContent = {
                Text(title, style = AppTypography.bodyEmphasis, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = supportingText?.let { { Text(it, style = supportingTextStyle, color = mutedColor) } },
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            colors = ListItemDefaults.colors(
                containerColor = if (selected) AppColors.selectionSurface else AppColors.surface,
                headlineColor = textColor,
                supportingColor = mutedColor,
            ),
        )
        // A left accent bar on top of the selection tint — found via review of
        // the reference layout that a flat tint alone reads weakly as "this
        // row is the one open in the detail pane"; a 3dp primary-colored edge
        // is the same affordance list/mail master-detail UIs converge on.
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(AppColors.primary),
            )
        }
    }
}
