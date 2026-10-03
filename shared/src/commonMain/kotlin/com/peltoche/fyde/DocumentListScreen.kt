package com.peltoche.fyde

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.action.AppIconButton
import com.peltoche.fyde.components.content.AppBadge
import com.peltoche.fyde.components.content.AppChip
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.feedback.AppEmptyState
import com.peltoche.fyde.components.feedback.AppErrorState
import com.peltoche.fyde.components.feedback.AppLoading
import com.peltoche.fyde.components.feedback.AppSkeleton
import com.peltoche.fyde.components.content.AppListItem
import com.peltoche.fyde.components.form.AppSearchField
import com.peltoche.fyde.components.navigation.AppTopBar
import com.peltoche.fyde.layout.AppContentContainer
import com.peltoche.fyde.layout.AppContentWidth
import com.peltoche.fyde.layout.AppScaffold
import com.peltoche.fyde.layout.AppTwoPaneLayout
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppRadius
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import com.peltoche.fyde.resources.Res
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
import com.peltoche.fyde.resources.document_favorite
import com.peltoche.fyde.resources.document_more_actions
import com.peltoche.fyde.resources.document_preview_title
import com.peltoche.fyde.resources.documents_count
import com.peltoche.fyde.resources.documents_title
import com.peltoche.fyde.resources.filter_amount
import com.peltoche.fyde.resources.filter_issuer
import com.peltoche.fyde.resources.filter_more
import com.peltoche.fyde.resources.filter_period
import com.peltoche.fyde.resources.filter_type
import com.peltoche.fyde.resources.no_documents_description
import com.peltoche.fyde.resources.no_documents_match
import com.peltoche.fyde.resources.no_documents_title
import com.peltoche.fyde.resources.no_matches_title
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.search_documents_placeholder
import com.peltoche.fyde.resources.search_results_count
import com.peltoche.fyde.resources.select_document_description
import com.peltoche.fyde.resources.select_document_title
import com.peltoche.fyde.resources.settings
import com.peltoche.fyde.resources.upload_document
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
     * than a spinner), and non-null once loaded (possibly empty).
     * [banner] surfaces a non-fatal problem (e.g. the changelog stream
     * dropping) without discarding what's already loaded. [serverReachable]
     * mirrors `fyde-sdk`'s own `is_server_reachable` check, polled
     * periodically by the caller — the upload button reflects it by
     * disabling itself whenever the server isn't currently reachable.
     */
    data class Loaded(
        val documents: List<FydeDocument>?,
        val banner: String? = null,
        val onDismissBanner: () -> Unit = {},
        val serverReachable: Boolean = true,
    ) : DocumentListUiState
}

@Composable
fun DocumentListScreen(
    state: DocumentListUiState,
    modifier: Modifier = Modifier,
    onUpload: () -> Unit = {},
    isUploading: Boolean = false,
    onOpenDocument: (FydeDocument) -> Unit = {},
    onOpenSettings: () -> Unit = {},
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
        is DocumentListUiState.Loaded -> LoadedDocumentList(state, modifier, onUpload, isUploading, onOpenDocument, onOpenSettings)
    }
}

