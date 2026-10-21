package com.peltoche.fyde

interface Platform {
    val name: String

    /**
     * Whether this is the desktop app, whose reference screens (the `D*`
     * ones) word some things for a computer rather than a phone.
     */
    val isDesktop: Boolean get() = false
}

expect fun getPlatform(): Platform