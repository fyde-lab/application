package com.peltoche.fyde

interface Platform {
    val name: String

    /**
     * Whether this is the desktop app, whose reference screens (the `D*`
     * ones) word some things for a computer rather than a phone.
     */
    val isDesktop: Boolean get() = false

    /**
     * Whether the OS has a back gesture (Android's system back, iOS's swipe
     * from the screen's edge) that `SystemBackHandler` should answer.
     */
    val hasSystemBack: Boolean get() = false

    /** Whether shortcuts take ⌘ (macOS), as in D2b's "⌘⌫", rather than a key of their own. */
    val usesCommandKey: Boolean get() = false
}

expect fun getPlatform(): Platform