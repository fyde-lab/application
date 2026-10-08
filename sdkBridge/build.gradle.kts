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

    // Without this, every separate Cargo invocation this plugin makes
    // (buildLibraryForBindings's host build to extract uniffi metadata, the
    // jvm/desktop target build, each Android ABI's NDK cross-compile) gets
    // its own `target/` directory under ../../sdk, each independently
    // rebuilding fyde-sdk's full native dependency graph (GTK/webkit2gtk/
    // wry, pulled in by its webview-backed scraper support) - which is what
    // exhausted CI runners' disk with "No space left on device" building
    // libfyde_sdk.a. Pointing every invocation at one shared target
    // directory lets them reuse each other's compiled dependency artifacts.
    targetDirectory = rootProject.layout.projectDirectory.dir(".cargo-target")
}

uniffi {
    // ch.ubique.uniffi.plugin >=1.3.0 already pins its default bindgen
    // install to the plugin's own release tag (previously it floated on
    // the bindgen repo's default branch), so no explicit bindgenFromGitTag
    // override is needed here - see the plugin's Constants.kt.
    generateFromLibrary()
}

// fyde-sdk's FFI (../../sdk/src/ffi.rs) exports almost entirely async fns
// (#[uniffi::export(async_runtime = "tokio")]), and every one of them fails
// to compile with ch.ubique.uniffi.plugin 1.3.1 (confirmed also on the
// unreleased git HEAD, with no version mismatch involved - both jvmMain and
// androidMain hit this identically):
//
//   Argument type mismatch: actual type is 'Any', but
//   'UniffiRustFutureContinuationCallback' was expected.
//
// Root cause: the generated `UniffiLib.ffi_*_rust_future_poll_*` methods
// type their `callback` parameter as the concrete
// `UniffiRustFutureContinuationCallback`, but the runtime's generic
// `uniffiRustCallAsync` (ch.ubique.uniffi:runtime's AsyncKt) hands that same
// value to the caller-supplied `pollFunc` lambda as erased `Any` - its type
// isn't parameterized over the callback type. The generated
// `{ future, callback, continuation -> ... }` lambda then passes that `Any`
// straight through, which doesn't type-check.
//
// This is a known upstream bug with a fix already written but not yet
// released as of v1.3.1 (see uniffi-kotlin-multiplatform-bindings'
// CHANGELOG.md, "[Unreleased]": "FFI callback parameters ... use their
// callback types instead of Any ... This includes the pollFunc of
// uniffiRustCallAsync") - and even that fix needs a matching runtime
// release, which doesn't exist yet either. Until both ship, patch the cast
// into the generated bindings ourselves.
val patchUniffiAsyncCallbacks by tasks.registering {
    dependsOn(tasks.named("buildBindings"))

    // Captured as Providers at configuration time: a `doLast` referencing
    // `layout`/`project` directly instead would capture the whole build
    // script object, which the configuration cache can't serialize.
    val bindingsRoot = layout.buildDirectory.dir("uniffi/bindings")
    val sourceSetNames = listOf("androidMain", "jvmMain")

    doLast {
        val pollLambda = Regex(
            """\{ future, callback, continuation -> (UniffiLib\.INSTANCE\.ffi_\w+)\(future, callback, continuation\) }""",
        )
        val root = bindingsRoot.get().asFile
        sourceSetNames.forEach { sourceSetName ->
            val bindingsDir = root.resolve("$sourceSetName/kotlin/uniffi/fyde_sdk")
            bindingsDir.listFiles { file -> file.extension == "kt" }?.forEach { file ->
                val original = file.readText()
                val patched = pollLambda.replace(original) { match ->
                    "{ future, callback, continuation -> ${match.groupValues[1]}(future, callback as UniffiRustFutureContinuationCallback, continuation) }"
                }
                if (patched != original) file.writeText(patched)
            }
        }
    }
}

tasks.matching { it.name.startsWith("compile") }.configureEach {
    dependsOn(patchUniffiAsyncCallbacks)
}
