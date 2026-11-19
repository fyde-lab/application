package io.crates.keyring

import android.content.Context

/**
 * The JNI entry point of the `android-native-keyring-store` crate, fyde-sdk's
 * credential store on Android (keys kept in the Android Keystore). The crate
 * reaches Android through the `ndk-context` crate, which nothing initializes
 * in a UniFFI/JNA app: without [initializeNdkContext], the SDK's first use of
 * the store fails with "android context was not initialized".
 *
 * The package, class and companion are fixed by the crate's exported symbol
 * (`Java_io_crates_keyring_Keyring_00024Companion_initializeNdkContext`),
 * which `libfyde_sdk_bridge.so` carries since the crate is linked into it.
 */
class Keyring {
    companion object {
        init {
            // JNI resolves external functions only in libraries loaded through
            // System.loadLibrary; JNA's own load of the bridge doesn't count.
            System.loadLibrary("fyde_sdk_bridge")
        }

        external fun initializeNdkContext(context: Context)
    }
}
