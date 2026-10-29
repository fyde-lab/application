package com.peltoche.fyde.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.appearance_dark
import com.peltoche.fyde.resources.appearance_light
import com.peltoche.fyde.resources.appearance_system
import org.jetbrains.compose.resources.StringResource

/**
 * Whether the app draws the light or the dark half of the picked
 * [AppThemeOption] — picked in Settings' Apparence et langue (M7). [System] follows
 * the platform's own setting (the default); [Light] and [Dark] override it,
 * e.g. on a desktop whose platform reports no dark mode at all.
 *
 * [id] is the stable key a saved choice is stored under — never change one
 * once shipped; [label] is the name shown in the Settings picker.
 */
enum class AppAppearance(val id: String, val label: StringResource) {
    System("system", Res.string.appearance_system),
    Light("light", Res.string.appearance_light),
    Dark("dark", Res.string.appearance_dark),
    ;

    /** Whether this appearance draws the dark palette, as [AppTheme]'s `darkTheme` expects. */
    @Composable
    fun isDark(): Boolean = when (this) {
        System -> isSystemInDarkTheme()
        Light -> false
        Dark -> true
    }

    companion object {
        /** The appearance used until the user picks another one. */
        val Default = System

        /** The appearance saved as [id], or [Default] for an unknown or missing one. */
        fun fromId(id: String?): AppAppearance = entries.find { it.id == id } ?: Default
    }
}
