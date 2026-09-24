package com.peltoche.fyde

import android.content.Context
import androidx.startup.Initializer

/**
 * The application [Context], captured automatically at process startup by
 * [AppContextInitializer] via `androidx.startup` (see
 * `shared/src/androidMain/AndroidManifest.xml`) so that [FydeSdk] can reach
 * Android's app-private storage directory without every caller having to
 * thread a `Context` through `FydeSdk.initialize`.
 */
internal lateinit var appContext: Context
    private set

/** Registered as an `androidx.startup` initializer; runs before any app code. */
class AppContextInitializer : Initializer<Context> {
    override fun create(context: Context): Context {
        appContext = context.applicationContext
        return appContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
