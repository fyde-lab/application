package com.peltoche.fyde

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.auth_error_login_failed
import com.peltoche.fyde.resources.auth_error_register_failed
import com.peltoche.fyde.resources.create_script_failed
import com.peltoche.fyde.resources.disable_script_failed
import com.peltoche.fyde.resources.error_unknown
import com.peltoche.fyde.resources.language_save_failed
import com.peltoche.fyde.resources.upload_failed
import com.peltoche.fyde.resources.uploads_failed
import com.peltoche.fyde.layout.AppWindowSizeClassProvider
import com.peltoche.fyde.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** How many of the most recently cached documents to show; the screen
 * doesn't paginate yet. */
private const val DOCUMENT_LIST_LIMIT = 100L

private const val FYDE_SERVER_URL = "http://127.0.0.1:40051"

/** How often to poll [FydeSdk.isServerReachable] while the document list is open. */
private const val SERVER_REACHABILITY_POLL_INTERVAL_MS = 200L

/** Which top-level screen [App] is currently showing. */
private sealed interface Screen {
    data object Login : Screen
    data object Register : Screen
    data object Documents : Screen
    data class DocumentViewer(val document: FydeDocument) : Screen
    data object Settings : Screen
    data object Scripts : Screen
}

@Composable
@Preview
fun App() {
    AppTheme {
        AppWindowSizeClassProvider {
            var screen by remember { mutableStateOf<Screen>(Screen.Login) }
            var authError by remember { mutableStateOf<String?>(null) }
            // The device's persisted language preference (see
            // FydeSdk.setLanguage/getLanguage), or `null` if never set (in
            // which case the platform's own locale is left alone). Wrapping
            // the `when` below in `key(language)` forces every screen to be
            // torn down and rebuilt from scratch whenever this changes,
            // which is what actually makes a language change "take" -
            // Compose Multiplatform Resources has no public API to swap the
            // language it resolves against directly, it always derives it
            // from the platform's own current locale (see
            // `applyPlatformLocale`'s doc), so forcing a full rebuild is
            // what gets `stringResource` calls to notice that locale
            // changed. A side effect: navigating away from `Screen.Login`/
            // `Screen.Register` isn't possible while `Screen.Settings` (the
            // only place `language` changes) is open, so the only screen
            // that actually loses in-flight state to this is
            // `Screen.Documents`, which reconnects from scratch same as a
            // manual retry would.
            var language by remember { mutableStateOf<String?>(null) }
            var languageError by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            // Startup check: if a session token is already persisted from a
            // previous run, skip straight to the document list instead of
            // making the user log in again. Also picks up and (re-)applies
            // this device's previously chosen language preference, if any -
            // needed every launch, not just the first, since
            // applyPlatformLocale's effect doesn't itself persist across
            // process restarts (unlike the fyde-sdk preference it reads).
            LaunchedEffect(Unit) {
                FydeSdk.initialize(FYDE_SERVER_URL).onSuccess {
                    FydeSdk.getLanguage().getOrNull()?.let { savedLanguage ->
                        applyPlatformLocale(savedLanguage)
                        language = savedLanguage
                    }
                    if (FydeSdk.isConnected().getOrDefault(false)) {
                        screen = Screen.Documents
                    }
                }
            }

            key(language) {
                when (val currentScreen = screen) {
                    Screen.Login -> LoginScreen(
                        errorMessage = authError,
                        onLogIn = { username, password ->
                            authError = null
                            scope.launch {
                                authenticate(username, password, FydeSdk::login)
                                    .onSuccess { screen = Screen.Documents }
                                    .onFailure { authError = it.message ?: getString(Res.string.auth_error_login_failed) }
                            }
                        },
                        onNavigateToRegister = {
                            authError = null
                            screen = Screen.Register
                        },
                    )
                    Screen.Register -> RegisterScreen(
                        errorMessage = authError,
                        onRegister = { username, password ->
                            authError = null
                            scope.launch {
                                authenticate(username, password, FydeSdk::register)
                                    .onSuccess { screen = Screen.Documents }
                                    .onFailure { authError = it.message ?: getString(Res.string.auth_error_register_failed) }
                            }
                        },
                        onNavigateToLogin = {
                            authError = null
                            screen = Screen.Login
                        },
                    )
                    Screen.Documents -> DocumentsScreen(
                        onLogout = { screen = Screen.Login },
                        onOpenDocument = { document -> screen = Screen.DocumentViewer(document) },
                        onOpenSettings = { screen = Screen.Settings },
                        onOpenScripts = { screen = Screen.Scripts },
                    )
                    is Screen.DocumentViewer -> PdfViewerScreen(
                        document = currentScreen.document,
                        onBack = { screen = Screen.Documents },
                    )
                    Screen.Scripts -> ScriptsScreen(onBack = { screen = Screen.Documents })
                    Screen.Settings -> SettingsScreen(
                        serverUrl = FYDE_SERVER_URL,
                        currentLanguage = language,
                        onLanguageChange = { newLanguage ->
                            applyPlatformLocale(newLanguage)
                            language = newLanguage
                            languageError = null
                            scope.launch {
                                FydeSdk.setLanguage(newLanguage)
                                    .onFailure { languageError = getString(Res.string.language_save_failed, it.message ?: getString(Res.string.error_unknown)) }
                            }
                        },
                        onBack = { screen = Screen.Documents },
                        languageError = languageError,
                        onDismissLanguageError = { languageError = null },
                    )
                }
            }
        }
    }
}

