package com.peltoche.fyde.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/**
 * The single entry point application screens theme themselves with — wraps
 * [MaterialTheme] with the app's color scheme, typography, and shapes, and
 * provides the two things M3 has no slot for ([LocalAppExtendedColors],
 * [LocalAppDataTextStyle]). [AppSpacing] and [AppDimensions] aren't provided
 * here — they're plain objects, not part of the composition (see their own
 * doc comments for why).
 *
 * Defaults to the platform's own light/dark setting; `darkTheme` stays an
 * explicit parameter so a future in-app theme toggle only needs to pass a
 * different value here, not thread one through every call site.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(darkTheme) {
        if (darkTheme) appDarkColorScheme() else appLightColorScheme()
    }
    val bodyFontFamily = rememberBodyFontFamily()
    val dataFontFamily = rememberDataFontFamily()
    val typography = remember(bodyFontFamily) { appTypography(bodyFontFamily) }
    val dataTextStyle = remember(dataFontFamily) { appDataTextStyle(dataFontFamily) }
    val extendedColors = remember(darkTheme) { extendedColorsFor(darkTheme) }
    val shapes = remember { appShapes() }

    CompositionLocalProvider(
        LocalAppExtendedColors provides extendedColors,
        LocalAppDataTextStyle provides dataTextStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}
