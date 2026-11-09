package com.peltoche.fyde

class JVMPlatform: Platform {
    override val name: String = "Desktop (${System.getProperty("os.name")})"
    override val isDesktop: Boolean = true
    override val usesCommandKey: Boolean = System.getProperty("os.name").startsWith("Mac")
}

actual fun getPlatform(): Platform = JVMPlatform()
