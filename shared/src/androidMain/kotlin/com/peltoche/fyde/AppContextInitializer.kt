package com.peltoche.fyde

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.startup.Initializer
import java.lang.ref.WeakReference
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

/**
 * The activity in the foreground, tracked from process startup, for what
 * must sit in a window ([AndroidWebviewEngine]'s WebViews). [onResumed]'s
 * callbacks run on the main thread each time one resumes.
 */
internal object ResumedActivity {
    private var current: WeakReference<Activity>? = null
    private val listeners = mutableListOf<(Activity) -> Unit>()

    /** Only read on the main thread. */
    val activity: Activity? get() = current?.get()

    fun onResumed(listener: (Activity) -> Unit) {
        synchronized(listeners) { listeners += listener }
    }

    internal fun track(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    current = WeakReference(activity)
                    synchronized(listeners) { listeners.toList() }.forEach { it(activity) }
                }

                override fun onActivityDestroyed(activity: Activity) {
                    if (current?.get() === activity) current = null
                }

                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            },
        )
    }
}

/** Registered as an `androidx.startup` initializer; runs before any app code. */
class AppContextInitializer : Initializer<Context> {
    override fun create(context: Context): Context {
        appContext = context.applicationContext
        ResumedActivity.track(appContext as Application)
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
