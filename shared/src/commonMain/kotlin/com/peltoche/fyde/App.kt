package com.peltoche.fyde

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.auth_error_login_failed
import com.peltoche.fyde.resources.auth_error_register_failed
import com.peltoche.fyde.resources.error_unknown
import com.peltoche.fyde.resources.live_updates_stopped
import com.peltoche.fyde.resources.upload_failed
import com.peltoche.fyde.resources.uploads_failed
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** How many of the most recently cached documents to show; the screen
 * doesn't paginate yet. */
private const val DOCUMENT_LIST_LIMIT = 100L

private const val FYDE_SERVER_URL = "http://127.0.0.1:40051"

/** Color tokens from DESIGN.md's dark (and only, for now) theme. */
private val FydeDarkColorScheme = darkColorScheme(
    background = Color(0xFF141414),
    surface = Color(0xFF141414),
    surfaceVariant = Color(0xFF242424),
    surfaceContainerLow = Color(0xFF191919),
    surfaceContainer = Color(0xFF1F1F1F),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF2A2A2A),
    onBackground = Color(0xFFF2F2F2),
    onSurface = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF8A8A8E),
    outline = Color(0xFF3A3A3C),
    outlineVariant = Color(0xFF2A2A2C),
    primary = PdfAccentColor,
    onPrimary = Color(0xFF141414),
    primaryContainer = Color(0xFF333B12),
    onPrimaryContainer = Color(0xFFE9F79B),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF141414),
    errorContainer = Color(0xFF3A1616),
    onErrorContainer = Color(0xFFFFB4A9),
)

/** Which top-level screen [App] is currently showing. */
private sealed interface Screen {
    data object Login : Screen
    data object Register : Screen
    data object Documents : Screen
}

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = FydeDarkColorScheme) {
        var screen by remember { mutableStateOf<Screen>(Screen.Login) }
        var authError by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        // Startup check: if a session token is already persisted from a
        // previous run, skip straight to the document list instead of
        // making the user log in again.
        LaunchedEffect(Unit) {
            FydeSdk.initialize(FYDE_SERVER_URL).onSuccess {
                if (FydeSdk.isConnected().getOrDefault(false)) {
                    screen = Screen.Documents
                }
            }
        }

        when (screen) {
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
            Screen.Documents -> DocumentsScreen()
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
private fun DocumentsScreen() {
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

        val initResult = FydeSdk.initialize(FYDE_SERVER_URL)
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

        // Re-lists the cached documents every time a new changelog event
        // comes in, so the screen stays in sync with what fyde-sdk has
        // materialized locally. Suspends for as long as the stream is open,
        // i.e. for the lifetime of this effect.
        FydeSdk.consume {
            launch {
                val refreshed = FydeSdk.listDocuments(offset = 0, limit = DOCUMENT_LIST_LIMIT)
                    .getOrElse { (uiState as? DocumentListUiState.Loaded)?.documents ?: emptyList() }
                uiState = DocumentListUiState.Loaded(documents = refreshed)
            }
        }.onFailure { error ->
            val current = uiState as? DocumentListUiState.Loaded ?: return@onFailure
            uiState = current.copy(
                banner = getString(Res.string.live_updates_stopped, error.message ?: ""),
                onDismissBanner = {
                    (uiState as? DocumentListUiState.Loaded)?.let { latest -> uiState = latest.copy(banner = null) }
                },
            )
        }
    }

    DocumentListScreen(state = uiState, onUpload = pickDocuments, isUploading = isUploading)
}
