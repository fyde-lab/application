package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.em
import com.peltoche.fyde.components.action.AppBackButton
import com.peltoche.fyde.components.action.AppButton
import com.peltoche.fyde.components.action.AppTextButton
import com.peltoche.fyde.components.feedback.AppNote
import com.peltoche.fyde.components.feedback.AppNoteKind
import com.peltoche.fyde.icons.AlertGlyph
import com.peltoche.fyde.icons.GlobeGlyph
import com.peltoche.fyde.icons.LockGlyph
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.CenteredScrollColumn
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.connect_source_domains
import com.peltoche.fyde.resources.connect_source_encrypted
import com.peltoche.fyde.resources.connect_source_encrypted_short
import com.peltoche.fyde.resources.connect_source_list_and
import com.peltoche.fyde.resources.connect_source_position
import com.peltoche.fyde.resources.connect_source_secret_help
import com.peltoche.fyde.resources.connect_source_skip
import com.peltoche.fyde.resources.connect_source_submit
import com.peltoche.fyde.resources.connect_source_submitting
import com.peltoche.fyde.resources.script_parameter_invalid_number
import com.peltoche.fyde.resources.script_parameter_required
import com.peltoche.fyde.resources.source_secret_keep
import com.peltoche.fyde.theme.AppColors
import com.peltoche.fyde.theme.AppDimensions
import com.peltoche.fyde.theme.AppIconSize
import com.peltoche.fyde.theme.AppSpacing
import com.peltoche.fyde.theme.AppTypography
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Connecter une source, the vault-creation step after [SourcesScreen]: each
 * ticked source in turn, its [position] out of [count], asks for one field
 * per script parameter before it's installed and its first run started.
 * Pure presentation: `App.kt` holds which source is up and what became of
 * the others ([sources], for the brand panel), and installs it through
 * [onConnect] with the typed values, or moves on through [onSkip].
 *
 * At Compact it's S3 inside onboarding (`design/assets/screens/S3-connecter.png`):
 * back and "Skip this source" on top, the fields scrolling, the connect
 * button pinned at the bottom. At Medium and Expanded it's DO7b
 * (`design/assets/screens/DO7b-connecter-une-source.png`):
 * [VaultCreationSplitLayout] at its sources step, listing [sources] with
 * their state, beside the form in a 440dp column, back at the pane's top
 * left and "Skip this source" at its top right.
 *
 * Both show the source's tile and what it fetches, an info note naming the
 * only sites it may reach (`allowedDomains`), the fields (a secret one says
 * it's the source's password, not Fyde's), and an ok note that they're
 * encrypted with the vault. [errorMessage] says why the last attempt failed,
 * as an error note; while [isSubmitting] the fields are disabled and the
 * button spins.
 *
 * Not drawn from the mockups: "Récupérer automatiquement" — `fyde-sdk` has
 * no scheduled runs yet (design-system.md § SDK dependencies).
 */
@Composable
fun ConnectSourceScreen(
    script: FydeScript,
    position: Int,
    count: Int,
    sources: List<VaultSource>,
    onConnect: (Map<String, FydeScriptParameterValue>) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isSubmitting: Boolean = false,
) {
    // Above both layouts, so resizing across Compact keeps what's typed;
    // keyed on the source, so the next one starts empty.
    val values = remember(script.id) { mutableStateMapOf<String, String>() }
    var showRequiredErrors by remember(script.id) { mutableStateOf(false) }
    val form = ConnectSourceForm(
        script = script,
        position = position,
        count = count,
        values = values,
        showRequiredErrors = showRequiredErrors,
        errorMessage = errorMessage,
        isSubmitting = isSubmitting,
        onSubmit = {
            val entered = validatedParameterValues(script.parameters, values)
            if (entered != null) onConnect(entered) else showRequiredErrors = true
        },
    )

    when (LocalAppWindowSizeClass.current) {
        AppWindowSizeClass.Compact -> CompactConnectSource(form, onSkip, onBack, modifier)
        AppWindowSizeClass.Medium, AppWindowSizeClass.Expanded -> VaultCreationSplitLayout(
            currentStep = VaultCreationStep.Sources,
            sources = sources,
            modifier = modifier,
        ) { paneModifier ->
            WideConnectSourcePane(form, onSkip, onBack, paneModifier)
        }
    }
}

/**
 * What a source's credentials form shows and edits: both layouts of
 * [ConnectSourceScreen], and the Sources tab's connect and update pages.
 * [position]/[count] are only known inside onboarding's queue. A secret in
 * [keptSecrets] is already installed, so leaving it blank keeps it.
 */
internal class ConnectSourceForm(
    val script: FydeScript,
    val values: SnapshotStateMap<String, String>,
    val showRequiredErrors: Boolean,
    val errorMessage: String?,
    val isSubmitting: Boolean,
    private val onSubmit: () -> Unit,
    val position: Int? = null,
    val count: Int? = null,
    val keptSecrets: Set<String> = emptySet(),
    /** False while submitting can't do anything yet (the Sources tab's rerun while that source runs). */
    val canSubmit: Boolean = true,
) {
    val parameters = orderedParameters(script.parameters)

    fun submit() {
        if (!isSubmitting && canSubmit) onSubmit()
    }
}

/** S3 inside onboarding. The fields scroll when the window is too short; the connect button stays pinned at the bottom. */
@Composable
private fun CompactConnectSource(
    form: ConnectSourceForm,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space6)
                    .padding(top = AppSpacing.space2)
                    .heightIn(min = AppDimensions.backButton),
            ) {
                AppBackButton(onClick = onBack, enabled = !form.isSubmitting, modifier = Modifier.align(Alignment.CenterStart))
                AppTextButton(
                    text = stringResource(Res.string.connect_source_skip),
                    onClick = onSkip,
                    enabled = !form.isSubmitting,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.space6),
            ) {
                Spacer(Modifier.height(AppSpacing.space4))
                ConnectSourceForm(form, encryptedNote = Res.string.connect_source_encrypted)
                Spacer(Modifier.height(AppSpacing.space6))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.space6)
                    .padding(top = AppSpacing.space4, bottom = AppSpacing.space5),
            ) {
                ConnectButton(form)
            }
        }
    }
}

