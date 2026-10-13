package com.peltoche.fyde

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.action.AppIconButton
import com.peltoche.fyde.components.content.AppBadge
import com.peltoche.fyde.components.content.AppChip
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppListItem
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.feedback.AppBannerSeverity
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.components.feedback.AppLoading
import com.peltoche.fyde.components.feedback.AppSkeleton
import com.peltoche.fyde.components.form.AppSearchField
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.components.navigation.AppTopBarAction
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppTwoPaneLayout
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.back
import com.peltoche.fyde.resources.clear_filters
import com.peltoche.fyde.resources.clear_search
import com.peltoche.fyde.resources.connect_failed_title
import com.peltoche.fyde.resources.connecting_message
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.resources.document_detail_created
import com.peltoche.fyde.resources.document_detail_keywords_title
import com.peltoche.fyde.resources.document_detail_purpose
import com.peltoche.fyde.resources.document_detail_size
import com.peltoche.fyde.resources.document_detail_source_category
import com.peltoche.fyde.resources.document_detail_source_subcategory
import com.peltoche.fyde.resources.document_detail_type
import com.peltoche.fyde.resources.document_detail_value_unset
import com.peltoche.fyde.resources.document_details_title
import com.peltoche.fyde.resources.document_download
import com.peltoche.fyde.resources.document_preview_title
import com.peltoche.fyde.resources.documents_count
import com.peltoche.fyde.resources.documents_title
import com.peltoche.fyde.resources.filter_period
import com.peltoche.fyde.resources.filter_type
import com.peltoche.fyde.resources.filter_type_all
import com.peltoche.fyde.resources.filter_purpose
import com.peltoche.fyde.resources.filter_purpose_all
import com.peltoche.fyde.resources.filter_source
import com.peltoche.fyde.resources.filter_source_all
import com.peltoche.fyde.resources.filter_subcategory
import com.peltoche.fyde.resources.filter_subcategory_all
import com.peltoche.fyde.resources.no_documents_description
import com.peltoche.fyde.resources.no_documents_match
import com.peltoche.fyde.resources.no_documents_match_filters
import com.peltoche.fyde.resources.no_documents_title
import com.peltoche.fyde.resources.no_matches_title
import com.peltoche.fyde.resources.offline_banner
import com.peltoche.fyde.resources.preview_unavailable_description
import com.peltoche.fyde.resources.preview_unavailable_title
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.search_documents_placeholder
import com.peltoche.fyde.resources.search_results_count
import com.peltoche.fyde.resources.select_document_description
import com.peltoche.fyde.resources.select_document_title
import com.peltoche.fyde.resources.upload_document
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Everything [DocumentListScreen] needs to render, and nothing it doesn't —
 * no `FydeSdk` types appear here. The caller (see `DocumentsScreen` in
 * `App.kt`) owns the SDK calls and maps their results into this state, so
 * the visual design here can be iterated on without touching that wiring.
 */
sealed interface DocumentListUiState {
    /** Not yet known whether the server is reachable; nothing to show at all. */
    data object Connecting : DocumentListUiState

    /** The initial connection failed outright. */
    data class ConnectFailed(val message: String, val onRetry: () -> Unit) : DocumentListUiState

    /**
     * Connected. [documents] is `null` while the first page is still being
     * fetched (known shape, unknown content — rendered as a skeleton rather
     * than a spinner), and non-null once loaded (possibly empty), newest
     * first. [banner] surfaces a non-fatal problem (e.g. a failed upload)
     * without discarding what's already loaded. [serverReachable] mirrors
     * `fyde-sdk`'s own `is_server_reachable` check, polled periodically by
     * the caller — while it's false an offline banner explains why the
     * upload button is disabled.
     *
     * [search] is what the search field and filter chips show; [results] are
     * the documents `fyde-sdk` found for it (`null` until the first search
     * returns), and [filterOptions] the choices it says are still worth
     * offering for it — see [FydeSearchFacets]. [documents] stays every cached
     * document, so "nothing imported yet" can be told apart from "nothing
     * matches".
     */
    data class Loaded(
        val documents: List<FydeDocument>?,
        val banner: String? = null,
        val onDismissBanner: () -> Unit = {},
        val serverReachable: Boolean = true,
        val search: DocumentSearch = DocumentSearch(),
        val results: List<FydeDocument>? = null,
        val filterOptions: DocumentFilterOptions = DocumentFilterOptions(),
    ) : DocumentListUiState
}

