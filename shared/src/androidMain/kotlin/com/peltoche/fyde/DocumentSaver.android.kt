package com.peltoche.fyde

import android.net.Uri
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

/** The file being saved while the system's create-document prompt is open. */
private class PendingSave(val fileName: String, val bytes: ByteArray)

@Composable
actual fun rememberDocumentSaver(onResult: (fileName: String, saved: Boolean?) -> Unit): (fileName: String, bytes: ByteArray) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnResult by rememberUpdatedState(onResult)
    val pending = remember { arrayOfNulls<PendingSave>(1) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri: Uri? ->
        val save = pending[0] ?: return@rememberLauncherForActivityResult
        pending[0] = null
        if (uri == null) {
            latestOnResult(save.fileName, null)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    checkNotNull(context.contentResolver.openOutputStream(uri)) { "no output stream for $uri" }
                        .use { output -> output.write(save.bytes) }
                }.isSuccess
            }
            latestOnResult(save.fileName, saved)
        }
    }

    return remember {
        { fileName, bytes ->
            pending[0] = PendingSave(fileName, bytes)
            launcher.launch(fileName)
        }
    }
}
