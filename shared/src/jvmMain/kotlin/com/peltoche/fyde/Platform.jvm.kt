package com.peltoche.fyde

class JVMPlatform: Platform {
    override val name: String = "Desktop (${System.getProperty("os.name")})"
    override val isDesktop: Boolean = true
}

actual fun getPlatform(): Platform = JVMPlatform()
