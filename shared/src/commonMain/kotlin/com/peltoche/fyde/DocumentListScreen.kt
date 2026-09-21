package com.peltoche.fyde

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.connect_failed_title
import com.peltoche.fyde.resources.connecting_message
import com.peltoche.fyde.resources.dismiss
import com.peltoche.fyde.resources.documents_title
import com.peltoche.fyde.resources.no_documents_description
import com.peltoche.fyde.resources.no_documents_match
import com.peltoche.fyde.resources.no_documents_title
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.resources.search_documents_placeholder
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
     * dropping) without discarding what's already loaded.
     */
    data class Loaded(
        val documents: List<FydeDocument>?,
        val banner: String? = null,
        val onDismissBanner: () -> Unit = {},
    ) : DocumentListUiState
}

@Composable
fun DocumentListScreen(
    state: DocumentListUiState,
    modifier: Modifier = Modifier,
    onUpload: () -> Unit = {},
    isUploading: Boolean = false,
) {
    when (state) {
        DocumentListUiState.Connecting -> ConnectingState(modifier)
        is DocumentListUiState.ConnectFailed -> ConnectFailedState(state, modifier)
        is DocumentListUiState.Loaded -> LoadedDocumentList(state, modifier, onUpload, isUploading)
    }
}

@Composable
private fun ConnectingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.connecting_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConnectFailedState(state: DocumentListUiState.ConnectFailed, modifier: Modifier = Modifier) {
    CenteredMessage(
        modifier = modifier,
        iconBackground = MaterialTheme.colorScheme.errorContainer,
        icon = {
            Text("!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        },
        title = stringResource(Res.string.connect_failed_title),
        description = state.message,
        actionLabel = stringResource(Res.string.retry),
        onAction = state.onRetry,
    )
}

@Composable
private fun LoadedDocumentList(
    state: DocumentListUiState.Loaded,
    modifier: Modifier = Modifier,
    onUpload: () -> Unit = {},
    isUploading: Boolean = false,
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        DocumentListTopBar(onUpload = onUpload, isUploading = isUploading)
        state.banner?.let { message ->
            StatusBanner(message = message, onDismiss = state.onDismissBanner)
        }
        SearchField(value = searchQuery, onValueChange = { searchQuery = it })

        val documents = state.documents
        when {
            documents == null -> DocumentListSkeleton(modifier = Modifier.weight(1f))

            documents.isEmpty() && searchQuery.isBlank() -> CenteredMessage(
                modifier = Modifier.weight(1f),
                iconBackground = MaterialTheme.colorScheme.surfaceContainerLow,
                icon = {
                    DocumentGlyph(
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        strokeWidth = 2.dp,
                    )
                },
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
                            .padding(top = 32.dp, start = 32.dp, end = 32.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(
                            text = stringResource(Res.string.no_documents_match, searchQuery),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(top = 4.dp),
                    ) {
                        items(filtered) { document -> DocumentRow(document.toUiDocument()) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentListTopBar(onUpload: () -> Unit = {}, isUploading: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(Res.string.documents_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        UploadButton(
            onUpload = onUpload,
            isUploading = isUploading,
            modifier = Modifier.align(Alignment.CenterEnd).padding(horizontal = 8.dp),
        )
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun UploadButton(onUpload: () -> Unit, isUploading: Boolean, modifier: Modifier = Modifier) {
    if (isUploading) {
        CircularProgressIndicator(
            modifier = modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
    } else {
        val description = stringResource(Res.string.upload_document)
        IconButton(
            onClick = onUpload,
            modifier = modifier.semantics { contentDescription = description },
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(stringResource(Res.string.search_documents_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            cursorColor = MaterialTheme.colorScheme.onBackground,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** A dismissible, non-fatal problem banner pinned above the list — the list itself stays put. */
@Composable
private fun StatusBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) {
            Text(stringResource(Res.string.dismiss), color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

/** The icon-in-circle / title / description / optional-action layout shared by empty and full-screen error states. */
@Composable
private fun CenteredMessage(
    modifier: Modifier = Modifier,
    iconBackground: Color,
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

/** Six placeholder rows shaped like [DocumentRow], pulsing while the first page loads. */
@Composable
private fun DocumentListSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(6) { SkeletonRow(alpha) }
    }
}

@Composable
private fun SkeletonRow(alpha: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)),
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)),
            )
        }
    }
}

private fun FydeDocument.toUiDocument() = Document(
    title = name,
    sizeLabel = formatSize(size),
    receivedLabel = formatDate(createdAt),
)

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
private fun formatDate(epochSeconds: Long): String {
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
