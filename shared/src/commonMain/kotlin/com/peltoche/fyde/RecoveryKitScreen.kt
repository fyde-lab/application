package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppButtonVariant
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.components.feedback.AppSnackbarHost
import com.peltoche.fyde.icons.AlertGlyph
import com.peltoche.fyde.icons.DownloadGlyph
import com.peltoche.fyde.icons.EyeGlyph
import com.peltoche.fyde.icons.PagesGlyph
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.CenteredScrollColumn
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.recovery_kit_confirm
import com.peltoche.fyde.resources.recovery_kit_explanation
import com.peltoche.fyde.resources.recovery_kit_failed
import com.peltoche.fyde.resources.recovery_kit_generating
import com.peltoche.fyde.resources.recovery_kit_later
import com.peltoche.fyde.resources.recovery_kit_never_copied
import com.peltoche.fyde.resources.recovery_kit_pdf_account
import com.peltoche.fyde.resources.recovery_kit_pdf_file_name
import com.peltoche.fyde.resources.recovery_kit_pdf_title
import com.peltoche.fyde.resources.recovery_kit_pdf_warning
import com.peltoche.fyde.resources.recovery_kit_print
import com.peltoche.fyde.resources.recovery_kit_print_failed
import com.peltoche.fyde.resources.recovery_kit_save_failed
import com.peltoche.fyde.resources.recovery_kit_save_pdf
import com.peltoche.fyde.resources.recovery_kit_saved
import com.peltoche.fyde.resources.recovery_kit_screenshots_blocked
import com.peltoche.fyde.resources.recovery_kit_title
import com.peltoche.fyde.resources.recovery_kit_word
import com.peltoche.fyde.resources.retry
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** What [RecoveryKitScreen] shows while `App.kt` asks the SDK for the account's recovery key. */
sealed interface RecoveryKitUiState {
    data object Generating : RecoveryKitUiState

    /**
     * The recovery key's [words], in order. A plain class, not a data class,
     * so they never end up in a generated `toString` (and from there in a
     * log).
     */
    class Ready(val words: List<String>) : RecoveryKitUiState

    class Failed(val message: String, val onRetry: () -> Unit) : RecoveryKitUiState
}

/**
 * Kit de récupération, the vault-creation step after the account: the
 * recovery key's words, shown once (the SDK stores them nowhere), to write
 * down, save as a PDF or print. They're the only way back into the vault if
 * the password is forgotten. Pure presentation, like [RegisterScreen]:
 * `App.kt` generates the key and moves on through [onDone] ("I've put it
 * somewhere safe") or [onLater].
 *
 * At Compact it's O6 (`design/assets/screens/O6-kit-recuperation.png`):
 * the step dots on top, the words in three columns, the actions pinned at
 * the bottom. At Medium and Expanded it's DO6
 * (`design/assets/screens/DO6-kit-de-recuperation.png`):
 * [VaultCreationSplitLayout] at its recovery-kit step beside the words in
 * four columns, centred in a 600dp column, the actions side by side. The
 * mockups' 12 words are the SDK's 24 here, so both grids are twice as tall.
 *
 * Neither layout has the mockups' back button: the account already exists
 * by now, so there's no form to go back to.
 *
 * The words are plain, unselectable text, never put on the clipboard; on
 * Android the window is also kept out of screenshots ([ScreenshotGuard]),
 * which the note under the words says instead.
 */
@Composable
fun RecoveryKitScreen(
    state: RecoveryKitUiState,
    username: String,
    onDone: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenshotGuard()
    val actions = rememberRecoveryKitActions(state, username)

    Box(modifier = modifier.fillMaxSize()) {
        when (LocalAppWindowSizeClass.current) {
            AppWindowSizeClass.Compact -> CompactRecoveryKit(state, actions, onDone, onLater)
            AppWindowSizeClass.Medium, AppWindowSizeClass.Expanded -> VaultCreationSplitLayout(
                currentStep = VaultCreationStep.RecoveryKit,
            ) { paneModifier ->
                WideRecoveryKitPane(state, actions, onDone, onLater, paneModifier)
            }
        }
        // Signed-out screens have no scaffold to host the PDF's confirmation.
        AppSnackbarHost(
            hostState = LocalAppSnackbarHostState.current,
            modifier = Modifier.align(Alignment.BottomCenter).safeContentPadding(),
        )
    }
}

/** Saving and printing the kit as a PDF; [print] is `null` where the platform can't ([rememberPdfPrinter]). */
private class RecoveryKitActions(val save: () -> Unit, val print: (() -> Unit)?)

