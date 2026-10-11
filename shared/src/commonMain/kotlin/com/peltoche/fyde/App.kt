package com.peltoche.fyde

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.addon_installed
import com.peltoche.fyde.resources.addon_toggle_failed
import com.peltoche.fyde.resources.addon_uninstalled
import com.peltoche.fyde.resources.addon_updated
import com.peltoche.fyde.resources.auth_error_login_failed
import com.peltoche.fyde.resources.auth_error_register_failed
import com.peltoche.fyde.resources.create_script_failed
import com.peltoche.fyde.resources.document_save_failed
import com.peltoche.fyde.resources.document_saved
import com.peltoche.fyde.resources.error_unknown
import com.peltoche.fyde.resources.language_save_failed
import com.peltoche.fyde.resources.log_out_failed
import com.peltoche.fyde.resources.script_created
import com.peltoche.fyde.resources.upload_failed
import com.peltoche.fyde.resources.uploads_failed
import com.peltoche.fyde.resources.uploads_succeeded
import com.peltoche.fyde.layout.AppWindowSizeClassProvider
import com.peltoche.fyde.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/**
 * How many cached documents [listAllDocuments] fetches per call. The list
 * itself isn't capped: pages are fetched until one comes back short.
 */
private const val DOCUMENT_PAGE_SIZE = 100L

private const val FYDE_SERVER_URL = "http://127.0.0.1:40051"

/**
 * How often to poll [FydeSdk.isServerReachable] while the document list is
 * open. Found via review: it used to poll every 200ms — five calls a second,
 * for a banner that only needs to notice an outage within a few seconds,
 * which on a phone is battery spent for nothing.
 */
private const val SERVER_REACHABILITY_POLL_INTERVAL_MS = 5_000L

/** Which top-level screen [App] is currently showing. */
private sealed interface Screen {
    data object Login : Screen
    data object Register : Screen
    data object Documents : Screen
    data object Settings : Screen
    data object Addons : Screen
}

@Composable
@Preview
fun App() {
    AppTheme {
        AppWindowSizeClassProvider {
            // One host for every screen's transient confirmations, above the
            // screens themselves so a message outlives the screen raising it.
            val snackbarHostState = remember { SnackbarHostState() }
            CompositionLocalProvider(LocalAppSnackbarHostState provides snackbarHostState) {
                AppContent()
            }
        }
    }
}

