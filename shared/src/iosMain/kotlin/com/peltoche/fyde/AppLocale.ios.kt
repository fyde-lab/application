package com.peltoche.fyde

import platform.Foundation.NSUserDefaults

/**
 * Unlike Desktop/Android (see `AppLocale.jvm.kt`/`AppLocale.android.kt`),
 * iOS/Foundation only reads the preferred-language list ("AppleLanguages")
 * once, at process startup, to pick `NSBundle`'s localization - there is no
 * supported way for an app to change its own already-running process's
 * locale, or to relaunch itself. So this persists the preference for
 * Foundation to pick up on the *next* launch; `App.kt`'s `key(language)`
 * recomposition still fires here today but has nothing to change yet,
 * since `Locale.current` on iOS doesn't re-read this until then.
 */
actual fun applyPlatformLocale(languageCode: String) {
    NSUserDefaults.standardUserDefaults.setObject(listOf(languageCode), forKey = "AppleLanguages")
}
