package com.peltoche.fyde

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.script_installed
import com.peltoche.fyde.resources.script_toggle_failed
import com.peltoche.fyde.resources.script_uninstalled
import com.peltoche.fyde.resources.script_updated
import com.peltoche.fyde.resources.auth_error_login_failed
import com.peltoche.fyde.resources.session_limit_failed
import com.peltoche.fyde.resources.auth_error_register_failed
import com.peltoche.fyde.resources.document_save_failed
import com.peltoche.fyde.resources.document_saved
import com.peltoche.fyde.resources.error_unknown
import com.peltoche.fyde.resources.language_save_failed
import com.peltoche.fyde.resources.log_out_failed
import com.peltoche.fyde.resources.search_failed
import com.peltoche.fyde.resources.session_expired
import com.peltoche.fyde.resources.sessions_disconnect_failed
import com.peltoche.fyde.resources.sessions_disconnected
import com.peltoche.fyde.resources.sessions_load_failed
import com.peltoche.fyde.resources.upload_failed
import com.peltoche.fyde.resources.uploads_failed
import com.peltoche.fyde.resources.uploads_succeeded
import com.peltoche.fyde.resources.usage_load_failed
import com.peltoche.fyde.layout.AppWindowSizeClassProvider
import com.peltoche.fyde.theme.AppAppearance
import com.peltoche.fyde.theme.AppTheme
import com.peltoche.fyde.theme.AppThemeOption
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/**
 * How many cached documents [listAllDocuments] fetches per call. The list
 * itself isn't capped: pages are fetched until one comes back short.
 */
private const val DOCUMENT_PAGE_SIZE = 100L

/**
 * How often to poll [FydeSdk.isServerReachable] while the document list is
 * open. Found via review: it used to poll every 200ms — five calls a second,
 * for a banner that only needs to notice an outage within a few seconds,
 * which on a phone is battery spent for nothing.
 */
private const val SERVER_REACHABILITY_POLL_INTERVAL_MS = 5_000L

/**
 * How often [FydeSdk.isConnected] is checked while a signed-in screen is
 * open, to notice fyde-sdk logging out on its own because its background
 * changelog sync found the session rejected — no app call fails then, so
 * [SessionExpiry] never hears of it. A local credential-store read, at the
 * same pace as [SERVER_REACHABILITY_POLL_INTERVAL_MS].
 */
private const val SESSION_CHECK_INTERVAL_MS = 5_000L

/**
 * How long typing in the search field must pause before it's searched, so a
 * word isn't searched once per letter. Filter picks aren't delayed.
 */
private const val SEARCH_DEBOUNCE_MS = 250L

/**
 * How many [FydeSdk.fetchIcon] calls [ScriptsScreenContainer] keeps in
 * flight at once: the listings leave icons out, so every script in the grid
 * needs its own call, and over a hundred at once would queue behind each
 * other on the one server connection anyway.
 */
private const val SCRIPT_ICON_FETCH_CONCURRENCY = 8

/**
 * Every script icon [ScriptsScreenContainer] has fetched, keyed by icon id
 * (an icon never changes once created), kept for the whole process so
 * reopening the Scripts screen doesn't decode them from the SDK's cache
 * again. Only touched from the UI thread.
 */
private val scriptIcons = mutableMapOf<String, ByteArray>()

/** Which top-level screen [App] is currently showing. */
private sealed interface Screen {
    /** No account open on this device: [WelcomeScreen], leading to [Register] or [Login]. */
    data object Welcome : Screen
    data object Login : Screen
    data object Register : Screen

    /**
     * The credentials checked out, but the account is at its plan's session
     * limit: [SessionLimitScreen] lists its [sessions] so the user picks one
     * to disconnect. Holds the credentials to rerun the login with, since
     * the server's login exchange is single-use — dropped again as soon as
     * the user leaves this screen. A plain class, not a data class, so the
     * password never ends up in a generated `toString`.
     */
    class SessionLimit(
        val username: String,
        val password: String,
        val allowInsecureStorage: Boolean,
        val maxSessions: Int,
        val sessions: List<FydeOpenSession>,
    ) : Screen

    data object Documents : Screen
    data object Settings : Screen
    data object Scripts : Screen
}

