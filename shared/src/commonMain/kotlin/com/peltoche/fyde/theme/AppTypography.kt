package com.peltoche.fyde.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.plex_mono_regular
import com.peltoche.fyde.resources.public_sans_medium
import com.peltoche.fyde.resources.public_sans_regular
import com.peltoche.fyde.resources.public_sans_semibold
import org.jetbrains.compose.resources.Font

/**
 * Public Sans, bundled — see `design/current/design-system.md` § Typography and
 * `shared/THIRD_PARTY_LICENSES.md`. Bundling (rather than each platform's
 * system font) is what makes text measure identically on Android, iOS,
 * desktop, and web.
 */
@Composable
internal fun rememberBodyFontFamily(): FontFamily {
    val regular = Font(Res.font.public_sans_regular, FontWeight.Normal)
    val medium = Font(Res.font.public_sans_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.public_sans_semibold, FontWeight.SemiBold)
    return remember(regular, medium, semiBold) { FontFamily(regular, medium, semiBold) }
}

/** IBM Plex Mono, bundled — used narrowly for the Data role (tabular figures). */
@Composable
internal fun rememberDataFontFamily(): FontFamily {
    val regular = Font(Res.font.plex_mono_regular, FontWeight.Normal)
    return remember(regular) { FontFamily(regular) }
}

/**
 * The full M3 [Typography] — all 15 slots, so stock M3 components that read
 * a slot no design-system.md role explicitly targets (e.g. `labelMedium`)
 * still get our bundled font rather than silently falling back to a system
 * default. The 7 slots the spec does name are fully retuned (size, line
 * height, weight, tracking); the rest keep M3's own baseline proportions
 * with only the font family swapped.
 */
internal fun appTypography(bodyFontFamily: FontFamily): Typography {
    val base = Typography()
    fun rebased(style: TextStyle) = style.copy(fontFamily = bodyFontFamily)

    return Typography(
        // Pass amount role — a document pass's headline value.
        displayLarge = TextStyle(fontFamily = bodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.88).sp),
        displayMedium = rebased(base.displayMedium),
        displaySmall = rebased(base.displaySmall),
        // Display role — rare, empty-state/onboarding headlines only.
        headlineLarge = TextStyle(fontFamily = bodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp),
        headlineMedium = rebased(base.headlineMedium),
        headlineSmall = rebased(base.headlineSmall),
        // Title role — screen titles.
        titleLarge = TextStyle(fontFamily = bodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        // Section role — group/section headers, dialog titles.
        titleMedium = TextStyle(fontFamily = bodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
        titleSmall = rebased(base.titleSmall),
        // Body emphasis role — primary list content.
        bodyLarge = TextStyle(fontFamily = bodyFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
        // Body role — standard copy.
        bodyMedium = TextStyle(fontFamily = bodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        // Caption role — metadata, helper text, timestamps.
        bodySmall = TextStyle(fontFamily = bodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
        labelLarge = rebased(base.labelLarge),
        labelMedium = rebased(base.labelMedium),
        // Micro role — status labels, badges, overline text.
        labelSmall = TextStyle(fontFamily = bodyFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    )
}

/** The Data role — outside M3's slot set entirely, so it needs its own style, not a Typography override. */
internal fun appDataTextStyle(monoFontFamily: FontFamily): TextStyle = TextStyle(
    fontFamily = monoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontFeatureSettings = "tnum",
)

/**
 * Provided by [AppTheme] with the real, mono-backed style. The fallback
 * here (system default font) only covers content composed without
 * [AppTheme], e.g. a `@Preview` that deliberately isolates one piece.
 */
val LocalAppDataTextStyle = staticCompositionLocalOf {
    TextStyle(fontFamily = FontFamily.Default, fontSize = 13.sp, lineHeight = 18.sp)
}

/**
 * The single API application screens use for type — the 8 roles from
 * design-system.md's table, plus [label] (M3's `labelLarge`, left at M3's
 * own default proportions): the spec doesn't yet name a distinct "button
 * label" role, and inventing one here would be a value the spec never
 * reviewed — see design-system.md § Open questions. [passAmount] is the
 * first role taken from the new table ahead of the rest of it.
 */
object AppTypography {
    val display: TextStyle @Composable get() = MaterialTheme.typography.headlineLarge
    val title: TextStyle @Composable get() = MaterialTheme.typography.titleLarge
    val section: TextStyle @Composable get() = MaterialTheme.typography.titleMedium
    val bodyEmphasis: TextStyle @Composable get() = MaterialTheme.typography.bodyLarge
    val body: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium
    val caption: TextStyle @Composable get() = MaterialTheme.typography.bodySmall
    val micro: TextStyle @Composable get() = MaterialTheme.typography.labelSmall
    val label: TextStyle @Composable get() = MaterialTheme.typography.labelLarge
    val data: TextStyle @Composable get() = LocalAppDataTextStyle.current

    /** design-system.md's `passAmount`: the one large value heading a document pass. */
    val passAmount: TextStyle @Composable get() = MaterialTheme.typography.displayLarge

    /** The sign-in and vault-creation brand panels' headline, 38/44: larger than [display]. */
    val brandHeadline: TextStyle @Composable get() = display.copy(fontSize = 38.sp, lineHeight = 44.sp)

    /** An uppercase group label in [micro], SemiBold and tracked (the sidebar's CATÉGORIES). */
    val overline: TextStyle @Composable get() = micro.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.72.sp)
}
