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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.feedback.AppSnackbarHost
import com.peltoche.fyde.components.navigation.AppNavigationItem
import com.peltoche.fyde.components.navigation.AppNavigationSidebarItem
import com.peltoche.fyde.layout.AppScaffold
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.encryption_badge_description
import com.peltoche.fyde.resources.encryption_badge_title
import com.peltoche.fyde.resources.nav_scripts
import com.peltoche.fyde.resources.nav_documents
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
 * rather than shipped as dead links. Scripts and Add-ons used to be two
 * peers listing and uninstalling the very same installed objects under two
 * names, so they were folded into one, Scripts (see [ScriptsScreen]).
 */
enum class SidebarDestination { Documents, Scripts, Settings }

/**
 * The one snackbar host every top-level destination shows transient
 * confirmations in ("Uploaded 3 documents", "Installed “Impots”"). Provided
 * once by `App`, above every screen, so a confirmation triggered just before
 * navigating away still gets shown on the next screen instead of being torn
 * down with the one that raised it.
 */
val LocalAppSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

/**
 * The shell every top-level destination ([DocumentListScreen],
 * [ScriptsScreen], [SettingsScreen]) and its drill-downs
 * ([ScriptDetailScreen], the compact document detail) compose into, so
 * navigation is one decision made once rather than re-derived per screen.
 *
 * Navigation is present at every [AppWindowSizeClass] — the sidebar at
 * Expanded, a bottom bar below that. Found via review: it used to be
 * desktop-only, which left Scripts unreachable below 840dp, and left it a
 * dead end (no back, no nav) if the window shrank while it was open.
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
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val windowSizeClass = LocalAppWindowSizeClass.current
    val isDesktop = windowSizeClass == AppWindowSizeClass.Expanded
    val snackbarHostState = LocalAppSnackbarHostState.current
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
            onOpenSettings = onOpenSettings,
        ),
        navigationHeader = if (isDesktop) { { SidebarHeader() } } else null,
        navigationFooter = if (isDesktop) { { SidebarFooter(current = current, onOpenSettings = onOpenSettings) } } else null,
        topBar = topBar,
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        content = content,
    )
}

@Composable
private fun appNavigationItems(
    current: SidebarDestination,
    includeSettings: Boolean,
    onOpenDocuments: () -> Unit,
    onOpenScripts: () -> Unit,
    onOpenSettings: () -> Unit,
): List<AppNavigationItem> {
    // Labelled after the destination it opens ("Documents", also its top
    // bar's title), not after one feature of it — found via review: it was
    // "Search" in the nav, "Documents" in the title bar and "My documents"
    // in the sidebar header, three names for one place.
    val items = mutableListOf(
        navigationItem(stringResource(Res.string.nav_documents), current == SidebarDestination.Documents, onOpenDocuments) { tint ->
            DocumentGlyph(tint = tint, modifier = Modifier.size(20.dp))
        },
        navigationItem(stringResource(Res.string.nav_scripts), current == SidebarDestination.Scripts, onOpenScripts) { tint ->
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

/** The product's own name, not a section name — this is the app's identity, not a destination. */
@Composable
private fun SidebarHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark()
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
 * Fyde's mark: the product logo ([FydeLogo]). Found via review: the sidebar
 * header used to draw [DocumentGlyph] — the very glyph of the Documents
 * destination right below it — so the product's identity read as a
 * duplicate nav icon. Full-color rather than outlined, so it can't be
 * mistaken for one of the outline destination glyphs either.
 */
@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    FydeLogo(modifier = modifier.size(28.dp))
}

/**
 * A static end-to-end-encryption trust badge (a real property of this app,
 * see the top-level design docs). Shown in the desktop sidebar's footer and
 * in Settings' Account section — found via review that it only ever
 * appeared in the sidebar, so phone and tablet users never saw it at all.
 */
@Composable
internal fun EncryptionBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
}

/**
 * Sidebar foot: [EncryptionBadge], then the real Settings destination —
 * drawn with [AppNavigationSidebarItem], the same component as every other
 * sidebar destination. Found via review: it used to be a hand-built row
 * with its own selected fill, text color and a ~44dp height, visibly
 * different from the 56dp items above it and under the 48dp touch-target
 * minimum.
 */
@Composable
private fun SidebarFooter(current: SidebarDestination, onOpenSettings: () -> Unit) {
    Column {
        // Same inset as the nav items, so the badge's edges line up with theirs.
        EncryptionBadge(modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
        Spacer(Modifier.height(AppSpacing.space2))
        AppNavigationSidebarItem(settingsNavigationItem(current, onOpenSettings))
        Spacer(Modifier.height(AppSpacing.space3))
    }
}