@Composable
fun DocumentListScreen(
    state: DocumentListUiState,
    modifier: Modifier = Modifier,
    onUpload: () -> Unit = {},
    isUploading: Boolean = false,
    onDownload: (FydeDocument) -> Unit = {},
    onSearchChange: (DocumentSearch) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenScripts: () -> Unit = {},
) {
    when (state) {
        DocumentListUiState.Connecting -> AppLoading(
            modifier = modifier,
            message = stringResource(Res.string.connecting_message),
        )
        is DocumentListUiState.ConnectFailed -> AppErrorState(
            modifier = modifier,
            icon = { AlertGlyph(modifier = Modifier.size(32.dp)) },
            title = stringResource(Res.string.connect_failed_title),
            description = state.message,
            actionLabel = stringResource(Res.string.retry),
            onAction = state.onRetry,
        )
        is DocumentListUiState.Loaded -> LoadedDocumentList(state, modifier, onUpload, isUploading, onDownload, onSearchChange, onOpenSettings, onOpenScripts)
    }
}

@Composable
private fun LoadedDocumentList(
    state: DocumentListUiState.Loaded,
    modifier: Modifier,
    onUpload: () -> Unit,
    isUploading: Boolean,
    onDownload: (FydeDocument) -> Unit,
    onSearchChange: (DocumentSearch) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenScripts: () -> Unit,
) {
    // The query and filters live in the caller ([DocumentListUiState.Loaded.search]),
    // which runs them through fyde-sdk; what's left here is presentation-only,
    // and lives *above* the Compact list/detail switch below, so opening a
    // document on a phone and coming back keeps the selection and the scroll
    // position. Found via review: the phone viewer used to be a separate
    // top-level screen that tore all of it down (and reconnected) on every back.
    val search = state.search
    // The id, not the document: the list is re-fetched on every changelog
    // event, and the open document should follow its fresh copy.
    var selectedId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val windowSizeClass = LocalAppWindowSizeClass.current
    val isDesktop = windowSizeClass == AppWindowSizeClass.Expanded

    val documents = state.documents
    val selectedDocument = selectedId?.let { id -> documents?.find { it.id == id } }

    if (windowSizeClass == AppWindowSizeClass.Compact && selectedDocument != null) {
        CompactDocumentDetail(
            document = selectedDocument,
            onBack = { selectedId = null },
            onDownload = { onDownload(selectedDocument) },
            onOpenScripts = onOpenScripts,
            onOpenSettings = onOpenSettings,
        )
        return
    }

    val visibleDocuments = state.results
    val clearAll = { onSearchChange(DocumentSearch()) }

    TopLevelScaffold(
        current = SidebarDestination.Documents,
        // Re-selecting Documents closes an open document, like a drill-down's back.
        onOpenDocuments = { selectedId = null },
        onOpenScripts = onOpenScripts,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.documents_title),
                windowSizeClass = windowSizeClass,
                sidebarAdjacent = isDesktop,
            ) {
                AppTopBarAction(
                    text = stringResource(Res.string.upload_document),
                    onClick = onUpload,
                    enabled = state.serverReachable,
                    loading = isUploading,
                    icon = { tint -> PlusGlyph(modifier = Modifier.size(14.dp), tint = tint) },
                    windowSizeClass = windowSizeClass,
                )
            }
        },
    ) { contentPadding ->
        // The whole content area shares one margin/max-width, not just the
        // list — found via review that the search field (and banner) sat
        // outside this container while the list didn't, so their edges
        // visibly failed to line up at Medium/Expanded.
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
                // Says why Upload is disabled, rather than leaving a greyed
                // button to explain itself. Not dismissible: it clears on its
                // own as soon as the server answers again.
                if (!state.serverReachable) {
                    AppBanner(message = stringResource(Res.string.offline_banner), severity = AppBannerSeverity.Warning)
                }
                state.banner?.let { message ->
                    AppBanner(message = message, onDismiss = state.onDismissBanner, dismissLabel = stringResource(Res.string.dismiss))
                }

                // The search field, filter chips, and results toolbar are part
                // of the *list* column, not full-width page chrome, so the
                // detail pane runs alongside them from the top, exactly like
                // the reference layout's two columns.
                AppTwoPaneLayout(
                    modifier = Modifier.weight(1f),
                    windowSizeClass = windowSizeClass,
                    primaryPaneWidth = if (isDesktop) AppDimensions.twoPaneListWidthExpanded else AppDimensions.twoPaneListWidth,
                    primary = {
                        // Trailing padding matches AppContentContainer's own
                        // leading margin on the other side of this column, so
                        // the list doesn't run flush against the detail pane.
                        Column(modifier = Modifier.fillMaxSize().padding(end = AppSpacing.space5)) {
                            Spacer(Modifier.height(AppSpacing.space3))
                            AppSearchField(
                                value = search.text,
                                onValueChange = { onSearchChange(search.copy(text = it)) },
                                placeholder = stringResource(Res.string.search_documents_placeholder),
                                onClear = { onSearchChange(search.copy(text = "")) },
                                clearContentDescription = stringResource(Res.string.clear_search),
                                leadingIcon = { SearchGlyph(modifier = Modifier.size(16.dp)) },
                            )

                            if (!documents.isNullOrEmpty()) {
                                Spacer(Modifier.height(AppSpacing.space2))
                                FilterChipRow(
                                    search = search,
                                    options = state.filterOptions,
                                    onSearchChange = onSearchChange,
                                )
                                if (visibleDocuments != null) {
                                    Spacer(Modifier.height(AppSpacing.space2))
                                    Text(
                                        text = if (search.text.isBlank()) {
                                            pluralStringResource(Res.plurals.documents_count, visibleDocuments.size, visibleDocuments.size)
                                        } else {
                                            pluralStringResource(Res.plurals.search_results_count, visibleDocuments.size, visibleDocuments.size, search.text)
                                        },
                                        style = AppTypography.caption,
                                        color = AppColors.textMuted,
                                    )
                                }
                                Spacer(Modifier.height(AppSpacing.space2))
                            }

                            when {
                                documents == null || visibleDocuments == null -> DocumentSkeletonList(modifier = Modifier.weight(1f))

                                documents.isEmpty() -> AppEmptyState(
                                    modifier = Modifier.weight(1f),
                                    icon = { DocumentGlyph(modifier = Modifier.size(40.dp), tint = AppColors.textMuted, strokeWidth = 2.dp) },
                                    title = stringResource(Res.string.no_documents_title),
                                    description = stringResource(Res.string.no_documents_description),
                                )

                                visibleDocuments.isEmpty() -> AppEmptyState(
                                    modifier = Modifier.weight(1f),
                                    icon = { DocumentGlyph(modifier = Modifier.size(40.dp), tint = AppColors.textMuted, strokeWidth = 2.dp) },
                                    title = stringResource(Res.string.no_matches_title),
                                    description = if (search.text.isNotBlank()) {
                                        stringResource(Res.string.no_documents_match, search.text)
                                    } else {
                                        stringResource(Res.string.no_documents_match_filters)
                                    },
                                    actionLabel = stringResource(
                                        if (search.filtersActive) Res.string.clear_filters else Res.string.clear_search,
                                    ),
                                    onAction = clearAll,
                                    // Upload, in the top bar, is this screen's one filled action.
                                    actionVariant = AppButtonVariant.Outlined,
                                )

                                else -> DocumentList(
                                    documents = visibleDocuments,
                                    listState = listState,
                                    selectedId = selectedId,
                                    rowMinHeight = rowMinHeightFor(windowSizeClass),
                                    onClick = { document -> selectedId = document.id },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    },
                    secondary = {
                        if (selectedDocument == null) {
                            AppEmptyState(
                                title = stringResource(Res.string.select_document_title),
                                description = stringResource(Res.string.select_document_description),
                                icon = { DocumentGlyph(modifier = Modifier.size(40.dp), tint = AppColors.textMuted, strokeWidth = 2.dp) },
                            )
                        } else {
                            DocumentDetailPane(document = selectedDocument, onDownload = { onDownload(selectedDocument) })
                        }
                    },
                )
            }
        }
    }
}

