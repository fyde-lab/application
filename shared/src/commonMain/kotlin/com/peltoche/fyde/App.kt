package com.peltoche.fyde

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.change_password_done
import com.peltoche.fyde.resources.change_password_failed
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
import com.peltoche.fyde.resources.appearance_save_failed
import com.peltoche.fyde.resources.language_save_failed
import com.peltoche.fyde.resources.log_out_failed
import com.peltoche.fyde.resources.session_expired
import com.peltoche.fyde.resources.sessions_disconnect_failed
import com.peltoche.fyde.resources.sessions_disconnected
import com.peltoche.fyde.resources.sessions_load_failed
import com.peltoche.fyde.resources.upload_failed
import com.peltoche.fyde.resources.uploads_failed
import com.peltoche.fyde.resources.uploads_succeeded
import com.peltoche.fyde.resources.usage_load_failed
import com.peltoche.fyde.resources.search_failed
import com.peltoche.fyde.resources.vault_load_failed
import com.peltoche.fyde.resources.document_moved
import com.peltoche.fyde.resources.documents_delete_failed
import com.peltoche.fyde.resources.documents_deleted
import com.peltoche.fyde.resources.documents_move_failed
import com.peltoche.fyde.resources.documents_moved
import com.peltoche.fyde.resources.folder_created
import com.peltoche.fyde.resources.folder_deleted
import com.peltoche.fyde.resources.folder_moved
import com.peltoche.fyde.resources.folder_saved
import com.peltoche.fyde.resources.vault_deleted
import com.peltoche.fyde.layout.AppWindowSizeClass
import com.peltoche.fyde.layout.AppWindowSizeClassProvider
import com.peltoche.fyde.layout.LocalAppWindowSizeClass
import com.peltoche.fyde.theme.AppAppearance
import com.peltoche.fyde.theme.AppTheme
import com.peltoche.fyde.theme.AppThemeOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * How many documents [searchAllDocuments] fetches per call. The list
 * itself isn't capped: pages are fetched until one comes back short.
 */
private const val DOCUMENT_PAGE_SIZE = 100L

/**
 * How often to poll [FydeSdk.isServerReachable] while the vault is open.
 * Found via review: it used to poll every 200ms — five calls a second,
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

/** How often [OpeningScreen]'s ring re-reads [FydeSdk.syncProgress], a local read. */
private const val SYNC_PROGRESS_POLL_INTERVAL_MS = 400L

/**
 * How long the vault's search waits after a keystroke before searching, so
 * typing a word runs one search rather than one per letter.
 */
private const val SEARCH_DEBOUNCE_MS = 250L

/** How many documents the vault's home lists as its latest arrivals. */
private const val LATEST_ARRIVALS = 5L

