import ch.ubique.uniffi.plugin.model.BuildTarget.RustTarget

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

    sourceSets {
        androidMain.dependencies {
            // fyde-sdk loads libonnxruntime.so at runtime on Android rather
            // than linking it in: this ships it for every ABI. Only its native
            // library is used, never its Java API.
            runtimeOnly(libs.onnxruntime.android)
            // The Kotlin half of rustls-platform-verifier, which fyde-sdk's
            // HTTPS clients verify certificates through: Android's checks are
            // reached over JNI (rust/src/android.rs, called by shared's
            // PlatformVerifier). Its version is Cargo.lock's, set below.
            implementation(libs.rustls.platform.verifier)
        }
    }
}

// rustls-platform-verifier's Kotlin half must be the exact version of the
// `rustls-platform-verifier-android` crate in rust/Cargo.lock (the Rust half
// calls into it by name), so it's read from there rather than pinned twice.
// A ValueSource, so the configuration cache re-reads it when the lock changes.
abstract class RustlsPlatformVerifierVersion : ValueSource<String, RustlsPlatformVerifierVersion.Params> {
    interface Params : ValueSourceParameters {
        val lockFile: RegularFileProperty
    }

    override fun obtain(): String {
        val lines = parameters.lockFile.get().asFile.readLines()
        val nameIndex = lines.indexOfFirst { it.trim() == "name = \"rustls-platform-verifier-android\"" }
        return lines.drop(nameIndex + 1)
            .takeIf { nameIndex >= 0 }
            ?.firstOrNull { it.trimStart().startsWith("version = ") }
            ?.substringAfter('"')
            ?.substringBefore('"')
            ?.takeIf { it.isNotEmpty() }
            ?: error("rustls-platform-verifier-android not found in rust/Cargo.lock")
    }
}

val rustlsPlatformVerifierVersion = providers.of(RustlsPlatformVerifierVersion::class.java) {
    parameters.lockFile.set(layout.projectDirectory.file("rust/Cargo.lock"))
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.rustls" && requested.name == "rustls-platform-verifier") {
            useVersion(rustlsPlatformVerifierVersion.get())
            because("must match rustls-platform-verifier-android in rust/Cargo.lock")
        }
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

// In release builds (`-PreleaseBuild=true`) the plugin compiles the jvm()
// library for all five desktop platforms (macOS arm64/x64, Linux arm64/x64,
// Windows x64), a list it hard-codes, so a release build fails on any host
// that can't cross-compile to all of them (Linux to Apple, for one). Desktop
// packages are built per OS anyway (jpackage only targets its host), so build
// only the host's: the jar's merge step just leaves the skipped platforms out.
// Android's and iOS's targets aren't in that list and still build as before.
// (Locals only inside configureEach: an onlyIf capturing the script's own
// properties can't be stored in the configuration cache.)
tasks.withType<ch.ubique.uniffi.plugin.tasks.CargoBuildTask>().configureEach {
    val target = rustTarget
    val host = RustTarget.forCurrentPlatform
    val skipped = setOf(
        RustTarget.Aarch64AppleDarwin,
        RustTarget.X64AppleDarwin,
        RustTarget.Aarch64LinuxGnu,
        RustTarget.X64LinuxGnu,
        RustTarget.X64WindowsGnu,
    ) - host
    onlyIf("builds only this host's desktop library") { target.get() !in skipped }
}

// The plugin points Android's cargo builds at the NDK's API 21 clang wrappers
// (`armv7a-linux-androideabi21-clang`, …), a level it hard-codes whatever the
// android target's minSdk. Below API 24, 32-bit bionic doesn't declare
// fseeko/ftello once _FILE_OFFSET_BITS is 64, which Lua's lprefix.h sets, so
// mlua-sys (via fyde-sdk's scrapers) fails to compile for armv7. Build against
// the app's minSdk instead, rewriting the CC/CXX/linker wrappers the plugin set.
val androidMinSdk: String = libs.versions.android.minSdk.get()
tasks.withType<ch.ubique.uniffi.plugin.tasks.CargoBuildTask>().configureEach {
    val target = rustTarget.get()
    if (target.isAndroid) {
        val pluginWrapper = "${target.ndkLlvmTriple}21-clang"
        val minSdkWrapper = "${target.ndkLlvmTriple}$androidMinSdk-clang"
        val environment = additionalEnvironment.get()
        check(environment.values.any { pluginWrapper in it }) {
            "No $pluginWrapper in ${target.rustTriple}'s cargo environment: the uniffi plugin changed how it picks the NDK API level"
        }
        additionalEnvironment.set(environment.mapValues { (_, value) -> value.replace(pluginWrapper, minSdkWrapper) })
    }
}

// fyde-sdk's HTTP client, wreq (scrapers' fyde.http and the on-device models'
// downloads), vendors a patched BoringSSL (btls-sys) that links the C++
// runtime, which on Android is the NDK's shared one: libfyde_sdk_bridge.so
// needs libc++_shared.so, which the NDK ships but nothing puts in the APK, so
// the library failed to load on devices ("library libc++_shared.so not
// found"). Bundle it beside the bridge for every ABI the plugin builds it for.
abstract class BundleCxxSharedTask : DefaultTask() {
    /** The NDK sysroot's `usr/lib`, holding one directory per target triple. */
    @get:Internal
    abstract val sysrootLib: DirectoryProperty

    /** Android ABI → the NDK triple its libraries are under. */
    @get:Input
    abstract val triplesByAbi: MapProperty<String, String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val libraries: List<Provider<RegularFile>>
        get() = triplesByAbi.get().values.map { sysrootLib.file("$it/libc++_shared.so") }

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun bundle() {
        val out = outputDir.get().asFile
        out.deleteRecursively()
        triplesByAbi.get().forEach { (abi, triple) ->
            sysrootLib.file("$triple/libc++_shared.so").get().asFile
                .copyTo(out.resolve("$abi/libc++_shared.so"))
        }
    }
}

val androidComponents =
    extensions.getByType<com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension>()
val ndkHostTag: String = System.getProperty("os.name").let { os ->
    when {
        os.contains("Mac", ignoreCase = true) -> "darwin-x86_64"
        os.contains("Windows", ignoreCase = true) -> "windows-x86_64"
        else -> "linux-x86_64"
    }
}
val bundleCxxShared = tasks.register<BundleCxxSharedTask>("bundleCxxShared") {
    sysrootLib.set(
        androidComponents.sdkComponents.sdkDirectory.map {
            it.dir("ndk/${libs.versions.android.ndk.get()}/toolchains/llvm/prebuilt/$ndkHostTag/sysroot/usr/lib")
        },
    )
    // The ABIs libfyde_sdk_bridge.so is built for.
    triplesByAbi.set(
        mapOf(
            "arm64-v8a" to "aarch64-linux-android",
            "armeabi-v7a" to "arm-linux-androideabi",
            "x86_64" to "x86_64-linux-android",
        ),
    )
    outputDir.set(layout.buildDirectory.dir("generated/cxxShared/jniLibs"))
}
androidComponents.onVariants { variant ->
    variant.sources.jniLibs?.addGeneratedSourceDirectory(bundleCxxShared, BundleCxxSharedTask::outputDir)
}
