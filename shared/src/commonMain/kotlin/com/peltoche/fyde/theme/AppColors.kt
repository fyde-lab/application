package com.peltoche.fyde.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * The handful of roles `design/design-system.md` § Color names that Material
 * 3's [ColorScheme] has no slot for at all (`success`, `warning`, the two
 * `selection` roles). Everything else in the spec maps onto a real M3 role —
 * see [appLightColorScheme]/[appDarkColorScheme] — and is read through
 * `MaterialTheme.colorScheme`, not duplicated here.
 */
data class AppExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val selectionSurface: Color,
    val selectionTextBackground: Color,
)

private val lightExtendedColors = AppExtendedColors(
    success = Color(0xFF2E7D46),
    onSuccess = Color(0xFFFFFFFF),
    warning = Color(0xFF95610A),
    onWarning = Color(0xFFFFFFFF),
    selectionSurface = Color(0xFFE7EFEF),
    selectionTextBackground = Color(0xFFBFE0E1),
)

private val darkExtendedColors = AppExtendedColors(
    success = Color(0xFF7FD98A),
    onSuccess = Color(0xFF0F2A17),
    warning = Color(0xFFE8B34D),
    onWarning = Color(0xFF2B1C02),
    selectionSurface = Color(0xFF1F3638),
    selectionTextBackground = Color(0xFF2C5457),
)

/**
 * Changes only when the theme itself switches (light/dark) — rare and
 * coarse-grained — so `staticCompositionLocalOf` is the right call here,
 * unlike M3's own `LocalColorScheme` which is read far more granularly.
 * [AppTheme] is the only place this is provided; the default here only
 * covers `@Preview`s or other content composed without it.
 */
val LocalAppExtendedColors = staticCompositionLocalOf { lightExtendedColors }

/**
 * Derives a muted "container" tone from an accent color for M3 roles the
 * spec doesn't name directly (`primaryContainer`, etc.) but that stock M3
 * components (chips, switches) do read. Blending toward the theme's own
 * background — rather than pure white/black — keeps containers inside the
 * same warm-neutral family as everything else instead of introducing a new,
 * uncalibrated tone.
 */
private fun containerOf(accent: Color, background: Color, towardBackground: Float): Color =
    lerp(accent, background, towardBackground)

/** Builds the light M3 [ColorScheme] from design-system.md's Color table. */
fun appLightColorScheme(): ColorScheme {
    val background = Color(0xFFF5F4F1)
    val surface = Color(0xFFFFFFFF)
    val text = Color(0xFF211E18)
    val primary = Color(0xFF1B5E63)
    val onPrimary = Color(0xFFFFFFFF)
    val secondary = Color(0xFF6B5636)
    val onSecondary = Color(0xFFFFFFFF)
    val error = Color(0xFFB3261E)
    val onError = Color(0xFFFFFFFF)

    return lightColorScheme(
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = Color(0xFFEDEBE6),
        onSurfaceVariant = Color(0xFF6B6558),
        surfaceContainerLowest = surface,
        surfaceContainerLow = Color(0xFFF9F8F5),
        // What stock menus (DropdownMenu, ExposedDropdownMenu) fill
        // themselves with — `surface-elevated`, like every other floating
        // surface. Left unset, it fell back to M3's baseline lavender.
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = Color(0xFFE7E4DE),
        surfaceBright = surface,
        surfaceDim = Color(0xFFE7E4DE),
        // M3 tints any `surface`-colored container with this at its tonal
        // elevation (dialogs, menus, the bottom bar). design-system.md §
        // Elevation steps tone explicitly (surface -> surface-elevated) and
        // uses shadows for floating things, so the tint is the surface
        // itself: a no-op. Left at its default (primary), light-mode dialogs
        // and menus were washed teal.
        surfaceTint = surface,
        inverseSurface = text,
        inverseOnSurface = background,
        inversePrimary = Color(0xFF5FBFC4),
        // Darkened from an earlier #C9C4B8 - measured ~1.5:1 against this
        // background, well under WCAG's 3:1 minimum for a UI component
        // boundary (visually confirmed too: the search field's border was
        // essentially invisible). #8F8A7C clears ~3.1:1 here and ~3.5:1
        // against surface.
        outline = Color(0xFF8F8A7C),
        outlineVariant = Color(0xFFE3E0DA),
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = containerOf(primary, background, 0.8f),
        onPrimaryContainer = text,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = containerOf(secondary, background, 0.8f),
        onSecondaryContainer = text,
        // No distinct tertiary hue at v1 — aliased to primary, same precedent
        // the current (pre-implementation) DESIGN.md already set.
        tertiary = primary,
        onTertiary = onPrimary,
        tertiaryContainer = containerOf(primary, background, 0.8f),
        onTertiaryContainer = text,
        error = error,
        onError = onError,
        errorContainer = containerOf(error, background, 0.8f),
        onErrorContainer = text,
    )
}