@Composable
private fun AppContent() {
    var screen by remember { mutableStateOf<Screen>(Screen.Login) }
    var authError by remember { mutableStateOf<String?>(null) }
    // Whether a login/register call is in flight: OPAQUE's key stretching
    // (Argon2id, 64 MiB) takes noticeable time, longest on a phone, so the
    // form shows it's working and can't be submitted a second time.
    var isAuthenticating by remember { mutableStateOf(false) }
    // The device's persisted language preference (see
    // FydeSdk.setLanguage/getLanguage), or `null` if never set (in which case
    // the platform's own locale is left alone). Wrapping the `when` below in
    // `key(language)` forces every screen to be torn down and rebuilt from
    // scratch whenever this changes, which is what actually makes a language
    // change "take" - Compose Multiplatform Resources has no public API to
    // swap the language it resolves against directly, it always derives it
    // from the platform's own current locale (see `applyPlatformLocale`'s
    // doc), so forcing a full rebuild is what gets `stringResource` calls to
    // notice that locale changed.
    var language by remember { mutableStateOf<String?>(null) }
    var languageError by remember { mutableStateOf<String?>(null) }
    var isLoggingOut by remember { mutableStateOf(false) }
    var logoutError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalAppSnackbarHostState.current
    // App-lifetime, so a scraper run outlives the screen that started it.
    val scraperRuns = remember { ScraperRuns(scope, snackbarHostState) }
    DisposableEffect(scraperRuns) {
        FydeSdk.setScraperHandler(scraperRuns)
        onDispose { FydeSdk.setScraperHandler(null) }
    }

    // Startup check: if a session token is already persisted from a previous
    // run, skip straight to the document list instead of making the user log
    // in again. Also picks up and (re-)applies this device's previously
    // chosen language preference, if any - needed every launch, not just the
    // first, since applyPlatformLocale's effect doesn't itself persist across
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

    fun authenticateWith(credentialCall: suspend (String, String) -> Result<Unit>, fallback: StringResource) =
        { username: String, password: String ->
            authError = null
            isAuthenticating = true
            scope.launch {
                authenticate(username, password, credentialCall)
                    .onSuccess { screen = Screen.Documents }
                    .onFailure { authError = it.userMessage(fallback) }
                isAuthenticating = false
            }
            Unit
        }

    key(language) {
        when (screen) {
            Screen.Login -> LoginScreen(
                errorMessage = authError,
                isSubmitting = isAuthenticating,
                onLogIn = authenticateWith(FydeSdk::login, Res.string.auth_error_login_failed),
                onNavigateToRegister = {
                    authError = null
                    screen = Screen.Register
                },
            )
            Screen.Register -> RegisterScreen(
                errorMessage = authError,
                isSubmitting = isAuthenticating,
                onRegister = authenticateWith(FydeSdk::register, Res.string.auth_error_register_failed),
                onNavigateToLogin = {
                    authError = null
                    screen = Screen.Login
                },
            )
            Screen.Documents -> DocumentsScreen(
                onOpenSettings = { screen = Screen.Settings },
                onOpenAddons = { screen = Screen.Addons },
            )
            Screen.Settings -> SettingsScreen(
                serverUrl = FYDE_SERVER_URL,
                currentLanguage = language,
                onLanguageChange = { newLanguage ->
                    applyPlatformLocale(newLanguage)
                    language = newLanguage
                    languageError = null
                    scope.launch {
                        FydeSdk.setLanguage(newLanguage)
                            .onFailure { languageError = getString(Res.string.language_save_failed, it.userMessage()) }
                    }
                },
                onOpenDocuments = { screen = Screen.Documents },
                onOpenAddons = { screen = Screen.Addons },
                // Found via review: this used to only navigate back to Login,
                // leaving the session token, the master key and the decrypted
                // document cache on the device — so a relaunch logged straight
                // back in, and logging in as anyone else failed.
                onLogout = {
                    logoutError = null
                    isLoggingOut = true
                    scope.launch {
                        FydeSdk.logout()
                            .onSuccess {
                                authError = null
                                screen = Screen.Login
                            }
                            .onFailure { logoutError = getString(Res.string.log_out_failed, it.userMessage()) }
                        isLoggingOut = false
                    }
                },
                isLoggingOut = isLoggingOut,
                logoutError = logoutError,
                onDismissLogoutError = { logoutError = null },
                languageError = languageError,
                onDismissLanguageError = { languageError = null },
            )
            Screen.Addons -> AddonsScreenContainer(
                scraperRuns = scraperRuns,
                onOpenDocuments = { screen = Screen.Documents },
                onOpenSettings = { screen = Screen.Settings },
            )
        }

        // Above whichever screen is showing: a running script can ask its
        // question after the user has navigated away from its page.
        scraperRuns.question?.let { pending ->
            ScraperQuestionDialog(
                scriptName = scraperRuns.running?.name.orEmpty(),
                question = pending.text,
                onAnswer = scraperRuns::answer,
            )
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
 * Every document cached locally, newest first. [FydeSdk.listDocuments]
 * pages oldest-first, so the screen's old single 100-document page silently
 * showed the *oldest* 100 and hid every newer one; this fetches every page.
 */
private suspend fun listAllDocuments(): Result<List<FydeDocument>> = runCatching {
    buildList {
        var offset = 0L
        do {
            val page = FydeSdk.listDocuments(offset = offset, limit = DOCUMENT_PAGE_SIZE).getOrThrow()
            addAll(page)
            offset += page.size
        } while (page.size.toLong() == DOCUMENT_PAGE_SIZE)
    }.sortedByDescending { it.createdAt }
}

/**
 * Owns the `fyde-sdk` connection and the changelog subscription, and maps
 * their results into [DocumentListUiState]. Deliberately has no opinion on
 * how that state is drawn — see [DocumentListScreen] for the visual design,
 * which can change without touching anything in this function.
 */
@Composable
private fun DocumentsScreen(
    onOpenSettings: () -> Unit,
    onOpenAddons: () -> Unit,
) {
    var retryKey by remember { mutableStateOf(0) }
    var uiState by remember { mutableStateOf<DocumentListUiState>(DocumentListUiState.Connecting) }
    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalAppSnackbarHostState.current

    fun showBanner(message: String) {
        val current = uiState as? DocumentListUiState.Loaded ?: return
        uiState = current.copy(
            banner = message,
            onDismissBanner = { (uiState as? DocumentListUiState.Loaded)?.let { latest -> uiState = latest.copy(banner = null) } },
        )
    }

    // Picks one or more local files and uploads each via fyde-sdk,
    // sequentially (one FydeClient, so no concurrent calls into it). No
    // manual refresh is needed on success: each upload publishes a "created"
    // changelog event, which the initialize() callback below is already
    // listening for and will re-list documents in response to.
    val pickDocuments = rememberDocumentPicker { paths ->
        if (paths.isEmpty()) return@rememberDocumentPicker
        isUploading = true
        scope.launch {
            val failures = paths.mapNotNull { path -> FydeSdk.uploadDocument(path).exceptionOrNull() }
            val succeeded = paths.size - failures.size
            if (failures.isNotEmpty()) {
                showBanner(
                    if (paths.size == 1) {
                        getString(Res.string.upload_failed, failures.first().userMessage())
                    } else {
                        getString(Res.string.uploads_failed, failures.size, paths.size)
                    },
                )
            }
            isUploading = false
            if (succeeded > 0) snackbarHostState.showSnackbar(getPluralString(Res.plurals.uploads_succeeded, succeeded, succeeded))
        }
    }

    val saveDocument = rememberDocumentSaver { fileName, saved ->
        if (saved == null) return@rememberDocumentSaver
        scope.launch {
            if (saved) {
                snackbarHostState.showSnackbar(getString(Res.string.document_saved, fileName))
            } else {
                showBanner(getString(Res.string.document_save_failed, fileName))
            }
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
                val current = uiState as? DocumentListUiState.Loaded
                val refreshed = listAllDocuments().getOrElse { current?.documents ?: emptyList() }
                uiState = current?.copy(documents = refreshed) ?: DocumentListUiState.Loaded(documents = refreshed)
            }
        }
        if (initResult.isFailure) {
            uiState = DocumentListUiState.ConnectFailed(
                message = initResult.exceptionOrNull()?.userMessage() ?: getString(Res.string.error_unknown),
                onRetry = { retryKey++ },
            )
            return@LaunchedEffect
        }

        uiState = DocumentListUiState.Loaded(documents = null)
        val documents = listAllDocuments().getOrElse { emptyList() }
        uiState = (uiState as? DocumentListUiState.Loaded)?.copy(documents = documents) ?: DocumentListUiState.Loaded(documents)
    }

    // Polls fyde-sdk's own reachability check independently of the changelog
    // stream above, so the upload button and the offline banner reflect
    // whether calls into the SDK are currently expected to succeed. Only
    // polls once loaded, the only state that shows it, so a failed
    // connection doesn't log a failure every tick.
    LaunchedEffect(retryKey) {
        while (true) {
            if (uiState is DocumentListUiState.Loaded) {
                val reachable = FydeSdk.isServerReachable().getOrDefault(false)
                (uiState as? DocumentListUiState.Loaded)?.let { current ->
                    if (current.serverReachable != reachable) uiState = current.copy(serverReachable = reachable)
                }
            }
            delay(SERVER_REACHABILITY_POLL_INTERVAL_MS)
        }
    }

    DocumentListScreen(
        state = uiState,
        onUpload = pickDocuments,
        isUploading = isUploading,
        onDownload = { document -> saveDocument(document.name, document.content) },
        onOpenSettings = onOpenSettings,
        onOpenAddons = onOpenAddons,
    )
}

/**
 * Owns the `fyde-sdk` scripts calls backing [AddonsScreen] and
 * [AddonDetailScreen]. The grid shows every public script plus the user's
 * own enabled private ones ([FydeSdk.listPublicScripts] +
 * [FydeSdk.listUserScripts]); the enabled subset is what's "installed", and
 * [FydeSdk.installedScriptParameters] supplies the values each was
 * installed with. Unlike [DocumentsScreen], there's no changelog-style live
 * subscription for scripts, so the listing is fetched once per screen open
 * (plus [retryKey] bumps) rather than kept in sync by a background stream.
 */
@Composable
private fun AddonsScreenContainer(
    scraperRuns: ScraperRuns,
    onOpenDocuments: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var retryKey by remember { mutableStateOf(0) }
    var uiState by remember { mutableStateOf<AddonsUiState>(AddonsUiState.Connecting) }
    var isCreating by remember { mutableStateOf(false) }
    // Which script's [AddonDetailScreen] is open, if any — a drill-down
    // local to this container rather than a top-level Screen, since the
    // detail view shares the same scripts/enabled/pending state this
    // container already owns.
    var viewedScriptId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalAppSnackbarHostState.current

    fun showBanner(message: String) {
        val current = uiState as? AddonsUiState.Loaded ?: return
        uiState = current.copy(
            banner = message,
            onDismissBanner = { (uiState as? AddonsUiState.Loaded)?.let { loaded -> uiState = loaded.copy(banner = null) } },
        )
    }

    LaunchedEffect(retryKey) {
        uiState = AddonsUiState.Connecting

        val initResult = FydeSdk.initialize(FYDE_SERVER_URL)
        if (initResult.isFailure) {
            uiState = AddonsUiState.ConnectFailed(
                message = initResult.exceptionOrNull()?.userMessage() ?: getString(Res.string.error_unknown),
                onRetry = { retryKey++ },
            )
            return@LaunchedEffect
        }

        uiState = AddonsUiState.Loaded(scripts = null)
        val publicScripts = FydeSdk.listPublicScripts().getOrElse { emptyList() }
        val userScripts = FydeSdk.listUserScripts().getOrElse { emptyList() }
        uiState = AddonsUiState.Loaded(
            scripts = (userScripts + publicScripts).distinctBy { it.id },
            enabledScriptIds = userScripts.map { it.id }.toSet(),
            installedParameters = FydeSdk.installedScriptParameters().getOrElse { emptyMap() },
        )
    }

    // Installing publishes [parameters] through the changelog (so the script
    // and the values it runs with sync to every device on the account) and
    // then enables the script, which is what the installed/available split
    // reads back; installing an already-installed script again replaces its
    // values (Save changes). Uninstalling only disables it: fyde-sdk has no
    // uninstall event yet.
    fun setInstalled(
        script: FydeScript,
        enabled: Boolean,
        parameters: Map<String, FydeScriptParameterValue> = emptyMap(),
    ) {
        val current = uiState as? AddonsUiState.Loaded
        if (current == null || script.id in current.pendingScriptIds) return
        val wasInstalled = script.id in current.enabledScriptIds
        uiState = current.copy(pendingScriptIds = current.pendingScriptIds + script.id)
        scope.launch {
            val result = if (enabled) {
                FydeSdk.installScript(script.id, parameters)
                    .mapCatching { FydeSdk.enableScript(script.id).getOrThrow() }
            } else {
                FydeSdk.disableScript(script.id)
            }
            val latest = uiState as? AddonsUiState.Loaded ?: return@launch
            val error = result.exceptionOrNull()
            if (error == null) {
                uiState = latest.copy(
                    enabledScriptIds = if (enabled) latest.enabledScriptIds + script.id else latest.enabledScriptIds - script.id,
                    installedParameters = if (enabled) latest.installedParameters + (script.id to parameters) else latest.installedParameters,
                    pendingScriptIds = latest.pendingScriptIds - script.id,
                )
                val confirmation = when {
                    !enabled -> Res.string.addon_uninstalled
                    wasInstalled -> Res.string.addon_updated
                    else -> Res.string.addon_installed
                }
                snackbarHostState.showSnackbar(getString(confirmation, script.name))
            } else {
                uiState = latest.copy(pendingScriptIds = latest.pendingScriptIds - script.id)
                showBanner(getString(Res.string.addon_toggle_failed, script.name, error.userMessage()))
            }
        }
    }

    // A newly created script is a parser (see FydeSdk.createScript), enabled
    // immediately (`create_script` alone doesn't — see `sdk/src/domains/
    // scripts/mod.rs`'s `Service` trait) so it classifies the next uploads
    // and lands in the Installed section. A parser takes no parameters, so
    // there's no installation to publish.
    fun createScript(name: String, isPublic: Boolean, script: String) {
        isCreating = true
        scope.launch {
            FydeSdk.createScript(
                name = name,
                isPublic = isPublic,
                icon = ByteArray(0),
                script = script,
                description = "",
                shortDescription = "",
                allowedDomains = emptyList(),
            )
                .mapCatching { created -> created.also { FydeSdk.enableScript(it.id).getOrThrow() } }
                .onSuccess { created ->
                    (uiState as? AddonsUiState.Loaded)?.let { latest ->
                        uiState = latest.copy(
                            scripts = listOf(created) + latest.scripts.orEmpty().filter { it.id != created.id },
                            enabledScriptIds = latest.enabledScriptIds + created.id,
                        )
                    }
                    isCreating = false
                    snackbarHostState.showSnackbar(getString(Res.string.script_created, created.name))
                }
                .onFailure { error ->
                    isCreating = false
                    showBanner(getString(Res.string.create_script_failed, error.userMessage()))
                }
        }
    }

    val loaded = uiState as? AddonsUiState.Loaded
    val viewedScript = viewedScriptId?.let { id -> loaded?.scripts?.find { it.id == id } }

    if (loaded != null && viewedScript != null) {
        AddonDetailScreen(
            script = viewedScript,
            isInstalled = viewedScript.id in loaded.enabledScriptIds,
            isPending = viewedScript.id in loaded.pendingScriptIds,
            installedParameters = loaded.installedParameters[viewedScript.id],
            // A failure triggered from here is shown here; it's cleared on
            // the way back so the grid doesn't re-announce one already seen.
            banner = loaded.banner,
            onDismissBanner = loaded.onDismissBanner,
            onBack = {
                viewedScriptId = null
                (uiState as? AddonsUiState.Loaded)?.let { latest -> uiState = latest.copy(banner = null) }
            },
            onInstall = { parameters -> setInstalled(viewedScript, true, parameters) },
            onUninstall = { setInstalled(viewedScript, false) },
            run = AddonRunState(
                isRunning = scraperRuns.running?.id == viewedScript.id,
                canRun = scraperRuns.running == null,
                step = scraperRuns.step,
                update = scraperRuns.update,
                failure = scraperRuns.failure?.takeIf { it.first == viewedScript.id }?.second,
            ),
            onRun = { scraperRuns.run(viewedScript) },
            onDismissRunFailure = scraperRuns::dismissFailure,
            onOpenDocuments = onOpenDocuments,
            onOpenSettings = onOpenSettings,
        )
    } else {
        AddonsScreen(
            state = uiState,
            onOpenDocuments = onOpenDocuments,
            onOpenSettings = onOpenSettings,
            onView = { script -> viewedScriptId = script.id },
            onCreateScript = ::createScript,
            isCreating = isCreating,
            runningScriptId = scraperRuns.running?.id,
        )
    }
}