/**
 * Connects to the fyde server and then runs [credentialCall] (either
 * [FydeSdk.login] or [FydeSdk.register]) against that connection, so the
 * session token it stores is set before [DocumentsScreen] opens the
 * changelog stream. [FydeSdk.initialize] is safe to call again from
 * [DocumentsScreen] afterwards — it's a no-op reconnect.
 */
private suspend fun authenticate(
    username: String,
    password: String,
    credentialCall: suspend (String, String) -> Result<Unit>,
): Result<Unit> =
    FydeSdk.initialize(FYDE_SERVER_URL).fold(
        onSuccess = { credentialCall(username, password) },
        onFailure = { Result.failure(it) },
    )

/**
 * Owns the `fyde-sdk` connection and the changelog subscription, and maps
 * their results into [DocumentListUiState]. Deliberately has no opinion on
 * how that state is drawn — see [DocumentListScreen] for the visual design,
 * which can change without touching anything in this function.
 */
@Composable
private fun DocumentsScreen(
    onLogout: () -> Unit,
    onOpenDocument: (FydeDocument) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenScripts: () -> Unit,
) {
    var retryKey by remember { mutableStateOf(0) }
    var uiState by remember { mutableStateOf<DocumentListUiState>(DocumentListUiState.Connecting) }
    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Picks one or more local files and uploads each via fyde-sdk,
    // sequentially (one FydeClient, so no concurrent calls into it). No
    // manual refresh is needed on success: each upload publishes a "created"
    // changelog event, which the FydeSdk.consume() loop below is already
    // listening for and will re-list documents in response to.
    val pickDocuments = rememberDocumentPicker { paths ->
        if (paths.isEmpty()) return@rememberDocumentPicker
        isUploading = true
        scope.launch {
            val failures = paths.mapNotNull { path -> FydeSdk.uploadDocument(path).exceptionOrNull() }
            if (failures.isNotEmpty()) {
                val current = uiState as? DocumentListUiState.Loaded
                if (current != null) {
                    val message = if (paths.size == 1) {
                        getString(Res.string.upload_failed, failures.first().message ?: getString(Res.string.error_unknown))
                    } else {
                        getString(Res.string.uploads_failed, failures.size, paths.size)
                    }
                    uiState = current.copy(
                        banner = message,
                        onDismissBanner = {
                            (uiState as? DocumentListUiState.Loaded)?.let { latest -> uiState = latest.copy(banner = null) }
                        },
                    )
                }
            }
            isUploading = false
        }
    }

    LaunchedEffect(retryKey) {
        uiState = DocumentListUiState.Connecting

        // Re-lists the cached documents every time a new changelog event
        // comes in, so the screen stays in sync with what fyde-sdk has
        // materialized locally. fyde-sdk consumes the changelog in the
        // background for the lifetime of the session (started by login/
        // register, or immediately here if one is already persisted), so
        // no separate streaming call is needed.
        val initResult = FydeSdk.initialize(FYDE_SERVER_URL) {
            scope.launch {
                val refreshed = FydeSdk.listDocuments(offset = 0, limit = DOCUMENT_LIST_LIMIT)
                    .getOrElse { (uiState as? DocumentListUiState.Loaded)?.documents ?: emptyList() }
                uiState = DocumentListUiState.Loaded(documents = refreshed)
            }
        }
        if (initResult.isFailure) {
            uiState = DocumentListUiState.ConnectFailed(
                message = initResult.exceptionOrNull()?.message ?: getString(Res.string.error_unknown),
                onRetry = { retryKey++ },
            )
            return@LaunchedEffect
        }

        uiState = DocumentListUiState.Loaded(documents = null)
        uiState = DocumentListUiState.Loaded(
            documents = FydeSdk.listDocuments(offset = 0, limit = DOCUMENT_LIST_LIMIT).getOrElse { emptyList() },
        )
    }

    // Polls fyde-sdk's own reachability check independently of the changelog
    // stream above, so the upload button reflects whether calls into the SDK
    // are currently expected to succeed rather than just whether that one
    // stream happened to drop.
    LaunchedEffect(retryKey) {
        while (true) {
            val reachable = FydeSdk.isServerReachable().getOrDefault(false)
            (uiState as? DocumentListUiState.Loaded)?.let { current ->
                uiState = current.copy(serverReachable = reachable)
            }
            delay(SERVER_REACHABILITY_POLL_INTERVAL_MS)
        }
    }

    DocumentListScreen(
        state = uiState,
        onUpload = pickDocuments,
        isUploading = isUploading,
        onLogout = onLogout,
        onOpenDocument = onOpenDocument,
        onOpenSettings = onOpenSettings,
        onOpenScripts = onOpenScripts,
    )
}

