package com.peltoche.fyde

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Fyde",
        icon = fydeLogoPainter(),
    ) {
        CompositionLocalProvider(LocalAppWindow provides window) {
            App()
        }
    }
}