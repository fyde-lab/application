// Carries the fyde-sdk (../../sdk) native library for Android, built via a
// hand-written JNI bridge (../../sdk/src/jni_android.rs) instead of the
// UniFFI/Gobley wiring :sdkBridge uses for Desktop/iOS: Gobley 0.3.7 doesn't
// yet support the com.android.kotlin.multiplatform.library plugin that
// :shared's android target uses (see ../shared/build.gradle.kts).
//
// This is a plain (non-multiplatform) com.android.library module on
// purpose: Android's own docs recommend exactly this pattern for shipping
// prebuilt native libraries into a KMP android target, since the KMP
// android plugin has no jniLibs DSL of its own — see
// https://developer.android.com/kotlin/multiplatform/plugin. :shared
// depends on this module only from androidMain (see
// ../shared/build.gradle.kts), so it has no Kotlin/Java source of its own,
// only a generated jniLibs tree.
//
// Prerequisites, all machine-local (not managed by this repo): the Android
// NDK (configured below via android.ndkVersion, installed under
// $ANDROID_HOME/ndk/<version>), cargo-ndk (`cargo install cargo-ndk`), and
// rustup with the Android Rust targets (`rustup target add
// aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
// i686-linux-android`).
import java.util.Properties

plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.peltoche.fyde.sdkbridge"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = libs.versions.android.ndk.get()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

val rustJniLibsDir = layout.buildDirectory.dir("rustJniLibs")

// AGP resolves the SDK location from local.properties/ANDROID_HOME on its
// own, but the modern DSL (used here) doesn't expose it back as a plain
// `sdkDirectory` property, and it won't propagate ANDROID_HOME/
// ANDROID_NDK_HOME into the Exec task's process environment either -
// cargo-ndk needs one of those env vars (or a bare $ANDROID_HOME/ndk/* to
// scan) to find the NDK, so the same local.properties/env lookup AGP does is
// repeated here to set it explicitly below.
val androidSdkDir = providers.environmentVariable("ANDROID_HOME").orElse(
    providers.environmentVariable("ANDROID_SDK_ROOT")
).orElse(
    provider {
        Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }.getProperty("sdk.dir")
    }
).get()
val ndkDir = file(androidSdkDir).resolve("ndk/${android.ndkVersion}")

// `rustup run <toolchain> cargo ...` does NOT put the toolchain's own bin/
// ahead of the rest of PATH (it only prepends ~/.cargo/bin, for rustup's
// *shims* - and this machine has no cargo/rustc shims there, only cargo
// subcommand plugins like cargo-ndk). Left alone, cargo-ndk's nested `cargo
// build --target ...` calls resolve a bare "cargo"/"rustc" through PATH and
// silently pick up this repo's sibling Nix toolchain instead, which has no
// Android targets installed and fails with "can't find crate for `core`".
// `rustup which cargo` gives the toolchain's real bin/ dir, which is
// prepended explicitly below so every nested cargo/rustc invocation resolves
// there first.
val rustupToolchainBinDir = providers.exec {
    commandLine("rustup", "which", "cargo")
}.standardOutput.asText.map { file(it.trim()).parentFile.absolutePath }

val cargoNdkBuildFydeSdk = tasks.register<Exec>("cargoNdkBuildFydeSdk") {
    inputs.dir(layout.projectDirectory.dir("../../sdk/src"))
    inputs.file(layout.projectDirectory.dir("../../sdk/Cargo.toml"))
    outputs.dir(rustJniLibsDir)

    workingDir = layout.projectDirectory.dir("../../sdk").asFile
    environment("ANDROID_NDK_HOME", ndkDir.absolutePath)
    environment("PATH", "${rustupToolchainBinDir.get()}:${System.getenv("PATH")}")
    // cargo-ndk sets a bare (unprefixed) CC pointing at the NDK's clang for
    // the target it's cross-compiling. Some dependencies (e.g.
    // libsqlite3-sys, via sqlx's bundled sqlite) also run a *host* C
    // compile as part of their build script, and the `cc` crate falls back
    // to that same bare CC when HOST_CC isn't set - handing the NDK's
    // Android clang a host target triple, which fails since it has no host
    // glibc headers. Setting HOST_CC restores the host compiler for that path.
    environment("HOST_CC", "gcc")
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a",
        "-t", "x86_64",
        "-t", "armeabi-v7a",
        "-t", "x86",
        "-o", rustJniLibsDir.get().asFile.absolutePath,
        "build",
    )
}

android.sourceSets.getByName("main") {
    // A plain File, not the Provider<Directory> itself: AGP's classic
    // sourceSet API rejects Providers. This means Gradle can't infer the
    // task dependency automatically, so it's wired explicitly below instead.
    jniLibs.srcDir(rustJniLibsDir.get().asFile)
}

tasks.named("preBuild") {
    dependsOn(cargoNdkBuildFydeSdk)
}
