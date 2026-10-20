package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog

@Composable
actual fun rememberDocumentPicker(onPicked: (List<String>) -> Unit): () -> Unit {
    val latestOnPicked by rememberUpdatedState(onPicked)
    val window by rememberUpdatedState(LocalAppWindow.current)
    return remember {
        {
            val dialog = ownedFileDialog(window, "Select documents to upload", FileDialog.LOAD)
            dialog.isMultipleMode = true
            dialog.isVisible = true
            latestOnPicked(dialog.files.map { it.absolutePath })
        }
    }
}
