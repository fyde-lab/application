package com.peltoche.fyde

import android.content.Context

/**
 * Hands `rustls-platform-verifier`, which fyde-sdk's HTTPS clients (the
 * scrapers' requests and the on-device models' downloads) verify certificates
 * through, the app context it needs to call Android's own certificate checks.
 * Without [initialize], the SDK's first HTTPS request panics.
 *
 * Its JNI symbol, `Java_com_peltoche_fyde_PlatformVerifier_initialize`, is
 * exported by `sdkBridge/rust/src/android.rs`, so this object's package and
 * name must stay as they are. The verifier's own Kotlin half
 * (`org.rustls:rustls-platform-verifier`) comes with `sdkBridge`.
 */
internal object PlatformVerifier {
    init {
        // JNI resolves external functions only in libraries loaded through
        // System.loadLibrary; JNA's own load of the bridge doesn't count.
        System.loadLibrary("fyde_sdk_bridge")
    }

    @JvmStatic
    external fun initialize(context: Context)
}
