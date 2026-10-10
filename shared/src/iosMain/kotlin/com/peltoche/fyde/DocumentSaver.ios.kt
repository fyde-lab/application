package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.window.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject

/**
 * Writes the file to `NSTemporaryDirectory` first, then hands that copy to
 * the system export picker, which lets the user pick where it goes (Files,
 * iCloud Drive, ...). Not verified against a real build, like
 * [rememberDocumentPicker]: the iOS targets only compile on a macOS host.
 */
@Composable
actual fun rememberDocumentSaver(onResult: (fileName: String, saved: Boolean?) -> Unit): (fileName: String, bytes: ByteArray) -> Unit {
    val viewController = LocalUIViewController.current
    val latestOnResult by rememberUpdatedState(onResult)
    val pendingName = remember { arrayOfNulls<String>(1) }

    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                pendingName[0]?.let { latestOnResult(it, true) }
                pendingName[0] = null
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                pendingName[0]?.let { latestOnResult(it, null) }
                pendingName[0] = null
            }
        }
    }

    return remember {
        { fileName, bytes ->
            val directory = NSTemporaryDirectory() + "export/"
            NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null)
            val url = NSURL.fileURLWithPath(directory + fileName)
            if (!bytes.toNSData().writeToURL(url, atomically = true)) {
                latestOnResult(fileName, false)
            } else {
                pendingName[0] = fileName
                val picker = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true)
                picker.delegate = delegate
                viewController.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.toULong())
}