/**
 * Compact (phone) document detail: the same [DocumentDetailPane] the wider
 * layouts show beside the list, pushed over it with a back chevron, inside
 * the same navigation shell. Rendered by [LoadedDocumentList] itself rather
 * than as a separate top-level screen, so the list's state survives it.
 */
@Composable
private fun CompactDocumentDetail(
    document: FydeDocument,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onOpenScripts: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    TopLevelScaffold(
        current = SidebarDestination.Documents,
        onOpenDocuments = onBack,
        onOpenScripts = onOpenScripts,
        onOpenSettings = onOpenSettings,
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.documents_title),
                onBack = onBack,
                backContentDescription = stringResource(Res.string.back),
            )
        },
    ) { contentPadding ->
        DocumentDetailPane(
            document = document,
            onDownload = onDownload,
            scrollable = true,
            modifier = Modifier.fillMaxSize().padding(contentPadding).background(AppColors.background),
        )
    }
}

private fun rowMinHeightFor(windowSizeClass: AppWindowSizeClass): Dp = when (windowSizeClass) {
    AppWindowSizeClass.Compact -> AppDimensions.rowHeightComfortable
    AppWindowSizeClass.Medium -> AppDimensions.rowHeightStandard
    AppWindowSizeClass.Expanded -> AppDimensions.rowHeightCompact
}

