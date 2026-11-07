package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.peltoche.fyde.layout.AppWindowSizeClassProvider
import com.peltoche.fyde.theme.AppTheme
import com.peltoche.fyde.theme.AppThemeOption
import java.io.File
import java.util.Locale
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders every screen, in fixed states built from fake data, to
 * `$FYDE_SCREENSHOTS_DIR/<screen>-<size>.png` at Compact, Medium and Expanded
 * widths, so a refactor can be checked pixel for pixel against a baseline
 * (`compare -metric AE before/x.png after/x.png`). Skipped unless
 * `FYDE_SCREENSHOTS_DIR` is set: it renders ~150 frames in software.
 *
 * Every screen is drawn as `App.kt` draws it (inside [AppTheme] and
 * [AppWindowSizeClassProvider]), in French, the product's first market.
 */
class ScreenshotsJvmTest {

    private val sizes = listOf(
        Size("compact", 390, 844),
        Size("medium", 834, 1194),
        Size("expanded", 1440, 900),
    )

    @Test
    fun renderScreens() {
        val dir = System.getenv("FYDE_SCREENSHOTS_DIR")?.let(::File) ?: return
        dir.mkdirs()
        applyPlatformLocale("fr")
        Locale.setDefault(Locale.FRANCE)
        val failures = mutableListOf<String>()
        for ((name, content) in ScreenshotFixtures.screens) {
            for (size in sizes) {
                val file = File(dir, "$name-${size.name}.png")
                runCatching { render(size, content, file) }
                    .onFailure { failures += "${file.name}: $it" }
            }
        }
        check(failures.isEmpty()) { "Some screens failed to render:\n" + failures.joinToString("\n") }
    }

    private fun render(size: Size, content: @Composable () -> Unit, file: File) {
        val scene = ImageComposeScene(width = size.width, height = size.height, density = Density(1f)) {
            AppTheme(theme = AppThemeOption.Default, darkTheme = false) {
                AppWindowSizeClassProvider { content() }
            }
        }
        try {
            // Several frames at fixed times: resources (strings, fonts) load
            // over the first ones, and fixed times keep animations repeatable.
            var image = scene.render(0)
            for (frame in 1..FRAMES) image = scene.render(frame * FRAME_NANOS)
            val png = requireNotNull(image.encodeToData(EncodedImageFormat.PNG)) { "PNG encoding failed" }
            file.writeBytes(png.bytes)
        } finally {
            scene.close()
        }
    }

    private data class Size(val name: String, val width: Int, val height: Int)

    private companion object {
        const val FRAMES = 20
        const val FRAME_NANOS = 100_000_000L
    }
}
