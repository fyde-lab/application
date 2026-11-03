package com.peltoche.fyde

import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.error_unknown
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * What every failed [FydeSdk] call fails with. [userMessage] is fyde-sdk's
 * own wording for the failure (its `FfiException` message), already in the
 * user's selected language and safe to show on screen as is, or `null` when
 * the failure didn't come from fyde-sdk itself (e.g. the native library
 * failed to load), in which case it has no wording fit for the user. The
 * technical description is never meant for the UI: fyde-sdk logs its own
 * through the log listener, and each platform's [FydeSdk] logs [cause].
 * [isClientAlreadyRunning] tells fyde-sdk's `FfiException.ClientAlreadyRunning`
 * apart: [FydeSdk.initialize] refusing to start while another instance of
 * the app already uses the same local data, which [App] shows in place of
 * every screen. [isRunStopped] tells `FfiException.ScraperRunStopped` apart:
 * a [FydeSdk.runInstalledScript] run ended by [FydeSdk.stopScriptRun], which
 * isn't a failure of its source.
 */
class FydeSdkException(
    val userMessage: String?,
    cause: Throwable,
    val isClientAlreadyRunning: Boolean = false,
    val isRunStopped: Boolean = false,
) : Exception(userMessage, cause)

/**
 * The wording to show the user for a failed [FydeSdk] call: fyde-sdk's
 * [FydeSdkException.userMessage] when there is one, [fallback] otherwise.
 * Never any other [Throwable.message], which is technical.
 */
internal suspend fun Throwable.userMessage(fallback: StringResource = Res.string.error_unknown): String =
    (this as? FydeSdkException)?.userMessage ?: getString(fallback)

/**
 * Announces every [FydeSdk] call that failed because the server rejected the
 * session token (fyde-sdk's `FfiException.SessionExpired`). By then fyde-sdk
 * has already logged out locally, so the app only has to go back to the
 * login screen: [App] collects [events] and does that wherever the failing
 * call came from. Each event carries the failure's user wording (see
 * [FydeSdkException.userMessage]).
 */
object SessionExpiry {
    private val expirations = MutableSharedFlow<String?>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val events: SharedFlow<String?> = expirations.asSharedFlow()

    /** Called by each platform's [FydeSdk] when a call fails with `SessionExpired`. */
    internal fun report(userMessage: String?) {
        expirations.tryEmit(userMessage)
    }
}
