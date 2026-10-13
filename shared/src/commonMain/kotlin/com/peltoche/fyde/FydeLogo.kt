package com.peltoche.fyde

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.fyde_logo
import org.jetbrains.compose.resources.painterResource

/**
 * Fyde's logo artwork. Public so platform entry points outside `shared`
 * (which can't see the internal `Res` class) can use it too, e.g. as the
 * desktop window icon.
 */
@Composable
fun fydeLogoPainter(): Painter = painterResource(Res.drawable.fyde_logo)

/**
 * Fyde's logo, drawn in full color. Decorative wherever it sits next to the
 * product name, so it carries no content description by default.
 */
@Composable
fun FydeLogo(modifier: Modifier = Modifier, contentDescription: String? = null) {
    Image(painter = fydeLogoPainter(), contentDescription = contentDescription, modifier = modifier)
}