/**
 * Owns the `fyde-sdk` scripts calls and maps their results into
 * [ScriptListUiState], mirroring how [DocumentsScreen] owns the documents
 * calls for [DocumentListScreen]. A newly created script is enabled
 * immediately (`create_script` alone doesn't enable it — see
 * `sdk/src/domains/scripts/mod.rs`'s `Service` trait), so it shows up in the
 * list, which is scoped to enabled scripts only.
 */
@Composable
private fun ScriptsScreen(onBack: () -> Unit) {
    var retryKey by remember { mutableStateOf(0) }
    var uiState by remember { mutableStateOf<ScriptListUiState>(ScriptListUiState.Connecting) }
    var isCreating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refreshScripts() {
        val refreshed = FydeSdk.listUserScripts().getOrElse { (uiState as? ScriptListUiState.Loaded)?.scripts ?: emptyList() }
        uiState = ScriptListUiState.Loaded(scripts = refreshed)
    }

    LaunchedEffect(retryKey) {
        uiState = ScriptListUiState.Connecting
        if (!FydeSdk.isServerReachable().getOrDefault(false)) {
            uiState = ScriptListUiState.ConnectFailed(
                message = getString(Res.string.error_unknown),
                onRetry = { retryKey++ },
            )
            return@LaunchedEffect
        }
        uiState = ScriptListUiState.Loaded(scripts = null)
        refreshScripts()
    }

    ScriptListScreen(
        state = uiState,
        onBack = onBack,
        isCreating = isCreating,
        onCreateScript = { name, isPublic, script ->
            isCreating = true
            scope.launch {
                FydeSdk.createScript(name = name, isPublic = isPublic, icon = ByteArray(0), script = script)
                    .mapCatching { created -> FydeSdk.enableScript(created.id).getOrThrow() }
                    .onSuccess { refreshScripts() }
                    .onFailure { error ->
                        val current = uiState as? ScriptListUiState.Loaded
                        if (current != null) {
                            val message = getString(Res.string.create_script_failed, error.message ?: getString(Res.string.error_unknown))
                            uiState = current.copy(
                                banner = message,
                                onDismissBanner = { (uiState as? ScriptListUiState.Loaded)?.let { latest -> uiState = latest.copy(banner = null) } },
                            )
                        }
                    }
                isCreating = false
            }
        },
        onDisableScript = { script ->
            scope.launch {
                FydeSdk.disableScript(script.id)
                    .onSuccess { refreshScripts() }
                    .onFailure { error ->
                        val current = uiState as? ScriptListUiState.Loaded
                        if (current != null) {
                            val message = getString(Res.string.disable_script_failed, error.message ?: getString(Res.string.error_unknown))
                            uiState = current.copy(
                                banner = message,
                                onDismissBanner = { (uiState as? ScriptListUiState.Loaded)?.let { latest -> uiState = latest.copy(banner = null) } },
                            )
                        }
                    }
            }
        },
    )
}
