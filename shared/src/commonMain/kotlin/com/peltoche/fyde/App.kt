package com.peltoche.fyde

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.connect_source_failed
import com.peltoche.fyde.resources.source_credentials_saved
import com.peltoche.fyde.resources.source_remove_failed
import com.peltoche.fyde.resources.source_removed
import com.peltoche.fyde.resources.source_update_failed
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
 * How many [FydeSdk.fetchIcon] calls [SourcesTabContainer] keeps in
 * flight at once: the listings leave icons out, so every script in the grid
 * needs its own call, and over a hundred at once would queue behind each
 * other on the one server connection anyway.
 */
private const val SCRIPT_ICON_FETCH_CONCURRENCY = 8

/**
 * Every script icon [SourcesTabContainer] has fetched, keyed by icon id
 * (an icon never changes once created), kept for the whole process so
 * reopening the Scripts screen doesn't decode them from the SDK's cache
 * again. Only touched from the UI thread.
 */
private val scriptIcons = mutableMapOf<String, ByteArray>()

/**
 * Fetches each of [scripts]' icons, [SCRIPT_ICON_FETCH_CONCURRENCY] at a
 * time, through the process-wide [scriptIcons] cache, handing each to
 * [onIcon] as it arrives. The listings leave icons out (see
 * [FydeSdk.fetchIcon]), so a screen shows its scripts first and fills the
 * icons in. A failed fetch isn't cached and keeps [ScriptIcon]'s
 * placeholder glyph. Returns once every fetch has settled.
 */
private suspend fun fetchScriptIcons(scripts: List<FydeScript>, onIcon: (scriptId: String, icon: ByteArray) -> Unit) {
    val iconFetches = Semaphore(SCRIPT_ICON_FETCH_CONCURRENCY)
    coroutineScope {
        scripts.forEach { script ->
            val iconId = script.iconId ?: return@forEach
            launch {
                val icon = scriptIcons[iconId]
                    ?: iconFetches.withPermit { FydeSdk.fetchIcon(iconId).getOrNull() }
                        ?.also { scriptIcons[iconId] = it }
                if (icon != null && icon.isNotEmpty()) onIcon(script.id, icon)
            }
        }
    }
}

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

    /**
     * The account was just created: [RecoveryKitScreen] shows its new
     * recovery key, for [username]'s vault, before the vault opens.
     */
    class RecoveryKit(val username: String) : Screen

    /**
     * Vault creation's sources step, after the recovery kit:
     * [SourcesScreen] picks the public scrapers to connect, within the plan.
     */
    data object Sources : Screen

    /**
     * Then each source ticked there in turn: [ConnectSourceScreen] asks for
     * its credentials, installs it and starts its first run.
     */
    data object ConnectSource : Screen

    /**
     * Once a source is connected, [SourceFetchingScreen] follows its first
     * run (DO7c) until the user moves on to the next one.
     */
    data object FetchingSource : Screen

    data object Documents : Screen
    data object Settings : Screen
    data object Scripts : Screen
}

/**
 * A sign-in ([isRegistration] `false`) or registration waiting on
 * [InsecureStorageSheet]: [attempt] reruns it, allowed to keep the keys in
 * the local database or not. Captures the typed credentials, so it lives
 * only while the sheet is open — a plain class, so they never end up in a
 * generated `toString`.
 */
private class PendingStorageConsent(
    val isRegistration: Boolean,
    val attempt: (allowInsecureStorage: Boolean) -> Unit,
)

/** Whether [this] is shown with no session open, before signing in. */
private val Screen.isSignedOut: Boolean
    get() = this == Screen.Welcome || this == Screen.Login || this == Screen.Register || this is Screen.SessionLimit

@Composable
@Preview
fun App() {
    // The light/dark appearance picked in Settings, held here rather than in
    // AppContent since AppTheme has to wrap every screen. Not persisted yet:
    // fyde-sdk has no preference for it (unlike the language).
    var appearance by remember { mutableStateOf(AppAppearance.Default) }
    AppTheme(theme = AppThemeOption.Default, darkTheme = appearance.isDark()) {
        AppWindowSizeClassProvider {
            // One host for every screen's transient confirmations, above the
            // screens themselves so a message outlives the screen raising it.
            val snackbarHostState = remember { SnackbarHostState() }
            CompositionLocalProvider(LocalAppSnackbarHostState provides snackbarHostState) {
                AppContent(
                    appearance = appearance,
                    onAppearanceChange = { appearance = it },
                )
            }
        }
    }
}

