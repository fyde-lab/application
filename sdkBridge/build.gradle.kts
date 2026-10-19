import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.tasks.CargoBuildTask
import java.io.ByteArrayOutputStream
import javax.inject.Inject

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

// fyde-sdk is an independently-versioned Rust repo, built here as a native
// library and exposed to Kotlin via UniFFI. It's built from its own checkout
// of the repo's master branch, never from the sibling ../../sdk working copy:
// that one carries whatever is being worked on in sdk, and building it here
// too made the two builds keep invalidating each other.
//
// Fetching runs in a ValueSource so it happens on every build, configuration
// cache hit or not: Gradle re-evaluates it to check the cache, and the
// commit it returns only invalidates the cache when master has moved. If the
// fetch fails (offline), the existing checkout is built as is.
abstract class SdkCheckout : ValueSource<String, SdkCheckout.Params> {
    interface Params : ValueSourceParameters {
        val url: Property<String>
        val branch: Property<String>
        val directory: DirectoryProperty
    }

    @get:Inject
    abstract val exec: ExecOperations

    private fun git(vararg args: String, ignoreFailure: Boolean = false): Pair<Boolean, String> {
        val out = ByteArrayOutputStream()
        val result = exec.exec {
            commandLine("git", "-C", parameters.directory.get().asFile.path, *args)
            standardOutput = out
            isIgnoreExitValue = ignoreFailure
        }
        return (result.exitValue == 0) to out.toString().trim()
    }

    override fun obtain(): String {
        val dir = parameters.directory.get().asFile
        if (!dir.resolve(".git").exists()) {
            dir.mkdirs()
            git("init", "--quiet")
            git("remote", "add", "origin", parameters.url.get())
        }
        val (fetched, _) = git("fetch", "--quiet", "--depth=1", "origin", parameters.branch.get(), ignoreFailure = true)
        if (fetched) {
            git("checkout", "--quiet", "--force", "--detach", "FETCH_HEAD")
        }
        val (hasHead, head) = git("rev-parse", "HEAD", ignoreFailure = true)
        check(hasHead) {
            "Could not fetch fyde-sdk's ${parameters.branch.get()} branch from ${parameters.url.get()} into $dir"
        }
        return head
    }
}

val sdkDirectory = rootProject.layout.buildDirectory.dir("sdk")
val sdkCommit = providers.of(SdkCheckout::class) {
    parameters.url = "https://github.com/fyde-lab/sdk.git"
    parameters.branch = "master"
    parameters.directory = sdkDirectory
}
logger.info("Building fyde-sdk at commit ${sdkCommit.get()}")

cargo {
    packageDirectory = sdkDirectory.get()
    ndkVersion = libs.versions.android.ndk.get()
}

uniffi {
    generateFromLibrary()
}

// The plugin builds fyde-sdk once for the host without --target (into
// target/debug) only to generate the bindings, then again with the host's
// --target triple for the JVM library: two full builds of the same crate in
// two cargo output directories. Building the bindings' library for the host
// triple too makes both the same cargo invocation, so whichever runs second
// is a no-op.
tasks.named<CargoBuildTask>("buildLibraryForBindings") {
    rustTarget.set(BuildTarget.RustTarget.forCurrentPlatform)
}