@Composable
private fun rememberRecoveryKitActions(state: RecoveryKitUiState, username: String): RecoveryKitActions {
    val snackbarHostState = LocalAppSnackbarHostState.current
    val scope = rememberCoroutineScope()
    fun announce(message: suspend () -> String) {
        scope.launch { snackbarHostState.showSnackbar(message()) }
    }

    val saver = rememberDocumentSaver { _, saved ->
        when (saved) {
            true -> announce { getString(Res.string.recovery_kit_saved) }
            false -> announce { getString(Res.string.recovery_kit_save_failed) }
            null -> Unit
        }
    }
    val printer = rememberPdfPrinter(onFailed = { announce { getString(Res.string.recovery_kit_print_failed) } })

    val fileName = stringResource(Res.string.recovery_kit_pdf_file_name)
    val title = stringResource(Res.string.recovery_kit_pdf_title)
    val account = stringResource(Res.string.recovery_kit_pdf_account, username)
    val warning = stringResource(Res.string.recovery_kit_pdf_warning)
    val words = (state as? RecoveryKitUiState.Ready)?.words.orEmpty()
    val intro = stringResource(Res.string.recovery_kit_explanation, words.size)
    // Built on each tap rather than kept: the PDF holds the words in clear.
    val pdf = { recoveryKitPdf(title = title, account = account, intro = intro, words = words, warning = warning) }

    return RecoveryKitActions(
        save = { saver(fileName, pdf()) },
        print = printer?.let { print -> { print(title, pdf()) } },
    )
}

/** O6. The content scrolls when the window is too short; the actions stay pinned at the bottom. */
@Composable
private fun CompactRecoveryKit(
    state: RecoveryKitUiState,
    actions: RecoveryKitActions,
    onDone: () -> Unit,
    onLater: () -> Unit,
) {
    Box(
        modifier = Modifier
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.space6),
            ) {
                // Where O6's back button and dots sit, minus the back button.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().height(AppDimensions.backButton).padding(top = AppSpacing.space2),
                ) {
                    VaultStepDots(VaultCreationStep.RecoveryKit)
                }
                Spacer(Modifier.height(AppSpacing.space4))
                RecoveryKitContent(state, actions, columns = 3, wordStyle = WordCellStyle.Compact, fillActions = true)
                Spacer(Modifier.height(AppSpacing.space6))
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(AppSpacing.space2),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space6)
                    .padding(top = AppSpacing.space4, bottom = AppSpacing.space5),
            ) {
                AppButton(
                    text = stringResource(Res.string.recovery_kit_confirm),
                    onClick = onDone,
                    enabled = state is RecoveryKitUiState.Ready,
                    modifier = Modifier.fillMaxWidth(),
                )
                AppTextButton(text = stringResource(Res.string.recovery_kit_later), onClick = onLater)
            }
        }
    }
}

/** DO6's pane: the content centred in a 600dp column, "Later" beside the confirm. */
@Composable
private fun WideRecoveryKitPane(
    state: RecoveryKitUiState,
    actions: RecoveryKitActions,
    onDone: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.background(AppColors.background).safeContentPadding()) {
        CenteredScrollColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = AppDimensions.authFormColumnWide)
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space7, vertical = AppSpacing.space10),
            ) {
                RecoveryKitContent(state, actions, columns = 4, wordStyle = WordCellStyle.Wide, fillActions = false)
                Spacer(Modifier.height(AppSpacing.space6))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Its own width and the confirm the rest, not the mockup's
                    // fixed 1:2 split, which wraps "Later (not recommended)".
                    AppTextButton(text = stringResource(Res.string.recovery_kit_later), onClick = onLater)
                    AppButton(
                        text = stringResource(Res.string.recovery_kit_confirm),
                        onClick = onDone,
                        enabled = state is RecoveryKitUiState.Ready,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * The title, what the words are for, the words in [columns] columns, the
 * PDF actions (each taking half the row when [fillActions], as on O6) and
 * the note under them — the same at every size but for [wordStyle]. While the key is being
 * generated a spinner stands in for the words; if that failed, the SDK's
 * reason and a retry.
 */
@Composable
private fun ColumnScope.RecoveryKitContent(
    state: RecoveryKitUiState,
    actions: RecoveryKitActions,
    columns: Int,
    wordStyle: WordCellStyle,
    fillActions: Boolean,
) {
    val words = (state as? RecoveryKitUiState.Ready)?.words
    Text(
        text = stringResource(Res.string.recovery_kit_title),
        style = AppTypography.display,
        color = AppColors.text,
    )
    Spacer(Modifier.height(AppSpacing.space3))
    Text(
        // The SDK's count once known; 24 is what it generates.
        text = stringResource(Res.string.recovery_kit_explanation, words?.size ?: RECOVERY_KEY_WORDS),
        style = AppTypography.body,
        color = AppColors.textMuted,
    )
    Spacer(Modifier.height(AppSpacing.space6))
    when (state) {
        RecoveryKitUiState.Generating -> Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = AppSpacing.space6),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(AppIconSize.md), strokeWidth = 2.dp, color = AppColors.primary)
            Text(stringResource(Res.string.recovery_kit_generating), style = AppTypography.body, color = AppColors.textMuted)
        }
        is RecoveryKitUiState.Failed -> {
            AppNote(
                message = stringResource(Res.string.recovery_kit_failed, state.message),
                kind = AppNoteKind.Error,
                icon = { tint -> AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint) },
            )
            Spacer(Modifier.height(AppSpacing.space4))
            AppButton(text = stringResource(Res.string.retry), onClick = state.onRetry, variant = AppButtonVariant.Outlined)
        }
        is RecoveryKitUiState.Ready -> RecoveryWordGrid(state.words, maxColumns = columns, style = wordStyle)
    }
    Spacer(Modifier.height(AppSpacing.space5))
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3), modifier = Modifier.fillMaxWidth()) {
        val actionModifier = if (fillActions) Modifier.weight(1f) else Modifier
        AppButton(
            text = stringResource(Res.string.recovery_kit_save_pdf),
            onClick = actions.save,
            enabled = words != null,
            variant = AppButtonVariant.Outlined,
            leadingIcon = { DownloadGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.text) },
            modifier = actionModifier,
        )
        val print = actions.print
        if (print != null) {
            AppButton(
                text = stringResource(Res.string.recovery_kit_print),
                onClick = print,
                enabled = words != null,
                variant = AppButtonVariant.Outlined,
                leadingIcon = { PagesGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.text) },
                modifier = actionModifier,
            )
        }
    }
    Spacer(Modifier.height(AppSpacing.space5))
    AppNote(
        message = stringResource(
            if (blocksScreenshots) Res.string.recovery_kit_screenshots_blocked else Res.string.recovery_kit_never_copied,
        ),
        kind = AppNoteKind.Info,
        icon = { tint -> EyeGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint, crossed = true) },
    )
}

