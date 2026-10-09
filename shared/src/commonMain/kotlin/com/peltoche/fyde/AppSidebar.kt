package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.navigation.AppNavigationItem
import com.peltoche.fyde.components.navigation.AppNavigationSidebarItem
import com.peltoche.fyde.layout.AppScaffold
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.encryption_badge_description
import com.peltoche.fyde.resources.encryption_badge_title
import com.peltoche.fyde.resources.nav_addons
import com.peltoche.fyde.resources.nav_search
import com.peltoche.fyde.resources.scripts_menu_item
import com.peltoche.fyde.resources.settings
import com.peltoche.fyde.resources.sidebar_app_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * The app's top-level destinations — peers, each reachable from every other
 * at every window size via [TopLevelScaffold]. Used to also carry several
 * `enabled = false` placeholders (All documents, Categories, Suppliers,
 * Issuers, Dates, Favorites) that never linked anywhere real — dropped
 * rather than shipped as dead links.
 */
enum class SidebarDestination { Documents, Scripts, Addons, Settings }

/**
 * The shell every top-level destination ([DocumentListScreen],
 * [ScriptListScreen], [AddonsScreen], [SettingsScreen]) and its drill-downs
 * ([AddonDetailScreen]) compose into, so navigation is one decision made
 * once rather than re-derived per screen.
 *
 * Navigation is present at every [AppWindowSizeClass] — the sidebar at
 * Expanded, a bottom bar below that. Found via review: it used to be
 * desktop-only, which left Scripts and Add-ons unreachable below 840dp, and
 * left Add-ons a dead end (no back, no nav) if the window shrank while it
 * was open. With four peer destinations the spec's "no bottom nav until 3+
 * peers" threshold is met.
 *
 * Medium gets the bottom bar too, not `AppNavigationLayout`'s rail: the rail
 * takes ~80dp of width, and Medium's document two-pane layout (fixed 360dp
 * list) can't spare it — at 600dp it would leave the preview pane ~120dp.
 */
@Composable
fun TopLevelScaffold(
    current: SidebarDestination,
    onOpenDocuments: () -> Unit,
    onOpenScripts: () -> Unit,
    onOpenAddons: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val windowSizeClass = LocalAppWindowSizeClass.current
    val isDesktop = windowSizeClass == AppWindowSizeClass.Expanded
    AppScaffold(
        modifier = modifier,
        // Only picks which navigation surface AppNavigationLayout draws —
        // content below still reads the real size class.
        windowSizeClass = if (isDesktop) AppWindowSizeClass.Expanded else AppWindowSizeClass.Compact,
        navigationItems = appNavigationItems(
            current = current,
            // The sidebar pins Settings in its footer (below the trust
            // badge); the bar and rail have no footer, so there it's a
            // regular item instead.
            includeSettings = !isDesktop,
            onOpenDocuments = onOpenDocuments,
            onOpenScripts = onOpenScripts,
            onOpenAddons = onOpenAddons,
            onOpenSettings = onOpenSettings,
        ),
        navigationHeader = if (isDesktop) { { SidebarHeader() } } else null,
        navigationFooter = if (isDesktop) { { SidebarFooter(current = current, onOpenSettings = onOpenSettings) } } else null,
        topBar = topBar,
        content = content,
    )
}

@Composable
private fun appNavigationItems(
    current: SidebarDestination,
    includeSettings: Boolean,
    onOpenDocuments: () -> Unit,
    onOpenScripts: () -> Unit,
    onOpenAddons: () -> Unit,
    onOpenSettings: () -> Unit,
): List<AppNavigationItem> {
    val items = mutableListOf(
        navigationItem(stringResource(Res.string.nav_search), current == SidebarDestination.Documents, onOpenDocuments) { tint ->
            SearchGlyph(tint = tint, modifier = Modifier.size(20.dp))
        },
        navigationItem(stringResource(Res.string.scripts_menu_item), current == SidebarDestination.Scripts, onOpenScripts) { tint ->
            CodeGlyph(tint = tint, modifier = Modifier.size(20.dp))
        },
        navigationItem(stringResource(Res.string.nav_addons), current == SidebarDestination.Addons, onOpenAddons) { tint ->
            PuzzleGlyph(tint = tint, modifier = Modifier.size(20.dp))
        },
    )
    if (includeSettings) items += settingsNavigationItem(current, onOpenSettings)
    return items
}

@Composable
private fun settingsNavigationItem(current: SidebarDestination, onOpenSettings: () -> Unit): AppNavigationItem =
    navigationItem(stringResource(Res.string.settings), current == SidebarDestination.Settings, onOpenSettings) { tint ->
        GearGlyph(tint = tint, modifier = Modifier.size(20.dp))
    }

/**
 * The glyphs here draw their own color rather than reading
 * `LocalContentColor`, so selection tint is passed in explicitly. Each
 * screen passes a no-op for its own destination; a drill-down passes its
 * back action instead, so re-selecting its parent returns there.
 */
private fun navigationItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (tint: Color) -> Unit,
): AppNavigationItem = AppNavigationItem(
    label = label,
    selected = selected,
    onClick = onClick,
    icon = { icon(if (selected) AppColors.primary else AppColors.textMuted) },
)

@Composable
private fun SidebarHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DocumentGlyph(modifier = Modifier.size(22.dp), tint = AppColors.primary, strokeWidth = 1.8.dp)
        Spacer(Modifier.width(AppSpacing.space3))
        Text(
            text = stringResource(Res.string.sidebar_app_title),
            style = AppTypography.section,
            color = AppColors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Sidebar foot: a static end-to-end-encryption trust badge (a real property
 * of this app, see the top-level design docs), then the real Settings
 * destination — drawn with [AppNavigationSidebarItem], the same component as
 * every other sidebar destination. Found via review: it used to be a
 * hand-built row with its own selected fill, text color and a ~44dp height,
 * visibly different from the 56dp items above it and under the 48dp
 * touch-target minimum.
 */
@Composable
private fun SidebarFooter(current: SidebarDestination, onOpenSettings: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                // Same inset as the nav items, so the badge's edges line up with theirs.
                .padding(NavigationDrawerItemDefaults.ItemPadding)
                .fillMaxWidth()
                .clip(AppRadius.md)
                .background(AppColors.success.copy(alpha = 0.10f))
                .padding(AppSpacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LockGlyph(modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(AppSpacing.space3))
            Column {
                Text(stringResource(Res.string.encryption_badge_title), style = AppTypography.caption, color = AppColors.text)
                Text(stringResource(Res.string.encryption_badge_description), style = AppTypography.micro, color = AppColors.textMuted)
            }
        }
        Spacer(Modifier.height(AppSpacing.space2))
        AppNavigationSidebarItem(settingsNavigationItem(current, onOpenSettings))
        Spacer(Modifier.height(AppSpacing.space3))
    }
}
