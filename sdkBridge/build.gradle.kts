plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.uniffiKotlinMultiplatform)
}

// Unlike Gobley (0.3.7, previously used here), ch.ubique.uniffi.plugin is
// built directly against com.android.kotlin.multiplatform.library, so the
// android target declared below is recognized without any workaround. See
// ../../CLAUDE.md and ../shared/build.gradle.kts, whose android target uses
// the same plugin. This is also why ../sdkBridgeAndroid (a hand-written JNI
// bridge that worked around Gobley's gap) and its NDK/cargo-ndk-based build
// no longer exist: Android now goes through this module's UniFFI bindings
// like Desktop/iOS already did.
//
// iOS targets are still gated to macOS hosts only, carried over from the
// Gobley setup: its cargo plugin actively shelled out `cargo rustc --target
// aarch64-apple-ios ... --print native-static-libs` during Gradle
// sync/configuration to probe link flags, which failed outright on
// non-Apple hosts (no Xcode/iOS SDK/linker available). This plugin's cargo
// integration hasn't been confirmed to behave differently, so the same
// guard is kept defensively until verified on a Linux host/CI.
val isMacOsHost: Boolean = System.getProperty("os.name").contains("Mac", ignoreCase = true)

kotlin {
    android {
        namespace = "com.peltoche.fyde.sdkbridge"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    jvm()

    if (isMacOsHost) {
        iosArm64()
        iosSimulatorArm64()
    }
}

// rust/ (fyde-sdk-bridge) is this app's own UniFFI layer over fyde-sdk
// (../../sdk, a sibling, independently-versioned Rust repo it depends on by
// path — not a Cargo workspace member), built here as a native library and
// exposed to Kotlin via UniFFI. See ../CLAUDE.md and ../../sdk/CLAUDE.md.
cargo {
    packageDirectory = layout.projectDirectory.dir("rust")
    ndkVersion = libs.versions.android.ndk.get()
}

uniffi {
    // Left unset, the plugin installs uniffi-bindgen-kotlin-multiplatform from
    // its repository's default branch, so each fresh checkout generated
    // whatever main produced that day — which stopped compiling against the
    // plugin's own runtime. Pin it to the plugin's release.
    bindgenFromGitTag(
        "https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings.git",
        "v${libs.versions.uniffiKotlinMultiplatform.get()}",
    )
    generateFromLibrary()
}