/** Whether [this] is shown with no session open, before signing in. */
private val Screen.isSignedOut: Boolean
    get() = this == Screen.Welcome || this == Screen.Login || this == Screen.Register || this is Screen.SessionLimit

@Composable
@Preview
fun App() {
    // The theme and light/dark appearance picked in Settings, held here rather
    // than in AppContent since AppTheme has to wrap every screen. Not persisted
    // yet: fyde-sdk has no preference for them (unlike the language).
    var theme by remember { mutableStateOf(AppThemeOption.Default) }
    var appearance by remember { mutableStateOf(AppAppearance.Default) }
    AppTheme(theme = theme, darkTheme = appearance.isDark()) {
        AppWindowSizeClassProvider {
            // One host for every screen's transient confirmations, above the
            // screens themselves so a message outlives the screen raising it.
            val snackbarHostState = remember { SnackbarHostState() }
            CompositionLocalProvider(LocalAppSnackbarHostState provides snackbarHostState) {
                AppContent(
                    theme = theme,
                    onThemeChange = { theme = it },
                    appearance = appearance,
                    onAppearanceChange = { appearance = it },
                )
            }
        }
    }
}

@Composable
private fun AppContent(
    theme: AppThemeOption,
    onThemeChange: (AppThemeOption) -> Unit,
    appearance: AppAppearance,
    onAppearanceChange: (AppAppearance) -> Unit,
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Welcome) }
    var authError by remember { mutableStateOf<String?>(null) }
    // Why LoginScreen opened by itself (the session ended), shown as a
    // warning rather than as an error: nothing the user did failed.
    var sessionNotice by remember { mutableStateOf<String?>(null) }
    // Whether a login/register call is in flight: OPAQUE's key stretching
    // (Argon2id, 64 MiB) takes noticeable time, longest on a phone, so the
    // form shows it's working and can't be submitted a second time.
    var isAuthenticating by remember { mutableStateOf(false) }
    // Whether this device has secure storage for the account's keys - when it
    // doesn't, RegisterScreen asks the user to accept keeping them in the
    // local database instead. Assumed available until checked, so the
    // checkbox never flashes on a device that doesn't need it.
    var isSecureStorageAvailable by remember { mutableStateOf(true) }
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
    var planUsage by remember { mutableStateOf<PlanUsageUiState>(PlanUsageUiState.Loading) }
    var planUsageAttempt by remember { mutableStateOf(0) }
    var sessions by remember { mutableStateOf<SessionsUiState>(SessionsUiState.Loading) }
    var sessionsAttempt by remember { mutableStateOf(0) }
    var disconnectingSessionId by remember { mutableStateOf<String?>(null) }
    var disconnectSessionError by remember { mutableStateOf<String?>(null) }
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
        FydeSdk.initialize(BuildConfig.SERVER_URL).onSuccess {
            FydeSdk.getLanguage().getOrNull()?.let { savedLanguage ->
                applyPlatformLocale(savedLanguage)
                language = savedLanguage
            }
            if (FydeSdk.isConnected().getOrDefault(false)) {
                screen = Screen.Documents
            }
        }
    }

    // Shows SessionLimitScreen instead of signing in when the account is at
    // its plan's session limit; [replaceSessionId] is the session picked
    // there to disconnect in favor of this device.
    fun logIn(username: String, password: String, allowInsecureStorage: Boolean, replaceSessionId: String? = null) {
        authError = null
        sessionNotice = null
        isAuthenticating = true
        scope.launch {
            authenticate(username, password) { user, pass ->
                FydeSdk.login(user, pass, allowInsecureStorage, replaceSessionId)
            }
                .onSuccess { outcome ->
                    screen = when (outcome) {
                        FydeLoginOutcome.LoggedIn -> Screen.Documents
                        is FydeLoginOutcome.SessionLimitReached -> Screen.SessionLimit(
                            username = username,
                            password = password,
                            allowInsecureStorage = allowInsecureStorage,
                            maxSessions = outcome.maxSessions,
                            sessions = outcome.sessions,
                        )
                    }
                }
                .onFailure {
                    val fallback = if (replaceSessionId == null) {
                        Res.string.auth_error_login_failed
                    } else {
                        Res.string.session_limit_failed
                    }
                    authError = it.userMessage(fallback)
                }
            isAuthenticating = false
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

    // fyde-sdk logs out by itself once the server rejects the session token
    // (expired, revoked, ...): back to the login form, saying why, from
    // whichever signed-in screen was open.
    suspend fun returnToLogin(message: String?) {
        if (screen.isSignedOut) return
        authError = null
        sessionNotice = message ?: getString(Res.string.session_expired)
        screen = Screen.Login
    }

    // A call that failed because the session was rejected...
    LaunchedEffect(Unit) {
        SessionExpiry.events.collect { message -> returnToLogin(message) }
    }

    // ...or the session gone without any call failing (see
    // SESSION_CHECK_INTERVAL_MS). Not while logging out from Settings, which
    // clears it on purpose and navigates by itself.
    LaunchedEffect(screen) {
        if (screen.isSignedOut) return@LaunchedEffect
        while (true) {
            delay(SESSION_CHECK_INTERVAL_MS)
            if (!isLoggingOut && FydeSdk.isConnected().getOrNull() == false) {
                returnToLogin(null)
                return@LaunchedEffect
            }
        }
    }

    // Re-checked every time the login or registration form opens, so a
    // keyring started or unlocked in the meantime is picked up; both offer
    // the local-database fallback when there's none.
    LaunchedEffect(screen) {
        if (screen == Screen.Register || screen == Screen.Login) {
            isSecureStorageAvailable = FydeSdk.initialize(BuildConfig.SERVER_URL)
                .mapCatching { FydeSdk.isSecureStorageAvailable().getOrThrow() }
                .getOrDefault(true)
        }
    }

    suspend fun fetchPlanUsage(): PlanUsageUiState = FydeSdk.getUsage().fold(
        onSuccess = { PlanUsageUiState.Loaded(it) },
        onFailure = {
            PlanUsageUiState.Failed(
                message = getString(Res.string.usage_load_failed, it.userMessage()),
                onRetry = { planUsageAttempt++ },
            )
        },
    )

    suspend fun fetchSessions(): SessionsUiState = FydeSdk.listSessions().fold(
        onSuccess = { SessionsUiState.Loaded(it) },
        onFailure = {
            SessionsUiState.Failed(
                message = getString(Res.string.sessions_load_failed, it.userMessage()),
                onRetry = { sessionsAttempt++ },
            )
        },
    )

    // Re-fetched every time Settings opens (and on retry): the account's
    // other devices change these figures, so a cached copy would go stale.
    LaunchedEffect(screen, planUsageAttempt) {
        if (screen != Screen.Settings) return@LaunchedEffect
        planUsage = PlanUsageUiState.Loading
        planUsage = fetchPlanUsage()
    }

    // Same for the devices list, which other devices change by connecting or
    // disconnecting.
    LaunchedEffect(screen, sessionsAttempt) {
        if (screen != Screen.Settings) return@LaunchedEffect
        disconnectSessionError = null
        sessions = SessionsUiState.Loading
        sessions = fetchSessions()
    }

    key(language) {
        when (screen) {
            Screen.Welcome -> WelcomeScreen(
                onCreateVault = { screen = Screen.Register },
                onOpenVault = { screen = Screen.Login },
            )
            Screen.Login -> LoginScreen(
                noticeMessage = sessionNotice,
                errorMessage = authError,
                isSubmitting = isAuthenticating,
                isSecureStorageAvailable = isSecureStorageAvailable,
                onLogIn = { username, password, allowInsecureStorage ->
                    logIn(username, password, allowInsecureStorage)
                },
                onNavigateToRegister = {
                    authError = null
                    sessionNotice = null
                    screen = Screen.Register
                },
                onBack = {
                    authError = null
                    sessionNotice = null
                    screen = Screen.Welcome
                },
            )
            is Screen.SessionLimit -> {
                val limit = screen as Screen.SessionLimit
                SessionLimitScreen(
                    maxSessions = limit.maxSessions,
                    sessions = limit.sessions,
                    errorMessage = authError,
                    isSubmitting = isAuthenticating,
                    onReplace = { sessionId ->
                        logIn(limit.username, limit.password, limit.allowInsecureStorage, sessionId)
                    },
                    onCancel = {
                        authError = null
                        screen = Screen.Login
                    },
                )
            }
            Screen.Register -> RegisterScreen(
                errorMessage = authError,
                isSubmitting = isAuthenticating,
                isSecureStorageAvailable = isSecureStorageAvailable,
                onRegister = { username, password, allowInsecureStorage ->
                    authenticateWith(
                        { user, pass -> FydeSdk.register(user, pass, allowInsecureStorage) },
                        Res.string.auth_error_register_failed,
                    )(username, password)
                },
                onNavigateToLogin = {
                    authError = null
                    screen = Screen.Login
                },
            )
            Screen.Documents -> DocumentsScreen(
                onOpenSettings = { screen = Screen.Settings },
                onOpenScripts = { screen = Screen.Scripts },
            )
            Screen.Settings -> SettingsScreen(
                currentTheme = theme,
                onThemeChange = onThemeChange,
                currentAppearance = appearance,
                onAppearanceChange = onAppearanceChange,
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
                onOpenScripts = { screen = Screen.Scripts },
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
                                // Logging out wipes this device's account, so it's back to Welcome.
                                screen = Screen.Welcome
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
                planUsage = planUsage,
                sessions = sessions,
                // Refreshes the list and the plan's device count in place,
                // without going back through their loading skeletons.
                onDisconnectSession = { session ->
                    disconnectSessionError = null
                    disconnectingSessionId = session.id
                    scope.launch {
                        val revoked = FydeSdk.revokeSession(session.id)
                            .onFailure {
                                disconnectSessionError = getString(Res.string.sessions_disconnect_failed, it.userMessage())
                            }
                        sessions = fetchSessions()
                        planUsage = fetchPlanUsage()
                        disconnectingSessionId = null
                        // Last: showSnackbar suspends until the message is gone.
                        if (revoked.isSuccess) {
                            snackbarHostState.showSnackbar(getString(Res.string.sessions_disconnected, session.deviceName))
                        }
                    }
                },
                disconnectingSessionId = disconnectingSessionId,
                disconnectSessionError = disconnectSessionError,
                onDismissDisconnectSessionError = { disconnectSessionError = null },
            )
            Screen.Scripts -> ScriptsScreenContainer(
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
private suspend fun <T> authenticate(
    username: String,
    password: String,
    credentialCall: suspend (String, String) -> Result<T>,
): Result<T> =
    FydeSdk.initialize(BuildConfig.SERVER_URL).fold(
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
 * Every document [FydeSdk.searchDocuments] finds for [text] and [filters],
 * best match first, fetching every page like [listAllDocuments].
 */
private suspend fun searchAllDocuments(text: String, filters: FydeSearchFilters): List<FydeDocument> = buildList {
    var offset = 0L
    do {
        val page = FydeSdk.searchDocuments(text, filters, FydeSearchOrder.RELEVANCE, offset, DOCUMENT_PAGE_SIZE).getOrThrow()
        addAll(page)
        offset += page.size
    } while (page.size.toLong() == DOCUMENT_PAGE_SIZE)
}

/** What a [DocumentSearch] found, and which filter choices `fyde-sdk` says are still worth offering for it. */
private class SearchOutcome(val results: List<FydeDocument>, val filterOptions: DocumentFilterOptions)

/**
 * Runs [search] through `fyde-sdk`: its facets (the filter choices still
 * worth offering, see [FydeSearchFacets]) and its results. A [DocumentKind]
 * has to be turned into the exact content types the SDK filters on first,
 * which only the SDK's own content-type facet knows — so with a kind picked,
 * facets are asked for twice: once to learn those content types (that facet
 * ignores the content-type filter anyway), then again with them applied, for
 * every other facet to be narrowed by the kind too.
 */
private suspend fun runSearch(search: DocumentSearch, now: Long): Result<SearchOutcome> = runCatching {
    var filters = search.toSearchFilters(now)
    var facets = FydeSdk.searchDocumentFacets(search.text, filters).getOrThrow()
    val kind = search.kind
    if (kind != null) {
        val contentTypes = contentTypesOf(kind, facets)
        // No document of that kind is left (e.g. it was deleted): nothing
        // can match, which an empty filter list (no constraint) can't say.
        if (contentTypes.isEmpty()) return@runCatching SearchOutcome(emptyList(), filterOptions(facets, search, now))
        filters = search.toSearchFilters(now, contentTypes)
        facets = FydeSdk.searchDocumentFacets(search.text, filters).getOrThrow()
    }
    SearchOutcome(searchAllDocuments(search.text, filters), filterOptions(facets, search, now))
}

@OptIn(ExperimentalTime::class)
private fun nowEpochSeconds(): Long = Clock.System.now().epochSeconds

/**
 * Owns the `fyde-sdk` connection and the changelog subscription, and maps
 * their results into [DocumentListUiState]. Deliberately has no opinion on
 * how that state is drawn — see [DocumentListScreen] for the visual design,
 * which can change without touching anything in this function.
 */
@Composable
private fun DocumentsScreen(
    onOpenSettings: () -> Unit,
    onOpenScripts: () -> Unit,
) {
    var retryKey by remember { mutableStateOf(0) }
    var uiState by remember { mutableStateOf<DocumentListUiState>(DocumentListUiState.Connecting) }
    var isUploading by remember { mutableStateOf(false) }
    // What the search field and filter chips show, applied as it changes.
    var search by remember { mutableStateOf(DocumentSearch()) }
    // [search]'s text once typing pauses; see SEARCH_DEBOUNCE_MS.
    var settledText by remember { mutableStateOf("") }
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
        val initResult = FydeSdk.initialize(BuildConfig.SERVER_URL) {
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

    LaunchedEffect(search.text) {
        if (search.text.isNotBlank()) delay(SEARCH_DEBOUNCE_MS)
        settledText = search.text
    }

    // Searches through fyde-sdk whenever the settled search changes, and
    // again whenever the cached documents do (every changelog event re-lists
    // them above), so results and filter choices never go stale. A newer
    // search cancels one still running. A failure keeps the last results.
    val cachedDocuments = (uiState as? DocumentListUiState.Loaded)?.documents
    val settledSearch = search.copy(text = settledText)
    LaunchedEffect(settledSearch, cachedDocuments) {
        if (cachedDocuments == null) return@LaunchedEffect
        runSearch(settledSearch, nowEpochSeconds())
            .onSuccess { outcome ->
                (uiState as? DocumentListUiState.Loaded)?.let { current ->
                    uiState = current.copy(results = outcome.results, filterOptions = outcome.filterOptions)
                }
            }
            .onFailure { showBanner(getString(Res.string.search_failed, it.userMessage())) }
    }

    DocumentListScreen(
        state = (uiState as? DocumentListUiState.Loaded)?.copy(search = search) ?: uiState,
        onUpload = pickDocuments,
        isUploading = isUploading,
        // Listings carry no content: fetch it right before saving.
        onDownload = { document ->
            scope.launch {
                val content = FydeSdk.getDocumentContent(document.id).getOrNull()
                if (content != null) {
                    saveDocument(document.downloadFileName, content)
                } else {
                    showBanner(getString(Res.string.document_save_failed, document.name))
                }
            }
        },
        loadContent = { document -> FydeSdk.getDocumentContent(document.id) },
        onSearchChange = { search = it },
        onOpenSettings = onOpenSettings,
        onOpenScripts = onOpenScripts,
    )
}

/**
 * Owns the `fyde-sdk` scripts calls backing [ScriptsScreen] and
 * [ScriptDetailScreen]. The grid shows every public script plus the user's
 * own enabled private ones ([FydeSdk.listPublicScripts] +
 * [FydeSdk.listUserScripts]); the enabled subset is what's "installed", and
 * [FydeSdk.installedScriptParameters] supplies the values each was
 * installed with. Unlike [DocumentsScreen], there's no changelog-style live
 * subscription for scripts, so the listing is fetched once per screen open
 * (plus [retryKey] bumps) rather than kept in sync by a background stream.
 */
@Composable
private fun ScriptsScreenContainer(
    scraperRuns: ScraperRuns,
    onOpenDocuments: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var retryKey by remember { mutableStateOf(0) }
    var uiState by remember { mutableStateOf<ScriptsUiState>(ScriptsUiState.Connecting) }
    // Which script's [ScriptDetailScreen] is open, if any — a drill-down
    // local to this container rather than a top-level Screen, since the
    // detail view shares the same scripts/enabled/pending state this
    // container already owns.
    var viewedScriptId by remember { mutableStateOf<String?>(null) }
    // The grid's search, kept here rather than in ScriptsScreen, which
    // leaves composition while a script's detail view is open: going back
    // finds the same query, as with the document list.
    var scriptQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalAppSnackbarHostState.current

    fun showBanner(message: String) {
        val current = uiState as? ScriptsUiState.Loaded ?: return
        uiState = current.copy(
            banner = message,
            onDismissBanner = { (uiState as? ScriptsUiState.Loaded)?.let { loaded -> uiState = loaded.copy(banner = null) } },
        )
    }

    LaunchedEffect(retryKey) {
        uiState = ScriptsUiState.Connecting

        val initResult = FydeSdk.initialize(BuildConfig.SERVER_URL)
        if (initResult.isFailure) {
            uiState = ScriptsUiState.ConnectFailed(
                message = initResult.exceptionOrNull()?.userMessage() ?: getString(Res.string.error_unknown),
                onRetry = { retryKey++ },
            )
            return@LaunchedEffect
        }

        uiState = ScriptsUiState.Loaded(scripts = null)
        val publicScripts = FydeSdk.listPublicScripts().getOrElse { emptyList() }
        val userScripts = FydeSdk.listUserScripts().getOrElse { emptyList() }
        val scripts = (userScripts + publicScripts).distinctBy { it.id }
        uiState = ScriptsUiState.Loaded(
            scripts = scripts,
            enabledScriptIds = userScripts.map { it.id }.toSet(),
            installedParameters = FydeSdk.installedScriptParameters().getOrElse { emptyMap() },
        )

        // The listings leave icons out (see FydeSdk.fetchIcon): the grid
        // shows first, then each icon is filled in as it arrives. A failed
        // fetch isn't cached and keeps ScriptIcon's placeholder glyph.
        val iconFetches = Semaphore(SCRIPT_ICON_FETCH_CONCURRENCY)
        coroutineScope {
            scripts.forEach { script ->
                val iconId = script.iconId ?: return@forEach
                launch {
                    val icon = scriptIcons[iconId]
                        ?: iconFetches.withPermit { FydeSdk.fetchIcon(iconId).getOrNull() }
                            ?.also { scriptIcons[iconId] = it }
                    if (icon == null || icon.isEmpty()) return@launch
                    val latest = uiState as? ScriptsUiState.Loaded ?: return@launch
                    uiState = latest.copy(
                        scripts = latest.scripts?.map { if (it.id == script.id) it.copy(icon = icon) else it },
                    )
                }
            }
        }
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
        val current = uiState as? ScriptsUiState.Loaded
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
            val latest = uiState as? ScriptsUiState.Loaded ?: return@launch
            val error = result.exceptionOrNull()
            if (error == null) {
                uiState = latest.copy(
                    enabledScriptIds = if (enabled) latest.enabledScriptIds + script.id else latest.enabledScriptIds - script.id,
                    installedParameters = if (enabled) latest.installedParameters + (script.id to parameters) else latest.installedParameters,
                    pendingScriptIds = latest.pendingScriptIds - script.id,
                )
                val confirmation = when {
                    !enabled -> Res.string.script_uninstalled
                    wasInstalled -> Res.string.script_updated
                    else -> Res.string.script_installed
                }
                snackbarHostState.showSnackbar(getString(confirmation, script.name))
            } else {
                uiState = latest.copy(pendingScriptIds = latest.pendingScriptIds - script.id)
                showBanner(getString(Res.string.script_toggle_failed, script.name, error.userMessage()))
            }
        }
    }

    val loaded = uiState as? ScriptsUiState.Loaded
    val viewedScript = viewedScriptId?.let { id -> loaded?.scripts?.find { it.id == id } }

    if (loaded != null && viewedScript != null) {
        ScriptDetailScreen(
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
                (uiState as? ScriptsUiState.Loaded)?.let { latest -> uiState = latest.copy(banner = null) }
            },
            onInstall = { parameters -> setInstalled(viewedScript, true, parameters) },
            onUninstall = { setInstalled(viewedScript, false) },
            run = ScriptRunState(
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
        ScriptsScreen(
            state = uiState,
            onOpenDocuments = onOpenDocuments,
            onOpenSettings = onOpenSettings,
            onView = { script -> viewedScriptId = script.id },
            runningScriptId = scraperRuns.running?.id,
            query = scriptQuery,
            onQueryChange = { scriptQuery = it },
        )
    }
}
