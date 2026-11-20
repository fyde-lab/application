package com.peltoche.fyde

import android.content.Context
import androidx.startup.Initializer
import io.crates.keyring.Keyring

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
        // fyde-sdk's Android credential store needs the app context on the
        // Rust side before the SDK is first used (see Keyring).
        Keyring.initializeNdkContext(appContext)
        // Its HTTPS certificate checks call Android's through the JVM, which
        // they need the app context for (see PlatformVerifier).
        PlatformVerifier.initialize(appContext)
        return appContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
