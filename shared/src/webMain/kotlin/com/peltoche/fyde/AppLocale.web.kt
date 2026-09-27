package com.peltoche.fyde

/**
 * No-op: `fyde-sdk` itself is unsupported on Web (see `FydeSdk.web.kt`), so
 * there's no persisted language preference to apply here yet either.
 */
actual fun applyPlatformLocale(languageCode: String) {
}
