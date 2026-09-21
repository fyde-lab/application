package com.peltoche.fyde

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun rememberDocumentPicker(onPicked: (List<String>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnPicked by rememberUpdatedState(onPicked)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isEmpty()) {
            latestOnPicked(emptyList())
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            latestOnPicked(
                withContext(Dispatchers.IO) {
                    uris.mapIndexedNotNull { index, uri -> copyToCache(context, uri, index) }
                },
            )
        }
    }

    return remember { { launcher.launch(arrayOf("*/*")) } }
}

/**
 * The Storage Access Framework only hands back a `content://` [Uri] — often
 * backed by another app entirely (Drive, a file provider, ...) — but
 * [FydeSdk.uploadDocument] needs a real filesystem path, so each picked file
 * is copied into the app's cache dir first. Copied into its own `uniquifier`
 * subdirectory (the file's index in the current pick) so that two same-named
 * files from different sources don't clobber each other — the filename
 * itself stays untouched, since `fyde-sdk` derives the document's display
 * name straight from it (see `sdk/src/services/documents/service.rs`).
 */
private fun copyToCache(context: Context, uri: Uri, uniquifier: Int): String? {
    val name = queryDisplayName(context, uri) ?: return null
    val directory = File(context.cacheDir, "upload-$uniquifier").apply { mkdirs() }
    val target = File(directory, name)
    val copied = context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
        true
    } ?: false
    return if (copied) target.absolutePath else null
}

private fun queryDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
