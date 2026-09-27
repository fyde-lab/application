package com.peltoche.fyde

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Unlike Desktop (see `AppLocale.jvm.kt`), Android's Compose resolves its
 * language qualifier from `android.os.LocaleList.getDefault()` on API 24+
 * (see `AndroidLocaleDelegateAPI24`/`ResourceEnvironment.android.kt` in
 * `androidx.compose.ui.text.intl`/`org.jetbrains.compose.resources`), which
 * plain `java.util.Locale.setDefault()` does not touch - only the
 * AndroidX-recommended per-app-language API actually updates it (and, on
 * API 33+, updates the platform `LocaleManager` too, which the OS itself
 * then uses to recreate every activity with the new configuration; on
 * API 24-32 it's a best-effort AppCompat-managed backport that needs an
 * `AppCompatActivity` to apply the same way, which `MainActivity` isn't -
 * this device's own language preference still persists there, it just
 * won't retheme the already-running screen until the next cold start).
 */
actual fun applyPlatformLocale(languageCode: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
}
