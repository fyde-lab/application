package com.peltoche.fyde

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Backed by a hand-written JNI bridge (`../../../sdk/src/jni_android.rs`)
 * rather than the UniFFI bindings `:sdkBridge` generates for Desktop/iOS:
 * Gobley doesn't yet support the `com.android.kotlin.multiplatform.library`
 * plugin this module's android target uses. See `../../../CLAUDE.md` and
 * `../../../sdkBridgeAndroid/build.gradle.kts`, which builds and bundles
 * the native library this loads.
 */
actual object FydeSdk {
    init {
        System.loadLibrary("fyde_sdk")
    }

    /** Returns an error message on failure, or `null` on success. */
    @JvmStatic
    private external fun nativeConnect(url: String): String?

    actual suspend fun initialize(serverUrl: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            // nativeConnect blocks the calling thread on a Tokio
            // Client::connect call, so it must run off the main dispatcher.
            when (val error = nativeConnect(serverUrl)) {
                null -> Result.success(Unit)
                else -> Result.failure(RuntimeException(error))
            }
        }
}
