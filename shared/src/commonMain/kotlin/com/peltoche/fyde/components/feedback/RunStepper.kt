package com.peltoche.fyde.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.AlertGlyph
import com.peltoche.fyde.CheckGlyph
import com.peltoche.fyde.StopGlyph
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/** Where a [RunStep] stands in its run. */
enum class RunStepState { Done, Current, ToDo, Failed, Stopped }

/**
 * One stage of a run as [RunStepper] lists it: its [label], its [state],
 * an optional [detail] at the far end ("12 trouvés", "5 / 12"), and, for
 * the current step, an optional determinate [progress] in `0..1` drawn as a
 * bar under it.
 */
@Immutable
data class RunStep(
    val label: String,
    val state: RunStepState,
    val detail: String? = null,
    val progress: Float? = null,
)

/**
 * design-system.md § Source status: a source's run as a vertical list of
 * its stages (Connexion → Recherche → Téléchargement i / n → Rangement, S4
 * and DO7c), one 44dp row each — a 22dp disc, the label, the detail.
 * Done is a `success` disc with a check, current a 2dp `warning` ring,
 * failed an `error` disc with an alert, stopped (the user ended the run
 * there) a `sunk` disc (`surfaceElevated` until the tokens migrate) with a
 * `borderStrong` ring and a muted stop square,
 * still to do a hairline ring with the label muted. The state is never in the colour alone: the disc's
 * glyph or emptiness says it too, and the detail carries the numbers.
 */
@Composable
fun RunStepper(steps: List<RunStep>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        steps.forEach { step ->
            RunStepRow(step)
            if (step.progress != null) {
                AppProgressBar(
                    progress = step.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = DiscColumn + AppSpacing.space3, bottom = AppSpacing.space3),
                )
            }
        }
    }
}

@Composable
private fun RunStepRow(step: RunStep) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = RowHeight),
    ) {
        Box(modifier = Modifier.width(DiscColumn), contentAlignment = Alignment.CenterStart) {
            StepDisc(step.state)
        }
        Spacer(Modifier.width(AppSpacing.space3))
        Text(
            text = step.label,
            style = AppTypography.bodyEmphasis,
            color = if (step.state == RunStepState.ToDo) AppColors.textMuted else AppColors.text,
            modifier = Modifier.weight(1f),
        )
        if (step.detail != null) {
            Spacer(Modifier.width(AppSpacing.space3))
            Text(
                text = step.detail,
                style = AppTypography.data,
                color = if (step.state == RunStepState.Failed) AppColors.error else AppColors.textMuted,
            )
        }
    }
}

@Composable
private fun StepDisc(state: RunStepState) {
    val base = Modifier.size(DiscSize).clip(CircleShape)
    Box(
        contentAlignment = Alignment.Center,
        modifier = when (state) {
            RunStepState.Done -> base.background(AppColors.success)
            RunStepState.Failed -> base.background(AppColors.error)
            RunStepState.Current -> base.border(AppDimensions.borderFocus, AppColors.warning, CircleShape)
            RunStepState.Stopped -> base.background(AppColors.surfaceElevated).border(1.5.dp, AppColors.borderStrong, CircleShape)
            RunStepState.ToDo -> base.border(1.5.dp, AppColors.border, CircleShape)
        },
    ) {
        when (state) {
            RunStepState.Done -> CheckGlyph(modifier = Modifier.size(AppIconSize.xs), tint = AppColors.onSuccess, strokeWidth = 2.dp)
            RunStepState.Failed -> AlertGlyph(modifier = Modifier.size(AppIconSize.xs), tint = AppColors.onError, strokeWidth = 2.dp)
            RunStepState.Stopped -> StopGlyph(modifier = Modifier.size(AppIconSize.xs), tint = AppColors.textMuted)
            RunStepState.Current, RunStepState.ToDo -> Unit
        }
    }
}

private val RowHeight = 44.dp
private val DiscSize = 22.dp

/** The disc's column, a little wider than the disc so labels line up whatever the state. */
private val DiscColumn = 28.dp
