package com.peltoche.fyde

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.content.AppBadge
import com.peltoche.fyde.components.content.AppBadgeVariant
import com.peltoche.fyde.components.content.AppCard
import com.peltoche.fyde.components.content.AppIconTile
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.components.feedback.AppLoading
import com.peltoche.fyde.components.feedback.AppSkeleton
import com.peltoche.fyde.components.form.AppSearchField
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.scripts_available_section
import com.peltoche.fyde.resources.scripts_empty_description
import com.peltoche.fyde.resources.scripts_empty_title
import com.peltoche.fyde.resources.scripts_installed_section
import com.peltoche.fyde.resources.script_running_badge
import com.peltoche.fyde.resources.scripts_title
import com.peltoche.fyde.resources.clear_search
import com.peltoche.fyde.resources.no_matches_title
import com.peltoche.fyde.resources.no_scripts_match
import com.peltoche.fyde.resources.search_scripts_placeholder
import com.peltoche.fyde.resources.connect_failed_title
import com.peltoche.fyde.resources.connecting_message
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.script_private_badge
import com.peltoche.fyde.resources.script_version
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Everything [ScriptsScreen] needs to render, and nothing it doesn't — no
 * `FydeSdk` types appear here beyond [FydeScript] itself, which is already
 * SDK-free (see its own doc comment). The caller (see `ScriptsScreenContainer`
 * in `App.kt`) owns the SDK calls and maps their results into this state,
 * same split as [DocumentListUiState].
 */
sealed interface ScriptsUiState {
    /** Not yet known whether the server is reachable; nothing to show at all. */
    data object Connecting : ScriptsUiState

    /** The initial connection failed outright. */
    data class ConnectFailed(val message: String, val onRetry: () -> Unit) : ScriptsUiState

    /**
     * Connected. [scripts] is `null` while the first page is still being
     * fetched (rendered as a skeleton grid rather than a spinner), and
     * non-null once loaded (possibly empty): every public script, plus the
     * user's own private ones they have enabled. [enabledScriptIds] marks
     * which of [scripts] the user currently has installed — those are
     * grouped into their own "Installed" section, ahead of every other
     * script. [installedParameters] holds the values each installed script
     * was last installed with, so [ScriptDetailScreen] can show and edit
     * them. [pendingScriptIds] marks a script whose install/uninstall call
     * is in flight, so a second tap can't race the first. [banner] surfaces
     * a failed call without discarding what's already loaded.
     */
    data class Loaded(
        val scripts: List<FydeScript>?,
        val enabledScriptIds: Set<String> = emptySet(),
        val installedParameters: Map<String, Map<String, FydeScriptParameterValue>> = emptyMap(),
        val pendingScriptIds: Set<String> = emptySet(),
        val banner: String? = null,
        val onDismissBanner: () -> Unit = {},
    ) : ScriptsUiState
}

/**
 * The one destination for scripts: public scripts to browse and install,
 * the ones already installed, and the user's own scripts. Found via review:
 * separate Scripts and Add-ons destinations listed and uninstalled the very
 * same installed objects under two names; they're folded into this one.
 * Renamed from Add-ons to Scripts since the SDK and the server only know
 * scripts.
 *
 * [query] narrows the grid to the scripts it matches (see [filterScripts]);
 * it's hoisted so it survives the drill-down into a script's page. Found via
 * review: over a hundred public scripts were one long alphabetical grid with
 * no way to find one but scrolling.
 */
