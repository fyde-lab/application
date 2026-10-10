# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`fyde-application` is a Kotlin Multiplatform + Compose Multiplatform client targeting Android, iOS, Desktop (JVM), and Web (JS + Wasm). It is one of several sibling repos under `fyde/` — see the top-level `../CLAUDE.md` for how it relates to `server`, `sdk`, `cli`, and `api-protos`.

**Current state:** a working client on `../sdk` (via the `sdkBridge` UniFFI bindings, wrapped by `shared/.../FydeSdk.kt`'s `expect object` and its per-platform `actual`s; Web is stubbed, since `fyde-sdk` can't compile to wasm). Three top-level destinations share one navigation shell (`AppSidebar.kt`'s `TopLevelScaffold`): Documents (`DocumentListScreen.kt` — list + detail with Type/Period filters, typed preview, download), Add-ons (`AddonsScreen.kt`/`AddonDetailScreen.kt` — public add-ons plus the user's own scripts, install/edit/uninstall, New script), and Settings (language, logout); plus `LoginScreen`/`RegisterScreen`. `App.kt` owns every `FydeSdk` call and maps results into each screen's `*UiState`; screens stay presentation-only. Visual rules live in `design/design-system.md` (source of truth, tokens under `shared/.../theme/`, components under `shared/.../components/`).

## Commands

Build/run/test each target via the Gradle wrapper from this directory (`./gradlew ...`):

```sh
./gradlew :androidApp:assembleDebug                        # Android app
./gradlew :desktopApp:run                                  # Desktop app
./gradlew :desktopApp:hotRun --auto                         # Desktop app, hot reload
./gradlew :webApp:wasmJsBrowserDevelopmentRun                # Web app, Wasm target (faster, modern browsers)
./gradlew :webApp:jsBrowserDevelopmentRun                    # Web app, JS target (slower, older browsers)
```

iOS has no Gradle run task: open `iosApp/iosApp.xcodeproj` in Xcode and run from there. Xcode builds the `shared` module's static iOS framework (`Shared`, see `shared/build.gradle.kts`) as part of that build.

Tests are per-target, all against code in `shared/src/*Test`:

```sh
./gradlew :shared:testAndroidHostTest      # Android
./gradlew :shared:jvmTest                  # Desktop (JVM)
./gradlew :shared:wasmJsTest               # Web, Wasm target
./gradlew :shared:jsTest                   # Web, JS target
./gradlew :shared:iosSimulatorArm64Test    # iOS
```

Toolchain: JDK 21 (Gradle toolchain via the Foojay resolver plugin, see `gradle/gradle-daemon-jvm.properties`), Kotlin 2.4.20, Compose Multiplatform 1.12.0, AGP 9.1.1 (versions pinned in `gradle/libs.versions.toml`). This directory has its own `shell.nix`/`.envrc` (not wired to the repo-root one) providing `jdk21`, the Rust toolchain `sdkBridge` needs to cross-compile `../sdk`, and the native libs (X11/GL/fontconfig) desktop's Compose UI needs at runtime — load it (`nix-shell` or direnv) before running Gradle, since `JAVA_HOME` is not otherwise set.

## Architecture

Four platform entry-point modules wrap one shared module, wired together in `settings.gradle.kts`:

- `shared/` — all application code and UI, organized as standard KMP source sets: `commonMain` (target-independent Compose UI and logic) plus `androidMain`, `iosMain`, `jvmMain`, `jsMain`, `wasmJsMain` for platform-specific code, each with a matching test source set (`commonTest`, `androidHostTest`, `iosTest`, `jvmTest`, `webTest`). Cross-platform APIs that need a platform-specific implementation follow Kotlin's `expect`/`actual` pattern — see `Platform.kt` (expect) and `Platform.android.kt`/`Platform.ios.kt`/`Platform.jvm.kt`/`Platform.wasmJs.kt` (actual) for the template example. `shared`'s Android build uses its own namespace, `com.peltoche.fyde.shared`, distinct from the app's `com.peltoche.fyde`.
- `androidApp/` — thin Android application shell (`MainActivity.kt`) that hosts the shared Compose UI; the only module with an `applicationId`/manifest.
- `desktopApp/` — thin JVM application shell (`main.kt`, entry point `com.peltoche.fyde.MainKt`) using Compose Desktop; packages to Dmg/Msi/Deb via `compose.desktop.application.nativeDistributions`.
- `webApp/` — thin Kotlin/JS + Kotlin/Wasm browser shell (`webMain/kotlin/.../main.kt`, `webMain/resources/index.html`); both JS and Wasm binaries are built from the same `webMain` source set.
- `iosApp/` — native Xcode project (Swift, `iosApp.xcodeproj`), not a Gradle module. `iOSApp.swift`/`ContentView.swift` embed the `shared` module's compiled `Shared.framework` as the UI entry point.

All four app modules depend on `:shared` and add no business logic of their own beyond bootstrapping the shared `App()` composable for their platform.
