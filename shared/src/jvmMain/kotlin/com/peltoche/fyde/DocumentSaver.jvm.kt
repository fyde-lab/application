package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.document_download
import java.awt.FileDialog
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun rememberDocumentSaver(onResult: (fileName: String, saved: Boolean?) -> Unit): (fileName: String, bytes: ByteArray) -> Unit {
    val latestOnResult by rememberUpdatedState(onResult)
    val title by rememberUpdatedState(stringResource(Res.string.document_download))
    val window by rememberUpdatedState(LocalAppWindow.current)
    val scope = rememberCoroutineScope()
    return remember {
        { fileName, bytes ->
            val dialog = ownedFileDialog(window, title, FileDialog.SAVE)
            dialog.file = fileName
            dialog.isVisible = true
            val directory = dialog.directory
            val chosen = dialog.file
            if (directory == null || chosen == null) {
                latestOnResult(fileName, null)
            } else {
                scope.launch {
                    val saved = withContext(Dispatchers.IO) { runCatching { File(directory, chosen).writeBytes(bytes) }.isSuccess }
                    latestOnResult(fileName, saved)
                }
            }
        }
    }
}
