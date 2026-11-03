package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.peltoche.fyde.components.action.AppBackButton
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppCircleButton
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.CategoryCard
import com.peltoche.fyde.components.content.CategoryCardPlaceholder
import com.peltoche.fyde.components.content.DocumentRow
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.components.feedback.AppLoading
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.components.feedback.AppSkeleton
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.connect_failed_title
import com.peltoche.fyde.resources.connecting_message
import com.peltoche.fyde.resources.documents_count
import com.peltoche.fyde.resources.no_documents_description
import com.peltoche.fyde.resources.no_documents_title
import com.peltoche.fyde.resources.offline_banner
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.vault_add
import com.peltoche.fyde.resources.vault_category_card_description
import com.peltoche.fyde.resources.vault_category_card_description_latest
import com.peltoche.fyde.resources.vault_import
import com.peltoche.fyde.resources.vault_latest
import com.peltoche.fyde.resources.vault_sealed
import com.peltoche.fyde.resources.vault_sealed_devices
import com.peltoche.fyde.resources.vault_title
import com.peltoche.fyde.resources.vault_unclassified
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Which vault page is open: the home's categories (V1, D6), or one category's documents (V3, D1). */
sealed interface VaultPage {
    data object Home : VaultPage

    /** [category]'s documents, narrowed by its chips and sorted by arrival. */
    data class Category(
        val category: FydeCategory,
        val subcategory: FydeSubcategory? = null,
        val year: YearFilter? = null,
        val newestFirst: Boolean = true,
    ) : VaultPage {
        val query: VaultQuery get() = VaultQuery(category = category, subcategory = subcategory, year = year)
    }
}

/**
 * Where the user is in the vault: a [page], the [document] open over it
 * (V6, or beside it at Expanded, D2) and, below Expanded, whether its
 * content is shown full screen ([viewerOpen], V7).
 */
data class VaultFocus(
    val page: VaultPage = VaultPage.Home,
    val document: FydeDocument? = null,
    val viewerOpen: Boolean = false,
)

/** A home card: how many documents [category] holds, and the newest of them. */
data class VaultCategorySummary(val category: FydeCategory, val count: Long, val newest: FydeDocument?)

/** The home's content: every category holding documents, in [CategoryDisplayOrder], and the latest arrivals. */
data class VaultHome(val categories: List<VaultCategorySummary>, val latest: List<FydeDocument>)

/**
 * What a category page shows for [page]: how many documents its "Tout" chip
 * covers, the chips' other choices, and its documents in [page]'s order.
 */
data class VaultCategoryContents(
    val page: VaultPage.Category,
    val allCount: Long,
    val options: VaultFilterOptions,
    val documents: List<FydeDocument>,
)

sealed interface VaultUiState {
    data object Connecting : VaultUiState

    data class ConnectFailed(val message: String, val onRetry: () -> Unit) : VaultUiState

    /**
     * Connected. [home] and [category] are `null` until loaded; [category]
     * may still hold an earlier page's contents while the open one loads.
     * [deviceCount] is `null` if the sessions couldn't be listed. [notice]
     * is the last action's failure, [loadError] why the page couldn't load.
     */
    data class Loaded(
        val home: VaultHome? = null,
        val category: VaultCategoryContents? = null,
        val serverReachable: Boolean = true,
        val deviceCount: Int? = null,
        val isUploading: Boolean = false,
        val notice: String? = null,
        val loadError: String? = null,
    ) : VaultUiState
}

/**
 * The vault (Coffre): the home's category cards, a category's documents by
 * month, a document's pass and its content. One page at a time below
 * Expanded; at Expanded a category's grid (D1), and an open document beside
 * its list (D2). Presentation only — `App.kt`'s vault container loads
 * everything and holds [focus].
 */