/** The app setting (`FydeSdk.setSetting`) the picked [AppAppearance]'s `id` is saved under. Never rename it. */
private const val APPEARANCE_SETTING = "appearance"

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
     * Vault creation's sources step, right after the account is created
     * (the vault starts with no folder):
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

    /**
     * Right after signing in: logging out and an expired session both wipe
     * this device's copy of the vault, so every sign-in replays the
     * account's history, which [OpeningScreen] (O9, DO9) shows arriving
     * until the user moves on to the vault.
     */
    data object Opening : Screen

    data object Vault : Screen
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
    // AppContent since AppTheme has to wrap every screen. AppContent saves it
    // on this device (FydeSdk.setSetting, under APPEARANCE_SETTING) and
    // restores it at startup, with the language.
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
    var appearanceError by remember { mutableStateOf<String?>(null) }
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
    var isChangingPassword by remember { mutableStateOf(false) }
    var changePasswordError by remember { mutableStateOf<String?>(null) }
    // M2's recovery kit row, re-asked each time Settings opens and after a
    // new key; the page's words only while it's open.
    var recoveryKeyStatus by remember { mutableStateOf<RecoveryKeyStatus>(RecoveryKeyStatus.Loading) }
    var recoveryKit by remember { mutableStateOf<RecoveryKitUiState>(RecoveryKitUiState.Idle) }
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
    // The account each source connected during onboarding got, by script id:
    // its first run is followed by installation id, and going back to it
    // replaces that account instead of adding a second one.
    var onboardingAccounts by remember { mutableStateOf<Map<String, SourceAccount>>(emptyMap()) }
    var connectError by remember { mutableStateOf<String?>(null) }
    var isConnectingSource by remember { mutableStateOf(false) }
    // The vault's folders while sources are connected, for where each files
    // its documents, and those created there that sync hasn't brought back yet.
    var onboardingFolders by remember { mutableStateOf<List<FydeFolder>>(emptyList()) }
    var onboardingCreatedFolders by remember { mutableStateOf<List<FydeFolder>>(emptyList()) }
    // "Wait here" on SourceFetchingScreen: move on by itself once the run succeeds.
    var isWaitingForFetch by remember { mutableStateOf(false) }
    // How far sync has got replaying the account's history, while
    // OpeningScreen shows it; null before its first answer.
    var syncProgress by remember { mutableStateOf<FydeSyncProgress?>(null) }
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
    // Bumped on every changelog event, whichever screen is open: the vault
    // reloads on it, and so does the sidebar on every destination.
    var documentChanges by remember { mutableStateOf(0) }
    // Bumped once a source is connected or removed or a device signed out,
    // for the sidebar's counts.
    var accountChanged by remember { mutableStateOf(0) }
    var sidebarSummary by remember { mutableStateOf(SidebarSummary()) }
    // The vault page the next visit opens on: home, or the folder picked
    // in the sidebar from another destination.
    var vaultEntry by remember { mutableStateOf(VaultFocus()) }
    // The folder sheet it opens with: the sidebar's "+" from another destination.
    var vaultEntrySheet by remember { mutableStateOf<FolderSheet?>(null) }

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
                FydeSdk.getSetting(APPEARANCE_SETTING).getOrNull()?.let { savedAppearance ->
                    onAppearanceChange(AppAppearance.fromId(savedAppearance))
                }
                if (FydeSdk.isConnected().getOrDefault(false)) {
                    screen = Screen.Vault
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
                        FydeLoginOutcome.LoggedIn -> Screen.Opening
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
                .onSuccess { screen = Screen.Sources }
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

    suspend fun fetchRecoveryKeyStatus(): RecoveryKeyStatus = FydeSdk.recoveryKeyStatus().fold(
        onSuccess = { createdAt -> createdAt?.let { RecoveryKeyStatus.Created(it) } ?: RecoveryKeyStatus.Missing },
        onFailure = { RecoveryKeyStatus.Unknown },
    )

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
        appearanceError = null
    }

    LaunchedEffect(settingsSection) {
        if (settingsSection != SettingsSection.ChangePassword) changePasswordError = null
        // The words are shown once: leaving the page (or Settings) drops them.
        if (settingsSection != SettingsSection.RecoveryKit && recoveryKit !is RecoveryKitUiState.Generating) {
            recoveryKit = RecoveryKitUiState.Idle
        }
    }

    LaunchedEffect(screen) {
        if (screen != Screen.Settings) return@LaunchedEffect
        recoveryKeyStatus = RecoveryKeyStatus.Loading
        recoveryKeyStatus = fetchRecoveryKeyStatus()
    }

    // Same for the devices list, which other devices change by connecting or
    // disconnecting.
    LaunchedEffect(screen, sessionsAttempt) {
        if (screen != Screen.Settings) return@LaunchedEffect
        disconnectSessionError = null
        sessions = SessionsUiState.Loading
        sessions = fetchSessions()
    }

    val signedIn = screen == Screen.Vault || screen == Screen.Settings || screen == Screen.Scripts
    val showsSidebar = LocalAppWindowSizeClass.current != AppWindowSizeClass.Compact
    // VaultContainer reads vaultEntry once, as it opens, so it's spent then:
    // any later way into the vault opens on its home.
    LaunchedEffect(signedIn, screen) {
        if (!signedIn) sidebarSummary = SidebarSummary()
        if (!signedIn || screen == Screen.Vault) {
            vaultEntry = VaultFocus()
            vaultEntrySheet = null
        }
    }
    // The sidebar's figures, not at Compact, which has no sidebar. Document
    // counts follow the changelog; sources and devices, which documents don't
    // change, follow each destination's opening and this device's own changes.
    // A failed call keeps the figures it would have replaced.
    LaunchedEffect(signedIn, showsSidebar, documentChanges) {
        if (!signedIn || !showsSidebar) return@LaunchedEffect
        val facets = FydeSdk.searchDocumentFacets("", FydeSearchFilters()).getOrNull() ?: return@LaunchedEffect
        val folders = FydeSdk.listFolders().getOrNull() ?: return@LaunchedEffect
        sidebarSummary = sidebarSummary.copy(
            // Every document has a content type, so that facet's counts add up to the vault's total.
            total = facets.contentTypes.sumOf { it.count },
            folders = VaultFolders(folders),
            folderCounts = facets.folderCounts(),
            unfiled = facets.unfiled,
        )
    }
    LaunchedEffect(screen, showsSidebar, accountChanged) {
        if (!signedIn || !showsSidebar) return@LaunchedEffect
        coroutineScope {
            // The sidebar counts accounts, the rows Sources shows (design-system.md § Accounts of a source).
            val installations = async { FydeSdk.listInstallations().getOrNull() }
            val sessions = async { FydeSdk.listSessions().getOrNull() }
            val sourceCount = installations.await()?.size
            val deviceCount = sessions.await()?.size
            sidebarSummary = sidebarSummary.copy(
                sourceCount = sourceCount ?: sidebarSummary.sourceCount,
                deviceCount = deviceCount ?: sidebarSummary.deviceCount,
            )
        }
    }
    val navigation = remember(sidebarSummary) {
        TopLevelNavigation(
            summary = sidebarSummary,
            onOpenVault = {
                vaultEntry = VaultFocus()
                screen = Screen.Vault
            },
            onOpenFolder = { folderId ->
                vaultEntry = VaultFocus(page = VaultPage.Folder(folderId))
                screen = Screen.Vault
            },
            onOpenScripts = { screen = Screen.Scripts },
            onOpenSettings = { screen = Screen.Settings },
            onNewFolder = {
                vaultEntry = VaultFocus()
                vaultEntrySheet = FolderSheet.Create(null)
                screen = Screen.Vault
            },
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
        onboardingAccounts = emptyMap()
        sourceSelection = emptySet()
        sourcesQuery = ""
        screen = Screen.Vault
    }

    // Re-read as the connect step opens and on every changelog event (a
    // folder created for an earlier source of the queue shows up there).
    LaunchedEffect(screen, documentChanges) {
        if (screen != Screen.ConnectSource) return@LaunchedEffect
        val synced = FydeSdk.listFolders().getOrNull() ?: return@LaunchedEffect
        onboardingFolders = synced
        onboardingCreatedFolders = onboardingCreatedFolders.filter { created -> synced.none { it.id == created.id } }
    }

    // Installs [script] with what was typed (publishing the values, encrypted,
    // to the account's devices), its documents filed in [folderId]
    // (automatically when null), and enables it, then starts its first run in
    // the background once the installation has synced here, and follows it.
    fun connectSource(script: FydeScript, parameters: Map<String, FydeScriptParameterValue>, folderId: String?) {
        connectError = null
        isConnectingSource = true
        scope.launch {
            val previous = onboardingAccounts[script.id]
            if (folderId != null) awaitFolderSynced(folderId)
            val result = FydeSdk.installScript(previous?.installationId, script.id, label = "", parameters, folderId)
                .mapCatching { id -> FydeSdk.enableScript(script.id).getOrThrow(); id }
            isConnectingSource = false
            // The session may have ended meanwhile, back to the login form.
            if (screen != Screen.ConnectSource) return@launch
            result
                .onSuccess { id ->
                    val account = SourceAccount(
                        installationId = id,
                        script = script,
                        label = provisionalLabel(script, parameters),
                        parameters = parameters,
                        shared = false,
                        folderId = folderId,
                    )
                    onboardingAccounts = onboardingAccounts + (script.id to account)
                    skippedSourceIds = skippedSourceIds - script.id
                    scraperRuns.runOnceInstalled(account, parameters)
                    screen = Screen.FetchingSource
                }
                .onFailure { connectError = getString(Res.string.connect_source_failed, script.name, it.userMessage()) }
        }
    }

    // The source on SourceFetchingScreen, if its first run is over: after
    // OpeningScreen's ring, from fyde-sdk's local count (polled: nothing
    // calls back on it). An account with nothing to replay skips it. A
    // failed read keeps the last one.
    LaunchedEffect(screen) {
        if (screen != Screen.Opening) return@LaunchedEffect
        syncProgress = null
        while (true) {
            FydeSdk.syncProgress().getOrNull()?.let { progress ->
                if (progress.caughtUp && progress.total == 0L) {
                    screen = Screen.Vault
                    return@LaunchedEffect
                }
                syncProgress = progress
            }
            delay(SYNC_PROGRESS_POLL_INTERVAL_MS)
        }
    }

    // "Wait here", a success moves on by itself, a failure stays to be seen.
    val fetchingOutcome = connectQueue.getOrNull(connectIndex)
        ?.takeIf { screen == Screen.FetchingSource }
        ?.let { onboardingAccounts[it.id] }
        ?.let { scraperRuns.outcomes[it.installationId] }
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
                onboardingAccounts[source.id]?.let { scraperRuns.isPending(it.installationId) } == true -> VaultSourceState.Running
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
                    screen = Screen.Vault
                },
            )
            Screen.ConnectSource -> connectQueue.getOrNull(connectIndex)?.let { script ->
                ConnectSourceScreen(
                    script = script,
                    position = connectIndex + 1,
                    count = connectQueue.size,
                    sources = vaultSources(current = VaultSourceState.Current),
                    folders = VaultFolders(onboardingFolders + onboardingCreatedFolders),
                    // Back to a source already connected: the folder it got.
                    initialFolderId = onboardingAccounts[script.id]?.folderId,
                    errorMessage = connectError,
                    isSubmitting = isConnectingSource,
                    onConnect = { parameters, folderId -> connectSource(script, parameters, folderId) },
                    createFolder = { parentId, form ->
                        FydeSdk.createFolder(parentId, form.name, form.appearance).fold(
                            onSuccess = { folder ->
                                onboardingCreatedFolders = onboardingCreatedFolders + folder
                                FolderCreation.Created(folder)
                            },
                            onFailure = { FolderCreation.Refused(it.userMessage()) },
                        )
                    },
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
                val account = onboardingAccounts[script.id]
                SourceFetchingScreen(
                    script = script,
                    position = connectIndex + 1,
                    count = connectQueue.size,
                    sources = vaultSources(current = VaultSourceState.Fetching),
                    status = account?.let { sourceFetchStatusOf(it.installationId, scraperRuns) } ?: SourceFetchStatus.Queued,
                    isLast = connectIndex == connectQueue.lastIndex,
                    isWaiting = isWaitingForFetch,
                    onWait = { isWaitingForFetch = true },
                    onRetry = { account?.let { scraperRuns.runOnceInstalled(it) } },
                    onNext = ::nextSource,
                )
            }
            Screen.Opening -> OpeningScreen(
                progress = syncProgress,
                onBrowse = { screen = Screen.Vault },
            )
            Screen.Vault -> VaultContainer(
                initialFocus = vaultEntry,
                initialFolderSheet = vaultEntrySheet,
                documentChanges = documentChanges,
                onDocumentChange = { scope.launch { documentChanges++ } },
                navigation = navigation,
            )
            Screen.Settings -> SettingsScreen(
                openSection = settingsSection,
                onOpenSectionChange = { settingsSection = it },
                currentAppearance = appearance,
                onAppearanceChange = { newAppearance ->
                    onAppearanceChange(newAppearance)
                    appearanceError = null
                    scope.launch {
                        FydeSdk.setSetting(APPEARANCE_SETTING, newAppearance.id)
                            .onFailure { appearanceError = getString(Res.string.appearance_save_failed, it.userMessage()) }
                    }
                },
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
                navigation = navigation,
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
                appearanceError = appearanceError,
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
                        accountChanged++
                        disconnectingSessionId = null
                        // Last: showSnackbar suspends until the message is gone.
                        if (revoked.isSuccess) {
                            snackbarHostState.showSnackbar(getString(Res.string.sessions_disconnected, session.deviceName))
                        }
                    }
                },
                disconnectingSessionId = disconnectingSessionId,
                disconnectSessionError = disconnectSessionError,
                analyzePassword = { FydeSdk.analyzePassword(it) },
                onChangePassword = { current, new ->
                    changePasswordError = null
                    isChangingPassword = true
                    scope.launch {
                        val changed = FydeSdk.changePassword(current, new)
                            .onFailure { changePasswordError = getString(Res.string.change_password_failed, it.userMessage()) }
                        isChangingPassword = false
                        if (changed.isSuccess) {
                            if (settingsSection == SettingsSection.ChangePassword) settingsSection = SettingsSection.Security
                            snackbarHostState.showSnackbar(getString(Res.string.change_password_done))
                        }
                    }
                },
                isChangingPassword = isChangingPassword,
                changePasswordError = changePasswordError,
                recoveryKeyStatus = recoveryKeyStatus,
                recoveryKit = recoveryKit,
                onGenerateRecoveryKey = {
                    recoveryKit = RecoveryKitUiState.Generating
                    scope.launch {
                        val generated = FydeSdk.generateRecoveryKey()
                        recoveryKit = generated.fold(
                            // Only shown if the page is still open: the words are never kept for later.
                            onSuccess = { if (settingsSection == SettingsSection.RecoveryKit) RecoveryKitUiState.Ready(it) else RecoveryKitUiState.Idle },
                            onFailure = { RecoveryKitUiState.Failed(it.userMessage()) },
                        )
                        if (generated.isSuccess) recoveryKeyStatus = fetchRecoveryKeyStatus()
                    }
                },
                onRecoveryKitDone = {
                    recoveryKit = RecoveryKitUiState.Idle
                    settingsSection = SettingsSection.Security
                },
            )
            Screen.Scripts -> SourcesTabContainer(
                scraperRuns = scraperRuns,
                changelogChanges = documentChanges,
                onSourcesChange = { accountChanged++ },
                navigation = navigation,
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
        // Keyed on the question itself: when several runs ask at once, the
        // next one's dialog starts empty even if it asks the same thing.
        scraperRuns.question?.let { pending ->
            key(pending) {
                ScraperQuestionDialog(
                    scriptName = pending.sourceName,
                    question = pending.text,
                    onAnswer = { text -> scraperRuns.answer(pending, text) },
                    onStop = { scraperRuns.stop(pending.installationId) },
                )
            }
        }
    }
}

