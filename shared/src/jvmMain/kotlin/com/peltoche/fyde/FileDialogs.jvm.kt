package com.peltoche.fyde

import androidx.compose.runtime.staticCompositionLocalOf
import java.awt.Dialog
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Window

/** The app's window, provided by `desktopApp`'s `main`, to own the file dialogs. */
val LocalAppWindow = staticCompositionLocalOf<Window?> { null }

/**
 * A [FileDialog] owned by [owner], the app's window ([LocalAppWindow]). Found
 * via testing on sway (Wayland, through XWayland): a dialog with no owner is
 * never shown there, while `setVisible(true)` still blocks until it closes,
 * so Upload and Download looked like they did nothing. An owned dialog is
 * transient for the app's window, which the compositor floats over it.
 */
internal fun ownedFileDialog(owner: Window?, title: String, mode: Int): FileDialog = when (owner) {
    is Frame -> FileDialog(owner, title, mode)
    is Dialog -> FileDialog(owner, title, mode)
    else -> FileDialog(null as Frame?, title, mode)
}