@Composable
fun VaultScreen(
    state: VaultUiState,
    focus: VaultFocus,
    onFocusChange: (VaultFocus) -> Unit,
    onUpload: () -> Unit,
    onSave: (FydeDocument) -> Unit,
    loadContent: suspend (FydeDocument) -> Result<ByteArray?>,
    onRetryLoad: () -> Unit,
    onOpenScripts: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = LocalAppWindowSizeClass.current
    TopLevelScaffold(
        current = TopLevelDestination.Vault,
        // Re-selecting the tab from one of its pages returns home.
        onOpenVault = { onFocusChange(VaultFocus()) },
        onOpenScripts = onOpenScripts,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    ) { contentPadding ->
        val pageModifier = Modifier.fillMaxSize().padding(contentPadding).background(AppColors.background)
        when (state) {
            VaultUiState.Connecting -> AppLoading(modifier = pageModifier, message = stringResource(Res.string.connecting_message))
            is VaultUiState.ConnectFailed -> AppErrorState(
                modifier = pageModifier,
                icon = { AlertGlyph(modifier = Modifier.size(AppIconSize.lg)) },
                title = stringResource(Res.string.connect_failed_title),
                description = state.message,
                actionLabel = stringResource(Res.string.retry),
                onAction = state.onRetry,
            )
            is VaultUiState.Loaded -> {
                val vault = Vault(state, focus, onFocusChange, onUpload, onSave, loadContent, onRetryLoad)
                if (windowSizeClass == AppWindowSizeClass.Expanded) {
                    WideVault(vault, pageModifier)
                } else {
                    NarrowVault(vault, pageModifier)
                }
            }
        }
    }
}

/** Everything the vault's pages read and call, passed down as one. */
internal class Vault(
    val state: VaultUiState.Loaded,
    val focus: VaultFocus,
    val onFocusChange: (VaultFocus) -> Unit,
    val onUpload: () -> Unit,
    val onSave: (FydeDocument) -> Unit,
    val loadContent: suspend (FydeDocument) -> Result<ByteArray?>,
    val onRetryLoad: () -> Unit,
) {
    val canUpload: Boolean get() = state.serverReachable && !state.isUploading

    fun openPage(page: VaultPage) = onFocusChange(VaultFocus(page = page))

    fun openDocument(document: FydeDocument) = onFocusChange(focus.copy(document = document, viewerOpen = false))

    fun closeDocument() = onFocusChange(focus.copy(document = null, viewerOpen = false))

    /** Narrows or re-sorts the open category, keeping the open document beside it. */
    fun refine(page: VaultPage.Category) = onFocusChange(focus.copy(page = page))

    /** [category]'s total, from the home's summary. */
    fun categoryCount(category: FydeCategory): Long? =
        state.home?.categories?.firstOrNull { it.category == category }?.count

    /** The open category page's contents, or `null` while its first load runs. */
    fun contents(page: VaultPage.Category): VaultCategoryContents? =
        state.category?.takeIf { it.page.category == page.category }
}

/** Compact and Medium: one page at a time, the home at the bottom of the stack. */
@Composable
private fun NarrowVault(vault: Vault, modifier: Modifier) {
    val focus = vault.focus
    val document = focus.document
    val page = focus.page
    AppContentContainer(modifier = modifier, width = AppContentWidth.Detail) {
        when {
            document != null && focus.viewerOpen -> key(document.id) {
                DocumentViewer(
                    document = document,
                    loadContent = vault.loadContent,
                    onClose = { vault.onFocusChange(focus.copy(viewerOpen = false)) },
                )
            }
            document != null -> key(document.id) { DocumentPage(vault, document) }
            page is VaultPage.Category -> CategoryPage(vault, page)
            else -> HomePage(vault)
        }
    }
}

/**
 * Expanded: the home (D6) or a category's grid (D1); with a document open,
 * its pass and content beside the category's list (D2), or in the home's
 * place when it was opened from there.
 */
