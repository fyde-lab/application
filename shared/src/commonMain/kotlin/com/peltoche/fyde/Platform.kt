package com.peltoche.fyde

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform