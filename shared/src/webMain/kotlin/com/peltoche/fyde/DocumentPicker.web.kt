package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** `fyde-sdk` isn't linked into the Web target (see `FydeSdk.web.kt`), so there's nothing to upload to. */
@Composable
actual fun rememberDocumentPicker(onPicked: (List<String>) -> Unit): () -> Unit {
    val latestOnPicked by rememberUpdatedState(onPicked)
    return remember { { latestOnPicked(emptyList()) } }
}
