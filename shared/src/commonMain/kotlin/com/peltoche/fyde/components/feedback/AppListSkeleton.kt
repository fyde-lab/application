package com.peltoche.fyde.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.peltoche.fyde.components.content.AppDivider
import com.peltoche.fyde.components.content.AppGroup
import com.peltoche.fyde.components.content.AppGroupRow
import com.peltoche.fyde.theme.AppRadius

/**
 * A list's place while it loads, shaped like its loaded rows so the page
 * doesn't jump: an [AppGroup] of [rows] rows at least [rowHeight] tall,
 * each a [tileSize] tile ([tileShape]) beside two bars sized by
 * [firstLine] and [secondLine], [lineGap] apart. Each list passes its own
 * rows' proportions.
 */
@Composable
fun AppListSkeleton(
    rows: Int,
    rowHeight: Dp,
    tileSize: Dp,
    firstLine: Modifier,
    secondLine: Modifier,
    lineGap: Dp,
    tileShape: Shape = AppRadius.sm,
) {
    AppGroup {
        repeat(rows) { index ->
            if (index > 0) AppDivider()
            AppGroupRow(minHeight = rowHeight) {
                AppSkeleton(modifier = Modifier.size(tileSize), shape = tileShape)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(lineGap)) {
                    AppSkeleton(modifier = firstLine)
                    AppSkeleton(modifier = secondLine)
                }
            }
        }
    }
}
