//! Android-only JNI entry points, called from the app's Kotlin side outside
//! UniFFI (whose bindings go through JNA and never hand Rust a `JNIEnv`).

use jni::EnvUnowned;
use jni::errors::ThrowRuntimeExAndDefault;
use jni::jni_mangle;
use jni::objects::{JClass, JObject};

/// Lets `rustls-platform-verifier` reach Android's own certificate checks,
/// which live on the JVM side: fyde-sdk's HTTPS clients (`reqwest`, for the
/// scrapers' `fyde.http` and the on-device models' downloads) verify servers
/// through it, and it panics on first use without this. Called once at
/// startup by `PlatformVerifier.initialize` (`shared`'s androidMain, from
/// `AppContextInitializer`) with the application context; a failure is
/// thrown back to Kotlin as a `RuntimeException`.
///
/// The verifier's Kotlin half, `org.rustls:rustls-platform-verifier`, must be
/// in the APK at the version of `rustls-platform-verifier-android` in
/// `Cargo.lock` (`sdkBridge/build.gradle.kts` reads it from there).
#[jni_mangle("com.peltoche.fyde.PlatformVerifier")]
pub fn initialize<'caller>(
    mut unowned_env: EnvUnowned<'caller>,
    _class: JClass<'caller>,
    context: JObject<'caller>,
) {
    unowned_env
        .with_env(|env| rustls_platform_verifier::android::init_with_env(env, context))
        .resolve::<ThrowRuntimeExAndDefault>();
}
