package com.peltoche.fyde

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.peltoche.fyde.components.content.AppImageTile
import com.peltoche.fyde.theme.AppDimensions

/**
 * A script's logo ([FydeScript.icon], PNG bytes) in an [AppImageTile],
 * decoded via [decodeIconBitmap]; the tile's fallback glyph when [icon] is
 * empty or isn't valid image data.
 */
@Composable
internal fun ScriptIcon(icon: ByteArray = ByteArray(0), modifier: Modifier = Modifier, size: Dp = AppDimensions.iconTile) {
    val bitmap = remember(icon) { decodeIconBitmap(icon) }
    AppImageTile(image = bitmap, modifier = modifier, size = size)
}
