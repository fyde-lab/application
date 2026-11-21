package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppCircleButton
import com.peltoche.fyde.components.action.AppRetryButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.action.onSecondaryClick
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppGroup
import com.peltoche.fyde.components.content.AppSectionTitle
import com.peltoche.fyde.components.content.FolderCard
import com.peltoche.fyde.components.content.FolderCardPlaceholder
import com.peltoche.fyde.components.content.DocumentRow
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.feedback.AppErrorNote
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.components.feedback.AppListSkeleton
import com.peltoche.fyde.components.feedback.AppLoading
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.icons.AlertGlyph
import com.peltoche.fyde.icons.AlertTriangleGlyph
import com.peltoche.fyde.icons.DocumentGlyph
import com.peltoche.fyde.icons.PlusGlyph
import com.peltoche.fyde.icons.SearchGlyph
import com.peltoche.fyde.icons.ShieldGlyph
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.connect_failed_title
import com.peltoche.fyde.resources.connecting_message
import com.peltoche.fyde.resources.documents_count
import com.peltoche.fyde.resources.folder_new
import com.peltoche.fyde.resources.no_documents_description
import com.peltoche.fyde.resources.no_documents_title
import com.peltoche.fyde.resources.offline_banner
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.vault_add
import com.peltoche.fyde.resources.vault_folder_card_description
import com.peltoche.fyde.resources.vault_folder_card_description_latest
import com.peltoche.fyde.resources.vault_folders
import com.peltoche.fyde.resources.vault_import
import com.peltoche.fyde.resources.vault_latest
import com.peltoche.fyde.resources.vault_sealed
import com.peltoche.fyde.resources.vault_sealed_devices
import com.peltoche.fyde.resources.vault_search
import com.peltoche.fyde.resources.vault_search_in
import com.peltoche.fyde.resources.vault_title
import com.peltoche.fyde.resources.vault_unclassified
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppShapes
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.theme.appFocusRing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Which vault page is open: the home's folders (V1, D6), one folder's
 * subfolders and documents (V3, D1), or the search (V4, V5).
 */
sealed interface VaultPage {
    data object Home : VaultPage

    /**
     * The folder [folderId]'s page, or À classer's for `null`: its
     * subfolders, then its documents (its subfolders' included), narrowed by
     * year and sorted by arrival.
     */
    data class Folder(
        val folderId: String?,
        val year: YearFilter? = null,
        val newestFirst: Boolean = true,
    ) : VaultPage {
        val filter: FydeFolderFilter get() = folderId?.let { FydeFolderFilter.Within(it) } ?: FydeFolderFilter.Unfiled
        val query: VaultQuery get() = VaultQuery(folder = filter, year = year)
    }

    /** The search for [query], opened from [origin], where Annuler returns. */
    data class Search(val query: VaultQuery, val origin: VaultPage) : VaultPage

    /**
     * V16 / D11: the documents similar to [document], opened from its page
     * over [origin], where back returns with [document] open again. [ticked]
     * is the selection, by document id (design-system.md § Selection).
     */
    data class Similar(
        val document: FydeDocument,
        val origin: VaultPage,
        val ticked: Set<String> = emptySet(),
    ) : VaultPage
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

/**
 * The documents the user asked to delete: one (V10, D2c) or the ticked ones
 * (V10b, D11b). [busy] while their deletions publish; [error] why some
 * failed, in the SDK's wording, [documents] then being those left.
 */
data class DocumentDeletion(val documents: List<FydeDocument>, val busy: Boolean = false, val error: String? = null) {
    constructor(document: FydeDocument, busy: Boolean = false, error: String? = null) : this(listOf(document), busy, error)
}

/**
 * The documents similar to [documentId] the SDK found, most similar first:
 * the open document's section (V6c, D2) or the similar page (V16, D11).
 */
data class VaultSimilarContents(val documentId: String, val documents: List<FydeSimilarDocument>)

/**
 * A folder card or row: how many documents [folder] holds, its subfolders'
 * included, and the newest of them. [folder] is `null` for À classer.
 */
data class VaultFolderSummary(val folder: FydeFolder?, val count: Long, val newest: FydeDocument? = null)

/**
 * The home's content: À classer while it holds documents, then every
 * top-level folder by name (empty ones included), and the latest arrivals.
 * [counts] is every folder's document count, its subfolders' included
 * (a folder without documents isn't in it), for the folder sheets.
 */
data class VaultHome(
    val folders: List<VaultFolderSummary>,
    val latest: List<FydeDocument>,
    val counts: Map<String, Long> = emptyMap(),
)

/**
 * What a folder page shows for [page]: its [subfolders] by name, how many
 * documents it holds in all ([count], whatever the year), the year chips,
 * and its documents in [page]'s order.
 */
data class VaultFolderContents(
    val page: VaultPage.Folder,
    val subfolders: List<VaultFolderSummary>,
    val count: Long,
    val options: VaultFilterOptions,
    val documents: List<FydeDocument>,
)

/**
 * What the search page shows for [query]: the facet chips' choices (V4's
 * type shortcuts among them) and, once [VaultQuery.showsResults], the
 * documents found, most relevant first.
 */
data class VaultSearchContents(
    val query: VaultQuery,
    val options: VaultFilterOptions,
    val documents: List<FydeDocument>,
)

sealed interface VaultUiState {
    data object Connecting : VaultUiState

