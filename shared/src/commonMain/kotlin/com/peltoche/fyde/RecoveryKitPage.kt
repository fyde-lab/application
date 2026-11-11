package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.components.overlay.AppDialog
import com.peltoche.fyde.icons.AlertGlyph
import com.peltoche.fyde.icons.DownloadGlyph
import com.peltoche.fyde.icons.EyeGlyph
import com.peltoche.fyde.icons.KeyGlyph
import com.peltoche.fyde.icons.PagesGlyph
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.cancel
import com.peltoche.fyde.resources.recovery_kit_confirm
import com.peltoche.fyde.resources.recovery_kit_explanation
import com.peltoche.fyde.resources.recovery_kit_failed
import com.peltoche.fyde.resources.recovery_kit_generate
import com.peltoche.fyde.resources.recovery_kit_generate_confirm
import com.peltoche.fyde.resources.recovery_kit_generate_confirm_title
import com.peltoche.fyde.resources.recovery_kit_generating
import com.peltoche.fyde.resources.recovery_kit_missing_note
import com.peltoche.fyde.resources.recovery_kit_never_copied
import com.peltoche.fyde.resources.recovery_kit_pdf_file_name
import com.peltoche.fyde.resources.recovery_kit_pdf_title
import com.peltoche.fyde.resources.recovery_kit_pdf_warning
import com.peltoche.fyde.resources.recovery_kit_print
import com.peltoche.fyde.resources.recovery_kit_print_failed
import com.peltoche.fyde.resources.recovery_kit_replaces_note
import com.peltoche.fyde.resources.recovery_kit_save_failed
import com.peltoche.fyde.resources.recovery_kit_save_pdf
import com.peltoche.fyde.resources.recovery_kit_saved
import com.peltoche.fyde.resources.recovery_kit_screenshots_blocked
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

/** Whether the account has a recovery key, from [FydeSdk.recoveryKeyStatus]: M2's row subtitle and the page's note. */
sealed interface RecoveryKeyStatus {
    data object Loading : RecoveryKeyStatus

    /** The account has no recovery key: M2's "À créer". */
    data object Missing : RecoveryKeyStatus

    /** The current key was generated at [epochSeconds]. */
    data class Created(val epochSeconds: Long) : RecoveryKeyStatus

    /** The SDK couldn't say (offline, ...): nothing is claimed either way. */
    data object Unknown : RecoveryKeyStatus
}

/** What the recovery kit page shows while `App.kt` asks the SDK for a new recovery key. */
sealed interface RecoveryKitUiState {
    /** No words on screen: the page offers to create a kit. */
    data object Idle : RecoveryKitUiState

    data object Generating : RecoveryKitUiState

    /**
     * The new recovery key's [words], in order. A plain class, not a data
     * class, so they never end up in a generated `toString` (and from there
     * in a log).
     */
    class Ready(val words: List<String>) : RecoveryKitUiState

    class Failed(val message: String) : RecoveryKitUiState
}

/**
 * Kit de récupération as a Settings page, reached from M2's row: O6's
 * content (design-system.md's screen map has M2 lead to "the same screen as
 * in vault creation") without the vault-creation step dots. The words are
 * shown once (the SDK stores them nowhere) to write down, save as a PDF or
 * print; they're the only way back into the vault if the password is
 * forgotten.
 *
 * Unlike O6, opening the page doesn't generate a key: each one replaces the
 * account's previous key, so the page waits for [onGenerate], asking first
 * unless [status] says there's no key to replace. Once the words are shown,
 * "I've put it somewhere safe" ([onDone]) drops them.
 *
 * The words are plain, unselectable text, never put on the clipboard; on
 * Android the window is also kept out of screenshots ([ScreenshotGuard])
 * while they're shown, which the note under them says instead.
 */
