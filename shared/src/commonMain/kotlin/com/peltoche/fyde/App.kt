package com.peltoche.fyde

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

/** How many of the most recently cached documents to show; the screen
 * doesn't paginate yet. */
private const val DOCUMENT_LIST_LIMIT = 100L

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

        when (screen) {
            // fyde-sdk has no auth API yet (see ../../../CLAUDE.md on the
            // Users service) — accepting any non-blank credentials here is a
            // placeholder until that's wired up.
            Screen.Login -> LoginScreen(
                onLogIn = { _, _ -> screen = Screen.Documents },
                onNavigateToRegister = { screen = Screen.Register },
            )
            Screen.Register -> RegisterScreen(
                onRegister = { _, _ -> screen = Screen.Documents },
                onNavigateToLogin = { screen = Screen.Login },
            )
            Screen.Documents -> DocumentsScreen()
        }
    }
}

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

    LaunchedEffect(retryKey) {
        uiState = DocumentListUiState.Connecting

        val initResult = FydeSdk.initialize("http://127.0.0.1:40051")
        if (initResult.isFailure) {
            uiState = DocumentListUiState.ConnectFailed(
                message = initResult.exceptionOrNull()?.message ?: "Unknown error",
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
                banner = "Live updates stopped: ${error.message}",
                onDismissBanner = {
                    (uiState as? DocumentListUiState.Loaded)?.let { latest -> uiState = latest.copy(banner = null) }
                },
            )
        }
    }

    DocumentListScreen(state = uiState)
}