    data class ConnectFailed(val message: String, val onRetry: () -> Unit) : VaultUiState

    /**
     * Connected. [home], [folder] and [search] are `null` until loaded;
     * [folder] and [search] may still hold an earlier query's contents
     * while the open one loads. [folders] is every folder synced here.
     * [deviceCount] is `null` if the sessions couldn't be listed. [notice]
     * is the last action's failure, [loadError] why the page couldn't load.
     * [deletion] is the document V10 / D2c asks about, while it's open;
     * [folderSheet] the folder sheet or dialog open (V12–V15, D7–D10).
     * [documentSimilar] is the open document's similar section, [similar]
     * the similar page's documents, each `null` until loaded.
     */
    data class Loaded(
        val folders: VaultFolders = VaultFolders(),
        val home: VaultHome? = null,
        val folder: VaultFolderContents? = null,
        val search: VaultSearchContents? = null,
        val documentSimilar: VaultSimilarContents? = null,
        val similar: VaultSimilarContents? = null,
        val serverReachable: Boolean = true,
        val deviceCount: Int? = null,
        val isUploading: Boolean = false,
        val notice: String? = null,
        val loadError: String? = null,
        val deletion: DocumentDeletion? = null,
        val folderSheet: FolderSheetState? = null,
        val documentMove: DocumentMove? = null,
    ) : VaultUiState
}

/**
 * The vault (Coffre): the home's folder cards, a folder's subfolders and
 * documents by month, a document's pass and its content. One page at a time
 * below Expanded; at Expanded a folder's grid (D1), and an open document
 * beside its list (D2). Presentation only — `App.kt`'s vault container loads
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
    onDeletionChange: (DocumentDeletion?) -> Unit,
    onDelete: (List<FydeDocument>) -> Unit,
    navigation: TopLevelNavigation,
    modifier: Modifier = Modifier,
    folderCallbacks: FolderCallbacks = FolderCallbacks(),
    loadThumbnail: suspend (id: String) -> Result<FydeThumbnail?> = { Result.success(null) },
) {
    val folders = (state as? VaultUiState.Loaded)?.folders ?: VaultFolders()
    // Kept while the vault is shown, gone with it (logout included).
    val currentLoadThumbnail by rememberUpdatedState(loadThumbnail)
    val thumbnails = remember { DocumentThumbnails(render = { id -> currentLoadThumbnail(id) }) }
    val currentFolders by rememberUpdatedState(folders)
    val callbacks by rememberUpdatedState(folderCallbacks)
    // Drag and drop is Expanded's, where a mouse is expected.
    val drag = if (LocalAppWindowSizeClass.current == AppWindowSizeClass.Expanded) {
        remember {
            VaultDrag(folders = { currentFolders }) { item, choice ->
                when (item) {
                    is DragItem.Document -> callbacks.onDropDocument(item.document, choice.folderId)
                    is DragItem.Folder -> choice.folderId?.let { callbacks.onDropFolder(item.folder.id, it) }
                }
            }
        }
    } else {
        null
    }
    CompositionLocalProvider(
        LocalVaultFolders provides folders,
        LocalVaultDrag provides drag,
        LocalDocumentThumbnails provides thumbnails,
    ) {
    VaultDragLayer(drag, modifier = modifier) {
    TopLevelScaffold(
        navigation = navigation.copy(
            // Re-selecting the tab from one of its pages returns home.
            onOpenVault = { onFocusChange(VaultFocus()) },
            onOpenFolder = { onFocusChange(VaultFocus(page = VaultPage.Folder(it))) },
            // Over the page that's open, as its own "+ Nouveau dossier" would.
            onNewFolder = { folderCallbacks.onSheetChange(FolderSheetState(FolderSheet.Create(null))) },
        ),
        selection = focus.page.sidebarSelection(),
    ) { contentPadding ->
        val windowSizeClass = LocalAppWindowSizeClass.current
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
                val vault = Vault(state, focus, onFocusChange, onUpload, onSave, loadContent, onRetryLoad, onDeletionChange, folderCallbacks)
                val wide = windowSizeClass == AppWindowSizeClass.Expanded
                if (wide) {
                    WideVault(vault, pageModifier)
                } else {
                    NarrowVault(vault, pageModifier)
                }
                FolderSheets(vault)
                MoveDocumentSheet(vault)
                state.deletion?.let { deletion ->
                    DeleteDocumentDialog(
                        deletion = deletion,
                        deviceCount = state.deviceCount,
                        wide = wide,
                        onConfirm = { onDelete(deletion.documents) },
                        onDismiss = { onDeletionChange(null) },
                    )
                }
            }
        }
    }
    }
    }
}

/** The sidebar's item for this page: a search keeps the one it was opened from. */
private fun VaultPage.sidebarSelection(): SidebarSelection = when (this) {
    VaultPage.Home -> SidebarSelection.Vault
    is VaultPage.Folder -> SidebarSelection.Folder(folderId)
    is VaultPage.Search -> origin.sidebarSelection()
    is VaultPage.Similar -> origin.sidebarSelection()
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
    val onDeletionChange: (DocumentDeletion?) -> Unit,
    val folderCallbacks: FolderCallbacks = FolderCallbacks(),
) {
    val canUpload: Boolean get() = state.serverReachable && !state.isUploading

    fun openPage(page: VaultPage) = onFocusChange(VaultFocus(page = page))

    /** Opens the search from the open page, within [folder] when there's one. */
    fun openSearch(folder: FydeFolderFilter? = null) =
        openPage(VaultPage.Search(VaultQuery(folder = folder), origin = focus.page))

    /** Edits the open search, closing any document opened from its results. */
    fun search(page: VaultPage.Search) = onFocusChange(VaultFocus(page = page))

    fun openDocument(document: FydeDocument) = onFocusChange(focus.copy(document = document, viewerOpen = false))

    fun closeDocument() = onFocusChange(focus.copy(document = null, viewerOpen = false))

    /** Opens a folder sheet (V12–V15, D7–D10). */
    fun openFolderSheet(sheet: FolderSheet) = folderCallbacks.onSheetChange(FolderSheetState(sheet))

    /** Opens V9 about [document]. */
    fun askMove(document: FydeDocument) = askMove(listOf(document))

    /** Opens V9 about one document, or V9b about several. */
    fun askMove(documents: List<FydeDocument>) = folderCallbacks.onDocumentMoveChange(DocumentMove(documents))

    /** Opens V10 / D2c about [document]. */
    fun askDelete(document: FydeDocument) = askDelete(listOf(document))

    /** Opens V10 / D2c about one document, or V10b / D11b about several. */
    fun askDelete(documents: List<FydeDocument>) = onDeletionChange(DocumentDeletion(documents))

    /**
     * Opens V16 / D11 for [document] over the open page. [keepOpen] (D11)
     * keeps it open beside the list; below Expanded the list takes the page.
     */
    fun openSimilar(document: FydeDocument, keepOpen: Boolean) = onFocusChange(
        VaultFocus(page = VaultPage.Similar(document, origin = focus.page), document = document.takeIf { keepOpen }),
    )

    /** Back from V16 / D11: the page it was opened over, its document open again. */
    fun closeSimilar(page: VaultPage.Similar) = onFocusChange(VaultFocus(page = page.origin, document = page.document))

    /** Ticks or unticks on V16 / D11, keeping the open document beside it. */
    fun tick(page: VaultPage.Similar, ticked: Set<String>) = onFocusChange(focus.copy(page = page.copy(ticked = ticked)))

    /** Back from a folder page: its parent folder's page, or home from the top level and À classer. */
    fun openParent(page: VaultPage.Folder) {
        val parentId = state.folders[page.folderId]?.parentId
        openPage(if (parentId != null) VaultPage.Folder(parentId) else VaultPage.Home)
    }

    /** Narrows or re-sorts the open folder, keeping the open document beside it. */
    fun refine(page: VaultPage.Folder) = onFocusChange(focus.copy(page = page))

    /** The open folder page's contents, or `null` while its first load runs. */
    fun contents(page: VaultPage.Folder): VaultFolderContents? =
        state.folder?.takeIf { it.page.folderId == page.folderId }
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
            page is VaultPage.Folder -> FolderPage(vault, page)
            page is VaultPage.Search -> SearchPage(vault, page)
            page is VaultPage.Similar -> SimilarPage(vault, page)
            else -> HomePage(vault)
        }
    }
}