/** How many words the SDK's recovery keys have (24-word BIP39), for the explanation before they arrive. */
private const val RECOVERY_KEY_WORDS = 24

/**
 * How a [RecoveryWordGrid] draws its cells: [cellHeight] tall, the word at
 * [wordSize], and at least [minCell] wide — enough for the longest BIP39
 * words (8 letters, at Plex Mono's 0.6em advance) after the number and the
 * padding.
 */
private enum class WordCellStyle(val cellHeight: Dp, val wordSize: TextUnit, val minCell: Dp) {
    /** O6: 14sp rather than the mockup's 15, so three columns fit from a 350dp-wide grid. */
    Compact(cellHeight = 44.dp, wordSize = 14.sp, minCell = 106.dp),

    /** DO6. */
    Wide(cellHeight = 46.dp, wordSize = 15.sp, minCell = 112.dp),
}

/**
 * The words row by row in up to [maxColumns] equal columns (`.words` in the
 * mockups): each a `surface` cell with a hairline border, its number muted
 * before the word in Plex Mono. Fewer columns where cells would be
 * narrower than [WordCellStyle.minCell] (a phone narrower than O6's): a
 * word cut short can't be written down. Each cell reads out as "Word n: …".
 */
@Composable
private fun RecoveryWordGrid(words: List<String>, maxColumns: Int, style: WordCellStyle) {
    BoxWithConstraints {
        val gap = AppSpacing.space3
        val fitting = ((maxWidth + gap) / (style.minCell + gap)).toInt()
        RecoveryWordRows(words, columns = fitting.coerceIn(2, maxColumns), style = style)
    }
}

@Composable
private fun RecoveryWordRows(words: List<String>, columns: Int, style: WordCellStyle) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.space3)) {
        words.withIndex().chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (index, word) ->
                    RecoveryWordCell(number = index + 1, word = word, style = style, modifier = Modifier.weight(1f))
                }
                // A short last row keeps its cells the width of the others.
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private val WordCellShape = RoundedCornerShape(12.dp)

@Composable
private fun RecoveryWordCell(number: Int, word: String, style: WordCellStyle, modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.recovery_kit_word, number, word)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(style.cellHeight)
            .background(AppColors.surface, WordCellShape)
            .border(AppDimensions.borderHairline, AppColors.border, WordCellShape)
            // The mockups' 10dp, off the spacing scale, to fit the longest words.
            .padding(horizontal = 10.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Text(
            text = number.toString(),
            style = AppTypography.data.copy(fontSize = 12.sp),
            color = AppColors.textMuted,
            modifier = Modifier.width(18.dp),
        )
        Text(
            text = word,
            style = AppTypography.data.copy(fontSize = style.wordSize, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
            color = AppColors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