/**
 * Connects to the fyde server and then runs [credentialCall] (either
 * [FydeSdk.login] or [FydeSdk.register]) against that connection, so the
 * session token it stores is set before [VaultContainer] opens the
 * changelog stream. [FydeSdk.initialize] is safe to call again from
 * [VaultContainer] afterwards — it's a no-op reconnect.
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

/** Every document [FydeSdk.searchDocuments] finds for [text] and [filters] in [order], fetching every page. */
private suspend fun searchAllDocuments(text: String, filters: FydeSearchFilters, order: FydeSearchOrder): List<FydeDocument> = buildList {
    var offset = 0L
    do {
        val page = FydeSdk.searchDocuments(text, filters, order, offset, DOCUMENT_PAGE_SIZE).getOrThrow()
        addAll(page)
        offset += page.size
    } while (page.size.toLong() == DOCUMENT_PAGE_SIZE)
}

/** Each folder's document count, its subfolders' included, by id. */
private fun FydeSearchFacets.folderCounts(): Map<String, Long> = folders.associate { it.value to it.count }

/** The newest document in [filter], `null` when it holds none. */
private suspend fun newestIn(filter: FydeFolderFilter): FydeDocument? =
    FydeSdk.searchDocuments("", FydeSearchFilters(folder = filter), FydeSearchOrder.NEWEST, 0, 1).getOrThrow().firstOrNull()

