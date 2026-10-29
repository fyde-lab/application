package com.peltoche.fyde.theme

import androidx.compose.material3.ColorScheme
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.theme_fyde
import org.jetbrains.compose.resources.StringResource

/**
 * The app's themes — each one a full palette,
 * light and dark, that [AppTheme] applies (the light/dark half still follows
 * the platform; a theme only decides *which* palette). Typography, shapes
 * and spacing are shared by every theme: design-system.md fixes them for the
 * whole app.
 *
 * Adding a theme means adding an entry here with its own two [ColorScheme]s
 * and [AppExtendedColors], built the way [appLightColorScheme] and
 * [appDarkColorScheme] are, plus a `theme_<name>` string in both resource
 * sets. Nothing else changes: screens read colors only through [AppColors].
 *
 * [id] is the stable key a saved choice is stored under — never change one
 * once shipped; [label] is the name a picker would show. Settings offers no
 * picker while [Fyde] is the only theme: one choice isn't a choice.
 */
enum class AppThemeOption(val id: String, val label: StringResource) {
    /** design-system.md's palette: warm neutral grays with a teal accent. */
    Fyde("fyde", Res.string.theme_fyde) {
        override fun colorScheme(darkTheme: Boolean): ColorScheme =
            if (darkTheme) appDarkColorScheme() else appLightColorScheme()

        override fun extendedColors(darkTheme: Boolean): AppExtendedColors = extendedColorsFor(darkTheme)
    },
    ;

    /** The Material 3 roles of this theme, for the platform's light or dark mode. */
    abstract fun colorScheme(darkTheme: Boolean): ColorScheme

    /** The roles Material 3 has no slot for (see [AppExtendedColors]). */
    abstract fun extendedColors(darkTheme: Boolean): AppExtendedColors

    companion object {
        /** The theme used until the user picks another one. */
        val Default = Fyde

        /** The theme saved as [id], or [Default] for an unknown or missing one. */
        fun fromId(id: String?): AppThemeOption = entries.find { it.id == id } ?: Default
    }
}