@Composable
fun ScriptsScreen(
    state: ScriptsUiState,
    modifier: Modifier = Modifier,
    onView: (FydeScript) -> Unit = {},
    runningScriptId: String? = null,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    onOpenDocuments: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    when (state) {
        ScriptsUiState.Connecting -> AppLoading(
            modifier = modifier,
            message = stringResource(Res.string.connecting_message),
        )
        is ScriptsUiState.ConnectFailed -> AppErrorState(
            modifier = modifier,
            icon = { AlertGlyph(modifier = Modifier.size(AppIconSize.lg)) },
            title = stringResource(Res.string.connect_failed_title),
            description = state.message,
            actionLabel = stringResource(Res.string.retry),
            onAction = state.onRetry,
        )
        is ScriptsUiState.Loaded -> LoadedScripts(state, modifier, onView, runningScriptId, query, onQueryChange, onOpenDocuments, onOpenSettings)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedScripts(
    state: ScriptsUiState.Loaded,
    modifier: Modifier,
    onView: (FydeScript) -> Unit,
    runningScriptId: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val windowSizeClass = LocalAppWindowSizeClass.current

    TopLevelScaffold(
        current = TopLevelDestination.Scripts,
        onOpenDocuments = onOpenDocuments,
        onOpenScripts = {},
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.scripts_title),
                windowSizeClass = windowSizeClass,
            )
        },
    ) { contentPadding ->
        AppContentContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(AppColors.background),
            width = AppContentWidth.List,
            windowSizeClass = windowSizeClass,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                state.banner?.let { message ->
                    AppBanner(message = message, onDismiss = state.onDismissBanner, dismissLabel = stringResource(Res.string.dismiss))
                }

                val scripts = state.scripts
                when {
                    scripts == null -> ScriptsSkeletonGrid(modifier = Modifier.weight(1f))

                    scripts.isEmpty() -> AppEmptyState(
                        modifier = Modifier.weight(1f),
                        icon = { PuzzleGlyph(modifier = Modifier.size(AppIconSize.xl), tint = AppColors.textMuted, strokeWidth = 2.dp) },
                        title = stringResource(Res.string.scripts_empty_title),
                        description = stringResource(Res.string.scripts_empty_description),
                    )

                    else -> {
                        Spacer(Modifier.height(AppSpacing.space3))
                        AppSearchField(
                            value = query,
                            onValueChange = onQueryChange,
                            placeholder = stringResource(Res.string.search_scripts_placeholder),
                            onClear = { onQueryChange("") },
                            clearContentDescription = stringResource(Res.string.clear_search),
                            leadingIcon = { SearchGlyph(modifier = Modifier.size(AppIconSize.sm)) },
                        )
                        val matches = filterScripts(scripts, query)
                        if (matches.isEmpty()) {
                            AppEmptyState(
                                modifier = Modifier.weight(1f),
                                icon = { PuzzleGlyph(modifier = Modifier.size(AppIconSize.xl), tint = AppColors.textMuted, strokeWidth = 2.dp) },
                                title = stringResource(Res.string.no_matches_title),
                                description = stringResource(Res.string.no_scripts_match, query.trim()),
                                actionLabel = stringResource(Res.string.clear_search),
                                onAction = { onQueryChange("") },
                                actionVariant = AppButtonVariant.Outlined,
                            )
                        } else {
                            ScriptsGrid(
                                scripts = matches,
                                enabledScriptIds = state.enabledScriptIds,
                                runningScriptId = runningScriptId,
                                onView = onView,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The scripts [query] matches, in their original order: those whose name,
 * short description or description contains every word of it, ignoring
 * case and accents ("aesio" finds "Aésio"). A blank query matches all.
 */
internal fun filterScripts(scripts: List<FydeScript>, query: String): List<FydeScript> {
    val words = foldForSearch(query).split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return scripts
    return scripts.filter { script ->
        val haystack = foldForSearch("${script.name} ${script.shortDescription} ${script.description}")
        words.all { it in haystack }
    }
}

/** [text] lowercased, with the Latin accents scripts' names and descriptions use (French, mostly) folded away. */
private fun foldForSearch(text: String): String = buildString(text.length) {
    text.lowercase().forEach { char ->
        append(
            when (char) {
                'à', 'á', 'â', 'ã', 'ä', 'å' -> "a"
                'æ' -> "ae"
                'ç' -> "c"
                'è', 'é', 'ê', 'ë' -> "e"
                'ì', 'í', 'î', 'ï' -> "i"
                'ñ' -> "n"
                'ò', 'ó', 'ô', 'õ', 'ö' -> "o"
                'œ' -> "oe"
                'ù', 'ú', 'û', 'ü' -> "u"
                'ý', 'ÿ' -> "y"
                else -> char.toString()
            },
        )
    }
}

/** Minimum grid cell width — `GridCells.Adaptive` fits as many columns of at least this as the width allows. */
private val ScriptCellMinWidth = 220.dp

/**
 * The installed/available split, each its own full-width section header
 * ahead of its cards — "Installed" always sorts first (one
 * [LazyVerticalGrid], two spans of items). A script's own [FydeScript.name]
 * breaks ties within each section, since the server gives no other useful
 * browsing order.
 */
@Composable
private fun ScriptsGrid(
    scripts: List<FydeScript>,
    enabledScriptIds: Set<String>,
    runningScriptId: String?,
    onView: (FydeScript) -> Unit,
    modifier: Modifier = Modifier,
) {
    val installed = scripts.filter { it.id in enabledScriptIds }.sortedBy { it.name.lowercase() }
    val available = scripts.filter { it.id !in enabledScriptIds }.sortedBy { it.name.lowercase() }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = ScriptCellMinWidth),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppSpacing.space7),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space4),
    ) {
        if (installed.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(stringResource(Res.string.scripts_installed_section, installed.size))
            }
            items(installed, key = { it.id }) { script ->
                ScriptCard(script = script, isRunning = script.id == runningScriptId, onClick = { onView(script) })
            }
        }
        if (available.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(stringResource(Res.string.scripts_available_section, available.size))
            }
            items(available, key = { it.id }) { script ->
                ScriptCard(script = script, onClick = { onView(script) })
            }
        }
    }
}

