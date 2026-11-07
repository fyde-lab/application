package com.peltoche.fyde

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import com.peltoche.fyde.components.feedback.AppSnackbarHost
import com.peltoche.fyde.components.navigation.AppNavigationBar
import com.peltoche.fyde.components.navigation.AppNavigationItem
import com.peltoche.fyde.icons.GearGlyph
import com.peltoche.fyde.icons.GridGlyph
import com.peltoche.fyde.icons.PlugGlyph
import com.peltoche.fyde.layout.AppWindowSizeClassProvider
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.layout.appWindowSizeClassOf
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.nav_scripts
import com.peltoche.fyde.resources.nav_vault
import com.peltoche.fyde.resources.settings
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppIconSize
import org.jetbrains.compose.resources.stringResource

/**
 * The one snackbar host every top-level destination shows transient
 * confirmations in ("Uploaded 3 documents", "Installed “Impots”"). Provided
 * once by `App`, above every screen, so a confirmation triggered just before
 * navigating away still gets shown on the next screen instead of being torn
 * down with the one that raised it.
 */
val LocalAppSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

/** A category the sidebar lists, with how many documents it holds. */
data class SidebarCategory(val category: FydeCategory, val count: Long)

/**
 * The sidebar's figures, loaded by `App.kt`: the vault's [total], the
 * categories holding documents (in [CategoryDisplayOrder]), the enabled
 * sources and the devices the vault is sealed to. `null` while unknown.
 */
data class SidebarSummary(
    val total: Long? = null,
    val categories: List<SidebarCategory> = emptyList(),
    val sourceCount: Int? = null,
    val deviceCount: Int? = null,
)

/**
 * Where the shell's navigation leads. `App.kt` builds it; each screen
 * overrides its own destination's handlers (`copy`), so re-selecting it
 * returns to its first page instead of reopening it.
 */
data class TopLevelNavigation(
    val summary: SidebarSummary = SidebarSummary(),
    val onOpenVault: () -> Unit,
    val onOpenCategory: (FydeCategory) -> Unit,
    val onOpenScripts: () -> Unit,
    val onOpenSettings: () -> Unit,
)

/**
 * The shell every top-level destination ([VaultScreen], [SourcesTabScreen],
 * [SettingsScreen]) and its drill-downs compose into, so navigation is one
 * decision made once rather than re-derived per screen.
 *
 * Compact has the bottom bar (Coffre, Sources, Settings). Medium has the
 * icon rail and Expanded the full sidebar (design-system.md § Layout, T2 and
 * T1/D1–D6). Beside the sidebar, the content gets the window size class of
 * the width left to it, so a page lays itself out for the room it really has
 * (computed, not measured: see [AppWindowSizeClassProvider]) — but never
 * Compact, whose phone pages (drill-down bars, stacked dialogs) don't belong
 * beside a sidebar.
 *
 * One layout tree at every size, so crossing a breakpoint keeps the
 * content's state instead of rebuilding it.
 */
@Composable
fun TopLevelScaffold(
    navigation: TopLevelNavigation,
    selection: SidebarSelection,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val windowSizeClass = LocalAppWindowSizeClass.current
    val snackbarHostState = LocalAppSnackbarHostState.current
    val windowWidth = LocalWindowInfo.current.containerDpSize.width
    val contentSizeClass = when (windowSizeClass) {
        AppWindowSizeClass.Expanded -> maxOf(AppWindowSizeClass.Medium, appWindowSizeClassOf(windowWidth - SidebarWidth))
        else -> windowSizeClass
    }
    Row(modifier = modifier.fillMaxSize()) {
        when (windowSizeClass) {
            AppWindowSizeClass.Compact -> Unit
            AppWindowSizeClass.Medium -> AppSidebarRail(navigation, selection)
            AppWindowSizeClass.Expanded -> AppSidebar(navigation, selection)
        }
        Box(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(LocalAppWindowSizeClass provides contentSizeClass) {
                Scaffold(
                    topBar = topBar,
                    bottomBar = {
                        if (windowSizeClass == AppWindowSizeClass.Compact) {
                            AppNavigationBar(items = bottomBarItems(navigation, selection))
                        }
                    },
                    snackbarHost = { AppSnackbarHost(snackbarHostState) },
                    containerColor = AppColors.background,
                    // Beside a sidebar, it keeps clear of the leading edge's insets itself.
                    contentWindowInsets = if (windowSizeClass == AppWindowSizeClass.Compact) {
                        ScaffoldDefaults.contentWindowInsets
                    } else {
                        ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    },
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun bottomBarItems(navigation: TopLevelNavigation, selection: SidebarSelection): List<AppNavigationItem> {
    val inVault = selection == SidebarSelection.Vault || selection is SidebarSelection.Category
    return listOf(
        bottomBarItem(stringResource(Res.string.nav_vault), inVault, navigation.onOpenVault) { tint ->
            GridGlyph(tint = tint, modifier = Modifier.size(AppIconSize.md))
        },
        bottomBarItem(stringResource(Res.string.nav_scripts), selection == SidebarSelection.Sources, navigation.onOpenScripts) { tint ->
            PlugGlyph(tint = tint, modifier = Modifier.size(AppIconSize.md))
        },
        bottomBarItem(stringResource(Res.string.settings), selection == SidebarSelection.Settings, navigation.onOpenSettings) { tint ->
            GearGlyph(tint = tint, modifier = Modifier.size(AppIconSize.md))
        },
    )
}

/** The glyphs draw their own color rather than reading `LocalContentColor`, so the selection tint is passed in. */
private fun bottomBarItem(
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