@Composable
private fun AppContent(
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
    // doesn't, signing in or registering first asks the user to accept
    // keeping them in the local database instead (InsecureStorageSheet, O4).
    // Assumed available until checked.
    var isSecureStorageAvailable by remember { mutableStateOf(true) }
    // The sign-in or registration InsecureStorageSheet is open for, rerun
    // with allowInsecureStorage once the user accepts. Holds the typed
    // credentials only while the sheet is open, like Screen.SessionLimit.
    var storageConsent by remember { mutableStateOf<PendingStorageConsent?>(null) }
    // Whether the sheet's Retry is looking for the keyring again.
    var isRecheckingStorage by remember { mutableStateOf(false) }
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
    // Settings' open page, held above `key(language)` so picking a language
    // there doesn't bounce back to its menu.
    var settingsSection by remember { mutableStateOf<SettingsSection?>(null) }
    var planUsage by remember { mutableStateOf<PlanUsageUiState>(PlanUsageUiState.Loading) }
    var planUsageAttempt by remember { mutableStateOf(0) }
    var sessions by remember { mutableStateOf<SessionsUiState>(SessionsUiState.Loading) }
    var sessionsAttempt by remember { mutableStateOf(0) }
    var disconnectingSessionId by remember { mutableStateOf<String?>(null) }
    var disconnectSessionError by remember { mutableStateOf<String?>(null) }
    // The recovery key's words while Screen.RecoveryKit is open, dropped as
    // soon as it closes.
    var recoveryKit by remember { mutableStateOf<RecoveryKitUiState>(RecoveryKitUiState.Generating) }
    var recoveryKitAttempt by remember { mutableStateOf(0) }
    // SourcesScreen's listing, and what's ticked there (in ticking order, the
    // order they'll be connected in) and searched for.
    var sources by remember { mutableStateOf<SourcesUiState>(SourcesUiState.Loading) }
    var sourcesAttempt by remember { mutableStateOf(0) }
    var sourceSelection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sourcesQuery by remember { mutableStateOf("") }
    // The ticked sources ConnectSourceScreen connects one by one (in ticking
    // order), which one is up, and those passed over on the way.
    var connectQueue by remember { mutableStateOf<List<FydeScript>>(emptyList()) }
    var connectIndex by remember { mutableStateOf(0) }
    var skippedSourceIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var connectError by remember { mutableStateOf<String?>(null) }
    var isConnectingSource by remember { mutableStateOf(false) }
    // "Wait here" on SourceFetchingScreen: move on by itself once the run succeeds.
    var isWaitingForFetch by remember { mutableStateOf(false) }
    // fyde-sdk's wording while startup is refused because another instance
    // of the app is running (see AlreadyRunningScreen), `null` otherwise.
    var alreadyRunningMessage by remember { mutableStateOf<String?>(null) }
    var startupAttempt by remember { mutableStateOf(0) }
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
    // Another instance of the app already holding this device's local data
    // makes fyde-sdk refuse to start: AlreadyRunningScreen then replaces every
    // screen, saying so, until a retry (bumping startupAttempt) gets through.
    LaunchedEffect(startupAttempt) {
        FydeSdk.initialize(BuildConfig.SERVER_URL)
            .onSuccess {
                alreadyRunningMessage = null
                FydeSdk.getLanguage().getOrNull()?.let { savedLanguage ->
                    applyPlatformLocale(savedLanguage)
                    language = savedLanguage
                }
                if (FydeSdk.isConnected().getOrDefault(false)) {
                    screen = Screen.Documents
                }
            }
            .onFailure {
                if ((it as? FydeSdkException)?.isClientAlreadyRunning == true) {
                    alreadyRunningMessage = it.userMessage()
                }
            }
    }

    // Runs [attempt] at once where the device has secure storage; otherwise
    // opens InsecureStorageSheet, which runs it allowed to fall back to the
    // local database once the user accepts.
    fun withStorageConsent(isRegistration: Boolean, attempt: (allowInsecureStorage: Boolean) -> Unit) {
        if (isSecureStorageAvailable) attempt(false) else storageConsent = PendingStorageConsent(isRegistration, attempt)
    }

    // fyde-sdk refuses to open a session without secure storage unless
    // allowed (SecureStorageUnavailable); the keyring may have gone away
    // (locked, stopped) since the form checked. Re-checked after a failed
    // attempt that wasn't allowed, so the user gets the sheet rather than
    // that error.
    suspend fun lostSecureStorage(allowInsecureStorage: Boolean): Boolean {
        if (allowInsecureStorage || FydeSdk.isSecureStorageAvailable().getOrNull() != false) return false
        isSecureStorageAvailable = false
        return true
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
                    if (lostSecureStorage(allowInsecureStorage)) {
                        storageConsent = PendingStorageConsent(isRegistration = false) { allow ->
                            logIn(username, password, allow, replaceSessionId)
                        }
                        isAuthenticating = false
                        return@launch
                    }
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

    fun register(username: String, password: String, allowInsecureStorage: Boolean) {
        authError = null
        isAuthenticating = true
        scope.launch {
            authenticate(username, password) { user, pass -> FydeSdk.register(user, pass, allowInsecureStorage) }
                .onSuccess { screen = Screen.RecoveryKit(username) }
                .onFailure {
                    if (lostSecureStorage(allowInsecureStorage)) {
                        storageConsent = PendingStorageConsent(isRegistration = true) { allow ->
                            register(username, password, allow)
                        }
                    } else {
                        authError = it.userMessage(Res.string.auth_error_register_failed)
                    }
                }
            isAuthenticating = false
        }
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
    // the local-database fallback (InsecureStorageSheet) when there's none.
    LaunchedEffect(screen) {
        // InsecureStorageSheet belongs to the form it was opened over:
        // leaving that form drops it, and the credentials it holds.
        storageConsent = null
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

    // Settings' notes have no dismiss button: a failure is cleared by trying
    // again, or by leaving and coming back.
    // Leaving resets the open page too, so Settings reopens on its menu.
    LaunchedEffect(screen) {
        if (screen != Screen.Settings) {
            settingsSection = null
            return@LaunchedEffect
        }
        logoutError = null
        languageError = null
    }

    // Same for the devices list, which other devices change by connecting or
    // disconnecting.
    LaunchedEffect(screen, sessionsAttempt) {
        if (screen != Screen.Settings) return@LaunchedEffect
        disconnectSessionError = null
        sessions = SessionsUiState.Loading
        sessions = fetchSessions()
    }

    // Generated once as the recovery kit opens (and on retry): each call
    // replaces the account's previous key, so it isn't asked for again
    // while the screen stays open.
    LaunchedEffect(screen, recoveryKitAttempt) {
        if (screen !is Screen.RecoveryKit) {
            recoveryKit = RecoveryKitUiState.Generating
            return@LaunchedEffect
        }
        recoveryKit = RecoveryKitUiState.Generating
        recoveryKit = FydeSdk.generateRecoveryKey().fold(
            onSuccess = { RecoveryKitUiState.Ready(it) },
            onFailure = { RecoveryKitUiState.Failed(message = it.userMessage(), onRetry = { recoveryKitAttempt++ }) },
        )
    }

    // Listed as the sources step opens (and on retry): the public scrapers
    // (parsers never show in the UI), then the plan's limit. During
    // onboarding nothing is ticked yet, so a fresh visit starts empty.
    LaunchedEffect(screen, sourcesAttempt) {
        if (screen != Screen.Sources) {
            sources = SourcesUiState.Loading
            return@LaunchedEffect
        }
        sources = SourcesUiState.Loading
        val listed = FydeSdk.listPublicScripts().getOrElse {
            sources = SourcesUiState.Failed(message = it.userMessage(), onRetry = { sourcesAttempt++ })
            return@LaunchedEffect
        }
        val scrapers = listed.filter { it.type == FydeScriptType.SCRAPER }.sortedBy { it.name.lowercase() }
        // Without the usage the counter is hidden; the server still refuses
        // a source past the plan when it's connected.
        val quota = FydeSdk.getUsage().getOrNull()?.let(::sourceQuotaOf)
        sources = SourcesUiState.Loaded(scrapers, quota)
        fetchScriptIcons(scrapers) { scriptId, icon ->
            val latest = sources as? SourcesUiState.Loaded ?: return@fetchScriptIcons
            sources = latest.copy(sources = latest.sources.map { if (it.id == scriptId) it.copy(icon = icon) else it })
        }
    }

    // On to the next ticked source, or, after the last, into the vault
    // (DO7g's summary and the notifications step don't exist yet).
    fun nextSource() {
        connectError = null
        isWaitingForFetch = false
        if (connectIndex + 1 < connectQueue.size) {
            connectIndex++
            screen = Screen.ConnectSource
            return
        }
        connectQueue = emptyList()
        skippedSourceIds = emptySet()
        sourceSelection = emptySet()
        sourcesQuery = ""
        screen = Screen.Documents
    }

    // Installs [script] with what was typed (publishing the values, encrypted,
    // to the account's devices) and enables it, then starts its first run in
    // the background once the installation has synced here, and follows it.
    fun connectSource(script: FydeScript, parameters: Map<String, FydeScriptParameterValue>) {
        connectError = null
        isConnectingSource = true
        scope.launch {
            val result = FydeSdk.installScript(script.id, parameters)
                .mapCatching { FydeSdk.enableScript(script.id).getOrThrow() }
            isConnectingSource = false
            // The session may have ended meanwhile, back to the login form.
            if (screen != Screen.ConnectSource) return@launch
            result
                .onSuccess {
                    skippedSourceIds = skippedSourceIds - script.id
                    scraperRuns.runOnceInstalled(script)
                    screen = Screen.FetchingSource
                }
                .onFailure { connectError = getString(Res.string.connect_source_failed, script.name, it.userMessage()) }
        }
    }

    // The source on SourceFetchingScreen, if its first run is over: after
    // "Wait here", a success moves on by itself, a failure stays to be seen.
    val fetchingOutcome = connectQueue.getOrNull(connectIndex)
        ?.takeIf { screen == Screen.FetchingSource }
        ?.let { scraperRuns.outcomes[it.id] }
    LaunchedEffect(fetchingOutcome, isWaitingForFetch) {
        if (!isWaitingForFetch || fetchingOutcome == null) return@LaunchedEffect
        if (fetchingOutcome is ScraperRunOutcome.Succeeded) nextSource() else isWaitingForFetch = false
    }

    // The ticked sources as the brand panel lists them, the one up [current].
    fun vaultSources(current: VaultSourceState) = connectQueue.mapIndexed { index, source ->
        VaultSource(
            name = source.name,
            state = when {
                index == connectIndex -> current
                index > connectIndex -> VaultSourceState.Next
                source.id in skippedSourceIds -> VaultSourceState.Skipped
                scraperRuns.isPending(source.id) -> VaultSourceState.Running
                else -> VaultSourceState.Connected
            },
        )
    }

    alreadyRunningMessage?.let { message ->
        AlreadyRunningScreen(message = message, onRetry = { startupAttempt++ })
        return
    }

    key(language) {
        when (screen) {
            Screen.Welcome -> WelcomeScreen(
                // The URL's host (and port, on a dev server), as DO1 names it.
                serverName = BuildConfig.SERVER_URL.substringAfter("://").substringBefore('/'),
                onCreateVault = { screen = Screen.Register },
                onOpenVault = { screen = Screen.Login },
            )
            Screen.Login -> LoginScreen(
                noticeMessage = sessionNotice,
                errorMessage = authError,
                isSubmitting = isAuthenticating,
                onLogIn = { username, password ->
                    withStorageConsent(isRegistration = false) { allow -> logIn(username, password, allow) }
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
                onRegister = { username, password ->
                    withStorageConsent(isRegistration = true) { allow -> register(username, password, allow) }
                },
                onNavigateToLogin = {
                    authError = null
                    screen = Screen.Login
                },
                onBack = {
                    authError = null
                    screen = Screen.Welcome
                },
            )
            is Screen.RecoveryKit -> RecoveryKitScreen(
                state = recoveryKit,
                username = (screen as Screen.RecoveryKit).username,
                // "Later" opens the vault too, keeping the key: the user may
                // have saved the PDF before choosing it.
                onDone = { screen = Screen.Sources },
                onLater = { screen = Screen.Sources },
            )
            Screen.Sources -> SourcesScreen(
                state = sources,
                selected = sourceSelection,
                onToggle = { script ->
                    sourceSelection = if (script.id in sourceSelection) {
                        sourceSelection - script.id
                    } else {
                        sourceSelection + script.id
                    }
                },
                query = sourcesQuery,
                onQueryChange = { sourcesQuery = it },
                // Each ticked source is then connected in turn (S3, DO7b);
                // the selection stays, for coming back from the first.
                onConnect = {
                    val listed = (sources as? SourcesUiState.Loaded)?.sources.orEmpty().associateBy { it.id }
                    connectQueue = sourceSelection.mapNotNull { listed[it] }
                    if (connectQueue.isNotEmpty()) {
                        connectIndex = 0
                        skippedSourceIds = emptySet()
                        connectError = null
                        screen = Screen.ConnectSource
                    }
                },
                onSkip = {
                    sourceSelection = emptySet()
                    sourcesQuery = ""
                    screen = Screen.Documents
                },
            )
            Screen.ConnectSource -> connectQueue.getOrNull(connectIndex)?.let { script ->
                ConnectSourceScreen(
                    script = script,
                    position = connectIndex + 1,
                    count = connectQueue.size,
                    sources = vaultSources(current = VaultSourceState.Current),
                    errorMessage = connectError,
                    isSubmitting = isConnectingSource,
                    onConnect = { parameters -> connectSource(script, parameters) },
                    onSkip = {
                        skippedSourceIds = skippedSourceIds + script.id
                        nextSource()
                    },
                    // Back to the previous source, or from the first to the
                    // picker, ticks kept.
                    onBack = {
                        connectError = null
                        if (connectIndex > 0) connectIndex-- else screen = Screen.Sources
                    },
                )
            }
            Screen.FetchingSource -> connectQueue.getOrNull(connectIndex)?.let { script ->
                SourceFetchingScreen(
                    script = script,
                    position = connectIndex + 1,
                    count = connectQueue.size,
                    sources = vaultSources(current = VaultSourceState.Fetching),
                    status = sourceFetchStatusOf(script.id, scraperRuns),
                    isLast = connectIndex == connectQueue.lastIndex,
                    isWaiting = isWaitingForFetch,
                    onWait = { isWaitingForFetch = true },
                    onRetry = { scraperRuns.runOnceInstalled(script) },
                    onNext = ::nextSource,
                )
            }
            Screen.Documents -> DocumentsScreen(
                onOpenSettings = { screen = Screen.Settings },
                onOpenScripts = { screen = Screen.Scripts },
            )
            Screen.Settings -> SettingsScreen(
                openSection = settingsSection,
                onOpenSectionChange = { settingsSection = it },
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
                languageError = languageError,
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
            )
            Screen.Scripts -> SourcesTabContainer(
                scraperRuns = scraperRuns,
                onOpenDocuments = { screen = Screen.Documents },
                onOpenSettings = { screen = Screen.Settings },
                onOpenPlan = {
                    settingsSection = SettingsSection.Plan
                    screen = Screen.Settings
                },
            )
        }

        // Over the login or registration form it was opened from.
        storageConsent?.let { pending ->
            InsecureStorageSheet(
                isRegistration = pending.isRegistration,
                isRetrying = isRecheckingStorage,
                onContinue = {
                    storageConsent = null
                    pending.attempt(true)
                },
                // A keyring found this time runs the attempt as it would
                // have gone without one missing; none, and the sheet stays.
                onRetry = {
                    scope.launch {
                        isRecheckingStorage = true
                        val available = FydeSdk.isSecureStorageAvailable().getOrDefault(false)
                        isRecheckingStorage = false
                        if (available && storageConsent === pending) {
                            isSecureStorageAvailable = true
                            storageConsent = null
                            pending.attempt(false)
                        }
                    }
                },
                onDismiss = { storageConsent = null },
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
 * Owns the `fyde-sdk` calls backing [SourcesTabScreen]: every scraper on
 * offer ([FydeSdk.listPublicScripts] + [FydeSdk.listUserScripts], parsers
 * left out), the user's enabled ones being their sources, installed with
 * [FydeSdk.installedScriptParameters]'s values, and the plan's source quota
 * ([FydeSdk.getUsage]). There's no live subscription for scripts, so all of
 * it is fetched once per visit (plus [retryKey] bumps).
 */
@Composable
private fun SourcesTabContainer(
    scraperRuns: ScraperRuns,
    onOpenDocuments: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlan: () -> Unit,
) {
    var retryKey by remember { mutableStateOf(0) }
    var uiState by remember { mutableStateOf<SourcesTabUiState>(SourcesTabUiState.Connecting) }
    var focus by remember { mutableStateOf<SourceFocus?>(null) }
    var catalogueOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalAppSnackbarHostState.current

    fun update(transform: (SourcesTabUiState.Loaded) -> SourcesTabUiState.Loaded) {
        (uiState as? SourcesTabUiState.Loaded)?.let { uiState = transform(it) }
    }

    suspend fun refreshQuota() {
        val quota = FydeSdk.getUsage().getOrNull()?.let(::sourceQuotaOf)
        update { it.copy(quota = quota) }
    }

    LaunchedEffect(retryKey) {
        uiState = SourcesTabUiState.Connecting

        val initResult = FydeSdk.initialize(BuildConfig.SERVER_URL)
        if (initResult.isFailure) {
            uiState = SourcesTabUiState.ConnectFailed(
                message = initResult.exceptionOrNull()?.userMessage() ?: getString(Res.string.error_unknown),
                onRetry = { retryKey++ },
            )
            return@LaunchedEffect
        }

        uiState = SourcesTabUiState.Loaded(scripts = null)
        val publicScripts = FydeSdk.listPublicScripts().getOrElse { emptyList() }
        val userScripts = FydeSdk.listUserScripts().getOrElse { emptyList() }
        val scripts = (userScripts + publicScripts).distinctBy { it.id }.filter { it.type == FydeScriptType.SCRAPER }
        uiState = SourcesTabUiState.Loaded(
            scripts = scripts,
            installedIds = userScripts.map { it.id }.toSet(),
            installedParameters = FydeSdk.installedScriptParameters().getOrElse { emptyMap() },
        )
        refreshQuota()

        fetchScriptIcons(scripts) { scriptId, icon ->
            update { latest -> latest.copy(scripts = latest.scripts?.map { if (it.id == scriptId) it.copy(icon = icon) else it }) }
        }
    }

    // Installing publishes [parameters] through the changelog (so the source
    // and the values it runs with sync to every device on the account), then
    // enables it; installing an installed source again replaces its values.
    fun install(
        script: FydeScript,
        parameters: Map<String, FydeScriptParameterValue>,
        onDone: suspend (Throwable?) -> Unit,
    ) {
        val current = uiState as? SourcesTabUiState.Loaded ?: return
        if (script.id in current.pendingIds) return
        uiState = current.copy(pendingIds = current.pendingIds + script.id, errors = current.errors - script.id)
        scope.launch {
            val error = FydeSdk.installScript(script.id, parameters)
                .mapCatching { FydeSdk.enableScript(script.id).getOrThrow() }
                .exceptionOrNull()
            update { latest ->
                latest.copy(
                    pendingIds = latest.pendingIds - script.id,
                    installedIds = if (error == null) latest.installedIds + script.id else latest.installedIds,
                    installedParameters = if (error == null) latest.installedParameters + (script.id to parameters) else latest.installedParameters,
                )
            }
            onDone(error)
        }
    }

    SourcesTabScreen(
        state = uiState,
        statuses = (uiState as? SourcesTabUiState.Loaded)?.installedIds.orEmpty()
            .mapNotNull { id -> sourceStatusOf(id, scraperRuns)?.let { id to it } }
            .toMap(),
        canRun = scraperRuns.running == null,
        focus = focus,
        onFocusChange = { focus = it },
        catalogueOpen = catalogueOpen,
        onCatalogueOpenChange = { catalogueOpen = it },
        query = query,
        onQueryChange = { query = it },
        onConnect = { script, parameters ->
            install(script, parameters) { error ->
                if (error == null) {
                    refreshQuota()
                    scraperRuns.runOnceInstalled(script, parameters)
                    focus = SourceFocus.Fetching(script.id)
                    catalogueOpen = false
                } else {
                    val message = getString(Res.string.connect_source_failed, script.name, error.userMessage())
                    update { it.copy(errors = it.errors + (script.id to message)) }
                }
            }
        },
        onUpdate = { script, parameters, rerun ->
            install(script, parameters) { error ->
                when {
                    error != null -> {
                        val message = getString(Res.string.source_update_failed, script.name, error.userMessage())
                        update { it.copy(errors = it.errors + (script.id to message)) }
                    }
                    rerun -> {
                        scraperRuns.runOnceInstalled(script, parameters)
                        focus = SourceFocus.Fetching(script.id)
                    }
                    else -> {
                        focus = SourceFocus.Detail(script.id)
                        snackbarHostState.showSnackbar(getString(Res.string.source_credentials_saved, script.name))
                    }
                }
            }
        },
        onRun = { script -> scraperRuns.run(script) },
        // Removing only disables the source: fyde-sdk has no uninstall event yet.
        onRemove = { script ->
            val current = uiState as? SourcesTabUiState.Loaded
            if (current != null && script.id !in current.pendingIds) {
                uiState = current.copy(pendingIds = current.pendingIds + script.id)
                scope.launch {
                    val error = FydeSdk.disableScript(script.id).exceptionOrNull()
                    update { latest ->
                        latest.copy(
                            pendingIds = latest.pendingIds - script.id,
                            installedIds = if (error == null) latest.installedIds - script.id else latest.installedIds,
                        )
                    }
                    if (error == null) {
                        if (focus?.scriptId == script.id) focus = null
                        refreshQuota()
                        snackbarHostState.showSnackbar(getString(Res.string.source_removed, script.name))
                    } else {
                        snackbarHostState.showSnackbar(getString(Res.string.source_remove_failed, script.name, error.userMessage()))
                    }
                }
            }
        },
        onOpenPlan = onOpenPlan,
        onOpenDocuments = onOpenDocuments,
        onOpenSettings = onOpenSettings,
    )
}

/** Where [scriptId]'s latest run in this app session is, or `null` if it hasn't been run or queued. */
private fun sourceStatusOf(scriptId: String, runs: ScraperRuns): SourceFetchStatus? = when {
    runs.running?.id == scriptId -> SourceFetchStatus.Running(runs.stepKind, runs.update, runs.found)
    runs.queued.any { it.id == scriptId } -> SourceFetchStatus.Queued
    else -> when (val outcome = runs.outcomes[scriptId]) {
        is ScraperRunOutcome.Succeeded -> SourceFetchStatus.Succeeded(outcome.found)
        is ScraperRunOutcome.Failed -> SourceFetchStatus.Failed(outcome.message, outcome.step, outcome.found)
        null -> null
    }
}

/** Where [scriptId]'s first run is, for [SourceFetchingScreen]: running, waiting its turn, or over. */
private fun sourceFetchStatusOf(scriptId: String, runs: ScraperRuns): SourceFetchStatus {
    if (runs.running?.id == scriptId) return SourceFetchStatus.Running(runs.stepKind, runs.update, runs.found)
    return when (val outcome = runs.outcomes[scriptId]) {
        is ScraperRunOutcome.Succeeded -> SourceFetchStatus.Succeeded(outcome.found)
        is ScraperRunOutcome.Failed -> SourceFetchStatus.Failed(outcome.message, outcome.step, outcome.found)
        null -> SourceFetchStatus.Queued
    }
}
