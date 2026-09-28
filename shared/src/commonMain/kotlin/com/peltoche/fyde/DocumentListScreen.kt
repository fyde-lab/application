package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peltoche.fyde.components.action.AppTextButton
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
import com.peltoche.fyde.resources.document_detail_name
import com.peltoche.fyde.resources.document_detail_size
import com.peltoche.fyde.resources.document_detail_type
import com.peltoche.fyde.resources.document_details_title
import com.peltoche.fyde.resources.documents_count
import com.peltoche.fyde.resources.documents_title
import com.peltoche.fyde.resources.filter_more
import com.peltoche.fyde.resources.filter_period
import com.peltoche.fyde.resources.filter_type
import com.peltoche.fyde.resources.no_documents_description
import com.peltoche.fyde.resources.no_documents_match
import com.peltoche.fyde.resources.no_documents_title
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.search_documents_placeholder
import com.peltoche.fyde.resources.select_document_description
import com.peltoche.fyde.resources.select_document_title
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
    onOpenScripts: () -> Unit = {},
) {
    when (state) {
        DocumentListUiState.Connecting -> AppLoading(
            modifier = modifier,
            message = stringResource(Res.string.connecting_message),
        )
        is DocumentListUiState.ConnectFailed -> AppErrorState(
            modifier = modifier,
            icon = {
                // "!" stands in for an icon glyph, not language — exempt from
                // the Regular/Medium/SemiBold-only rule real text follows.
                Text("!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = AppColors.error)
            },
            title = stringResource(Res.string.connect_failed_title),
            description = state.message,
            actionLabel = stringResource(Res.string.retry),
            onAction = state.onRetry,
        )
        is DocumentListUiState.Loaded -> LoadedDocumentList(state, modifier, onUpload, isUploading, onOpenDocument, onOpenSettings, onOpenScripts)
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
    onOpenScripts: () -> Unit = {},
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
            sidebarNavigationItems(current = SidebarDestination.Documents, onOpenDocuments = {}, onOpenScripts = onOpenScripts)
        } else {
            emptyList()
        },
        navigationHeader = if (isDesktop) { { SidebarHeader() } } else null,
        navigationFooter = if (isDesktop) { { SidebarFooter(current = SidebarDestination.Documents, onOpenSettings = onOpenSettings) } } else null,
        topBar = {
            AppTopBar(title = stringResource(Res.string.documents_title), windowSizeClass = windowSizeClass) {
                AppTextButton(
                    text = stringResource(Res.string.upload_document),
                    onClick = onUpload,
                    enabled = state.serverReachable,
                    loading = isUploading,
                    leadingIcon = {
                        // "+" stands in for an icon glyph, not language.
                        Text("+", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    },
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
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                state.banner?.let { message ->
                    AppBanner(message = message, onDismiss = state.onDismissBanner, dismissLabel = stringResource(Res.string.dismiss))
                }
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

                val documents = state.documents
                val filteredCount = remember(documents, searchQuery) {
                    documents?.count { it.name.contains(searchQuery, ignoreCase = true) } ?: 0
                }

                if (isDesktop && documents != null) {
                    Spacer(Modifier.height(AppSpacing.space4))
                    Text(
                        text = stringResource(Res.string.documents_count, filteredCount),
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

                    else -> {
                        val filtered = remember(documents, searchQuery) {
                            documents.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        }
                        if (filtered.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(top = AppSpacing.space8, start = AppSpacing.space8, end = AppSpacing.space8),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Text(
                                    text = stringResource(Res.string.no_documents_match, searchQuery),
                                    style = AppTypography.body,
                                    color = AppColors.textMuted,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            AppTwoPaneLayout(
                                modifier = Modifier.weight(1f),
                                windowSizeClass = windowSizeClass,
                                primary = {
                                    DocumentList(
                                        documents = filtered,
                                        selectedId = selectedDocument?.id,
                                        rowMinHeight = rowMinHeightFor(windowSizeClass),
                                        onClick = { document ->
                                            if (windowSizeClass == AppWindowSizeClass.Compact) {
                                                onOpenDocument(document)
                                            } else {
                                                selectedDocument = document
                                            }
                                        },
                                    )
                                },
                                secondary = { DocumentPreviewPane(document = selectedDocument) },
                            )
                        }
                    }
                }
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
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = AppSpacing.space2)) {
        items(documents, key = { it.id }) { document ->
            AppListItem(
                title = document.name,
                supportingText = "${formatSize(document.size)} · ${formatDate(document.createdAt)}",
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.space5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileTypeIcon(document, size = 40.dp)
            Spacer(Modifier.width(AppSpacing.space4))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = AppTypography.bodyEmphasis,
                    color = AppColors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(AppSpacing.space1))
                Text(
                    text = "${formatSize(document.size)} · ${formatDate(document.createdAt)}",
                    style = AppTypography.data,
                    color = AppColors.textMuted,
                )
            }
        }
        AppDivider()
        DocumentDetailsSection(document)
        AppDivider()
        PdfViewer(pdfBytes = document.content, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}

/**
 * The document's own known fields (name/type/size/created), laid out as
 * label/value rows above the preview — no OCR or extraction pipeline exists
 * (the server only ever sees ciphertext, see ../CLAUDE.md), so unlike a
 * document-intelligence product this section surfaces real document
 * metadata rather than fabricated extracted fields.
 */
@Composable
private fun DocumentDetailsSection(document: FydeDocument) {
    val (typeLabel, _) = fileTypeVisualsFor(document)
    Column(modifier = Modifier.fillMaxWidth().padding(AppSpacing.space5)) {
        Text(
            text = stringResource(Res.string.document_details_title),
            style = AppTypography.section,
            color = AppColors.text,
        )
        Spacer(Modifier.height(AppSpacing.space4))
        DetailRow(stringResource(Res.string.document_detail_name), document.name)
        DetailRow(stringResource(Res.string.document_detail_type), typeLabel)
        DetailRow(stringResource(Res.string.document_detail_size), formatSize(document.size))
        DetailRow(stringResource(Res.string.document_detail_created), formatDate(document.createdAt))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.space2)) {
        Text(
            text = label,
            style = AppTypography.caption,
            color = AppColors.textMuted,
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = value,
            style = AppTypography.body,
            color = AppColors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * A folded-corner file icon on a rounded, tinted chip, drawn without an icon
 * pack (see [DocumentGlyph]). The tint/label vary by file type — reusing the
 * design system's own semantic tokens (`primary`/`secondary`/`success`/
 * `warning`), not an arbitrary per-type rainbow — so a mixed document list
 * stays scannable-by-type while the palette stays within
 * design-system.md's "one disciplined accent" rule.
 */
@Composable
private fun FileTypeIcon(document: FydeDocument, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val (label, tint) = fileTypeVisualsFor(document)
    Box(
        modifier = modifier
            .size(size)
            .clip(AppRadius.md)
            .background(tint.copy(alpha = 0.14f)),
    ) {
        DocumentGlyph(
            tint = tint,
            modifier = Modifier
                .align(Alignment.Center)
                .size(20.dp)
                .padding(bottom = 4.dp),
        )
        Text(
            text = label,
            color = tint,
            fontWeight = FontWeight.SemiBold,
            fontSize = 7.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
        )
    }
}

/** Short type label + accent tint for [FileTypeIcon], derived from real [FydeDocument] fields only. */
@Composable
private fun fileTypeVisualsFor(document: FydeDocument): Pair<String, Color> {
    val extension = document.name.substringAfterLast('.', "").uppercase().take(4)
    val type = document.contentType.lowercase()
    return when {
        type.contains("pdf") -> "PDF" to AppColors.primary
        type.contains("spreadsheet") || type.contains("excel") || type.contains("csv") ->
            extension.ifEmpty { "XLS" } to AppColors.success
        type.startsWith("image/") -> extension.ifEmpty { "IMG" } to AppColors.warning
        type.contains("word") || type.contains("document") -> extension.ifEmpty { "DOC" } to AppColors.secondary
        else -> extension.ifEmpty { "FILE" } to AppColors.textMuted
    }
}

/**
 * Non-functional filter affordances (Type/Period/more) matching the desktop
 * reference layout's filter row — [FydeDocument] has no émetteur/montant
 * fields to filter on, so these are visible, disabled placeholders rather
 * than a fabricated filtering feature.
 */
@Composable
private fun FilterChipRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilterChip(stringResource(Res.string.filter_type))
        Spacer(Modifier.width(AppSpacing.space3))
        FilterChip(stringResource(Res.string.filter_period))
        Spacer(Modifier.width(AppSpacing.space3))
        Text(
            text = "+ ${stringResource(Res.string.filter_more)}",
            style = AppTypography.caption,
            color = AppColors.textMuted,
        )
    }
}

@Composable
private fun FilterChip(label: String) {
    Row(
        modifier = Modifier
            .clip(AppRadius.sm)
            .border(AppDimensions.borderHairline, AppColors.border, AppRadius.sm)
            .padding(horizontal = AppSpacing.space3, vertical = AppSpacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = AppTypography.caption, color = AppColors.textMuted)
        Spacer(Modifier.width(AppSpacing.space1))
        ChevronRightGlyph(
            modifier = Modifier.size(10.dp).rotate(90f),
            tint = AppColors.textMuted,
        )
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