@Composable
internal fun ColumnScope.RecoveryKitPage(
    status: RecoveryKeyStatus,
    state: RecoveryKitUiState,
    onGenerate: () -> Unit,
    onDone: () -> Unit,
) {
    val compact = LocalAppWindowSizeClass.current == AppWindowSizeClass.Compact
    var confirming by remember { mutableStateOf(false) }

    Text(
        // The SDK's count once known; 24 is what it generates.
        text = stringResource(
            Res.string.recovery_kit_explanation,
            (state as? RecoveryKitUiState.Ready)?.words?.size ?: RECOVERY_KEY_WORDS,
        ),
        style = AppTypography.body,
        color = AppColors.textMuted,
    )
    Spacer(Modifier.height(AppSpacing.space6))
    when (state) {
        is RecoveryKitUiState.Ready -> {
            ScreenshotGuard()
            RecoveryWordGrid(
                state.words,
                maxColumns = if (compact) 3 else 4,
                style = if (compact) WordCellStyle.Compact else WordCellStyle.Wide,
            )
            Spacer(Modifier.height(AppSpacing.space5))
            RecoveryKitActionsRow(state.words, fillActions = compact)
            Spacer(Modifier.height(AppSpacing.space5))
            AppNote(
                message = stringResource(
                    if (blocksScreenshots) Res.string.recovery_kit_screenshots_blocked else Res.string.recovery_kit_never_copied,
                ),
                kind = AppNoteKind.Info,
                icon = { tint -> EyeGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint, crossed = true) },
            )
            Spacer(Modifier.height(AppSpacing.space6))
            AppButton(
                text = stringResource(Res.string.recovery_kit_confirm),
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        RecoveryKitUiState.Generating -> Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.space4),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = AppSpacing.space6),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(AppIconSize.md), strokeWidth = 2.dp, color = AppColors.primary)
            Text(stringResource(Res.string.recovery_kit_generating), style = AppTypography.body, color = AppColors.textMuted)
        }
        RecoveryKitUiState.Idle, is RecoveryKitUiState.Failed -> {
            if (status == RecoveryKeyStatus.Missing) {
                AppNote(
                    message = stringResource(Res.string.recovery_kit_missing_note),
                    kind = AppNoteKind.Warning,
                    icon = { tint -> AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint) },
                )
            } else {
                AppNote(
                    message = stringResource(Res.string.recovery_kit_replaces_note),
                    kind = AppNoteKind.Info,
                    icon = { tint -> KeyGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
                )
            }
            if (state is RecoveryKitUiState.Failed) {
                Spacer(Modifier.height(AppSpacing.space4))
                AppNote(
                    message = stringResource(Res.string.recovery_kit_failed, state.message),
                    kind = AppNoteKind.Error,
                    icon = { tint -> AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint) },
                )
            }
            Spacer(Modifier.height(AppSpacing.space6))
            AppButton(
                text = stringResource(if (state is RecoveryKitUiState.Failed) Res.string.retry else Res.string.recovery_kit_generate),
                // A retry was already confirmed; without a key there's nothing to void.
                onClick = {
                    if (state is RecoveryKitUiState.Failed || status == RecoveryKeyStatus.Missing) onGenerate() else confirming = true
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirming) {
        AppDialog(
            onDismissRequest = { confirming = false },
            title = stringResource(Res.string.recovery_kit_generate_confirm_title),
            text = stringResource(Res.string.recovery_kit_replaces_note),
            confirmLabel = stringResource(Res.string.recovery_kit_generate_confirm),
            onConfirm = {
                confirming = false
                onGenerate()
            },
            dismissLabel = stringResource(Res.string.cancel),
            onDismiss = { confirming = false },
        )
    }
}

/**
 * Save the PDF and, where the platform can ([rememberPdfPrinter]), Print,
 * each taking half the row when [fillActions] (as on O6).
 */
@Composable
private fun RecoveryKitActionsRow(words: List<String>, fillActions: Boolean) {
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
    val warning = stringResource(Res.string.recovery_kit_pdf_warning)
    val intro = stringResource(Res.string.recovery_kit_explanation, words.size)
    // Built on each tap rather than kept: the PDF holds the words in clear.
    val pdf = { recoveryKitPdf(title = title, intro = intro, words = words, warning = warning) }

    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.space3), modifier = Modifier.fillMaxWidth()) {
        val actionModifier = if (fillActions) Modifier.weight(1f) else Modifier
        AppButton(
            text = stringResource(Res.string.recovery_kit_save_pdf),
            onClick = { saver(fileName, pdf()) },
            variant = AppButtonVariant.Outlined,
            leadingIcon = { DownloadGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.text) },
            modifier = actionModifier,
        )
        if (printer != null) {
            AppButton(
                text = stringResource(Res.string.recovery_kit_print),
                onClick = { printer(title, pdf()) },
                variant = AppButtonVariant.Outlined,
                leadingIcon = { PagesGlyph(modifier = Modifier.size(AppIconSize.sm), tint = AppColors.text) },
                modifier = actionModifier,
            )
        }
    }
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
 * narrower than [WordCellStyle.minCell]: a word cut short can't be written
 * down. Each cell reads out as "Word n: …".
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
