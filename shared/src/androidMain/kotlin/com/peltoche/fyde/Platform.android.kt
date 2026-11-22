package com.peltoche.fyde

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val hasSystemBack: Boolean = true
}

actual fun getPlatform(): Platform = AndroidPlatform()