/**
 * A row's supporting line: what the document *is* — its purpose and source
 * ("Invoice · Telecom") — when the SDK's classifier has said, falling back to
 * its file type and size, then its date either way. Found via review that
 * rows only ever showed type/size/date, while the classification that tells
 * documents apart (the reference layout's tags and issuer) sat unused in the
 * detail pane.
 */
@Composable
private fun rowSummary(document: FydeDocument): String {
    val classification = listOfNotNull(document.purpose?.label(), document.sourceCategory?.label())
    val description = classification.ifEmpty { listOf(document.typeCode, localizedSize(document.size)) }
    return (description + localizedDate(document.createdAt)).joinToString(" · ")
}

@Composable
private fun DocumentList(
    documents: List<FydeDocument>,
    listState: LazyListState,
    selectedId: String?,
    rowMinHeight: Dp,
    onClick: (FydeDocument) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(state = listState, modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(top = AppSpacing.space2)) {
        items(documents, key = { it.id }) { document ->
            AppListItem(
                title = document.name,
                supportingText = rowSummary(document),
                leadingContent = { FileTypeIcon(document) },
                trailingContent = { ChevronRightGlyph(modifier = Modifier.size(12.dp)) },
                selected = document.id == selectedId,
                onClick = { onClick(document) },
                modifier = Modifier.heightIn(min = rowMinHeight),
            )
            AppDivider()
        }
    }
}

/**
 * A record detail pane (design-system.md § Detail panes): header with the
 * type icon, name, metadata and actions, then the details grid, keywords,
 * and the document's own preview. [scrollable] (Compact) scrolls the whole
 * pane with the preview at a fixed height, since a phone has no room left
 * for a preview that only gets what the sections above leave over.
 */
@Composable
private fun DocumentDetailPane(
    document: FydeDocument,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
) {
    Column(modifier = if (scrollable) modifier.verticalScroll(rememberScrollState()) else modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.space5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileTypeIcon(document, size = 40.dp)
            Spacer(Modifier.width(AppSpacing.space4))
            Column(modifier = Modifier.weight(1f)) {
                // Unlike the list row (always 1 line — a list is scanned),
                // this is the one place the full name matters more than
                // row-height consistency, so it gets 2 lines first.
                Text(
                    text = document.name,
                    style = AppTypography.bodyEmphasis,
                    color = AppColors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(AppSpacing.space1))
                Text(
                    text = "${document.typeCode} · ${localizedSize(document.size)} · ${localizedDate(document.createdAt)}",
                    style = AppTypography.data,
                    color = AppColors.textMuted,
                )
            }
            // Only actions that work. Favorite and "more" used to sit here
            // as permanently disabled icons with nothing behind them;
            // they're gone until those features exist.
            AppIconButton(onClick = onDownload, contentDescription = stringResource(Res.string.document_download)) {
                DownloadGlyph(modifier = Modifier.size(16.dp), tint = AppColors.text)
            }
        }
        AppDivider()
        DocumentDetailsSection(document)
        AppDivider()
        DocumentKeywordsSection(document)
        AppDivider()
        Text(
            text = stringResource(Res.string.document_preview_title),
            style = AppTypography.section,
            color = AppColors.text,
            modifier = Modifier.padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space4),
        )
        DocumentPreview(
            document = document,
            modifier = if (scrollable) Modifier.fillMaxWidth().height(560.dp) else Modifier.fillMaxWidth().weight(1f),
        )
    }
}

/**
 * The document's content, previewed the way its [DocumentKind] allows: PDFs
 * through [PdfViewer], images decoded and drawn, anything else a plain "no
 * preview" state pointing at Download. Found via review: every document went
 * through the PDF engine, so an image or a spreadsheet showed a red engine
 * error instead.
 */
