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

    actual suspend fun recoveryKeyStatus(): Result<Long?> = unsupported()

    actual suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = unsupported()

    actual suspend fun syncProgress(): Result<FydeSyncProgress> = unsupported()

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

    actual suspend fun similarDocuments(id: String, limit: Long): Result<List<FydeSimilarDocument>> = unsupported()

    actual suspend fun getDocumentContent(id: String): Result<ByteArray?> = unsupported()

    actual suspend fun documentThumbnail(id: String, maxWidth: Int, maxHeight: Int): Result<FydeThumbnail?> = unsupported()

    actual suspend fun uploadDocument(path: String): Result<String> = unsupported()

    actual suspend fun moveDocument(id: String, folderId: String?): Result<Unit> = unsupported()

    actual suspend fun listFolders(): Result<List<FydeFolder>> = unsupported()

    actual suspend fun createFolder(
        parentId: String?,
        name: String,
        appearance: FydeFolderAppearance?,
    ): Result<FydeFolder> = unsupported()

    actual suspend fun updateFolder(
        id: String,
        name: String?,
        appearance: FydeFolderAppearance?,
    ): Result<Unit> = unsupported()

    actual suspend fun moveFolder(id: String, parentId: String?): Result<Unit> = unsupported()

    actual suspend fun deleteFolder(id: String, contents: FydeFolderContents): Result<Unit> = unsupported()

    actual suspend fun deleteDocument(id: String): Result<Unit> = unsupported()

    actual suspend fun setLanguage(language: String): Result<Unit> = unsupported()

    actual suspend fun getLanguage(): Result<String?> = unsupported()

    actual suspend fun enableScript(scriptId: String): Result<Unit> = unsupported()

    actual suspend fun disableScript(scriptId: String): Result<Unit> = unsupported()

    actual suspend fun installScript(
        installationId: String?,
        scriptId: String,
        label: String,
        parameters: Map<String, FydeScriptParameterValue>,
        folderId: String?,
    ): Result<String> = unsupported()

    actual suspend fun uninstallScript(installationId: String): Result<Unit> = unsupported()

    actual suspend fun listUserScripts(): Result<List<FydeScript>> = unsupported()

    actual suspend fun listPublicScripts(): Result<List<FydeScript>> = unsupported()

    actual suspend fun fetchIcon(iconId: String): Result<ByteArray> = unsupported()

    actual suspend fun listInstallations(): Result<List<FydeInstallation>> = unsupported()

    actual fun setScraperHandler(handler: FydeScraperHandler?) {
    }

    actual suspend fun runInstalledScript(installationId: String): Result<Unit> = unsupported()

    actual suspend fun stopScriptRun(installationId: String): Result<Boolean> = unsupported()
}
