package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.window.LocalUIViewController
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

/**
 * Not verified against a real build: this project's toolchain (see
 * `../../CLAUDE.md`) only compiles the iOS targets on a macOS host, which
 * this change was made without access to. Exercise this on a Mac/Xcode
 * before shipping.
 */
@Composable
actual fun rememberDocumentPicker(onPicked: (List<String>) -> Unit): () -> Unit {
    val viewController = LocalUIViewController.current
    val latestOnPicked by rememberUpdatedState(onPicked)

    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>,
            ) {
                val paths = didPickDocumentsAtURLs
                    .filterIsInstance<NSURL>()
                    .mapIndexedNotNull { index, url -> copyToTemporaryDirectory(url, index) }
                latestOnPicked(paths)
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                latestOnPicked(emptyList())
            }
        }
    }

    return remember {
        {
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeItem))
            picker.allowsMultipleSelection = true
            picker.delegate = delegate
            viewController.presentViewController(picker, animated = true, completion = null)
        }
    }
}

/**
 * [UIDocumentPickerViewController] hands back a security-scoped [NSURL] that
 * is often outside the app's sandbox, but [FydeSdk.uploadDocument] needs a
 * real filesystem path, so each picked file is copied into
 * `NSTemporaryDirectory` first, inside the required access-scope bracket.
 * Copied into its own `uniquifier` subdirectory (the file's index in the
 * current pick) so that two same-named files from different sources don't
 * clobber each other — the filename itself stays untouched, since
 * `fyde-sdk` derives the document's display name straight from it (see
 * `sdk/src/services/documents/service.rs`).
 */
private fun copyToTemporaryDirectory(url: NSURL, uniquifier: Int): String? {
    val name = url.lastPathComponent ?: return null
    if (!url.startAccessingSecurityScopedResource()) return null
    try {
        val directory = NSTemporaryDirectory() + "upload-$uniquifier/"
        NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null)
        val destination = NSURL.fileURLWithPath(directory + name)
        NSFileManager.defaultManager.removeItemAtURL(destination, null)
        return if (NSFileManager.defaultManager.copyItemAtURL(url, destination, null)) destination.path else null
    } finally {
        url.stopAccessingSecurityScopedResource()
    }
}