@Composable
private fun DocumentPreview(document: FydeDocument, modifier: Modifier = Modifier) {
    when (document.kind) {
        DocumentKind.PDF -> PdfViewer(pdfBytes = document.content, modifier = modifier)
        DocumentKind.IMAGE -> {
            val image = remember(document.id, document.content) { decodeIconBitmap(document.content) }
            if (image != null) {
                Box(modifier = modifier.padding(horizontal = AppSpacing.space5), contentAlignment = Alignment.TopCenter) {
                    Image(bitmap = image, contentDescription = document.name, contentScale = ContentScale.Fit)
                }
            } else {
                PreviewUnavailable(document, modifier)
            }
        }
        else -> PreviewUnavailable(document, modifier)
    }
}

@Composable
private fun PreviewUnavailable(document: FydeDocument, modifier: Modifier = Modifier) {
    AppEmptyState(
        modifier = modifier,
        icon = { DocumentGlyph(modifier = Modifier.size(40.dp), tint = AppColors.textMuted, strokeWidth = 2.dp) },
        title = stringResource(Res.string.preview_unavailable_title),
        description = stringResource(Res.string.preview_unavailable_description, document.typeCode),
    )
}

/**
 * The document's own known fields (type/size/created, plus the SDK's
 * classifier output — source/purpose), laid out as a dense 2-column
 * label/value grid (label above value), matching the reference layout's
 * "Informations extraites" panel. Name is deliberately not repeated here —
 * it's already the pane's header. Subjects are shown separately as tag
 * pills (see [DocumentKeywordsSection]).
 */
@Composable
private fun DocumentDetailsSection(document: FydeDocument) {
    val unsetValue = stringResource(Res.string.document_detail_value_unset)
    val fields = listOf(
        stringResource(Res.string.document_detail_type) to document.typeCode,
        stringResource(Res.string.document_detail_size) to localizedSize(document.size),
        stringResource(Res.string.document_detail_created) to localizedDate(document.createdAt),
        stringResource(Res.string.document_detail_source_category) to (document.sourceCategory?.label() ?: unsetValue),
        stringResource(Res.string.document_detail_source_subcategory) to (document.sourceSubCategory?.label() ?: unsetValue),
        stringResource(Res.string.document_detail_purpose) to (document.purpose?.label() ?: unsetValue),
    )
    Column(modifier = Modifier.fillMaxWidth().padding(AppSpacing.space5)) {
        Text(
            text = stringResource(Res.string.document_details_title),
            style = AppTypography.section,
            color = AppColors.text,
        )
        Spacer(Modifier.height(AppSpacing.space4))
        fields.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (label, value) ->
                    DetailCell(label, value, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(AppSpacing.space4))
        }
    }
}

/** One cell of an information grid ([DocumentDetailsSection], a script's saved parameters): a muted caption label stacked above the value. */
@Composable
internal fun DetailCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = AppTypography.caption, color = AppColors.textMuted)
        Spacer(Modifier.height(AppSpacing.space1))
        Text(
            text = value,
            style = AppTypography.body,
            color = AppColors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The document's subjects, rendered as tag pills under a "Keywords" heading —
 * the reference layout's "Mots-clés" section.
 */
@Composable
private fun DocumentKeywordsSection(document: FydeDocument) {
    Column(modifier = Modifier.fillMaxWidth().padding(AppSpacing.space5)) {
        Text(
            text = stringResource(Res.string.document_detail_keywords_title),
            style = AppTypography.section,
            color = AppColors.text,
        )
        Spacer(Modifier.height(AppSpacing.space3))
        if (document.subjects.isEmpty()) {
            Text(
                text = stringResource(Res.string.document_detail_value_unset),
                style = AppTypography.body,
                color = AppColors.textMuted,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.space2),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.space2),
            ) {
                document.subjects.forEach { subject -> AppBadge(text = subject) }
            }
        }
    }
}

/**
 * A folded-corner file icon on a rounded, tinted chip, drawn without an icon
 * pack (see [DocumentGlyph]), tinted per [DocumentKind] (see
 * [DocumentKind.tint] for why never with a semantic-state color). The short
 * type code is shown as text in the caller's own supporting line, not baked
 * into this chip, where it would drop below a readable size.
 */
@Composable
private fun FileTypeIcon(document: FydeDocument, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val tint = document.kind.tint()
    Box(
        modifier = modifier
            .size(size)
            .clip(AppRadius.md)
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        DocumentGlyph(tint = tint, modifier = Modifier.size(20.dp))
    }
}