@Composable
private fun WideVault(vault: Vault, modifier: Modifier) {
    val document = vault.focus.document
    val page = vault.focus.page
    when {
        document != null && page is VaultPage.Category -> Row(modifier = modifier) {
            Box(
                modifier = Modifier
                    .width(AppDimensions.documentListColumn)
                    .fillMaxHeight()
                    .background(AppColors.surface),
            ) {
                CategoryListColumn(vault, page, selected = document)
            }
            AppDivider(vertical = true)
            key(document.id) { DocumentBeside(vault, document, onBack = null) }
        }
        document != null -> Row(modifier = modifier) {
            key(document.id) { DocumentBeside(vault, document, onBack = vault::closeDocument) }
        }
        page is VaultPage.Category -> AppContentContainer(modifier = modifier, width = AppContentWidth.List) {
            CategoryGridPage(vault, page)
        }
        else -> AppContentContainer(modifier = modifier, width = AppContentWidth.List) { HomePage(vault) }
    }
}

/** V1 and D6: the title and seal, the category cards, the latest arrivals. */
@Composable
private fun HomePage(vault: Vault) {
    val home = vault.state.home
    val columns = when (LocalAppWindowSizeClass.current) {
        AppWindowSizeClass.Compact -> 2
        AppWindowSizeClass.Medium -> 3
        AppWindowSizeClass.Expanded -> 4
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(AppSpacing.space5))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.vault_title),
                    style = AppTypography.display,
                    color = AppColors.text,
                    modifier = Modifier.semantics { heading() },
                )
                SealLine(vault.state.deviceCount)
            }
            ImportAction(vault)
        }
        VaultNotes(vault)
        when {
            home == null -> {
                Spacer(Modifier.height(AppSpacing.space7))
                VaultGrid(items = List(columns * 2) { it }, columns = columns) { CategoryCardPlaceholder() }
                Spacer(Modifier.height(AppSpacing.space7))
                DocumentRowSkeletons()
            }
            home.categories.isEmpty() && home.latest.isEmpty() -> AppEmptyState(
                modifier = Modifier.padding(top = AppSpacing.space9),
                icon = { DocumentGlyph(modifier = Modifier.size(AppIconSize.xl), tint = AppColors.textMuted) },
                title = stringResource(Res.string.no_documents_title),
                description = stringResource(Res.string.no_documents_description),
                actionLabel = stringResource(Res.string.vault_import).takeIf { vault.canUpload },
                onAction = vault.onUpload.takeIf { vault.canUpload },
            )
            else -> {
                Spacer(Modifier.height(AppSpacing.space7))
                VaultGrid(items = home.categories, columns = columns) { summary -> CategorySummaryCard(vault, summary) }
                if (home.latest.isNotEmpty()) {
                    SectionTitle(stringResource(Res.string.vault_latest))
                    SettingsGroup {
                        home.latest.forEachIndexed { index, document ->
                            if (index > 0) AppDivider()
                            VaultDocumentRow(
                                document = document,
                                subtitle = listOfNotNull(document.categoryLabel(), document.documentType?.label()).joinToString(" · "),
                                onClick = { vault.openDocument(document) },
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(AppSpacing.space7))
    }
}

/** "Scellé · 3 appareils", under the home's title: the vault's documents are encrypted to this many devices. */
@Composable
private fun SealLine(deviceCount: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = AppSpacing.space2)) {
        ShieldGlyph(modifier = Modifier.size(AppIconSize.sm))
        Spacer(Modifier.width(AppSpacing.space2))
        Text(
            text = if (deviceCount != null) {
                pluralStringResource(Res.plurals.vault_sealed_devices, deviceCount, deviceCount)
            } else {
                stringResource(Res.string.vault_sealed)
            },
            style = AppTypography.caption,
            color = AppColors.textMuted,
        )
    }
}

/** Import: a "+" circle on phones, a labelled button wider up. Paused while offline or uploading. */
@Composable
internal fun ImportAction(vault: Vault) {
    if (LocalAppWindowSizeClass.current == AppWindowSizeClass.Compact) {
        AppCircleButton(
            onClick = vault.onUpload,
            contentDescription = stringResource(Res.string.vault_add),
            enabled = vault.canUpload,
        ) {
            PlusGlyph(modifier = Modifier.size(AppIconSize.md), tint = if (vault.canUpload) AppColors.text else AppColors.textMuted)
        }
    } else {
        AppButton(
            text = stringResource(Res.string.vault_import),
            onClick = vault.onUpload,
            enabled = vault.state.serverReachable,
            loading = vault.state.isUploading,
            leadingIcon = { PlusGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.onPrimary) },
        )
    }
}

