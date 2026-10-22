package com.peltoche.fyde

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.peltoche.fyde.components.feedback.AppBanner
import com.peltoche.fyde.components.feedback.AppProgressBar
import com.peltoche.fyde.components.feedback.AppSkeleton
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.plan_label
import com.peltoche.fyde.resources.plan_name_admin
import com.peltoche.fyde.resources.plan_name_basic
import com.peltoche.fyde.resources.plan_name_free
import com.peltoche.fyde.resources.plan_section_title
import com.peltoche.fyde.resources.usage_at_limit
import com.peltoche.fyde.resources.usage_devices_label
import com.peltoche.fyde.resources.usage_of_limit
import com.peltoche.fyde.resources.usage_over_limit
import com.peltoche.fyde.resources.usage_scripts_label
import com.peltoche.fyde.resources.usage_storage_label
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.stringResource

/**
 * What [SettingsScreen]'s plan section shows. The caller (see `App.kt`'s
 * `Screen.Settings` branch) owns the [FydeSdk.getUsage] call and maps its
 * result into this state, same split as [ScriptsUiState]. Fetched every time
 * Settings opens rather than cached: the account's other devices move these
 * figures too.
 */
sealed interface PlanUsageUiState {
    data object Loading : PlanUsageUiState

    data class Failed(val message: String, val onRetry: () -> Unit) : PlanUsageUiState

    data class Loaded(val usage: FydePlanUsage) : PlanUsageUiState
}

/**
 * How close one figure is to its plan's limit, deciding its meter's color:
 * `primary` while there's room, `warning` from [NEAR_LIMIT_RATIO] and at the
 * limit, `error` past it — which only happens after a move to a smaller plan
 * (see [FydePlanUsage]).
 */
internal enum class UsageLevel { NORMAL, NEAR, FULL, OVER }

private const val NEAR_LIMIT_RATIO = 0.9

internal fun usageLevel(used: Long, limit: Long): UsageLevel = when {
    used > limit -> UsageLevel.OVER
    used == limit -> UsageLevel.FULL
    used >= limit * NEAR_LIMIT_RATIO -> UsageLevel.NEAR
    else -> UsageLevel.NORMAL
}

/** The filled share of a meter, clamped to the bar: an over-limit figure draws it full. */
internal fun usageFraction(used: Long, limit: Long): Float =
    if (limit <= 0) 1f else (used.toDouble() / limit).coerceIn(0.0, 1.0).toFloat()

/** The plan section of [SettingsScreen]: the plan's name, then one meter per limit. */
@Composable
internal fun PlanUsageSection(state: PlanUsageUiState) {
    SectionTitle(stringResource(Res.string.plan_section_title))
    when (state) {
        PlanUsageUiState.Loading -> PlanUsageSkeleton()
        is PlanUsageUiState.Failed -> {
            AppBanner(message = state.message)
            RetryButton(onClick = state.onRetry)
        }
        is PlanUsageUiState.Loaded -> {
            val usage = state.usage
            SettingsRow(label = stringResource(Res.string.plan_label), value = planName(usage.planId))
            UsageMeter(
                label = stringResource(Res.string.usage_storage_label),
                used = usage.totalSize,
                limit = usage.maxSize,
                format = { localizedSize(it) },
            )
            UsageMeter(
                label = stringResource(Res.string.usage_devices_label),
                used = usage.openSessions.toLong(),
                limit = usage.maxSessions.toLong(),
            )
            UsageMeter(
                label = stringResource(Res.string.usage_scripts_label),
                used = usage.enabledScripts.toLong(),
                limit = usage.maxScripts.toLong(),
            )
        }
    }
}

/** The server's plan ids are identifiers, not display text: known ones get translated names. */
@Composable
internal fun planName(planId: String): String = when (planId) {
    "free" -> stringResource(Res.string.plan_name_free)
    "basic" -> stringResource(Res.string.plan_name_basic)
    "admin" -> stringResource(Res.string.plan_name_admin)
    else -> planId.replaceFirstChar { it.uppercase() }
}

/** Shaped like the loaded section (a plan row, three meters), so it doesn't jump on load. */
@Composable
private fun PlanUsageSkeleton() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.space3)) {
        AppSkeleton(Modifier.width(120.dp).height(14.dp))
        Spacer(Modifier.height(AppSpacing.space2))
        AppSkeleton(Modifier.width(80.dp).height(18.dp))
        repeat(3) {
            Spacer(Modifier.height(AppSpacing.space5))
            AppSkeleton(Modifier.fillMaxWidth().height(14.dp))
            Spacer(Modifier.height(AppSpacing.space2))
            AppSkeleton(Modifier.fillMaxWidth().height(AppDimensions.progressBarHeight))
        }
    }
}

/**
 * One limit: its [label] and "used of limit" on one line, a bar under them,
 * and — once the limit is reached — a caption saying so, so the state never
 * relies on the bar's color alone.
 */
@Composable
private fun UsageMeter(
    label: String,
    used: Long,
    limit: Long,
    format: @Composable (Long) -> String = { it.toString() },
) {
    val level = usageLevel(used, limit)
    val fraction = usageFraction(used, limit)
    val color = when (level) {
        UsageLevel.NORMAL -> AppColors.primary
        UsageLevel.NEAR, UsageLevel.FULL -> AppColors.warning
        UsageLevel.OVER -> AppColors.error
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.space3)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = AppTypography.caption,
                color = AppColors.textMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(Res.string.usage_of_limit, format(used), format(limit)),
                style = AppTypography.caption,
                color = AppColors.text,
            )
        }
        Spacer(Modifier.height(AppSpacing.space2))
        AppProgressBar(progress = fraction, color = color, modifier = Modifier.fillMaxWidth())
        when (level) {
            UsageLevel.FULL -> MeterCaption(stringResource(Res.string.usage_at_limit), AppColors.warning)
            UsageLevel.OVER -> MeterCaption(stringResource(Res.string.usage_over_limit), AppColors.error)
            UsageLevel.NORMAL, UsageLevel.NEAR -> Unit
        }
    }
}

@Composable
private fun MeterCaption(text: String, color: Color) {
    Text(
        text = text,
        style = AppTypography.micro,
        color = color,
        modifier = Modifier.padding(top = AppSpacing.space2),
    )
}
