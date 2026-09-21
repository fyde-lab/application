package com.peltoche.fyde

import androidx.compose.runtime.Composable

/**
 * Returns a launcher that opens the platform's native file picker (allowing
 * one or more files to be selected at once) and invokes [onPicked] with the
 * local absolute paths to the chosen files, or an empty list if the user
 * cancelled. [FydeSdk.uploadDocument] needs a real filesystem path, so
 * platforms whose native picker only hands back an opaque handle (Android's
 * `content://` Uri, iOS's security-scoped URL) copy each file into local
 * storage before returning its path. Composable because Android's
 * implementation must register its `ActivityResult` contract during
 * composition, before the returned launcher can be invoked.
 */
@Composable
expect fun rememberDocumentPicker(onPicked: (List<String>) -> Unit): () -> Unit
