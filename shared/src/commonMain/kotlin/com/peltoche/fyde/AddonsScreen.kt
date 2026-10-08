package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.content.AppCard
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.components.feedback.AppLoading
import com.peltoche.fyde.components.feedback.AppSkeleton
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppScaffold
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.addon_version
import com.peltoche.fyde.resources.addons_available_section
import com.peltoche.fyde.resources.addons_empty_description
import com.peltoche.fyde.resources.addons_empty_title
import com.peltoche.fyde.resources.addons_installed_section
import com.peltoche.fyde.resources.addons_title
import com.peltoche.fyde.resources.addon_view_button
import com.peltoche.fyde.resources.connect_failed_title
import com.peltoche.fyde.resources.connecting_message
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.resources.retry
import org.jetbrains.compose.resources.stringResource

/**
 * Everything [AddonsScreen] needs to render, and nothing it doesn't — no
 * `FydeSdk` types appear here beyond [FydeScript] itself, which is already
 * SDK-free (see its own doc comment). The caller (see `AddonsScreenContainer`
 * in `App.kt`) owns the SDK calls and maps their results into this state,
 * same split as [DocumentListUiState]/[ScriptListUiState].
 */
sealed interface AddonsUiState {
    /** Not yet known whether the server is reachable; nothing to show at all. */
    data object Connecting : AddonsUiState

    /** The initial connection failed outright. */
    data class ConnectFailed(val message: String, val onRetry: () -> Unit) : AddonsUiState

    /**
     * Connected. [scripts] is `null` while the first page is still being
     * fetched (known shape, unknown content — rendered as a skeleton grid
     * rather than a spinner), and non-null once loaded (possibly empty).
     * [enabledScriptIds] marks which of [scripts] the authenticated user
     * currently has installed — those are grouped into their own "Installed"
     * section, ahead of every other public script. [pendingScriptIds] marks
     * a script whose install/uninstall call (triggered from
     * [AddonDetailScreen], opened via a card's "View" button) is in flight,
     * so a second tap there can't race the first. [banner] surfaces a
     * non-fatal problem (an install/uninstall call failing) without
     * discarding what's already loaded.
     */
    data class Loaded(
        val scripts: List<FydeScript>?,
        val enabledScriptIds: Set<String> = emptySet(),
        val pendingScriptIds: Set<String> = emptySet(),
        val banner: String? = null,
        val onDismissBanner: () -> Unit = {},
    ) : AddonsUiState
}

@Composable
fun AddonsScreen(
    state: AddonsUiState,
    modifier: Modifier = Modifier,
    onView: (FydeScript) -> Unit = {},
    onOpenDocuments: () -> Unit = {},
    onOpenScripts: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    when (state) {
        AddonsUiState.Connecting -> AppLoading(
            modifier = modifier,
            message = stringResource(Res.string.connecting_message),
        )
        is AddonsUiState.ConnectFailed -> AppErrorState(
            modifier = modifier,
            icon = { AlertGlyph(modifier = Modifier.size(32.dp)) },
            title = stringResource(Res.string.connect_failed_title),
            description = state.message,
            actionLabel = stringResource(Res.string.retry),
            onAction = state.onRetry,
        )
        is AddonsUiState.Loaded -> LoadedAddons(state, modifier, onView, onOpenDocuments, onOpenScripts, onOpenSettings)
    }
}