/**
 * The Type, Period, Source, Source subtype and Purpose filters, each a chip
 * opening its choices. Only choices `fyde-sdk` says still lead to documents
 * are offered (see [DocumentFilterOptions] and [FydeSearchFacets]), each with
 * how many: picking a source narrows the subtypes and purposes on offer to
 * that source's, and a chip with nothing to offer isn't shown at all — so
 * Source subtype only appears once the documents in view have one. Found via
 * review that Issuer, Amount and "More filters" used to be permanently
 * disabled placeholders; every chip here works.
 */
@Composable
private fun FilterChipRow(
    search: DocumentSearch,
    options: DocumentFilterOptions,
    onSearchChange: (DocumentSearch) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space2),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space2),
    ) {
        FacetMenuChip(
            title = stringResource(Res.string.filter_type),
            allLabel = stringResource(Res.string.filter_type_all),
            selected = search.kind,
            options = options.kinds,
            optionLabel = { stringResource(it.label) },
            onSelect = { onSearchChange(search.copy(kind = it)) },
        )
        if (options.periods.size > 1) {
            FilterMenuChip(
                label = if (search.period == PeriodFilter.ANY) stringResource(Res.string.filter_period) else stringResource(search.period.label),
                selected = search.period != PeriodFilter.ANY,
                options = options.periods.map { it to null },
                optionLabel = { period -> stringResource(period.label) },
                onSelect = { onSearchChange(search.copy(period = it)) },
            )
        }
        FacetMenuChip(
            title = stringResource(Res.string.filter_source),
            allLabel = stringResource(Res.string.filter_source_all),
            selected = search.sourceCategory,
            options = options.sourceCategories,
            optionLabel = { it.label() },
            onSelect = { onSearchChange(search.withSourceCategory(it)) },
        )
        FacetMenuChip(
            title = stringResource(Res.string.filter_subcategory),
            allLabel = stringResource(Res.string.filter_subcategory_all),
            selected = search.sourceSubCategory,
            options = options.sourceSubCategories,
            optionLabel = { it.label() },
            onSelect = { onSearchChange(search.copy(sourceSubCategory = it)) },
        )
        FacetMenuChip(
            title = stringResource(Res.string.filter_purpose),
            allLabel = stringResource(Res.string.filter_purpose_all),
            selected = search.purpose,
            options = options.purposes,
            optionLabel = { it.label() },
            onSelect = { onSearchChange(search.copy(purpose = it)) },
        )
    }
}

/**
 * A [FilterMenuChip] over one facet's [options]: "all" first, then each
 * value with its document count. Hidden while there's nothing to pick and
 * nothing picked.
 */
@Composable
private fun <T : Any> FacetMenuChip(
    title: String,
    allLabel: String,
    selected: T?,
    options: List<FydeFacetCount<T>>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
) {
    if (options.isEmpty() && selected == null) return
    FilterMenuChip(
        label = selected?.let { optionLabel(it) } ?: title,
        selected = selected != null,
        options = listOf<Pair<T?, Long?>>(null to null) + options.map { it.value to it.count },
        optionLabel = { value -> value?.let { optionLabel(it) } ?: allLabel },
        onSelect = onSelect,
    )
}

/** An [AppChip] that opens a menu of [options]; its label shows the active choice once one is picked. */
@Composable
private fun <T> FilterMenuChip(
    label: String,
    selected: Boolean,
    // Each choice, with how many documents it leads to when known.
    options: List<Pair<T, Long?>>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AppChip(text = label, showChevron = true, selected = selected, onClick = { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = AppColors.surfaceElevated,
        ) {
            options.forEach { (option, count) ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option), style = AppTypography.body, color = AppColors.text) },
                    trailingIcon = count?.let {
                        { Text(it.toString(), style = AppTypography.caption, color = AppColors.textMuted) }
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun DocumentSkeletonList(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(6) { SkeletonRow() }
    }
}

@Composable
private fun SkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.space5, vertical = AppSpacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppSkeleton(modifier = Modifier.size(44.dp), shape = AppRadius.md)
        Spacer(Modifier.width(AppSpacing.space4))
        Column {
            AppSkeleton(modifier = Modifier.size(width = 160.dp, height = 14.dp))
            Spacer(Modifier.height(AppSpacing.space1))
            AppSkeleton(modifier = Modifier.size(width = 100.dp, height = 10.dp))
        }
    }
}