/** [folders]' cards: each one's count (subfolders included) and newest document; an empty one has none to look up. */
private suspend fun folderSummaries(folders: List<FydeFolder>, counts: Map<String, Long>): List<VaultFolderSummary> =
    folders.map { folder ->
        val count = counts[folder.id] ?: 0
        VaultFolderSummary(folder, count, if (count > 0) newestIn(FydeFolderFilter.Within(folder.id)) else null)
    }

/**
 * The vault's home: À classer while it holds documents, then every
 * top-level folder by name with its count (from the facets) and newest
 * document, then the latest arrivals wherever they are.
 */
private suspend fun loadVaultHome(folders: VaultFolders): VaultHome {
    val facets = FydeSdk.searchDocumentFacets("", FydeSearchFilters()).getOrThrow()
    val unfiled = if (facets.unfiled > 0) {
        listOf(VaultFolderSummary(null, facets.unfiled, newestIn(FydeFolderFilter.Unfiled)))
    } else {
        emptyList()
    }
    val latest = FydeSdk.searchDocuments("", FydeSearchFilters(), FydeSearchOrder.NEWEST, 0, LATEST_ARRIVALS).getOrThrow()
    val counts = facets.folderCounts()
    return VaultHome(unfiled + folderSummaries(folders.topLevel, counts), latest, counts)
}

/**
 * A folder page's subfolders and documents. The vault-wide facets give
 * every folder's count (the folder facet ignores the folder filter); the
 * page's own facets, with the folder and year applied, give the year chips
 * (the date span ignores only the date filters).
 */
private suspend fun loadVaultFolder(page: VaultPage.Folder, folders: VaultFolders): VaultFolderContents {
    val query = page.query
    val all = FydeSdk.searchDocumentFacets("", FydeSearchFilters()).getOrThrow()
    val counts = all.folderCounts()
    val facets = FydeSdk.searchDocumentFacets("", query.toSearchFilters()).getOrThrow()
    val order = if (page.newestFirst) FydeSearchOrder.NEWEST else FydeSearchOrder.OLDEST
    val subfolders = page.folderId?.let { folders.children(it) }.orEmpty()
    return VaultFolderContents(
        page = page,
        subfolders = subfolders.map { VaultFolderSummary(it, counts[it.id] ?: 0) },
        count = if (page.folderId == null) all.unfiled else counts[page.folderId] ?: 0,
        options = vaultFilterOptions(facets, query),
        documents = searchAllDocuments("", query.toSearchFilters(), order),
    )
}

/**
 * The search page's chips and, once it [VaultQuery.showsResults], every
 * document found: most relevant first for typed text, newest first for a
 * type or year alone.
 */
private suspend fun loadVaultSearch(query: VaultQuery): VaultSearchContents {
    val text = query.text.trim()
    val filters = query.toSearchFilters()
    val facets = FydeSdk.searchDocumentFacets(text, filters).getOrThrow()
    val documents = if (query.showsResults) {
        searchAllDocuments(text, filters, if (text.isNotEmpty()) FydeSearchOrder.RELEVANCE else FydeSearchOrder.NEWEST)
    } else {
        emptyList()
    }
    return VaultSearchContents(query, vaultFilterOptions(facets, query), documents)
}

/**
 * What [publishEach] got through: the ids of the documents it [done], those
 * [refused] and the first [refusal] (`null` when none was).
 */
private class BatchOutcome(val total: Int, val done: Set<String>, val refused: List<FydeDocument>, val refusal: Throwable?) {
    /** The error note: the SDK's wording alone for one document, else how many went with it. */
    suspend fun message(refusal: Throwable, several: PluralStringResource): String = if (total == 1) {
        refusal.userMessage()
    } else {
        getPluralString(several, refused.size, refused.size, total, refusal.userMessage())
    }
}

/**
 * Publishes [change] for each of [documents] in turn — fyde-sdk has no batch
 * call, and one `FydeClient` takes no concurrent ones — going on past a
 * refusal, so one refused document doesn't hold the others back. What was
 * sent stays sent.
 */
private suspend fun publishEach(documents: List<FydeDocument>, change: suspend (FydeDocument) -> Result<Unit>): BatchOutcome {
    val done = mutableSetOf<String>()
    val refused = mutableListOf<FydeDocument>()
    var refusal: Throwable? = null
    for (document in documents) {
        change(document)
            .onSuccess { done += document.id }
            .onFailure {
                if (it is CancellationException) throw it
                refused += document
                if (refusal == null) refusal = it
            }
    }
    return BatchOutcome(documents.size, done, refused, refusal)
}

/**
 * Owns the `fyde-sdk` connection and the changelog subscription, and maps
 * their results into [VaultUiState]: the folders, the home's summary, the
 * open folder's contents and the open search, all reloaded on every changelog event. Holds which
 * page is open ([VaultFocus]); [VaultScreen] only draws it.
 */
