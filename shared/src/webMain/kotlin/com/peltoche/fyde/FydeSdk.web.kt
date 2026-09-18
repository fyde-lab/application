package com.peltoche.fyde

actual object FydeSdk {
    actual suspend fun initialize(serverUrl: String): Result<Unit> =
        Result.failure(
            UnsupportedOperationException(
                "fyde-sdk cannot be linked into the Web target: it depends on tokio/tonic/sqlx, " +
                    "none of which compile to wasm32; see ../../../CLAUDE.md"
            )
        )
}
