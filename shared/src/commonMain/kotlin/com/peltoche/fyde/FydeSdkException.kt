package com.peltoche.fyde

import com.peltoche.fyde.resources.Res
import com.peltoche.fyde.resources.error_unknown
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
 */
class FydeSdkException(val userMessage: String?, cause: Throwable) : Exception(userMessage, cause)

/**
 * The wording to show the user for a failed [FydeSdk] call: fyde-sdk's
 * [FydeSdkException.userMessage] when there is one, [fallback] otherwise.
 * Never any other [Throwable.message], which is technical.
 */
internal suspend fun Throwable.userMessage(fallback: StringResource = Res.string.error_unknown): String =
    (this as? FydeSdkException)?.userMessage ?: getString(fallback)
