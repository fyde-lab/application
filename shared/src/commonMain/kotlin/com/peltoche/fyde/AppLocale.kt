package com.peltoche.fyde

/**
 * Best-effort: switches this process's own locale to [languageCode] (e.g.
 * "en", "fr") so that Compose Multiplatform's resource resolution
 * (`values-<lang>`) picks it up. Compose Multiplatform Resources has no
 * public API to override the language it resolves against directly (its
 * `ComposeEnvironment`/`LocalComposeEnvironment` hooks are `internal` as of
 * the version pinned here) - it always derives the language from the
 * platform's own current locale instead, so this changes *that* instead,
 * platform by platform. Pair with forcing recomposition (see `App.kt`'s
 * `key(language)`): Compose caches the resolved resource environment per
 * composition and won't otherwise notice the platform locale changed
 * underneath it.
 */
expect fun applyPlatformLocale(languageCode: String)
