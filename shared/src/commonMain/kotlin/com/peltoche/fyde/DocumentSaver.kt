package com.peltoche.fyde

import androidx.compose.runtime.Composable

/**
 * Returns a launcher that asks the user where to save a file — the
 * platform's native save dialog (desktop), Storage Access Framework create
 * prompt (Android), or export picker (iOS) — pre-filled with `fileName`,
 * then writes `bytes` there. [onResult] gets that same `fileName` and
 * `true` once written, `false` if writing failed, or `null` if the user
 * cancelled. The counterpart of [rememberDocumentPicker], used to download
 * a document's already-decrypted content out of the app.
 */
@Composable
expect fun rememberDocumentSaver(onResult: (fileName: String, saved: Boolean?) -> Unit): (fileName: String, bytes: ByteArray) -> Unit
