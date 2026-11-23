package com.peltoche.fyde

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.icons.CheckCircleGlyph
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.CenteredScrollColumn
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.opening_browse
import com.peltoche.fyde.resources.opening_browse_note
import com.peltoche.fyde.resources.opening_done_note
import com.peltoche.fyde.resources.opening_explanation_computer
import com.peltoche.fyde.resources.opening_explanation_device
import com.peltoche.fyde.resources.opening_of_total
import com.peltoche.fyde.resources.opening_percent
import com.peltoche.fyde.resources.opening_progress_description
import com.peltoche.fyde.resources.opening_received
import com.peltoche.fyde.resources.opening_title
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * How far the replay has got, in whole percent: rounded down, so 100 only
 * once [consumed] reaches [total]; an empty history is 100.
 */
internal fun replayPercent(consumed: Long, total: Long): Int =
    if (total <= 0L) 100 else (consumed.coerceIn(0L, total) * 100 / total).toInt()

/** The replay ring's diameter and stroke: `.ringp`, 200px around a 176px disc. */
private val RingSize = 200.dp
private val RingStroke = 12.dp

/**
 * Ouverture: a device just signed into replays the account's whole history
 * (logging out and an expired session both wipe this device's copy, so every
 * sign-in starts from an empty vault). [progress] is [FydeSdk.syncProgress]
 * as [App] polls it, `null` before the first answer. A ring shows how much
 * has arrived ("66 %", "142 sur 214 éléments"), naming this device, with Parcourir le coffre
 * ([onBrowse]) as the only action: what has arrived can be browsed already,
 * and the rest keeps arriving in the background after it.
 *
 * The count is of changelog events, not documents: the server only relays
 * ciphertext, so it can't tell a document's event from a folder's or a
 * source's. The ring turns without a total until the server has counted
 * (or if it can't), then fills; once caught up, the note says everything
 * has arrived.
 *
 * At Compact it's O9 (`design/proposals/000-original-design/assets/screens/O9-ouverture.png`), the button
 * pinned at the bottom. At Medium and Expanded it's DO9
 * (`design/proposals/000-original-design/assets/screens/DO9-ouverture-sur-cet-ordinateur.png`):
 * [SignInSplitLayout]'s brand panel beside the content centred in an
 * [AppDimensions.authFormColumn] column. No back: the session is open.
 */
@Composable
fun OpeningScreen(
    progress: FydeSyncProgress?,
    onBrowse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (LocalAppWindowSizeClass.current) {
        AppWindowSizeClass.Compact -> CompactOpening(progress, onBrowse, modifier)
        AppWindowSizeClass.Medium, AppWindowSizeClass.Expanded -> SignInSplitLayout(modifier) { paneModifier ->
            WideOpeningPane(progress, onBrowse, paneModifier)
        }
    }
}

/** O9. The content scrolls when the window is too short; the button stays pinned at the bottom. */
@Composable
private fun CompactOpening(progress: FydeSyncProgress?, onBrowse: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background)
            .safeContentPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppDimensions.maxWidthDetail)
                .fillMaxSize(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.space6),
            ) {
                Spacer(Modifier.height(AppSpacing.space10 + AppSpacing.space6))
                OpeningContent(progress)
                Spacer(Modifier.height(AppSpacing.space6))
            }
            AppButton(
                text = stringResource(Res.string.opening_browse),
                onClick = onBrowse,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space6)
                    .padding(top = AppSpacing.space4, bottom = AppSpacing.space5),
            )
        }
    }
}

/** DO9's pane: the content and the button centred in an [AppDimensions.authFormColumn] column. */
@Composable
private fun WideOpeningPane(progress: FydeSyncProgress?, onBrowse: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier.background(AppColors.background).safeContentPadding()) {
        CenteredScrollColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = AppDimensions.authFormColumn)
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space7, vertical = AppSpacing.space8),
            ) {
                OpeningContent(progress)
                Spacer(Modifier.height(AppSpacing.space5))
                AppButton(
                    text = stringResource(Res.string.opening_browse),
                    onClick = onBrowse,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** The ring, the title, the line naming this device and the note — the same at every size. */
@Composable
private fun OpeningContent(progress: FydeSyncProgress?) {
    ReplayRing(progress)
    Spacer(Modifier.height(AppSpacing.space7))
    Text(
        text = stringResource(Res.string.opening_title),
        style = AppTypography.title,
        color = AppColors.text,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(AppSpacing.space2))
    Text(
        text = stringResource(
            if (getPlatform().isDesktop) Res.string.opening_explanation_computer else Res.string.opening_explanation_device,
        ),
        style = AppTypography.body,
        color = AppColors.textMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = AppSpacing.space2),
    )
    Spacer(Modifier.height(AppSpacing.space6))
    AppNote(
        message = stringResource(
            if (progress?.caughtUp == true) Res.string.opening_done_note else Res.string.opening_browse_note,
        ),
        kind = AppNoteKind.Ok,
        icon = { tint -> CheckCircleGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * `.ringp`: a `primary` arc over a `sunk` track (`border` until the tokens
 * migrate), filled as far as [progress] has got, with the percentage in
 * the middle and the count under it ("66 %", "142 sur 214 éléments"). Without
 * a total yet, a quarter arc turns and the middle shows how many have been
 * received ("142", "éléments reçus"). Read out as one progress bar.
 */
@Composable
private fun ReplayRing(progress: FydeSyncProgress?) {
    val consumed = progress?.consumed ?: 0L
    val total = progress?.total
    val fraction = when {
        total == null -> null
        total <= 0L -> 1f
        else -> (consumed.toFloat() / total).coerceIn(0f, 1f)
    }
    val sweep by animateFloatAsState(targetValue = (fraction ?: 0f) * 360f, label = "replaySweep")
    val turning = rememberInfiniteTransition(label = "replayTurning")
    val rotation by turning.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1600, easing = LinearEasing), RepeatMode.Restart),
        label = "replayRotation",
    )

    // `sunk` (#E9E6DD) is closest to `border` among today's tokens:
    // `surfaceElevated`, the usual stand-in, is near-white and vanishes here.
    val track = AppColors.border
    val arc = AppColors.primary
    val figure = if (total != null) {
        stringResource(Res.string.opening_percent, replayPercent(consumed, total))
    } else {
        consumed.toString()
    }
    val caption = if (total != null) {
        pluralStringResource(
            Res.plurals.opening_of_total,
            total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            consumed.toString(),
            total.toString(),
        )
    } else {
        pluralStringResource(Res.plurals.opening_received, consumed.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }
    val description = if (total != null) {
        stringResource(Res.string.opening_progress_description, consumed.toString(), total.toString())
    } else {
        "$consumed $caption"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(RingSize)
            .clearAndSetSemantics {
                contentDescription = description
                progressBarRangeInfo = if (fraction != null) {
                    ProgressBarRangeInfo(fraction, 0f..1f)
                } else {
                    ProgressBarRangeInfo.Indeterminate
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = RingStroke.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            drawCircle(color = track, radius = (size.minDimension - strokeWidth) / 2f, style = Stroke(width = strokeWidth))
            if (fraction == null) {
                drawArc(
                    color = arc,
                    startAngle = rotation - 90f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
            } else if (sweep > 0f) {
                drawArc(
                    color = arc,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.space1),
            modifier = Modifier.padding(horizontal = RingStroke + AppSpacing.space4),
        ) {
            Text(
                text = figure,
                style = AppTypography.data.copy(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold),
                color = AppColors.text,
                maxLines = 1,
            )
            Text(
                text = caption,
                style = AppTypography.caption,
                color = AppColors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