@Composable
private fun VaultContainer(
    initialFocus: VaultFocus,
    initialFolderSheet: FolderSheet?,
    documentChanges: Int,
    onDocumentChange: () -> Unit,
    navigation: TopLevelNavigation,
) {
    var retryKey by remember { mutableStateOf(0) }
    var connected by remember { mutableStateOf(false) }
    var connectError by remember { mutableStateOf<String?>(null) }
    // Bumped by Retry after a failed load; the changelog bumps documentChanges.
    var reloadKey by remember { mutableStateOf(0) }
    val changeKey = documentChanges to reloadKey
    var focus by remember { mutableStateOf(initialFocus) }
    var folders by remember { mutableStateOf<VaultFolders?>(null) }
    var home by remember { mutableStateOf<VaultHome?>(null) }
    var folder by remember { mutableStateOf<VaultFolderContents?>(null) }
    var search by remember { mutableStateOf<VaultSearchContents?>(null) }
    var documentSimilar by remember { mutableStateOf<VaultSimilarContents?>(null) }
    var similar by remember { mutableStateOf<VaultSimilarContents?>(null) }
    var serverReachable by remember { mutableStateOf(true) }
    var deviceCount by remember { mutableStateOf<Int?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var deletion by remember { mutableStateOf<DocumentDeletion?>(null) }
    var folderSheet by remember { mutableStateOf(initialFolderSheet?.let(::FolderSheetState)) }
    var documentMove by remember { mutableStateOf<DocumentMove?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalAppSnackbarHostState.current

    // A reload can bring the open document's new metadata (its
    // classification landing after the upload, say).
    fun refreshOpenDocument(documents: List<FydeDocument>) {
        val open = focus.document ?: return
        val fresh = documents.firstOrNull { it.id == open.id } ?: return
        if (fresh != open) focus = focus.copy(document = fresh)
    }

    suspend fun reportLoadFailure(error: Throwable, message: StringResource = Res.string.vault_load_failed) {
        if (error is CancellationException) throw error
        loadError = getString(message, error.userMessage())
    }

    // Picks one or more local files and uploads each via fyde-sdk,
    // sequentially (one FydeClient, so no concurrent calls into it). No
    // manual refresh is needed on success: each upload publishes a "created"
    // changelog event, which the initialize() callback below is already
    // listening for and will reload the vault in response to.
    val pickDocuments = rememberDocumentPicker { paths ->
        if (paths.isEmpty()) return@rememberDocumentPicker
        isUploading = true
        notice = null
        scope.launch {
            val failures = paths.mapNotNull { path -> FydeSdk.uploadDocument(path).exceptionOrNull() }
            val succeeded = paths.size - failures.size
            if (failures.isNotEmpty()) {
                notice = if (paths.size == 1) {
                    getString(Res.string.upload_failed, failures.first().userMessage())
                } else {
                    getString(Res.string.uploads_failed, failures.size, paths.size)
                }
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
                notice = getString(Res.string.document_save_failed, fileName)
            }
        }
    }

    // fyde-sdk consumes the changelog in the background for the lifetime of
    // the session (started by login/register, or immediately here if one is
    // already persisted); its callback only says something changed.
    LaunchedEffect(retryKey) {
        connected = false
        connectError = null
        val initResult = FydeSdk.initialize(BuildConfig.SERVER_URL) { onDocumentChange() }
        if (initResult.isFailure) {
            connectError = initResult.exceptionOrNull()?.userMessage() ?: getString(Res.string.error_unknown)
            return@LaunchedEffect
        }
        connected = true
    }

    // Folders first: the home's cards and the open folder's page are drawn from them.
    LaunchedEffect(connected, changeKey) {
        if (!connected) return@LaunchedEffect
        runCatching { VaultFolders(FydeSdk.listFolders().getOrThrow()) }
            .onSuccess { loaded ->
                folders = loaded
                // A folder deleted meanwhile, here or on another device, closes its page.
                val open = (focus.page as? VaultPage.Folder)?.folderId
                if (open != null && loaded[open] == null) focus = VaultFocus()
            }
            .onFailure { reportLoadFailure(it) }
    }

    LaunchedEffect(connected, changeKey, folders) {
        val loadedFolders = folders
        if (!connected || loadedFolders == null) return@LaunchedEffect
        runCatching { loadVaultHome(loadedFolders) }
            .onSuccess {
                home = it
                loadError = null
                refreshOpenDocument(it.latest)
            }
            .onFailure { reportLoadFailure(it) }
        FydeSdk.listSessions().getOrNull()?.let { deviceCount = it.size }
    }

    // A newer page (a chip picked, the sort flipped) cancels one still
    // loading. The last contents stay up meanwhile within the same folder,
    // so its chips don't vanish while picking one.
    val folderPage = focus.page as? VaultPage.Folder
    LaunchedEffect(connected, changeKey, folderPage, folders) {
        val loadedFolders = folders
        if (!connected || folderPage == null || loadedFolders == null) return@LaunchedEffect
        if (folder?.page?.folderId != folderPage.folderId) folder = null
        runCatching { loadVaultFolder(folderPage, loadedFolders) }
            .onSuccess {
                folder = it
                loadError = null
                refreshOpenDocument(it.documents)
            }
            .onFailure { reportLoadFailure(it) }
    }

    // Typing restarts this effect, so a search only runs once the text has
    // stood still for SEARCH_DEBOUNCE_MS; picking a chip searches at once.
    val searchQuery = (focus.page as? VaultPage.Search)?.query
    LaunchedEffect(connected, changeKey, searchQuery) {
        if (searchQuery == null) search = null
        if (!connected || searchQuery == null) return@LaunchedEffect
        if (searchQuery.text.isNotBlank() && searchQuery.text != search?.query?.text) delay(SEARCH_DEBOUNCE_MS)
        runCatching { loadVaultSearch(searchQuery) }
            .onSuccess {
                search = it
                loadError = null
                refreshOpenDocument(it.documents)
            }
            .onFailure { reportLoadFailure(it, Res.string.search_failed) }
    }

    // The open document's similar section (V6c, D2). It's an aside: when the
    // SDK can't say (the document deleted meanwhile, say) it's hidden, not an error.
    val openDocumentId = focus.document?.id
    LaunchedEffect(connected, changeKey, openDocumentId) {
        if (openDocumentId == null) documentSimilar = null
        if (!connected || openDocumentId == null) return@LaunchedEffect
        FydeSdk.similarDocuments(openDocumentId, SIMILAR_SECTION_LIMIT)
            .onSuccess { documentSimilar = VaultSimilarContents(openDocumentId, it) }
            .onFailure {
                if (it is CancellationException) throw it
                documentSimilar = null
            }
    }

    // The similar page (V16, D11): reloaded on every change, so deleted
    // documents leave it and moved ones show their new folder once synced.
    val similarOf = (focus.page as? VaultPage.Similar)?.document?.id
    LaunchedEffect(connected, changeKey, similarOf) {
        if (similarOf == null || similar?.documentId != similarOf) similar = null
        if (!connected || similarOf == null) return@LaunchedEffect
        runCatching { FydeSdk.similarDocuments(similarOf, SIMILAR_PAGE_LIMIT).getOrThrow() }
            .onSuccess { documents ->
                similar = VaultSimilarContents(similarOf, documents)
                loadError = null
                refreshOpenDocument(documents.map { it.document })
            }
            .onFailure { reportLoadFailure(it) }
    }

    // After a batch change: the ticks of the documents it reached clear and,
    // with the document the similar page started from deleted, that page
    // gives way to the one it was opened from.
    fun afterBatch(done: Set<String>, deleted: Boolean) {
        var next = focus
        if (deleted && next.document?.id in done) next = next.copy(document = null, viewerOpen = false)
        val page = next.page
        if (page is VaultPage.Similar) {
            next = if (deleted && page.document.id in done) {
                VaultFocus(page = page.origin)
            } else {
                next.copy(page = page.copy(ticked = page.ticked - done))
            }
        }
        focus = next
    }

    // Polls fyde-sdk's own reachability check independently of the changelog
    // stream above, so the Import button and the offline note reflect
    // whether calls into the SDK are currently expected to succeed.
    LaunchedEffect(connected) {
        while (connected) {
            serverReachable = FydeSdk.isServerReachable().getOrDefault(false)
            delay(SERVER_REACHABILITY_POLL_INTERVAL_MS)
        }
    }

    VaultScreen(
        state = when {
            connectError != null -> VaultUiState.ConnectFailed(message = connectError.orEmpty(), onRetry = { retryKey++ })
            !connected -> VaultUiState.Connecting
            else -> VaultUiState.Loaded(
                folders = folders ?: VaultFolders(),
                home = home,
                folder = folder,
                search = search,
                documentSimilar = documentSimilar,
                similar = similar,
                serverReachable = serverReachable,
                deviceCount = deviceCount,
                isUploading = isUploading,
                notice = notice,
                loadError = loadError,
                deletion = deletion,
                folderSheet = folderSheet,
                documentMove = documentMove,
            )
        },
        focus = focus,
        onFocusChange = {
            focus = it
            notice = null
        },
        onUpload = pickDocuments,
        // Listings carry no content: fetch it right before saving.
        onSave = { document ->
            notice = null
            scope.launch {
                val content = FydeSdk.getDocumentContent(document.id).getOrNull()
                if (content != null) {
                    saveDocument(document.downloadFileName, content)
                } else {
                    notice = getString(Res.string.document_save_failed, document.name)
                }
            }
        },
        loadContent = { document -> FydeSdk.getDocumentContent(document.id) },
        // Never asked for where none is drawn (Android, iOS).
        loadThumbnail = if (FydeSdk.showsThumbnails) {
            { id -> FydeSdk.documentThumbnail(id) }
        } else {
            null
        },
        onRetryLoad = {
            loadError = null
            reloadKey++
        },
        onDeletionChange = { deletion = it },
        // Each document leaves the lists once sync consumes its Deleted event,
        // which reloads them like any other change.
        onDelete = { documents ->
            deletion = DocumentDeletion(documents, busy = true)
            scope.launch {
                val outcome = publishEach(documents) { FydeSdk.deleteDocument(it.id) }
                afterBatch(outcome.done, deleted = true)
                val refusal = outcome.refusal
                if (refusal == null) {
                    deletion = null
                    snackbarHostState.showSnackbar(
                        if (documents.size == 1) {
                            getString(Res.string.vault_deleted)
                        } else {
                            getPluralString(Res.plurals.documents_deleted, documents.size, documents.size)
                        },
                    )
                } else {
                    deletion = DocumentDeletion(outcome.refused, error = outcome.message(refusal, Res.plurals.documents_delete_failed))
                }
            }
        },
        navigation = navigation,
        folderCallbacks = remember(scope, snackbarHostState) {
            // Each change only publishes its event: the folders and pages
            // reload once sync consumes it, like any other change.
            fun publish(confirmation: StringResource, onDone: () -> Unit = {}, change: suspend () -> Result<*>) {
                val open = folderSheet ?: return
                folderSheet = open.copy(busy = true, error = null)
                scope.launch {
                    change()
                        .onSuccess {
                            folderSheet = null
                            onDone()
                            snackbarHostState.showSnackbar(getString(confirmation))
                        }
                        .onFailure { folderSheet = open.copy(busy = false, error = it.userMessage()) }
                }
            }
            FolderCallbacks(
                onSheetChange = { folderSheet = it },
                onDocumentMoveChange = { documentMove = it },
                // A drop has no sheet to keep a failure in: both outcomes are snackbars.
                onDropDocument = { document, folderId ->
                    scope.launch {
                        FydeSdk.moveDocument(document.id, folderId).fold(
                            onSuccess = { snackbarHostState.showSnackbar(getString(Res.string.document_moved)) },
                            onFailure = { snackbarHostState.showSnackbar(it.userMessage()) },
                        )
                    }
                },
                onDropFolder = { id, parentId ->
                    scope.launch {
                        FydeSdk.moveFolder(id, parentId).fold(
                            onSuccess = { snackbarHostState.showSnackbar(getString(Res.string.folder_moved)) },
                            onFailure = { snackbarHostState.showSnackbar(it.userMessage()) },
                        )
                    }
                },
                // The documents show their new folder once sync consumes the moves.
                onMoveDocuments = { documents, folderId ->
                    documentMove = DocumentMove(documents, busy = true)
                    scope.launch {
                        val outcome = publishEach(documents) { FydeSdk.moveDocument(it.id, folderId) }
                        afterBatch(outcome.done, deleted = false)
                        val refusal = outcome.refusal
                        if (refusal == null) {
                            documentMove = null
                            snackbarHostState.showSnackbar(
                                if (documents.size == 1) {
                                    getString(Res.string.document_moved)
                                } else {
                                    getPluralString(Res.plurals.documents_moved, documents.size, documents.size)
                                },
                            )
                        } else {
                            documentMove = DocumentMove(outcome.refused, error = outcome.message(refusal, Res.plurals.documents_move_failed))
                        }
                    }
                },
                onCreate = { parentId, form ->
                    publish(Res.string.folder_created) { FydeSdk.createFolder(parentId, form.name, form.appearance) }
                },
                onUpdate = { id, form ->
                    publish(Res.string.folder_saved) { FydeSdk.updateFolder(id, form.name, form.appearance) }
                },
                onMove = { id, parentId -> publish(Res.string.folder_moved) { FydeSdk.moveFolder(id, parentId) } },
                onDelete = { id, contents ->
                    // Its page, or a subfolder's, gives way to its parent's (home from the top level).
                    val parentId = folders?.get(id)?.parentId
                    val gone = folders?.withDescendants(id).orEmpty()
                    publish(
                        Res.string.folder_deleted,
                        onDone = {
                            val open = (focus.page as? VaultPage.Folder)?.folderId
                            if (open != null && open in gone) {
                                focus = VaultFocus(page = if (parentId != null) VaultPage.Folder(parentId) else VaultPage.Home)
                            }
                        },
                    ) { FydeSdk.deleteFolder(id, contents) }
                },
            )
        },
    )
}

/**
 * Owns the `fyde-sdk` calls backing [SourcesTabScreen]: every scraper on
 * offer ([FydeSdk.listPublicScripts] + [FydeSdk.listUserScripts], parsers
 * left out), the user's accounts ([FydeSdk.listInstallations], re-read on
 * every changelog event, [changelogChanges], so accounts added or removed
 * on another device show up), and the plan's source quota
 * ([FydeSdk.getUsage]). Scripts and quota are fetched once per visit
 * (plus [retryKey] bumps).
 */
@Composable
private fun SourcesTabContainer(
    scraperRuns: ScraperRuns,
    changelogChanges: Int,
    onSourcesChange: () -> Unit,
    navigation: TopLevelNavigation,
    onOpenPlan: () -> Unit,
) {
    var retryKey by remember { mutableStateOf(0) }
    var uiState by remember { mutableStateOf<SourcesTabUiState>(SourcesTabUiState.Connecting) }
    var focus by remember { mutableStateOf<SourceFocus?>(null) }
    // The vault's folders, for where each account files its documents, and
    // those created here that sync hasn't brought back yet.
    var syncedFolders by remember { mutableStateOf<List<FydeFolder>>(emptyList()) }
    var createdFolders by remember { mutableStateOf<List<FydeFolder>>(emptyList()) }
    var catalogueOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    // Each account's last report on this device, by installation id: its
    // status until this app session runs it.
    var lastRuns by remember { mutableStateOf<Map<String, FydeScraperLastRun>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalAppSnackbarHostState.current

    fun update(transform: (SourcesTabUiState.Loaded) -> SourcesTabUiState.Loaded) {
        (uiState as? SourcesTabUiState.Loaded)?.let { uiState = transform(it) }
    }

    suspend fun refreshQuota() {
        val quota = FydeSdk.getUsage().getOrNull()?.let(::sourceQuotaOf)
        update { it.copy(quota = quota) }
    }

    fun showFolders() {
        createdFolders = createdFolders.filter { created -> syncedFolders.none { it.id == created.id } }
        update { it.copy(folders = VaultFolders(syncedFolders + createdFolders)) }
    }

    suspend fun refreshFolders() {
        syncedFolders = FydeSdk.listFolders().getOrNull() ?: return
        showFolders()
    }

    suspend fun refreshLastRuns() {
        // Latest first, so the first of an installation's is its last.
        lastRuns = FydeSdk.lastScriptRuns().getOrNull()?.distinctBy { it.installationId }?.associateBy { it.installationId } ?: return
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
            installations = FydeSdk.listInstallations().getOrElse { emptyList() },
        )
        refreshFolders()
        refreshLastRuns()
        refreshQuota()

        fetchScriptIcons(scripts) { scriptId, icon ->
            update { latest -> latest.copy(scripts = latest.scripts?.map { if (it.id == scriptId) it.copy(icon = icon) else it }) }
        }
    }

    // What sync has brought: an account published here (shown at once,
    // provisionally, below) replaced by the SDK's own, one added or removed
    // on another device. One being installed or removed here keeps its
    // provisional state until its own call ends.
    LaunchedEffect(changelogChanges) {
        if (uiState !is SourcesTabUiState.Loaded || changelogChanges == 0) return@LaunchedEffect
        refreshFolders()
        refreshLastRuns()
        val synced = FydeSdk.listInstallations().getOrNull() ?: return@LaunchedEffect
        update { latest ->
            val inFlight = latest.installations.filter { it.id in latest.pendingIds }
            latest.copy(installations = synced.filter { it.id !in latest.pendingIds } + inFlight)
        }
    }

    // Installing publishes the account's values through the changelog (so it
    // syncs to every device on the account) and enables its source, idempotent
    // for one already enabled; [existing] reinstalls that account, else it's a
    // new one, its documents filed in [folderId] (automatically when null).
    // Shown at once with a provisional label until sync returns the SDK's.
    fun install(
        script: FydeScript,
        existing: SourceAccount?,
        label: String,
        parameters: Map<String, FydeScriptParameterValue>,
        folderId: String?,
        onDone: suspend (SourceAccount?, Throwable?) -> Unit,
    ) {
        val current = uiState as? SourcesTabUiState.Loaded ?: return
        val key = existing?.installationId ?: script.id
        if (key in current.pendingIds) return
        uiState = current.copy(pendingIds = current.pendingIds + key, errors = current.errors - key)
        scope.launch {
            if (folderId != null) awaitFolderSynced(folderId)
            val result = FydeSdk.installScript(existing?.installationId, script.id, label, parameters, folderId)
                .mapCatching { id -> FydeSdk.enableScript(script.id).getOrThrow(); id }
            val installed = result.getOrNull()?.let { id ->
                FydeInstallation(id, script.id, label.trim().ifEmpty { provisionalLabel(script, parameters) }, parameters, folderId)
            }
            update { latest ->
                latest.copy(
                    pendingIds = latest.pendingIds - key,
                    installations = if (installed == null) {
                        latest.installations
                    } else {
                        latest.installations.filter { it.id != installed.id } + installed
                    },
                )
            }
            val account = installed?.let { (uiState as? SourcesTabUiState.Loaded)?.accounts?.find { it.installationId == installed.id } }
            onDone(account, result.exceptionOrNull())
        }
    }

    SourcesTabScreen(
        state = uiState,
        statuses = (uiState as? SourcesTabUiState.Loaded)?.installations.orEmpty()
            .mapNotNull { installation ->
                (sourceStatusOf(installation.id, scraperRuns) ?: lastRuns[installation.id]?.let(::sourceStatusOfReport))
                    ?.let { installation.id to it }
            }
            .toMap(),
        runningIds = scraperRuns.runningIds,
        stoppingIds = scraperRuns.stoppingIds,
        focus = focus,
        onFocusChange = { focus = it },
        catalogueOpen = catalogueOpen,
        onCatalogueOpenChange = { catalogueOpen = it },
        query = query,
        onQueryChange = { query = it },
        onConnect = { script, label, parameters, folderId ->
            install(script, existing = null, label, parameters, folderId) { account, error ->
                if (account != null) {
                    refreshQuota()
                    onSourcesChange()
                    scraperRuns.runOnceInstalled(account, parameters)
                    focus = SourceFocus.Fetching(account.installationId)
                    catalogueOpen = false
                } else {
                    val message = getString(Res.string.connect_source_failed, script.name, error?.userMessage().orEmpty())
                    update { it.copy(errors = it.errors + (script.id to message)) }
                }
            }
        },
        onUpdate = { existing, label, parameters, rerun ->
            // Editing the credentials keeps the account's folder.
            install(existing.script, existing, label, parameters, existing.folderId) { account, error ->
                val id = existing.installationId
                when {
                    account == null -> {
                        val message = getString(Res.string.source_update_failed, existing.name, error?.userMessage().orEmpty())
                        update { it.copy(errors = it.errors + (id to message)) }
                    }
                    rerun -> {
                        scraperRuns.runOnceInstalled(account, parameters)
                        focus = SourceFocus.Fetching(id)
                    }
                    else -> {
                        focus = SourceFocus.Detail(id)
                        snackbarHostState.showSnackbar(getString(Res.string.source_credentials_saved, account.name))
                    }
                }
            }
        },
        // Reinstalls the account as it is, its documents now filed in
        // [folderId]; only later documents go there. No snackbar on success:
        // the row shows the new folder at once.
        onChangeFolder = { account, folderId ->
            install(account.script, account, account.label, account.parameters, folderId) { installed, error ->
                if (installed == null) {
                    snackbarHostState.showSnackbar(getString(Res.string.source_update_failed, account.name, error?.userMessage().orEmpty()))
                }
            }
        },
        createFolder = { parentId, form ->
            FydeSdk.createFolder(parentId, form.name, form.appearance).fold(
                onSuccess = { folder ->
                    createdFolders = createdFolders + folder
                    showFolders()
                    FolderCreation.Created(folder)
                },
                onFailure = { FolderCreation.Refused(it.userMessage()) },
            )
        },
        onRun = { account -> scraperRuns.run(account) },
        onStop = { account -> scraperRuns.stop(account.installationId) },
        // Uninstalling erases the account's credentials and sign-in on every
        // device; its source is disabled with its last account, freeing its
        // place in the plan.
        onRemove = { account ->
            val current = uiState as? SourcesTabUiState.Loaded
            val id = account.installationId
            if (current != null && id !in current.pendingIds) {
                uiState = current.copy(pendingIds = current.pendingIds + id)
                scope.launch {
                    scraperRuns.stop(id)
                    val last = current.accountsOf(account.script.id).none { it.installationId != id }
                    val error = FydeSdk.uninstallScript(id)
                        .mapCatching { if (last) FydeSdk.disableScript(account.script.id).getOrThrow() }
                        .exceptionOrNull()
                    update { latest ->
                        latest.copy(
                            pendingIds = latest.pendingIds - id,
                            installations = if (error == null) latest.installations.filter { it.id != id } else latest.installations,
                        )
                    }
                    if (error == null) {
                        if (focus.let { it is SourceFocus.Detail && it.installationId == id || it is SourceFocus.Credentials && it.installationId == id }) {
                            focus = null
                        }
                        refreshQuota()
                        onSourcesChange()
                        snackbarHostState.showSnackbar(getString(Res.string.source_removed, account.name))
                    } else {
                        snackbarHostState.showSnackbar(getString(Res.string.source_remove_failed, account.name, error.userMessage()))
                    }
                }
            }
        },
        onOpenPlan = onOpenPlan,
        navigation = navigation,
    )
}

/**
 * Waits, up to [FOLDER_SYNC_TIMEOUT_MS], for the folder [id] to sync to this
 * device: [FydeSdk.createFolder] only publishes it, and [FydeSdk.installScript]
 * refuses a folder it doesn't know yet. Past the wait, the install says so.
 */
private suspend fun awaitFolderSynced(id: String) {
    withTimeoutOrNull(FOLDER_SYNC_TIMEOUT_MS) {
        while (FydeSdk.listFolders().getOrNull()?.any { it.id == id } != true) delay(FOLDER_SYNC_POLL_MS)
    }
}

private const val FOLDER_SYNC_POLL_MS = 300L
private const val FOLDER_SYNC_TIMEOUT_MS = 15_000L

/**
 * Where [installationId]'s latest run in this app session is: waiting for
 * its installation to reach this device, running, or over; `null` if it
 * hasn't been run here (the Sources tab then shows its last report's, see
 * [sourceStatusOfReport]).
 */
private fun sourceStatusOf(installationId: String, runs: ScraperRuns): SourceFetchStatus? {
    runs.runOf(installationId)?.let { run ->
        return if (run.awaitingInstallation) SourceFetchStatus.Queued else SourceFetchStatus.Running(run.stepKind, run.update, run.found)
    }
    return when (val outcome = runs.outcomes[installationId]) {
        is ScraperRunOutcome.Succeeded -> SourceFetchStatus.Succeeded(outcome.found)
        is ScraperRunOutcome.Failed -> SourceFetchStatus.Failed(outcome.message, outcome.step, outcome.found)
        is ScraperRunOutcome.Stopped -> SourceFetchStatus.Stopped(outcome.step, outcome.update, outcome.found)
        null -> null
    }
}

/** [sourceStatusOf] for [SourceFetchingScreen], which opens as the run is about to start. */
private fun sourceFetchStatusOf(installationId: String, runs: ScraperRuns): SourceFetchStatus =
    sourceStatusOf(installationId, runs) ?: SourceFetchStatus.Queued