/**
 * Expanded: the home (D6), a folder's grid (D1) or the search; with a
 * document open, its pass and content beside the folder's list or the
 * search's results (D2), or in the home's place when it was opened from there.
 */
@Composable
private fun WideVault(vault: Vault, modifier: Modifier) {
    val document = vault.focus.document
    val page = vault.focus.page
    VaultShortcuts(vault, modifier) { shortcutsModifier ->
        when {
            document != null -> Row(modifier = shortcutsModifier) {
                if (page !is VaultPage.Home) {
                    DocumentBesideList(vault, page, document)
                } else {
                    key(document.id) { DocumentBeside(vault, document, onBack = vault::closeDocument) }
                }
            }
            page is VaultPage.Folder -> AppContentContainer(modifier = shortcutsModifier, width = AppContentWidth.List) {
                FolderGridPage(vault, page)
            }
            page is VaultPage.Search -> AppContentContainer(modifier = shortcutsModifier, width = AppContentWidth.Detail) {
                SearchPage(vault, page)
            }
            page is VaultPage.Similar -> AppContentContainer(modifier = shortcutsModifier, width = AppContentWidth.Detail) {
                SimilarPage(vault, page)
            }
            else -> AppContentContainer(modifier = shortcutsModifier, width = AppContentWidth.List) { HomePage(vault) }
        }
    }
}