/** Builds the dark M3 [ColorScheme] from design-system.md's Color table. */
fun appDarkColorScheme(): ColorScheme {
    val background = Color(0xFF16140F)
    val surface = Color(0xFF1E1B16)
    val surfaceContainerHigh = Color(0xFF282420)
    val text = Color(0xFFEDE9E1)
    val primary = Color(0xFF5FBFC4)
    val onPrimary = Color(0xFF06232A)
    val secondary = Color(0xFFB99569)
    val onSecondary = Color(0xFF2B1B08)
    val error = Color(0xFFF2938A)
    val onError = Color(0xFF3A0F0B)

    return darkColorScheme(
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surfaceContainerHigh,
        onSurfaceVariant = Color(0xFF9B9484),
        surfaceContainerLowest = Color(0xFF110F0B),
        surfaceContainerLow = Color(0xFF1A1712),
        // Same as the light theme: menus are `surface-elevated`.
        surfaceContainer = surfaceContainerHigh,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = Color(0xFF332D25),
        surfaceBright = Color(0xFF332D25),
        surfaceDim = background,
        // Same as the light theme: tone steps are explicit, not M3's tint.
        surfaceTint = surface,
        inverseSurface = text,
        inverseOnSurface = background,
        inversePrimary = Color(0xFF1B5E63),
        // Same fix as the light theme's outline: #4A4438 measured ~1.9:1
        // against this background. #7A7364 clears ~3.9:1.
        outline = Color(0xFF7A7364),
        outlineVariant = Color(0xFF35302A),
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = containerOf(primary, background, 0.65f),
        onPrimaryContainer = text,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = containerOf(secondary, background, 0.65f),
        onSecondaryContainer = text,
        tertiary = primary,
        onTertiary = onPrimary,
        tertiaryContainer = containerOf(primary, background, 0.65f),
        onTertiaryContainer = text,
        error = error,
        onError = onError,
        errorContainer = containerOf(error, background, 0.65f),
        onErrorContainer = text,
    )
}

/**
 * The single API application screens use for color — a facade over both
 * `MaterialTheme.colorScheme` (everything design-system.md maps onto a real
 * M3 role) and [LocalAppExtendedColors] (the handful of roles that need it),
 * so call sites never need to know which of the two backs a given token.
 */
object AppColors {
    val background: Color @Composable get() = MaterialTheme.colorScheme.background
    val surface: Color @Composable get() = MaterialTheme.colorScheme.surface
    val surfaceElevated: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val border: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val borderStrong: Color @Composable get() = MaterialTheme.colorScheme.outline
    val text: Color @Composable get() = MaterialTheme.colorScheme.onBackground
    val textMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val primary: Color @Composable get() = MaterialTheme.colorScheme.primary
    val onPrimary: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
    val secondary: Color @Composable get() = MaterialTheme.colorScheme.secondary
    val onSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSecondary
    val error: Color @Composable get() = MaterialTheme.colorScheme.error
    val onError: Color @Composable get() = MaterialTheme.colorScheme.onError
    val warning: Color @Composable get() = LocalAppExtendedColors.current.warning
    val onWarning: Color @Composable get() = LocalAppExtendedColors.current.onWarning
    val success: Color @Composable get() = LocalAppExtendedColors.current.success
    val onSuccess: Color @Composable get() = LocalAppExtendedColors.current.onSuccess
    val selectionSurface: Color @Composable get() = LocalAppExtendedColors.current.selectionSurface
    val selectionTextBackground: Color @Composable get() = LocalAppExtendedColors.current.selectionTextBackground

    /**
     * design-system.md § Color — a tinted container: [accent] at one low
     * alpha, for a small fill whose color carries meaning (a badge, a
     * warning banner, a file-kind tile, the error state's circle). One
     * value everywhere — found via review: the same idea was drawn at 10%,
     * 14% and 15% depending on the file.
     */
    fun tintOf(accent: Color): Color = accent.copy(alpha = TINT_ALPHA)
}

private const val TINT_ALPHA = 0.15f

internal fun extendedColorsFor(darkTheme: Boolean): AppExtendedColors =
    if (darkTheme) darkExtendedColors else lightExtendedColors
