import gobley.gradle.GobleyHost
import gobley.gradle.cargo.dsl.jvm

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.gobleyCargo)
    alias(libs.plugins.gobleyUniffi)
    alias(libs.plugins.kotlinAtomicfu)
}

// Deliberately jvm + iOS only, not android/js/wasmJs: Gobley 0.3.7 doesn't
// yet recognize the AGP 9 `com.android.kotlin.multiplatform.library` plugin
// used by :shared's android target (fails with "Android JVM targets are
// added, but Android Gradle Plugin is not found"), and Gobley doesn't
// support JS/Wasm targets at all yet. This module is kept separate from
// :shared so those targets are unaffected; see ../shared/build.gradle.kts
// and ../../CLAUDE.md.
//
// The iOS targets are further gated to macOS hosts only: Gobley's cargo
// plugin doesn't just skip cross-compiling to Apple targets when it can't
// (the way Kotlin's own klib/cinterop machinery does, with a warning) - it
// actively shells out `cargo rustc --target aarch64-apple-ios ... --print
// native-static-libs` during Gradle sync/configuration to probe link flags,
// which fails outright with exit code 101 on non-Apple hosts (no Xcode/iOS
// SDK/linker available - not something any Rust target or toolchain install
// can fix). Declaring these targets at all is what triggers that probe, so
// they're only declared when actually on macOS.
val isMacOsHost: Boolean = GobleyHost.current.platform == GobleyHost.Platform.MacOS

kotlin {
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
    // This app isn't published as a multiplatform JVM library (just run
    // locally), so only build/embed the Rust side for the host actually
    // running the build instead of cross-compiling for every desktop OS/arch.
    publishJvmArtifacts = false
    builds.jvm {
        embedRustLibrary = (rustTarget == GobleyHost.current.rustTarget)
    }
}

uniffi {
    generateFromLibrary {
        namespace = "fyde_sdk"
    }
}
