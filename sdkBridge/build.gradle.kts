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
kotlin {
    jvm()

    iosArm64()
    iosSimulatorArm64()
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
