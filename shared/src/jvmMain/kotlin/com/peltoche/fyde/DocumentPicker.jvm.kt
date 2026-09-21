package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame

@Composable
actual fun rememberDocumentPicker(onPicked: (List<String>) -> Unit): () -> Unit {
    val latestOnPicked by rememberUpdatedState(onPicked)
    return remember {
        {
            val dialog = FileDialog(null as Frame?, "Select documents to upload", FileDialog.LOAD)
            dialog.isMultipleMode = true
            dialog.isVisible = true
            latestOnPicked(dialog.files.map { it.absolutePath })
        }
    }
}
