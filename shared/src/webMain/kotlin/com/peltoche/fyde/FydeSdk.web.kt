package com.peltoche.fyde

actual object FydeSdk {
    private fun <T> unsupported(): Result<T> = Result.failure(
        UnsupportedOperationException(
            "fyde-sdk cannot be linked into the Web target: it depends on tokio/tonic/sqlx, " +
                "none of which compile to wasm32; see ../../../CLAUDE.md"
        )
    )

    actual suspend fun initialize(
        serverUrl: String,
        onDocumentChange: ((FydeChangelogEvent) -> Unit)?,
    ): Result<Unit> = unsupported()

    actual suspend fun login(username: String, password: String): Result<Unit> = unsupported()

    actual suspend fun register(username: String, password: String): Result<Unit> = unsupported()

    actual suspend fun isConnected(): Result<Boolean> = unsupported()

    actual suspend fun isServerReachable(): Result<Boolean> = unsupported()

    actual suspend fun listDocuments(offset: Long, limit: Long): Result<List<FydeDocument>> = unsupported()

    actual suspend fun uploadDocument(path: String): Result<String> = unsupported()

    actual suspend fun setLanguage(language: String): Result<Unit> = unsupported()

    actual suspend fun getLanguage(): Result<String?> = unsupported()

    actual suspend fun createScript(
        name: String,
        isPublic: Boolean,
        icon: ByteArray,
        script: String,
        description: String,
        shortDescription: String,
        allowedDomains: List<String>,
        parameters: Map<String, FydeScriptParameter>,
    ): Result<FydeScript> = unsupported()

    actual suspend fun enableScript(scriptId: String): Result<Unit> = unsupported()

    actual suspend fun disableScript(scriptId: String): Result<Unit> = unsupported()

    actual suspend fun installScript(
        scriptId: String,
        parameters: Map<String, FydeScriptParameterValue>,
    ): Result<Unit> = unsupported()

    actual suspend fun listUserScripts(): Result<List<FydeScript>> = unsupported()

    actual suspend fun listPublicScripts(): Result<List<FydeScript>> = unsupported()
}
