package com.peltoche.fyde

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLocaleJvmTest {
    // Doesn't go through Res.string.*/getString(): resolving an actual
    // resource pulls in Compose Resources' getSystemEnvironment(), which
    // reaches into Skiko for dark-theme detection - unavailable headlessly
    // in some CI-like environments (throws LibraryLoadException there) and
    // orthogonal to what this test is actually about. What matters here -
    // that resolution is keyed off Locale.getDefault() - is confirmed by
    // reading Compose Multiplatform Resources' own source directly, see
    // AppLocale.jvm.kt's doc (DesktopPlatformLocale.desktop.kt/
    // ResourceEnvironment.desktop.kt both read it for the language
    // qualifier specifically); this test only needs to confirm
    // applyPlatformLocale itself does what it claims.
    @Test
    fun `applyPlatformLocale sets the JVM's default locale`() {
        val originalLocale = Locale.getDefault()
        try {
            applyPlatformLocale("fr")
            assertEquals("fr", Locale.getDefault().language)

            applyPlatformLocale("en")
            assertEquals("en", Locale.getDefault().language)
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