/** D2: the folder's list, the search's results or the similar documents (D11), then [document] beside them. */
@Composable
private fun RowScope.DocumentBesideList(vault: Vault, page: VaultPage, document: FydeDocument) {
    Box(
        modifier = Modifier
            .width(AppDimensions.documentListColumn)
            .fillMaxHeight()
            .background(AppColors.surface),
    ) {
        when (page) {
            is VaultPage.Folder -> FolderListColumn(vault, page, selected = document)
            is VaultPage.Search -> SearchPage(vault, page, selected = document)
            is VaultPage.Similar -> SimilarPage(vault, page, selected = document)
            VaultPage.Home -> Unit
        }
    }
    AppDivider(vertical = true)
    key(document.id) { DocumentBeside(vault, document, onBack = null) }
}

/**
 * Expanded's keyboard (design-system.md § Layout), on the open document,
 * else the open folder: ⇧⌘N a new folder (a subfolder of the open one),
 * F2 renames the open folder, ⇧⌘M moves the document or folder (V9, D8),
 * ⌘⌫ or Delete deletes it (D2c, D9). Ctrl stands for ⌘ off macOS. The
 * area takes the focus as the page or document opens and again once a
 * sheet closes, so the keys reach it without a click; a focused text
 * field keeps them.
 */
@Composable
private fun VaultShortcuts(vault: Vault, modifier: Modifier, content: @Composable (Modifier) -> Unit) {
    val focus = remember { FocusRequester() }
    val state = vault.state
    val idle = state.deletion == null && state.folderSheet == null && state.documentMove == null
    val document = vault.focus.document
    val folderId = (vault.focus.page as? VaultPage.Folder)?.folderId
    val usesCommandKey = getPlatform().usesCommandKey
    // The search page focuses its own field as it opens.
    val searching = vault.focus.page is VaultPage.Search && document == null
    LaunchedEffect(document?.id, vault.focus.page, idle) { if (idle && !searching) focus.requestFocus() }
    content(
        modifier
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || !idle) return@onKeyEvent false
                val command = if (usesCommandKey) event.isMetaPressed else event.isCtrlPressed
                when {
                    command && event.isShiftPressed && event.key == Key.N -> vault.openFolderSheet(FolderSheet.Create(folderId))
                    event.key == Key.F2 && document == null && folderId != null -> vault.openFolderSheet(FolderSheet.Edit(folderId))
                    command && event.isShiftPressed && event.key == Key.M -> when {
                        document != null -> vault.askMove(document)
                        folderId != null -> vault.openFolderSheet(FolderSheet.Move(folderId))
                        else -> return@onKeyEvent false
                    }
                    event.key == Key.Delete || (event.key == Key.Backspace && command) -> when {
                        document != null -> vault.askDelete(document)
                        folderId != null -> vault.openFolderSheet(FolderSheet.Delete(folderId))
                        else -> return@onKeyEvent false
                    }
                    else -> return@onKeyEvent false
                }
                true
            },
    )
}

