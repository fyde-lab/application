import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// :sdkBridge only declares its iOS targets on macOS hosts (see
// sdkBridge/build.gradle.kts), since its cargo plugin needs Xcode/an iOS SDK
// to probe link flags. iosMain here depends on :sdkBridge, so declaring
// these targets unconditionally breaks KMP dependency resolution on
// non-macOS hosts ("Unresolved platforms: [iosArm64, iosSimulatorArm64]") —
// gate them the same way to keep the two modules' target sets in sync.
val isMacOsHost: Boolean = System.getProperty("os.name").contains("Mac", ignoreCase = true)

compose.resources {
    packageOfResClass = "com.peltoche.fyde.resources"
}

kotlin {
    if (isMacOsHost) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "Shared"
                isStatic = true
            }
        }
    }

    jvm()
    
    js {
        browser()
        binaries.executable()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    android {
       namespace = "com.peltoche.fyde.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(project(":sdkBridge"))
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
        // :sdkBridge wraps fyde-sdk (../../sdk) via UniFFI and targets
        // jvm/android/iOS (wired per-target below, including androidMain
        // above) but not js/wasmJs — see sdkBridge/build.gradle.kts. Kept
        // out of commonMain so js/wasmJs compilations of :shared, which have
        // no matching target in :sdkBridge, don't break; FydeSdk's
        // expect/actual in commonMain/FydeSdk.kt is the cross-platform seam
        // instead.
        jvmMain.dependencies {
            implementation(project(":sdkBridge"))
        }
        if (isMacOsHost) {
            iosMain.dependencies {
                implementation(project(":sdkBridge"))
            }
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}