/** DO7b's form pane: back at the top left and "Skip this source" at the top right, around the form centred in a 440dp column. */
@Composable
private fun WideConnectSourcePane(
    form: ConnectSourceForm,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.background(AppColors.background).safeContentPadding()) {
        CenteredScrollColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = AppDimensions.authFormColumn)
                    .fillMaxWidth()
                    // Clears the back button and the link above the form when the window is short.
                    .padding(horizontal = AppSpacing.space7, vertical = AppSpacing.space10 + AppSpacing.space8),
            ) {
                ConnectSourceForm(form, encryptedNote = Res.string.connect_source_encrypted_short)
                Spacer(Modifier.height(AppSpacing.space6))
                ConnectButton(form)
            }
        }
        AppBackButton(
            onClick = onBack,
            enabled = !form.isSubmitting,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = AppSpacing.space9, top = AppSpacing.space8),
        )
        AppTextButton(
            text = stringResource(Res.string.connect_source_skip),
            onClick = onSkip,
            enabled = !form.isSubmitting,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = AppSpacing.space9, top = AppSpacing.space8),
        )
    }
}

/**
 * "Source 1 of 3", the source's header, the sites it may reach, the last
 * attempt's failure, its fields and the note that they're encrypted —
 * [encryptedNote] worded for the room each layout has.
 */
@Composable
internal fun ConnectSourceForm(
    form: ConnectSourceForm,
    encryptedNote: StringResource,
) {
    val script = form.script
    val firstField = remember(script.id) { FocusRequester() }
    // Land the cursor in the first field on arrival, so the form can be
    // filled and submitted from the keyboard alone (Enter submits).
    LaunchedEffect(script.id) { if (form.parameters.isNotEmpty()) firstField.requestFocus() }

    if (form.position != null && form.count != null) {
        Text(
            text = stringResource(Res.string.connect_source_position, form.position, form.count).uppercase(),
            style = AppTypography.caption.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.04.em),
            color = AppColors.textMuted,
        )
        Spacer(Modifier.height(AppSpacing.space4))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ScriptIcon(icon = script.icon, size = AppDimensions.iconTileLarge)
        Spacer(Modifier.width(AppSpacing.space5))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = script.name,
                style = AppTypography.title,
                color = AppColors.text,
                modifier = Modifier.semantics { heading() },
            )
            val summary = script.shortDescription.ifBlank { script.description }
            if (summary.isNotBlank()) {
                Text(text = summary, style = AppTypography.body, color = AppColors.textMuted)
            }
        }
    }
    if (script.allowedDomains.isNotEmpty()) {
        Spacer(Modifier.height(AppSpacing.space5))
        AllowedDomainsNote(script.allowedDomains)
    }
    if (form.errorMessage != null) {
        Spacer(Modifier.height(AppSpacing.space5))
        AppNote(
            message = form.errorMessage,
            kind = AppNoteKind.Error,
            icon = { tint -> AlertGlyph(modifier = Modifier.size(AppIconSize.sm), tint = tint) },
        )
    }
    Spacer(Modifier.height(AppSpacing.space6))
    ConnectSourceFields(form, firstField)
    if (form.parameters.isNotEmpty()) {
        Spacer(Modifier.height(AppSpacing.space6))
        AppNote(
            message = stringResource(encryptedNote),
            kind = AppNoteKind.Ok,
            icon = { tint -> LockGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
        )
    }
}

