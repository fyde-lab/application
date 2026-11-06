package com.peltoche.fyde

actual object FydeSdk {
    /**
     * Fails like every other platform's [FydeSdk] does for a failure that
     * isn't fyde-sdk's own: logged to the browser console in full, with no
     * wording for the UI beyond a generic one (see [userMessage]).
     */
    private fun <T> unsupported(): Result<T> {
        val error = UnsupportedOperationException(
            "fyde-sdk cannot be linked into the Web target: it depends on tokio/tonic/sqlx, " +
                "none of which compile to wasm32; see ../../../CLAUDE.md"
        )
        println("[FydeSdk] ERROR: ${error.stackTraceToString()}")
        return Result.failure(FydeSdkException(null, error))
    }

    actual suspend fun initialize(
        serverUrl: String,
        onDocumentChange: ((FydeChangelogEvent) -> Unit)?,
    ): Result<Unit> = unsupported()

    actual suspend fun login(
        username: String,
        password: String,
        allowInsecureStorage: Boolean,
        replaceSessionId: String?,
    ): Result<FydeLoginOutcome> = unsupported()

    actual suspend fun register(username: String, password: String, allowInsecureStorage: Boolean): Result<Unit> =
        unsupported()

    actual suspend fun isSecureStorageAvailable(): Result<Boolean> = unsupported()

    actual suspend fun isConnected(): Result<Boolean> = unsupported()

    actual suspend fun logout(): Result<Unit> = unsupported()

    actual suspend fun isServerReachable(): Result<Boolean> = unsupported()

    actual suspend fun getUsage(): Result<FydePlanUsage> = unsupported()

    actual suspend fun listSessions(): Result<List<FydeOpenSession>> = unsupported()

    actual suspend fun revokeSession(sessionId: String): Result<Unit> = unsupported()

    actual suspend fun generateRecoveryKey(): Result<List<String>> = unsupported()

    actual suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = unsupported()

    actual suspend fun analyzePassword(password: String): Result<FydePasswordAnalysis> = unsupported()

    actual suspend fun listDocuments(offset: Long, limit: Long): Result<List<FydeDocument>> = unsupported()

    actual suspend fun searchDocuments(
        text: String,
        filters: FydeSearchFilters,
        order: FydeSearchOrder,
        offset: Long,
        limit: Long,
    ): Result<List<FydeDocument>> = unsupported()

    actual suspend fun searchDocumentFacets(text: String, filters: FydeSearchFilters): Result<FydeSearchFacets> =
        unsupported()

    actual suspend fun getDocumentContent(id: String): Result<ByteArray?> = unsupported()

    actual suspend fun uploadDocument(path: String): Result<String> = unsupported()

    actual suspend fun setLanguage(language: String): Result<Unit> = unsupported()

    actual suspend fun getLanguage(): Result<String?> = unsupported()

    actual suspend fun createScript(
        name: String,
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

    actual suspend fun fetchIcon(iconId: String): Result<ByteArray> = unsupported()

    actual suspend fun installedScriptParameters(): Result<Map<String, Map<String, FydeScriptParameterValue>>> =
        unsupported()

    actual fun setScraperHandler(handler: FydeScraperHandler?) {
    }

    actual suspend fun runInstalledScript(scriptId: String): Result<Unit> = unsupported()

    actual suspend fun stopScriptRun(scriptId: String): Result<Boolean> = unsupported()
}
