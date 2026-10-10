package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** `fyde-sdk` isn't linked into the Web target (see `FydeSdk.web.kt`), so there's never a document to save. */
@Composable
actual fun rememberDocumentSaver(onResult: (fileName: String, saved: Boolean?) -> Unit): (fileName: String, bytes: ByteArray) -> Unit {
    val latestOnResult by rememberUpdatedState(onResult)
    return remember { { fileName, _ -> latestOnResult(fileName, false) } }
}
