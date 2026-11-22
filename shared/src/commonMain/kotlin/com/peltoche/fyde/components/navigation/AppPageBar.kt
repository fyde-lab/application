package com.peltoche.fyde.components.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.peltoche.fyde.components.action.AppBackButton
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography

/**
 * A page's bar below Expanded (`sources-account.html`'s `.navb`): the back
 * circle at the start, an optional [title] centred in `bodyEmphasis`, an
 * optional [trailing] action at the end. [enabled] disables the back button
 * while the page is busy. The system back gesture does what the back button
 * does (see [AppBackButton]).
 *
 * Not [AppTopBar], whose title starts at the content edge and whose height
 * follows the size class: this is the reference screens' centred bar, the
 * page's own title following below it.
 */
@Composable
fun AppPageBar(
    title: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    handlesSystemBack: Boolean = true,
) {
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = AppDimensions.topBarHeight),
        contentAlignment = Alignment.Center,
    ) {
        AppBackButton(
            onClick = onBack,
            enabled = enabled,
            handlesSystemBack = handlesSystemBack,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        if (title != null) {
            Text(
                text = title,
                style = AppTypography.bodyEmphasis,
                color = AppColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = AppDimensions.backButton + AppSpacing.space3),
            )
        }
        if (trailing != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
    }
}
