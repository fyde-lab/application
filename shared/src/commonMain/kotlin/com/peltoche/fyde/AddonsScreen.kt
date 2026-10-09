package com.peltoche.fyde

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.content.AppCard
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.components.feedback.AppLoading
import com.peltoche.fyde.components.feedback.AppSkeleton
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.addons_available_section
import com.peltoche.fyde.resources.addons_empty_description
import com.peltoche.fyde.resources.addons_empty_title
import com.peltoche.fyde.resources.addons_installed_section
import com.peltoche.fyde.resources.addons_title
import com.peltoche.fyde.resources.connect_failed_title
import com.peltoche.fyde.resources.connecting_message
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.script_version
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
     * [AddonDetailScreen], opened by clicking a card) is in flight, so a
     * second tap there can't race the first. [banner] surfaces a non-fatal
     * problem (an install/uninstall call failing) without discarding what's
     * already loaded — on whichever of the grid or [AddonDetailScreen] is
     * showing.
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

    TopLevelScaffold(
        current = SidebarDestination.Addons,
        onOpenDocuments = onOpenDocuments,
        onOpenScripts = onOpenScripts,
        onOpenAddons = {},
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        topBar = { AppTopBar(title = stringResource(Res.string.addons_title), windowSizeClass = windowSizeClass) },
    ) { contentPadding ->
        AppContentContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(AppColors.background),
            width = AppContentWidth.List,
            windowSizeClass = windowSizeClass,
            sidebarAdjacent = windowSizeClass == AppWindowSizeClass.Expanded,
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

/** Minimum grid cell width — `GridCells.Adaptive` fits as many columns of at least this as the width allows. */
private val AddonCellMinWidth = 220.dp

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
        columns = GridCells.Adaptive(minSize = AddonCellMinWidth),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppSpacing.space7),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space4),
    ) {
        if (installed.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(stringResource(Res.string.addons_installed_section, installed.size))
            }
            items(installed, key = { it.id }) { script ->
                AddonCard(script = script, onClick = { onView(script) })
            }
        }
        if (available.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(stringResource(Res.string.addons_available_section, available.size))
            }
            items(available, key = { it.id }) { script ->
                AddonCard(script = script, onClick = { onView(script) })
            }
        }
    }
}

/** Card summary lines — fixed, so every card in a grid row is the same height. */
private const val CARD_SUMMARY_LINES = 2

/**
 * One grid cell: icon chip, name, and a short summary. The whole card is
 * the click target that opens [AddonDetailScreen].
 *
 * Found via review: each card used to carry its own outlined "View" button,
 * which at desktop width left the name ~108dp (about 12 characters) beside
 * it — the real cause of the truncation a 2-line name was papering over.
 * Wrapping names to 2 lines broke design-system.md's one-line rule for
 * scannable titles and made cards in the same grid row different heights.
 * Without the button the name has the full width, stays on one line, and
 * the second line carries [FydeScript.shortDescription] (written for exactly
 * this) instead of a bare version number.
 */
@Composable
private fun AddonCard(
    script: FydeScript,
    onClick: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
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
                    text = script.shortDescription.ifBlank { script.description },
                    style = AppTypography.caption,
                    color = AppColors.textMuted,
                    minLines = CARD_SUMMARY_LINES,
                    maxLines = CARD_SUMMARY_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * A script's version and last-update date as one Data-role line — shared by
 * [AddonDetailScreen] and [ScriptListScreen]'s rows. Found via review: the
 * two screens showed the same fact as "Version 3" (Caption) and a hardcoded,
 * unlocalized "v3 · date" (Data).
 */
@Composable
internal fun scriptVersionLine(script: FydeScript): String =
    "${stringResource(Res.string.script_version, script.version)} · ${formatDate(script.lastUpdated)}"

/**
 * [icon]'s image content never fills [AddonIcon]'s full chip — publisher
 * logos arrive in wildly inconsistent shapes/paddings (a flat-filled square,
 * a circular mark on transparent, ...; see each scraper's `logo.png` under
 * `scripts/scripts/` in the `scripts` repo), and filling the chip edge-to-edge
 * let each one's own background show through (or not) at random, so chips
 * looked inconsistently "framed" next to each other. Insetting every logo
 * the same amount makes the chip read as a deliberate, uniform frame instead.
 */
private const val ICON_IMAGE_SCALE = 0.72f
private const val ICON_GLYPH_SCALE = 0.45f

/**
 * A neutral chip showing [icon] (the script's own PNG logo, see
 * [FydeScript.icon]), decoded via [decodeIconBitmap]. Falls back to a drawn
 * [PuzzleGlyph] when [icon] is empty or isn't valid image data. [size]
 * defaults to the grid card's compact chip; [AddonDetailScreen] passes a
 * larger one for a stronger hero presence, [ScriptListScreen] a row-sized one.
 *
 * Neutral (page background + hairline border), not tinted with `primary`.
 * Found via review: third-party brand marks (red, blue, purple) on a teal
 * wash clashed, and spent the one accent color on decoration rather than
 * emphasis (design-system.md § Direction, "one disciplined accent").
 */
@Composable
internal fun AddonIcon(icon: ByteArray = ByteArray(0), modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val bitmap = remember(icon) { decodeIconBitmap(icon) }
    Box(
        modifier = modifier
            .size(size)
            .clip(AppRadius.md)
            .background(AppColors.background)
            .border(AppDimensions.borderHairline, AppColors.border, AppRadius.md),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size * ICON_IMAGE_SCALE).clip(AppRadius.sm),
            )
        } else {
            PuzzleGlyph(tint = AppColors.textMuted, modifier = Modifier.size(size * ICON_GLYPH_SCALE))
        }
    }
}

@Composable
private fun AddonsSkeletonGrid(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = AddonCellMinWidth),
        modifier = modifier.fillMaxSize(),
        // Top padding stands in for the section header the loaded grid opens with.
        contentPadding = PaddingValues(top = AppSpacing.space7, bottom = AppSpacing.space7),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space4),
    ) {
        items(6) { SkeletonCard() }
    }
}

/** Same anatomy as [AddonCard] — icon, one name line, two summary lines — so loading doesn't shift the layout. */
@Composable
private fun SkeletonCard() {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            AppSkeleton(modifier = Modifier.size(44.dp), shape = AppRadius.md)
            Spacer(Modifier.width(AppSpacing.space4))
            Column(modifier = Modifier.weight(1f)) {
                AppSkeleton(modifier = Modifier.padding(vertical = 4.dp).size(width = 120.dp, height = 14.dp))
                Spacer(Modifier.height(AppSpacing.space1))
                AppSkeleton(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().height(10.dp))
                AppSkeleton(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(0.6f).height(10.dp))
            }
        }
    }
}
