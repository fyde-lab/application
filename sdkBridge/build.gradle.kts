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

// fyde-sdk (../../sdk) is a sibling, independently-versioned Rust repo — not
// a Cargo workspace member — built here as a native library and exposed to
// Kotlin via UniFFI. See ../../sdk/CLAUDE.md and ../../CLAUDE.md.
cargo {
    packageDirectory = layout.projectDirectory.dir("../../sdk")
    ndkVersion = libs.versions.android.ndk.get()
}

uniffi {
    generateFromLibrary()
}

// Outside release builds (the plugin's `-PreleaseBuild=true`), fyde-sdk
// serves its tokio runtime's tasks to tokio-console (127.0.0.1:6669; forward
// it with `adb forward tcp:6669 tcp:6669` on Android). The SDK gates this on
// `--cfg tokio_unstable` rather than a Cargo feature because this plugin can't
// pass `--features` to the `cargo build` it runs, only environment variables.
// Every cargo task (bindings library included) gets the same RUSTFLAGS, so
// they don't keep invalidating each other's build cache.
val isReleaseBuild: Boolean = providers.gradleProperty("releaseBuild").orNull == "true"

if (!isReleaseBuild) {
    val rustFlags = listOfNotNull(System.getenv("RUSTFLAGS"), "--cfg tokio_unstable").joinToString(" ")
    tasks.withType<ch.ubique.uniffi.plugin.tasks.CargoBuildTask>().configureEach {
        additionalEnvironment.put("RUSTFLAGS", rustFlags)
    }
}