/**
 * "Fyde will only connect to **edf.fr** and **particulier.edf.fr**, from
 * your devices.", each site in SemiBold.
 */
@Composable
private fun AllowedDomainsNote(domains: List<String>) {
    // "a", "a and b", "a, b and c": the conjunction follows the language.
    val joined = if (domains.size < 2) {
        domains.joinToString()
    } else {
        stringResource(Res.string.connect_source_list_and, domains.dropLast(1).joinToString(", "), domains.last())
    }
    val message = stringResource(Res.string.connect_source_domains, joined)
    AppNote(
        message = withDomainsEmphasized(message, domains),
        kind = AppNoteKind.Info,
        icon = { tint -> GlobeGlyph(modifier = Modifier.size(AppIconSize.md), tint = tint) },
    )
}

/**
 * One field per parameter, in [orderedParameters] order, [firstField]
 * requesting the one keyed [focusedKey] (the first by default). Enter (or the
 * soft keyboard's action key) moves to the next field, and submits from the last.
 * A required field left empty is flagged once a submission has been tried.
 */
@Composable
internal fun ConnectSourceFields(form: ConnectSourceForm, firstField: FocusRequester, focusedKey: String? = null) {
    val focusedIndex = form.parameters.indexOfFirst { it.key == focusedKey }.coerceAtLeast(0)
    val requiredError = stringResource(Res.string.script_parameter_required)
    val invalidNumberError = stringResource(Res.string.script_parameter_invalid_number)
    val secretHelp = stringResource(Res.string.connect_source_secret_help, form.script.name)
    val keptSecretHelp = "$secretHelp ${stringResource(Res.string.source_secret_keep)}"
    form.parameters.forEachIndexed { index, (key, parameter) ->
        if (index > 0) Spacer(Modifier.height(AppSpacing.space5))
        val value = form.values[key].orEmpty()
        val isLast = index == form.parameters.lastIndex
        ScriptParameterField(
            parameter = parameter,
            value = value,
            onValueChange = { form.values[key] = it },
            errorText = when {
                value.isBlank() -> requiredError.takeIf {
                    parameter.required && parameter.type != FydeScriptParameterType.BOOLEAN &&
                        key !in form.keptSecrets && form.showRequiredErrors
                }
                parameter.type == FydeScriptParameterType.NUMBER && value.toDoubleOrNull()?.isFinite() != true -> invalidNumberError
                else -> null
            },
            supportingText = when {
                key in form.keptSecrets -> keptSecretHelp
                parameter.secret -> secretHelp
                else -> null
            },
            enabled = !form.isSubmitting,
            imeAction = if (isLast) ImeAction.Go else ImeAction.Next,
            onImeAction = { if (isLast) form.submit() },
            modifier = if (index == focusedIndex) Modifier.focusRequester(firstField) else Modifier,
        )
    }
}

/** "Connect EDF" unless [text] says otherwise, spinning while the source is installed. */
@Composable
internal fun ConnectButton(form: ConnectSourceForm, text: String = stringResource(Res.string.connect_source_submit, form.script.name)) {
    AppButton(
        text = text,
        onClick = form::submit,
        enabled = form.canSubmit,
        loading = form.isSubmitting,
        loadingText = stringResource(Res.string.connect_source_submitting, form.script.name),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** [text] with each of [domains], where it appears, in SemiBold — hostnames are what the note is about. */
internal fun withDomainsEmphasized(text: String, domains: List<String>): AnnotatedString = buildAnnotatedString {
    append(text)
    var from = 0
    domains.forEach { domain ->
        val start = text.indexOf(domain, from)
        if (start < 0) return@forEach
        addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, start + domain.length)
        from = start + domain.length
    }
}
