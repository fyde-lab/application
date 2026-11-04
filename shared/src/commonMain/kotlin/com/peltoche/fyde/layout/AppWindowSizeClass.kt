package com.peltoche.fyde.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * design-system.md § Responsive behavior's three breakpoints, matching
 * Compose Multiplatform's own `WindowSizeClass` width boundaries (600dp,
 * 840dp) without depending on that library directly — this app only ever
 * needs the width class, and a plain enum keeps every layout primitive
 * usable without pulling in an adaptive/window-size-class artifact whose
 * KMP target coverage isn't guaranteed at this Compose Multiplatform
 * version.
 */
enum class AppWindowSizeClass { Compact, Medium, Expanded }

private val mediumBreakpoint = 600.dp
private val expandedBreakpoint = 840.dp

/** Classifies by *actual available width*, never by platform or device type. */
fun appWindowSizeClassOf(width: Dp): AppWindowSizeClass = when {
    width < mediumBreakpoint -> AppWindowSizeClass.Compact
    width < expandedBreakpoint -> AppWindowSizeClass.Medium
    else -> AppWindowSizeClass.Expanded
}

/**
 * [AppWindowSizeClassProvider] is the only place this is given a real value
 * tied to the actual window. The `Compact` fallback here only covers
 * content composed without it (a `@Preview`, or a catalogue frame that
 * deliberately overrides `windowSizeClass` on the primitives directly).
 */
val LocalAppWindowSizeClass = staticCompositionLocalOf { AppWindowSizeClass.Compact }

/**
 * Classifies the window's width once, near the root of the app, and
 * provides the resulting [AppWindowSizeClass] to everything below — the
 * "use the available window size, not the device type" strategy the design
 * system is built on. Read from the window rather than measured with
 * `BoxWithConstraints`, so a class change recomposes before layout: one
 * applied during layout disposes open popups mid-pass, which crashes the
 * desktop renderer ("RootNodeOwner is already disposed").
 */
@Composable
fun AppWindowSizeClassProvider(content: @Composable () -> Unit) {
    val width = LocalWindowInfo.current.containerDpSize.width
    CompositionLocalProvider(LocalAppWindowSizeClass provides appWindowSizeClassOf(width)) {
        Box(modifier = Modifier.fillMaxSize()) { content() }
    }
}