/** Card summary lines — fixed, so every card in a grid row is the same height. */
private const val CARD_SUMMARY_LINES = 2

/**
 * One grid cell: icon chip, name, and a short summary. The whole card is
 * the click target that opens [ScriptDetailScreen]. The user's own private
 * scripts carry a "Private" badge, since they share the grid with public
 * scripts. The name always stays on one line (see design-system.md §
 * Typography, "A scannable row's title is one line").
 */
@Composable
private fun ScriptCard(
    script: FydeScript,
    onClick: () -> Unit,
    isRunning: Boolean = false,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            ScriptIcon(icon = script.icon)
            Spacer(Modifier.width(AppSpacing.space4))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = script.name,
                        style = AppTypography.bodyEmphasis,
                        color = AppColors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isRunning) {
                        Spacer(Modifier.width(AppSpacing.space2))
                        AppBadge(text = stringResource(Res.string.script_running_badge), variant = AppBadgeVariant.Primary)
                    } else if (!script.isPublic) {
                        Spacer(Modifier.width(AppSpacing.space2))
                        AppBadge(text = stringResource(Res.string.script_private_badge))
                    }
                }
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
 * [ScriptDetailScreen] and the grid.
 */
@Composable
internal fun scriptVersionLine(script: FydeScript): String =
    "${stringResource(Res.string.script_version, script.version)} · ${localizedDate(script.lastUpdated)}"

/**
 * [icon]'s image content never fills [ScriptIcon]'s full chip — publisher
 * logos arrive in wildly inconsistent shapes/paddings, and filling the chip
 * edge-to-edge let each one's own background show through (or not) at
 * random. Insetting every logo the same amount makes the chip read as a
 * deliberate, uniform frame instead.
 */
private const val ICON_IMAGE_SCALE = 0.72f
private const val ICON_GLYPH_SCALE = 0.45f

/**
 * A neutral chip showing [icon] (the script's own PNG logo, see
 * [FydeScript.icon]), decoded via [decodeIconBitmap]. Falls back to a drawn
 * [PuzzleGlyph] when [icon] is empty or isn't valid image data. Neutral
 * (page background + hairline border), not tinted with `primary`: third-party
 * brand marks on a teal wash clashed, and spent the one accent color on
 * decoration rather than emphasis.
 */
@Composable
internal fun ScriptIcon(icon: ByteArray = ByteArray(0), modifier: Modifier = Modifier, size: Dp = AppDimensions.iconTile) {
    val bitmap = remember(icon) { decodeIconBitmap(icon) }
    AppIconTile(modifier = modifier, size = size) {
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
private fun ScriptsSkeletonGrid(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = ScriptCellMinWidth),
        modifier = modifier.fillMaxSize(),
        // Top padding stands in for the section header the loaded grid opens with.
        contentPadding = PaddingValues(top = AppSpacing.space7, bottom = AppSpacing.space7),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space4),
    ) {
        items(6) { SkeletonCard() }
    }
}

/** Same anatomy as [ScriptCard] — icon, one name line, two summary lines — so loading doesn't shift the layout. */
@Composable
private fun SkeletonCard() {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            AppSkeleton(modifier = Modifier.size(AppDimensions.iconTile), shape = AppShapes.iconTile)
            Spacer(Modifier.width(AppSpacing.space4))
            Column(modifier = Modifier.weight(1f)) {
                AppSkeleton(modifier = Modifier.padding(vertical = AppSpacing.space2).size(width = 120.dp, height = 14.dp))
                Spacer(Modifier.height(AppSpacing.space1))
                AppSkeleton(modifier = Modifier.padding(vertical = AppSpacing.space2).fillMaxWidth().height(10.dp))
                AppSkeleton(modifier = Modifier.padding(vertical = AppSpacing.space2).fillMaxWidth(0.6f).height(10.dp))
            }
        }
    }
}
