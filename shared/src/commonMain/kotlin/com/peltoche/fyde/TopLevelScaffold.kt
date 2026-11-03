package com.peltoche.fyde

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppIconButton
import com.peltoche.fyde.components.feedback.AppSnackbarHost
import com.peltoche.fyde.components.navigation.AppNavigationItem
import com.peltoche.fyde.layout.AppScaffold
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.app_title
import com.peltoche.fyde.resources.encryption_badge_description
import com.peltoche.fyde.resources.encryption_badge_title
import com.peltoche.fyde.resources.nav_vault
import com.peltoche.fyde.resources.nav_scripts
import com.peltoche.fyde.resources.settings
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
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
 * names, so they were folded into one, Scripts, shown as Sources (see
 * [SourcesTabScreen]).
 */
enum class TopLevelDestination { Vault, Scripts, Settings }

/**
 * The one snackbar host every top-level destination shows transient
 * confirmations in ("Uploaded 3 documents", "Installed “Impots”"). Provided
 * once by `App`, above every screen, so a confirmation triggered just before
 * navigating away still gets shown on the next screen instead of being torn
 * down with the one that raised it.
 */
val LocalAppSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

/**
 * The shell every top-level destination ([VaultScreen],
 * [SourcesTabScreen], [SettingsScreen]) and its drill-downs
 * (a category, a document, a source's pages) compose into, so
 * navigation is one decision made once rather than re-derived per screen.
 *
 * Navigation is present at every [AppWindowSizeClass] — a top bar at
 * Expanded, a bottom bar below that. Found via review: it used to be
 * desktop-only, which left Scripts unreachable below 840dp, and left it a
 * dead end (no back, no nav) if the window shrank while it was open.
 *
 * At Expanded the top bar carries the product's name, Vault and Scripts
 * as tabs, then the encryption indicator, Settings and [primaryAction] at
 * its far end. It replaced a sidebar that spent a whole column on two
 * destinations. [primaryAction] is only drawn there: below Expanded a screen
 * keeps its primary action in its own [topBar].
 *
 * Medium gets the bottom bar too, not `AppNavigationLayout`'s rail: the rail
 * takes ~80dp of width a Medium window's three-column category grid can't
 * spare.
 */
@Composable
fun TopLevelScaffold(
    current: TopLevelDestination,
    onOpenVault: () -> Unit,
    onOpenScripts: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    primaryAction: (@Composable () -> Unit)? = null,
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
            // The top bar has Settings as an icon at its far end; the
            // bottom bar has no such end, so there it's a regular item.
            includeSettings = !isDesktop,
            onOpenVault = onOpenVault,
            onOpenScripts = onOpenScripts,
            onOpenSettings = onOpenSettings,
        ),
        navigationHeader = if (isDesktop) { { BrandHeader() } } else null,
        navigationTrailing = if (isDesktop) {
            {
                EncryptionIndicator()
                SettingsButton(selected = current == TopLevelDestination.Settings, onClick = onOpenSettings)
                if (primaryAction != null) {
                    Spacer(Modifier.width(AppSpacing.space3))
                    primaryAction()
                }
            }
        } else {
            null
        },
        topBar = topBar,
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        content(contentPadding)
    }
}

@Composable
private fun appNavigationItems(
    current: TopLevelDestination,
    includeSettings: Boolean,
    onOpenVault: () -> Unit,
    onOpenScripts: () -> Unit,
    onOpenSettings: () -> Unit,
): List<AppNavigationItem> {
    // Labelled after the destination it opens ("Vault", also its title),
    // not after one feature of it, so one place has one name.
    val items = mutableListOf(
        navigationItem(stringResource(Res.string.nav_vault), current == TopLevelDestination.Vault, onOpenVault) { tint ->
            GridGlyph(tint = tint, modifier = Modifier.size(AppIconSize.md))
        },
        navigationItem(stringResource(Res.string.nav_scripts), current == TopLevelDestination.Scripts, onOpenScripts) { tint ->
            CodeGlyph(tint = tint, modifier = Modifier.size(AppIconSize.md))
        },
    )
    if (includeSettings) {
        items += navigationItem(stringResource(Res.string.settings), current == TopLevelDestination.Settings, onOpenSettings) { tint ->
            GearGlyph(tint = tint, modifier = Modifier.size(AppIconSize.md))
        }
    }
    return items
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
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrandMark()
        Spacer(Modifier.width(AppSpacing.space3))
        Text(
            text = stringResource(Res.string.app_title),
            style = AppTypography.section,
            color = AppColors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Fyde's mark: the product logo ([FydeLogo]). Found via review: the
 * navigation header used to draw [DocumentGlyph] — the very glyph of the
 * Documents destination right beside it — so the product's identity read as
 * a duplicate nav icon. Full-color rather than outlined, so it can't be
 * mistaken for one of the outline destination glyphs either.
 */
@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    FydeLogo(modifier = modifier.size(28.dp))
}

/** The Settings destination at the desktop top bar's far end, tinted like a selected tab while open. */
@Composable
private fun SettingsButton(selected: Boolean, onClick: () -> Unit) {
    AppIconButton(onClick = onClick, contentDescription = stringResource(Res.string.settings)) {
        GearGlyph(tint = if (selected) AppColors.primary else AppColors.textMuted, modifier = Modifier.size(AppIconSize.md))
    }
}

/**
 * The end-to-end-encryption indicator: the lock alone, its
 * title and description in a tooltip on hover and in its accessibility
 * label. Not a button — it states a property of the app, there is nothing
 * to open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EncryptionIndicator() {
    val title = stringResource(Res.string.encryption_badge_title)
    val description = stringResource(Res.string.encryption_badge_description)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text("$title — $description") } },
        state = rememberTooltipState(),
    ) {
        Box(
            modifier = Modifier
                .size(AppDimensions.touchTargetMin)
                .semantics { contentDescription = "$title. $description" },
            contentAlignment = Alignment.Center,
        ) {
            LockGlyph(tint = AppColors.textMuted, modifier = Modifier.size(AppIconSize.md))
        }
    }
}
