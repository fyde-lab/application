package com.peltoche.fyde

class JVMPlatform: Platform {
    override val name: String = "Desktop (${System.getProperty("os.name")})"
}

actual fun getPlatform(): Platform = JVMPlatform()