@Composable
private fun CategorySummaryCard(vault: Vault, summary: VaultCategorySummary) {
    val name = summary.category.label()
    val count = pluralStringResource(Res.plurals.documents_count, summary.count.toInt(), summary.count.toInt())
    val newest = summary.newest
    val caption = newest?.let { listOf(it.documentType?.label() ?: it.name, localizedShortDate(it.createdAt)).joinToString(" · ") }
    CategoryCard(
        name = name,
        count = summary.count.toString(),
        caption = caption,
        color = summary.category.colors(),
        contentDescription = if (caption != null) {
            stringResource(Res.string.vault_category_card_description_latest, name, count, caption)
        } else {
            stringResource(Res.string.vault_category_card_description, name, count)
        },
        onClick = { vault.openPage(VaultPage.Category(summary.category)) },
        icon = { tint -> CategoryGlyph(summary.category, tint) },
    )
}

/**
 * The page's notes under its header: offline (uploading paused), the last
 * action's failure, and why the page couldn't load, with a retry.
 */
@Composable
internal fun VaultNotes(vault: Vault) {
    val state = vault.state
    if (!state.serverReachable) {
        AppNote(
            message = stringResource(Res.string.offline_banner),
            kind = AppNoteKind.Warning,
            icon = { tint -> AlertTriangleGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint) },
            modifier = Modifier.padding(top = AppSpacing.space5),
        )
    }
    state.notice?.let { notice ->
        AppNote(
            message = notice,
            kind = AppNoteKind.Error,
            icon = { tint -> AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint) },
            modifier = Modifier.padding(top = AppSpacing.space5),
        )
    }
    state.loadError?.let { error ->
        Column(modifier = Modifier.padding(top = AppSpacing.space5)) {
            SettingsErrorNote(error)
            RetryButton(onClick = vault.onRetryLoad)
        }
    }
}

/** [items] in rows of [columns] equal cells, the last row's empty cells left blank. */
@Composable
internal fun <T> VaultGrid(
    items: List<T>,
    columns: Int,
    spacing: Dp = AppSpacing.space4,
    item: @Composable (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEach { Box(modifier = Modifier.weight(1f)) { item(it) } }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A document's row in its category's colours. */
@Composable
internal fun VaultDocumentRow(
    document: FydeDocument,
    subtitle: String?,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    DocumentRow(
        title = document.name,
        subtitle = subtitle?.takeIf { it.isNotEmpty() },
        trailing = localizedShortDate(document.createdAt),
        color = document.category.colors(),
        onClick = onClick,
        selected = selected,
        icon = { tint -> CategoryGlyph(document.category, tint) },
    )
}

/** A list's place while its documents load. */
@Composable
internal fun DocumentRowSkeletons(count: Int = 4) {
    SettingsGroup {
        repeat(count) { index ->
            if (index > 0) AppDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppDimensions.rowHeightStandard)
                    .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
            ) {
                AppSkeleton(modifier = Modifier.size(AppDimensions.iconTile))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
                    AppSkeleton(modifier = Modifier.fillMaxWidth(0.6f).height(AppSpacing.space5))
                    AppSkeleton(modifier = Modifier.fillMaxWidth(0.35f).height(AppSpacing.space4))
                }
            }
        }
    }
}

/**
 * A page's bar below Expanded: back at the start, [title] centred, an
 * optional [trailing] action at the end.
 */
@Composable
internal fun VaultPageBar(title: String?, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = AppDimensions.topBarHeight),
        contentAlignment = Alignment.Center,
    ) {
        AppBackButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
        if (title != null) {
            Text(
                text = title,
                style = AppTypography.bodyEmphasis,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = AppDimensions.backButton + AppSpacing.space3),
            )
        }
        if (trailing != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
    }
}

/** The document's category, or "À classer" without one. */
@Composable
internal fun FydeDocument.categoryLabel(): String =
    category?.label() ?: stringResource(Res.string.vault_unclassified)
