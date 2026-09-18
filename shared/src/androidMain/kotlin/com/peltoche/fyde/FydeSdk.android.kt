package com.peltoche.fyde

actual object FydeSdk {
    actual suspend fun initialize(serverUrl: String): Result<Unit> =
        Result.failure(
            UnsupportedOperationException(
                "fyde-sdk is not yet wired for Android (Gobley doesn't support the " +
                    "com.android.kotlin.multiplatform.library plugin yet); see ../../../CLAUDE.md"
            )
        )
}