/** V1 and D6: the title and seal, the folder cards, the latest arrivals. */
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
            SearchLauncher(vault, folder = null)
            Spacer(Modifier.width(AppSpacing.space3))
            ImportAction(vault)
        }
        VaultNotes(vault)
        when {
            home == null -> {
                Spacer(Modifier.height(AppSpacing.space7))
                VaultGrid(items = List(columns * 2) { it }, columns = columns) { FolderCardPlaceholder() }
                Spacer(Modifier.height(AppSpacing.space7))
                DocumentRowSkeletons()
            }
            home.folders.isEmpty() && home.latest.isEmpty() -> AppEmptyState(
                modifier = Modifier.padding(top = AppSpacing.space9),
                icon = { DocumentGlyph(modifier = Modifier.size(AppIconSize.xl), tint = AppColors.textMuted) },
                title = stringResource(Res.string.no_documents_title),
                description = stringResource(Res.string.no_documents_description),
                actionLabel = stringResource(Res.string.vault_import).takeIf { vault.canUpload },
                onAction = vault.onUpload.takeIf { vault.canUpload },
            )
            else -> {
                SectionTitleWithAction(
                    title = stringResource(Res.string.vault_folders),
                    action = stringResource(Res.string.folder_new),
                    onAction = { vault.openFolderSheet(FolderSheet.Create(null)) },
                )
                VaultGrid(items = home.folders, columns = columns) { summary -> FolderSummaryCard(vault, summary) }
                if (home.latest.isNotEmpty()) {
                    AppSectionTitle(stringResource(Res.string.vault_latest))
                    AppGroup {
                        home.latest.forEachIndexed { index, document ->
                            if (index > 0) AppDivider()
                            VaultDocumentRow(
                                document = document,
                                onClick = { vault.openDocument(document) },
                                showsFolder = true,
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
        Text(text = sealedText(deviceCount), style = AppTypography.caption, color = AppColors.textMuted)
    }
}

/** "Scellé · 3 appareils", or "Scellé" while the device count is unknown. */
@Composable
internal fun sealedText(deviceCount: Int?): String = if (deviceCount != null) {
    pluralStringResource(Res.plurals.vault_sealed_devices, deviceCount, deviceCount)
} else {
    stringResource(Res.string.vault_sealed)
}

/**
 * Opens the search, within [folder] if given: a field-looking button in
 * the Expanded headers (D1, D6), a circle below and in narrow columns.
 */
@Composable
internal fun SearchLauncher(
    vault: Vault,
    folder: FydeFolderFilter?,
    asField: Boolean = LocalAppWindowSizeClass.current == AppWindowSizeClass.Expanded,
) {
    val label = if (folder != null) {
        stringResource(Res.string.vault_search_in, folder.label())
    } else {
        stringResource(Res.string.vault_search)
    }
    if (asField) {
        val interactionSource = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .width(AppDimensions.searchLauncherWidth)
                .heightIn(min = AppDimensions.touchTargetMin)
                .clip(AppShapes.input)
                .clickable(interactionSource = interactionSource, indication = ripple(), role = Role.Button) {
                    vault.openSearch(folder)
                }
                .appFocusRing(interactionSource, shape = AppShapes.input)
                .background(AppColors.surface)
                .border(AppDimensions.borderHairline, AppColors.borderStrong, AppShapes.input)
                .padding(horizontal = AppSpacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchGlyph(modifier = Modifier.size(AppIconSize.sm))
            Spacer(Modifier.width(AppSpacing.space3))
            Text(text = label, style = AppTypography.body, color = AppColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        AppCircleButton(onClick = { vault.openSearch(folder) }, contentDescription = label) {
            SearchGlyph(modifier = Modifier.size(AppIconSize.md), tint = AppColors.text)
        }
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

/** A folder's card (À classer's for none), opening its page. */
@Composable
internal fun FolderSummaryCard(vault: Vault, summary: VaultFolderSummary) {
    val folder = summary.folder
    val name = folder?.name ?: stringResource(Res.string.vault_unclassified)
    val count = pluralStringResource(Res.plurals.documents_count, summary.count.toInt(), summary.count.toInt())
    val newest = summary.newest
    val caption = newest?.let { listOf(it.documentType?.label() ?: it.name, localizedShortDate(it.createdAt)).joinToString(" · ") }
    FolderCard(
        modifier = Modifier
            .vaultDropTarget(FolderChoice(folder?.id))
            .then(if (folder != null) Modifier.vaultDragSource(DragItem.Folder(folder)) else Modifier),
        name = name,
        count = summary.count.toString(),
        caption = caption,
        color = LocalVaultFolders.current.appearanceOf(folder?.id).colors(),
        contentDescription = if (caption != null) {
            stringResource(Res.string.vault_folder_card_description_latest, name, count, caption)
        } else {
            stringResource(Res.string.vault_folder_card_description, name, count)
        },
        onClick = { vault.openPage(VaultPage.Folder(folder?.id)) },
        empty = summary.count == 0L,
        icon = { tint -> if (folder != null) FolderMark(folder, tint) else FolderIconGlyph(null, tint) },
    )
}

/** A section's title with a "+" action at its end ("Dossiers · + Nouveau dossier"). */
@Composable
internal fun SectionTitleWithAction(title: String, action: String, onAction: () -> Unit, adds: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.space6, bottom = AppSpacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = AppTypography.section,
            color = AppColors.text,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        AppTextButton(
            text = action,
            onClick = onAction,
            leadingIcon = if (adds) {
                { PlusGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.primary) }
            } else {
                null
            },
        )
    }
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
            AppErrorNote(error)
            AppRetryButton(onClick = vault.onRetryLoad)
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

/**
 * A document's row in its top-level folder's colours: its issuer (or, with
 * [showsFolder], where it's filed) and date under its name, its pass's
 * amount at the end (design-system.md § Components). With
 * [onDelete] (D2's list), a right-click opens D2b's menu on it.
 */
@Composable
internal fun VaultDocumentRow(
    document: FydeDocument,
    onClick: () -> Unit,
    selected: Boolean = false,
    highlightTerms: List<String> = emptyList(),
    showsFolder: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onMove: (() -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val place = if (showsFolder) document.folderLabel() else document.issuer
    Box(
        modifier = Modifier
            .vaultDragSource(DragItem.Document(document))
            .then(if (onDelete != null) Modifier.onSecondaryClick { menuOpen = true } else Modifier),
    ) {
        DocumentRow(
            title = document.name,
            subtitle = listOfNotNull(place, localizedShortDate(document.createdAt)).joinToString(" · "),
            amount = rowAmount(document),
            color = document.colors(),
            onClick = onClick,
            selected = selected,
            highlightTerms = highlightTerms,
            thumbnail = rememberDocumentThumbnail(document),
            icon = { tint -> DocumentFolderGlyph(document, tint) },
        )
        if (onDelete != null) {
            DocumentActionsMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, onDelete = onDelete, onMove = onMove)
        }
    }
}

/** A list's place while its documents load. */
@Composable
internal fun DocumentRowSkeletons(count: Int = 4) {
    AppListSkeleton(
        rows = count,
        rowHeight = AppDimensions.rowHeightStandard,
        tileSize = AppDimensions.iconTile,
        firstLine = Modifier.fillMaxWidth(0.6f).height(AppSpacing.space5),
        secondLine = Modifier.fillMaxWidth(0.35f).height(AppSpacing.space4),
        lineGap = AppSpacing.space3,
    )
}

/** "Logement › Énergie" for a folder, "À classer" for the root. */
@Composable
internal fun FydeFolderFilter.label(): String = when (this) {
    FydeFolderFilter.Unfiled -> folderPathLabel(null)
    is FydeFolderFilter.Within -> folderPathLabel(folderId)
}
