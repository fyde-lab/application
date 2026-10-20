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
 * [theme] picks the palette (see [AppThemeOption], chosen in Settings);
 * `darkTheme` its light or dark half, defaulting to the platform's own
 * setting. Both stay explicit parameters so a change only needs to pass a
 * different value here, not thread one through every call site.
 */
@Composable
fun AppTheme(
    theme: AppThemeOption = AppThemeOption.Default,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(theme, darkTheme) { theme.colorScheme(darkTheme) }
    val bodyFontFamily = rememberBodyFontFamily()
    val dataFontFamily = rememberDataFontFamily()
    val typography = remember(bodyFontFamily) { appTypography(bodyFontFamily) }
    val dataTextStyle = remember(dataFontFamily) { appDataTextStyle(dataFontFamily) }
    val extendedColors = remember(theme, darkTheme) { theme.extendedColors(darkTheme) }
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