@Composable
private fun LoadedDocumentList(
    state: DocumentListUiState.Loaded,
    modifier: Modifier = Modifier,
    onUpload: () -> Unit = {},
    isUploading: Boolean = false,
    onOpenDocument: (FydeDocument) -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    // Which document is highlighted in the Medium/Expanded preview pane —
    // presentation-only, like searchQuery, so it stays local rather than
    // being hoisted to the caller (see AppTwoPaneLayout's doc comment).
    var selectedDocument by remember { mutableStateOf<FydeDocument?>(null) }
    val windowSizeClass = LocalAppWindowSizeClass.current
    // The persistent sidebar is desktop-only chrome — Compact/Medium keep
    // today's top-bar-only navigation unchanged, so this stays empty there
    // rather than forcing AppNavigationLayout's bar/rail surfaces to adopt
    // destinations they were never designed to show.
    val isDesktop = windowSizeClass == AppWindowSizeClass.Expanded

    AppScaffold(
        modifier = modifier,
        windowSizeClass = windowSizeClass,
        navigationItems = if (isDesktop) {
            sidebarNavigationItems(current = SidebarDestination.Documents, onOpenDocuments = {})
        } else {
            emptyList()
        },
        navigationHeader = if (isDesktop) { { SidebarHeader() } } else null,
        navigationFooter = if (isDesktop) { { SidebarFooter(current = SidebarDestination.Documents, onOpenSettings = onOpenSettings) } } else null,
        topBar = {
            AppTopBar(title = stringResource(Res.string.documents_title), windowSizeClass = windowSizeClass) {
                // The persistent sidebar already gives Expanded a route to
                // Settings (see SidebarFooter) — this is the only route for
                // Compact/Medium, which have no sidebar at all. Without it,
                // Settings (and the logout it hosts) was unreachable below
                // desktop width.
                if (!isDesktop) {
                    AppIconButton(onClick = onOpenSettings, contentDescription = stringResource(Res.string.settings)) {
                        GearGlyph(modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(AppSpacing.space2))
                }
                AppButton(
                    text = stringResource(Res.string.upload_document),
                    onClick = onUpload,
                    variant = AppButtonVariant.Filled,
                    enabled = state.serverReachable,
                    loading = isUploading,
                    leadingIcon = { PlusGlyph(modifier = Modifier.size(14.dp), tint = AppColors.onPrimary) },
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
                state.banner?.let { message ->
                    AppBanner(message = message, onDismiss = state.onDismissBanner, dismissLabel = stringResource(Res.string.dismiss))
                }

                val documents = state.documents
                val filteredDocuments = remember(documents, searchQuery) {
                    documents?.filter { it.name.contains(searchQuery, ignoreCase = true) } ?: emptyList()
                }

                // The search field, filter chips, and results toolbar are part
                // of the *list* column, not full-width page chrome — found via
                // review of the reference layout that putting them outside the
                // two-pane split let the search bar stretch wider than the list
                // it searches, while the detail pane only started below all of
                // that instead of running alongside it from the top. Wrapping
                // the whole left column (search → filters → results) inside
                // [AppTwoPaneLayout]'s primary slot keeps its width pinned to
                // the list column and lets the detail pane sit beside it, top
                // to bottom, exactly like the reference's two columns.
                AppTwoPaneLayout(
                    modifier = Modifier.weight(1f),
                    windowSizeClass = windowSizeClass,
                    primaryPaneWidth = if (isDesktop) AppDimensions.twoPaneListWidthExpanded else AppDimensions.twoPaneListWidth,
                    primary = {
                        // Trailing padding matches AppContentContainer's own
                        // leading margin (space5, 16dp) on the other side of
                        // this column — found via review that the list column
                        // had a visible gap before the sidebar on its left but
                        // ran flush up against the divider/detail pane on its
                        // right, an asymmetry nothing else in the screen has.
                        Column(modifier = Modifier.fillMaxSize().padding(end = AppSpacing.space5)) {
                            // Matches the space3 gap below the search field
                            // (before the filter row) so it doesn't sit flush
                            // under the top bar divider while having visible
                            // breathing room underneath.
                            Spacer(Modifier.height(AppSpacing.space3))
                            AppSearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = stringResource(Res.string.search_documents_placeholder),
                                onClear = { searchQuery = "" },
                                clearContentDescription = stringResource(Res.string.clear_search),
                                leadingIcon = { SearchGlyph(modifier = Modifier.size(16.dp)) },
                            )

                            if (isDesktop) {
                                Spacer(Modifier.height(AppSpacing.space3))
                                FilterChipRow()
                            }

                            if (isDesktop && documents != null) {
                                Spacer(Modifier.height(AppSpacing.space4))
                                // Sort-by and list/grid-view controls were
                                // removed — they were non-functional
                                // placeholders (no real sort order, no grid
                                // layout) and read as broken affordances
                                // rather than useful chrome. Just the count
                                // remains; re-add once those features exist.
                                Text(
                                    text = if (searchQuery.isBlank()) {
                                        stringResource(Res.string.documents_count, filteredDocuments.size)
                                    } else {
                                        stringResource(Res.string.search_results_count, filteredDocuments.size, searchQuery)
                                    },
                                    style = AppTypography.caption,
                                    color = AppColors.textMuted,
                                )
                                Spacer(Modifier.height(AppSpacing.space2))
                            }

                            when {
                                documents == null -> DocumentSkeletonList(modifier = Modifier.weight(1f))

                                documents.isEmpty() && searchQuery.isBlank() -> AppEmptyState(
                                    modifier = Modifier.weight(1f),
                                    icon = { DocumentGlyph(modifier = Modifier.size(40.dp), tint = AppColors.textMuted, strokeWidth = 2.dp) },
                                    title = stringResource(Res.string.no_documents_title),
                                    description = stringResource(Res.string.no_documents_description),
                                )

                                filteredDocuments.isEmpty() -> AppEmptyState(
                                    modifier = Modifier.weight(1f),
                                    icon = { DocumentGlyph(modifier = Modifier.size(40.dp), tint = AppColors.textMuted, strokeWidth = 2.dp) },
                                    title = stringResource(Res.string.no_matches_title),
                                    description = stringResource(Res.string.no_documents_match, searchQuery),
                                    actionLabel = stringResource(Res.string.clear_search),
                                    onAction = { searchQuery = "" },
                                )

                                else -> DocumentList(
                                    documents = filteredDocuments,
                                    selectedId = selectedDocument?.id,
                                    rowMinHeight = rowMinHeightFor(windowSizeClass),
                                    onClick = { document ->
                                        if (windowSizeClass == AppWindowSizeClass.Compact) {
                                            onOpenDocument(document)
                                        } else {
                                            selectedDocument = document
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    },
                    secondary = { DocumentPreviewPane(document = selectedDocument) },
                )
            }
        }
    }
}

private fun rowMinHeightFor(windowSizeClass: AppWindowSizeClass): Dp = when (windowSizeClass) {
    AppWindowSizeClass.Compact -> AppDimensions.rowHeightComfortable
    AppWindowSizeClass.Medium -> AppDimensions.rowHeightStandard
    AppWindowSizeClass.Expanded -> AppDimensions.rowHeightCompact
}

@Composable
private fun DocumentList(
    documents: List<FydeDocument>,
    selectedId: String?,
    rowMinHeight: Dp,
    onClick: (FydeDocument) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(top = AppSpacing.space2)) {
        items(documents, key = { it.id }) { document ->
            val (typeLabel, _) = fileTypeVisualsFor(document)
            AppListItem(
                title = document.name,
                supportingText = "$typeLabel · ${formatSize(document.size)} · ${formatDate(document.createdAt)}",
                supportingTextStyle = AppTypography.data,
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

/** The Medium/Expanded secondary pane: the selected document's real preview, or a prompt to pick one. */
@Composable
private fun DocumentPreviewPane(document: FydeDocument?) {
    if (document == null) {
        AppEmptyState(
            title = stringResource(Res.string.select_document_title),
            description = stringResource(Res.string.select_document_description),
            icon = { DocumentGlyph(modifier = Modifier.size(40.dp), tint = AppColors.textMuted, strokeWidth = 2.dp) },
        )
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        val (typeLabel, _) = fileTypeVisualsFor(document)
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.space5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileTypeIcon(document, size = 40.dp)
            Spacer(Modifier.width(AppSpacing.space4))
            Column(modifier = Modifier.weight(1f)) {
                // Unlike the list row (AppListItem, always 1 line — a list is
                // scanned), this is the one place the full name matters more
                // than row-height consistency, so it gets 2 lines before
                // falling back to ellipsis.
                Text(
                    text = document.name,
                    style = AppTypography.bodyEmphasis,
                    color = AppColors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(AppSpacing.space1))
                Text(
                    text = "$typeLabel · ${formatSize(document.size)} · ${formatDate(document.createdAt)}",
                    style = AppTypography.data,
                    color = AppColors.textMuted,
                )
            }
            // Non-functional today, like the filter chips — visible
            // affordances a document detail pane is expected to have,
            // without fabricating upload/sharing behavior that doesn't
            // exist. `enabled = false` (plus a dimmed glyph tint, since
            // these draw their own color rather than reading
            // AppIconButton's content color) so each reads as "not yet,"
            // not as a button that silently does nothing when pressed.
            val inertTint = AppColors.textMuted.copy(alpha = 0.5f)
            AppIconButton(onClick = {}, contentDescription = stringResource(Res.string.document_favorite), enabled = false) {
                StarGlyph(modifier = Modifier.size(16.dp), tint = inertTint)
            }
            AppIconButton(onClick = {}, contentDescription = stringResource(Res.string.document_download), enabled = false) {
                DownloadGlyph(modifier = Modifier.size(16.dp), tint = inertTint)
            }
            AppIconButton(onClick = {}, contentDescription = stringResource(Res.string.document_more_actions), enabled = false) {
                MoreGlyph(modifier = Modifier.size(16.dp), tint = inertTint)
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
        PdfViewer(pdfBytes = document.content, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}

/**
 * The document's own known fields (type/size/created, plus the SDK's
 * classifier output — source/purpose, derived from the document's transcript
 * rather than OCR, see `../../sdk/src/domains/documents/parser`), laid out as
 * a dense 2-column label/value grid (label above value) rather than one
 * label-left/value-right column per row — matching the reference layout's
 * "Informations extraites" panel, and fitting roughly twice as many fields
 * in the same vertical space, consistent with the density-is-a-feature
 * principle in `design/design-system.md`. Name is deliberately not repeated
 * here — it's already the pane's header, immediately above. Subjects is
 * shown separately as tag pills (see [DocumentKeywordsSection]), not as a
 * grid cell — a comma-joined list of values doesn't fit this grid's
 * one-value-per-cell shape.
 */
@Composable
private fun DocumentDetailsSection(document: FydeDocument) {
    val (typeLabel, _) = fileTypeVisualsFor(document)
    val unsetValue = stringResource(Res.string.document_detail_value_unset)
    val fields = listOf(
        stringResource(Res.string.document_detail_type) to typeLabel,
        stringResource(Res.string.document_detail_size) to formatSize(document.size),
        stringResource(Res.string.document_detail_created) to formatDate(document.createdAt),
        stringResource(Res.string.document_detail_source_category) to (document.sourceCategory?.toLabel() ?: unsetValue),
        stringResource(Res.string.document_detail_source_subcategory) to (document.sourceSubCategory?.toLabel() ?: unsetValue),
        stringResource(Res.string.document_detail_purpose) to (document.purpose?.toLabel() ?: unsetValue),
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

/** Title-cases an enum's constant name (e.g. `REAL_ESTATE` -> `Real Estate`) for display. */
private fun Enum<*>.toLabel(): String =
    name.split('_').joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

/** One cell of [DocumentDetailsSection]'s grid: a muted caption label stacked above the value. */
@Composable
private fun DetailCell(label: String, value: String, modifier: Modifier = Modifier) {
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
 * the reference layout's "Mots-clés" section. Pulled out of
 * [DocumentDetailsSection]'s grid because a multi-value field doesn't fit
 * that grid's one-value-per-cell shape; a wrapped row of [AppBadge]s is the
 * natural shape for a set of tags instead.
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
 * pack (see [DocumentGlyph]). The tint varies by file type, drawn only from
 * [fileTypeVisualsFor]'s palette — never from `success`/`warning`/`error`,
 * which mean "this succeeded" / "this needs caution" / "this failed"
 * everywhere else in the app (banners, badges, the encryption trust badge).
 * A spreadsheet or an image is neither a warning nor a success, so
 * file-type categorization must not borrow those tokens just because they
 * happen to be visually distinct colors.
 *
 * [fileTypeVisualsFor]'s short type label ("PDF", "XLS"...) is shown as
 * text in the caller's own supporting-text line (see [DocumentList] and
 * [DocumentPreviewPane]), not baked into this chip — a label small enough
 * to fit inside a 44dp chip dropped below any platform's readable floor,
 * and being `sp`-scaled meant it could grow past the chip's own bounds for
 * anyone with larger system text settings on.
 */
@Composable
private fun FileTypeIcon(document: FydeDocument, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val (_, tint) = fileTypeVisualsFor(document)
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
 * Short type label + accent tint for [FileTypeIcon], derived from real
 * [FydeDocument] fields only. The tint draws exclusively from the app's two
 * brand accents — `primary` and `secondary`, the latter already scoped by
 * design-system.md's Color table to "folder/document-type accents" — plus
 * two blends between them, so four file types stay visually distinct
 * without reaching for a semantic-state color (see [FileTypeIcon]'s doc
 * comment) or inventing a fifth hue the "one disciplined accent" rule
 * doesn't allow for.
 */
@Composable
private fun fileTypeVisualsFor(document: FydeDocument): Pair<String, Color> {
    val extension = document.name.substringAfterLast('.', "").uppercase().take(4)
    val type = document.contentType.lowercase()
    val primary = AppColors.primary
    val secondary = AppColors.secondary
    return when {
        type.contains("pdf") -> "PDF" to primary
        type.contains("spreadsheet") || type.contains("excel") || type.contains("csv") ->
            extension.ifEmpty { "XLS" } to secondary
        type.startsWith("image/") -> extension.ifEmpty { "IMG" } to lerp(primary, secondary, 0.5f)
        type.contains("word") || type.contains("document") ->
            extension.ifEmpty { "DOC" } to lerp(secondary, AppColors.text, 0.3f)
        else -> extension.ifEmpty { "FILE" } to AppColors.textMuted
    }
}

/**
 * Non-functional filter affordances (Type/Issuer/Period/Amount/more) matching
 * the desktop reference layout's filter row — [FydeDocument] has no
 * issuer/amount fields to filter on, so these are visible, disabled
 * placeholders rather than a fabricated filtering feature.
 */
@Composable
private fun FilterChipRow() {
    // FlowRow, not Row: the primary (list) pane is a fixed 360dp wide, and
    // five chips don't reliably fit on one line at that width — found via
    // review that a plain Row let the trailing "+ More filters" chip
    // overflow straight into the detail pane's divider/column instead of
    // wrapping. Wrapping to a second line keeps every chip inside the list
    // column no matter how the labels localize.
    // None of these has an onClick — there's no real filtering feature
    // behind any of them yet, and AppChip(enabled = false) dims each one so
    // the row reads as "not yet wired up" rather than looking like a
    // working control a tap or click will silently do nothing on.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.space2),
    ) {
        AppChip(text = stringResource(Res.string.filter_type), enabled = false, showChevron = true)
        AppChip(text = stringResource(Res.string.filter_issuer), enabled = false, showChevron = true)
        AppChip(text = stringResource(Res.string.filter_period), enabled = false, showChevron = true)
        AppChip(text = stringResource(Res.string.filter_amount), enabled = false, showChevron = true)
        // No trailing chevron — unlike the others this isn't a dropdown.
        // The leading "+" lives in the string resource itself (not
        // concatenated here) so a translation controls the whole visible
        // label, not just part of it.
        AppChip(text = stringResource(Res.string.filter_more), enabled = false)
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

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

/**
 * Formats a Unix epoch-seconds timestamp as "YYYY-MM-DD", converting via
 * Howard Hinnant's `civil_from_days` proleptic-Gregorian algorithm (pure
 * integer math) rather than a platform clock/date API, so it behaves
 * identically on every Kotlin Multiplatform target without an extra
 * dependency.
 */
internal fun formatDate(epochSeconds: Long): String {
    val days = floorDiv(epochSeconds, 86_400L)
    val z = days + 719_468L
    val era = floorDiv(if (z >= 0) z else z - 146_096L, 146_097L)
    val doe = z - era * 146_097L
    val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
    val year = yoe + era * 400L
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
    val civilYear = if (month <= 2) year + 1 else year
    return "$civilYear-${pad2(month)}-${pad2(day)}"
}

private fun floorDiv(x: Long, y: Long): Long {
    val q = x / y
    return if (x % y != 0L && (x < 0) != (y < 0)) q - 1 else q
}

private fun pad2(n: Int): String = if (n < 10) "0$n" else "$n"
