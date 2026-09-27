package com.peltoche.fyde

import java.util.Locale

/**
 * Compose Multiplatform Resources' desktop target resolves its language
 * qualifier from `java.util.Locale.getDefault()` (both the composable path,
 * via `androidx.compose.ui.text.intl.Locale.current`, and the non-composable
 * `getSystemResourceEnvironment()` - see `DesktopPlatformLocale.desktop.kt`/
 * `ResourceEnvironment.desktop.kt` in `org.jetbrains.compose.ui`/
 * `.components.resources`), so changing the JVM's own default locale is
 * sufficient here, unlike Android (see `AppLocale.android.kt`).
 */
actual fun applyPlatformLocale(languageCode: String) {
    Locale.setDefault(Locale.forLanguageTag(languageCode))
}
