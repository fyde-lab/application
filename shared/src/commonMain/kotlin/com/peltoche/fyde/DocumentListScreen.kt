package com.peltoche.fyde

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peltoche.fyde.components.action.AppMenuAction
import com.peltoche.fyde.components.action.AppMenuButton
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
import com.peltoche.fyde.resources.documents_title
import com.peltoche.fyde.resources.log_out
import com.peltoche.fyde.resources.no_documents_description
import com.peltoche.fyde.resources.no_documents_match
import com.peltoche.fyde.resources.no_documents_title
import com.peltoche.fyde.resources.profile_menu
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.search_documents_placeholder
import com.peltoche.fyde.resources.scripts_menu_item
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
    onLogout: () -> Unit = {},
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
        is DocumentListUiState.Loaded -> LoadedDocumentList(state, modifier, onUpload, isUploading, onLogout, onOpenDocument, onOpenSettings, onOpenScripts)
    }
}

@Composable
private fun LoadedDocumentList(
    state: DocumentListUiState.Loaded,
    modifier: Modifier = Modifier,
    onUpload: () -> Unit = {},
    isUploading: Boolean = false,
    onLogout: () -> Unit = {},
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

    AppScaffold(
        modifier = modifier,
        windowSizeClass = windowSizeClass,
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
                Spacer(Modifier.width(AppSpacing.space2))
                AppMenuButton(
                    contentDescription = stringResource(Res.string.profile_menu),
                    actions = listOf(
                        AppMenuAction(label = stringResource(Res.string.scripts_menu_item), onClick = onOpenScripts),
                        AppMenuAction(label = stringResource(Res.string.settings), onClick = onOpenSettings),
                        AppMenuAction(label = stringResource(Res.string.log_out), onClick = onLogout),
                    ),
                ) {
                    AvatarGlyph(modifier = Modifier.size(18.dp), tint = AppColors.textMuted)
                }
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
                )

                val documents = state.documents
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
                leadingContent = { PdfFileIcon() },
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
        Column(modifier = Modifier.fillMaxWidth().padding(AppSpacing.space5)) {
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
        AppDivider()
        PdfViewer(pdfBytes = document.content, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}

/**
 * The round avatar in the top-right corner's [AppMenuButton] trigger. Just a
 * placeholder glyph — the app has no profile picture of its own yet.
 */
@Composable
private fun AvatarGlyph(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier) {
        val headRadius = size.minDimension * 0.28f
        drawCircle(
            color = tint,
            radius = headRadius,
            center = Offset(size.width / 2f, size.height * 0.32f),
        )
        val shouldersPath = Path().apply {
            val shoulderTop = size.height * 0.62f
            moveTo(0f, size.height)
            cubicTo(
                0f, shoulderTop,
                size.width, shoulderTop,
                size.width, size.height,
            )
            close()
        }
        drawPath(shouldersPath, color = tint)
    }
}

/** A folded-corner PDF file icon on a rounded chip, drawn without an icon pack (see [DocumentGlyph]). */
@Composable
private fun PdfFileIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(AppRadius.md)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        DocumentGlyph(
            modifier = Modifier
                .align(Alignment.Center)
                .size(20.dp)
                .padding(bottom = 4.dp),
        )
        Text(
            text = "PDF",
            color = AppColors.primary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 7.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
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