@Composable
private fun LoadedAddons(
    state: AddonsUiState.Loaded,
    modifier: Modifier = Modifier,
    onView: (FydeScript) -> Unit = {},
    onOpenDocuments: () -> Unit = {},
    onOpenScripts: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val windowSizeClass = LocalAppWindowSizeClass.current
    // Same desktop-only persistent sidebar as DocumentListScreen/
    // ScriptListScreen/SettingsScreen — see DocumentListScreen's isDesktop
    // doc comment for why Compact/Medium stay top-bar-only.
    val isDesktop = windowSizeClass == AppWindowSizeClass.Expanded

    AppScaffold(
        modifier = modifier,
        windowSizeClass = windowSizeClass,
        navigationItems = if (isDesktop) {
            sidebarNavigationItems(current = SidebarDestination.Addons, onOpenDocuments = onOpenDocuments, onOpenScripts = onOpenScripts, onOpenAddons = {})
        } else {
            emptyList()
        },
        navigationHeader = if (isDesktop) { { SidebarHeader() } } else null,
        navigationFooter = if (isDesktop) { { SidebarFooter(current = SidebarDestination.Addons, onOpenSettings = onOpenSettings) } } else null,
        topBar = {
            AppTopBar(title = stringResource(Res.string.addons_title), windowSizeClass = windowSizeClass)
        },
    ) { contentPadding ->
        AppContentContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(AppColors.background),
            width = AppContentWidth.List,
            windowSizeClass = windowSizeClass,
            sidebarAdjacent = isDesktop,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                state.banner?.let { message ->
                    AppBanner(message = message, onDismiss = state.onDismissBanner, dismissLabel = stringResource(Res.string.dismiss))
                }

                val scripts = state.scripts
                when {
                    scripts == null -> AddonsSkeletonGrid(modifier = Modifier.weight(1f))

                    scripts.isEmpty() -> AppEmptyState(
                        modifier = Modifier.weight(1f),
                        icon = { PuzzleGlyph(modifier = Modifier.size(40.dp), tint = AppColors.textMuted, strokeWidth = 2.dp) },
                        title = stringResource(Res.string.addons_empty_title),
                        description = stringResource(Res.string.addons_empty_description),
                    )

                    else -> AddonsGrid(
                        scripts = scripts,
                        enabledScriptIds = state.enabledScriptIds,
                        onView = onView,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * The installed/available split, each its own full-width section header
 * ahead of its cards — "Installed" always sorts first, satisfying "on top
 * the installed ones" without requiring a second scrollable region (one
 * [LazyVerticalGrid], two spans of items). A script's own [FydeScript.name]
 * breaks ties within each section, since the server gives no other ordering
 * signal (`list_public_scripts` orders by last-updated, which isn't a
 * useful browsing order for a grid the user scans by name).
 */
@Composable
private fun AddonsGrid(
    scripts: List<FydeScript>,
    enabledScriptIds: Set<String>,
    onView: (FydeScript) -> Unit,
    modifier: Modifier = Modifier,
) {
    val installed = scripts.filter { it.id in enabledScriptIds }.sortedBy { it.name.lowercase() }
    val available = scripts.filter { it.id !in enabledScriptIds }.sortedBy { it.name.lowercase() }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = AppSpacing.space4),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space4),
    ) {
        if (installed.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(stringResource(Res.string.addons_installed_section, installed.size))
            }
            items(installed, key = { it.id }) { script ->
                AddonCard(script = script, onView = { onView(script) })
            }
        }
        if (available.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(stringResource(Res.string.addons_available_section, available.size))
            }
            items(available, key = { it.id }) { script ->
                AddonCard(script = script, onView = { onView(script) })
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = AppTypography.micro,
        color = AppColors.textMuted,
        modifier = Modifier.padding(vertical = AppSpacing.space2),
    )
}

/** One grid cell: icon chip, name + version, and a "View" button that opens [AddonDetailScreen]. */
@Composable
private fun AddonCard(
    script: FydeScript,
    onView: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AddonIcon(icon = script.icon)
            Spacer(Modifier.width(AppSpacing.space4))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = script.name,
                    style = AppTypography.bodyEmphasis,
                    color = AppColors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(AppSpacing.space1))
                Text(
                    text = stringResource(Res.string.addon_version, script.version.toInt()),
                    style = AppTypography.caption,
                    color = AppColors.textMuted,
                )
            }
            Spacer(Modifier.width(AppSpacing.space3))
            AppButton(text = stringResource(Res.string.addon_view_button), onClick = onView, variant = AppButtonVariant.Outlined)
        }
    }
}

/**
 * A tinted chip showing [icon] (the script's own PNG logo, see
 * [FydeScript.icon]), decoded via [decodeIconBitmap]. Falls back to a drawn
 * [PuzzleGlyph] — same shape as [DocumentListScreen]'s `FileTypeIcon`, every
 * addon sharing one tint since there's no per-type palette to vary by here —
 * when [icon] is empty or isn't valid image data. Also used by
 * [AddonDetailScreen].
 */
@Composable
internal fun AddonIcon(icon: ByteArray = ByteArray(0), modifier: Modifier = Modifier) {
    val bitmap = remember(icon) { decodeIconBitmap(icon) }
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(AppRadius.md)
            .background(AppColors.primary.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(44.dp).clip(AppRadius.md),
            )
        } else {
            PuzzleGlyph(tint = AppColors.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AddonsSkeletonGrid(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = AppSpacing.space4),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space4),
    ) {
        items(6) { SkeletonCard() }
    }
}

@Composable
private fun SkeletonCard() {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppSkeleton(modifier = Modifier.size(44.dp), shape = AppRadius.md)
            Spacer(Modifier.width(AppSpacing.space4))
            Column(modifier = Modifier.weight(1f)) {
                AppSkeleton(modifier = Modifier.size(width = 120.dp, height = 14.dp))
                Spacer(Modifier.height(AppSpacing.space1))
                AppSkeleton(modifier = Modifier.size(width = 70.dp, height = 10.dp))
            }
        }
    }
}
