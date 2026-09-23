package com.peltoche.fyde

actual object FydeSdk {
    private fun <T> unsupported(): Result<T> = Result.failure(
        UnsupportedOperationException(
            "fyde-sdk cannot be linked into the Web target: it depends on tokio/tonic/sqlx, " +
                "none of which compile to wasm32; see ../../../CLAUDE.md"
        )
    )

    actual suspend fun initialize(serverUrl: String): Result<Unit> = unsupported()

    actual suspend fun login(username: String, password: String): Result<Unit> = unsupported()

    actual suspend fun register(username: String, password: String): Result<Unit> = unsupported()

    actual suspend fun isConnected(): Result<Boolean> = unsupported()

    actual suspend fun isServerReachable(): Result<Boolean> = unsupported()

    actual suspend fun uploadDocument(path: String): Result<String> = unsupported()
}
