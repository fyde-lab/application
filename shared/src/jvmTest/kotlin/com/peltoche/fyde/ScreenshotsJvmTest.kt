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
 * `FYDE_SCREENSHOTS_DIR` is set: it renders ~190 frames in software.
 * `FYDE_SCREENSHOTS_SCALE` (default 1) renders at that pixel density, the
 * same layout in more pixels: `design/capture.sh` uses 2 for
 * `design/current/screens/`.
 *
 * Every screen is drawn as `App.kt` draws it (inside [AppTheme] and
 * [AppWindowSizeClassProvider]), in English (the UI's default strings).
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
        val scale = System.getenv("FYDE_SCREENSHOTS_SCALE")?.toFloat() ?: 1f
        dir.mkdirs()
        applyPlatformLocale("en")
        Locale.setDefault(Locale.UK)
        val failures = mutableListOf<String>()
        for ((name, content) in ScreenshotFixtures.screens) {
            for (size in sizes) {
                val file = File(dir, "$name-${size.name}.png")
                runCatching { render(size, scale, content, file) }
                    .onFailure { failures += "${file.name}: $it" }
            }
        }
        check(failures.isEmpty()) { "Some screens failed to render:\n" + failures.joinToString("\n") }
    }

    private fun render(size: Size, scale: Float, content: @Composable () -> Unit, file: File) {
        val scene = ImageComposeScene(
            width = (size.width * scale).toInt(),
            height = (size.height * scale).toInt(),
            density = Density(scale),
        ) {
            AppTheme(theme = AppThemeOption.Default, darkTheme = false) {
                AppWindowSizeClassProvider { content() }
            }
        }
        try {
            // Several frames at fixed times: resources (strings, fonts) load
            // over the first ones, and fixed times keep animations repeatable.
            var image = scene.render(0)
            // Real time between them too, for work off the UI thread (a PDF
            // preview loads and rasterizes on Dispatchers.IO).
            for (frame in 1..FRAMES) {
                Thread.sleep(FRAME_WAIT_MS)
                image = scene.render(frame * FRAME_NANOS)
            }
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
        const val FRAME_WAIT_MS = 25L
    }
}
