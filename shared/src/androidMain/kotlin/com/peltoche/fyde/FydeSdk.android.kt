package com.peltoche.fyde

import android.util.Log
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import uniffi.fyde_sdk.DocumentChangeListener
import uniffi.fyde_sdk.FfiBackgroundJobs
import uniffi.fyde_sdk.FfiChangelogEvent
import uniffi.fyde_sdk.FfiClientConfig
import uniffi.fyde_sdk.FfiDocumentType
import uniffi.fyde_sdk.FfiEventType
import uniffi.fyde_sdk.FfiFolder
import uniffi.fyde_sdk.FfiFolderAppearance
import uniffi.fyde_sdk.FfiFolderColor
import uniffi.fyde_sdk.FfiFolderContents
import uniffi.fyde_sdk.FfiFolderFilter
import uniffi.fyde_sdk.FfiFolderIcon
import uniffi.fyde_sdk.FfiException
import uniffi.fyde_sdk.FfiLogLevel
import uniffi.fyde_sdk.FfiLoginOutcome
import uniffi.fyde_sdk.FfiMetadata
import uniffi.fyde_sdk.FfiOpenSession
import uniffi.fyde_sdk.FfiPasswordAnalysis
import uniffi.fyde_sdk.FfiPasswordStrength
import uniffi.fyde_sdk.FfiProgressEvent
import uniffi.fyde_sdk.FfiProgressStep
import uniffi.fyde_sdk.FfiScript
import uniffi.fyde_sdk.FfiScriptMetadata
import uniffi.fyde_sdk.FfiScriptParameter
import uniffi.fyde_sdk.FfiScriptParameterType
import uniffi.fyde_sdk.FfiScriptType
import uniffi.fyde_sdk.FfiSearchFacets
import uniffi.fyde_sdk.FfiSearchFilters
import uniffi.fyde_sdk.FfiSearchOrder
import uniffi.fyde_sdk.FfiAttribute
import uniffi.fyde_sdk.FfiStorage
import uniffi.fyde_sdk.FfiSyncProgress
import uniffi.fyde_sdk.FfiUsage
import uniffi.fyde_sdk.FydeClient
import uniffi.fyde_sdk.LogListener
import uniffi.fyde_sdk.ScraperListener
import kotlin.concurrent.Volatile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val LOG_TAG = "FydeSdk"

/** Forwards every SDK log line to Android's `Log`, mirroring its level. */
private class AndroidLogListener : LogListener {
    override fun onLog(level: FfiLogLevel, line: String) {
        val priority = when (level) {
            FfiLogLevel.OFF -> return
            FfiLogLevel.ERROR -> Log.ERROR
            FfiLogLevel.WARN -> Log.WARN
            FfiLogLevel.INFO -> Log.INFO
            FfiLogLevel.DEBUG -> Log.DEBUG
            FfiLogLevel.TRACE -> Log.VERBOSE
        }
        Log.println(priority, LOG_TAG, line)
    }
}

/**
 * The data directory fyde-sdk persists local state under on Android (its
 * database lands in an `android/` subdirectory): the app's private,
 * per-app storage directory (backed up on app uninstall/data-clear, and
 * off-limits to other apps).
 */
private fun defaultDataDirectory(): String = appContext.filesDir.absolutePath

/**
 * Where the sdk downloads its on-device models: outside [defaultDataDirectory]
 * (logout empties the client's directory there, and models aren't user data),
 * and kept out of backups, since they can always be downloaded again.
 */
private fun defaultModelsDirectory(): String = File(appContext.noBackupFilesDir, "models").absolutePath

/**
 * Runs [block], turning any failure into a [FydeSdkException] whose
 * [FydeSdkException.userMessage] the UI can show as is, and logging it, since
 * the UI never shows the technical description. A fyde-sdk [FfiException]
 * already carries user-facing wording, and fyde-sdk has itself logged its
 * technical description through [AndroidLogListener] just before, so only which call
 * failed and with which error (the exception's variant) are logged here;
 * anything else (e.g. the native library failing to load) is logged in
 * full. Cancellation is rethrown rather than reported as a failure. A
 * `SessionExpired` failure is also reported to [SessionExpiry].
 */
private inline fun <T> sdkCall(operation: String, block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: FfiException) {
    Log.w(LOG_TAG, "FydeSdk.$operation failed: ${e::class.simpleName}")
    if (e is FfiException.SessionExpired) SessionExpiry.report(e.message)
    Result.failure(
        FydeSdkException(
            e.message,
            e,
            isClientAlreadyRunning = e is FfiException.ClientAlreadyRunning,
            isRunStopped = e is FfiException.ScraperRunStopped,
        ),
    )
} catch (e: Throwable) {
    Log.e(LOG_TAG, "FydeSdk.$operation failed", e)
    Result.failure(FydeSdkException(null, e))
}

actual object FydeSdk {
    private var client: FydeClient? = null

    /** Serializes [initialize], so concurrent first calls can't create two clients. */
    private val initLock = Mutex()

    @Volatile
    private var documentChangeCallback: ((FydeChangelogEvent) -> Unit)? = null

    @Volatile
    private var scraperHandler: FydeScraperHandler? = null

    /** [sdkCall] against the client [initialize] created, failing if there's none yet. */
    private inline fun <T> withClient(operation: String, block: (FydeClient) -> T): Result<T> =
        sdkCall(operation) { block(checkNotNull(client) { "FydeSdk.initialize must be called first" }) }

    actual suspend fun initialize(
        serverUrl: String,
        onDocumentChange: ((FydeChangelogEvent) -> Unit)?,
    ): Result<Unit> {
        onDocumentChange?.let { documentChangeCallback = it }
        return initLock.withLock {
            if (client != null) return@withLock Result.success(Unit)
            sdkCall("initialize") {
                val config = FfiClientConfig(
                    url = serverUrl,
                    // The sdk keeps this client's database in its own
                    // "android" subdirectory of the data directory.
                    clientName = "android",
                    storage = FfiStorage.Disk(path = defaultDataDirectory()),
                    modelsDir = defaultModelsDirectory(),
                    // No thumbnails drawn in the background, to spare the
                    // battery: each is drawn when first shown instead.
                    backgroundJobs = FfiBackgroundJobs(sync = true, modelPreloading = true, thumbnails = false),
                    logLevel = FfiLogLevel.INFO,
                )
                // Both listeners forward to whatever is registered *now*, so
                // later callers can swap them without a new client.
                val documentListener = object : DocumentChangeListener {
                    override fun onEvent(event: FfiChangelogEvent) {
                        documentChangeCallback?.invoke(event.toFydeChangelogEvent())
                    }
                }
                val scraperListener = object : ScraperListener {
                    override fun onProgress(installationId: String, event: FfiProgressEvent) {
                        scraperHandler?.onProgress(installationId, event.toFydeScraperProgress())
                    }

                    // Called on the scraper's own blocking thread, so it may
                    // block until the user answers.
                    override fun onQuestion(installationId: String, question: String): String =
                        scraperHandler?.let { handler -> runBlocking { handler.onQuestion(installationId, question) } } ?: ""
                }
                // fyde.browser's webview: one isolated WebView profile per run.
                val browserEngine = AndroidWebviewEngine(appContext)
                client = FydeClient.init(config, AndroidLogListener(), documentListener, scraperListener, browserEngine)
            }
        }
    }

    actual suspend fun login(
        username: String,
        password: String,
        allowInsecureStorage: Boolean,
        replaceSessionId: String?,
    ): Result<FydeLoginOutcome> = withClient("login") { client ->
        client.login(username, password, getPlatform().name, allowInsecureStorage, replaceSessionId)
            .toFydeLoginOutcome()
    }

    actual suspend fun register(username: String, password: String, allowInsecureStorage: Boolean): Result<Unit> =
        withClient("register") { client ->
            client.createUser(username, password, getPlatform().name, allowInsecureStorage)
        }.map { }

    actual suspend fun isSecureStorageAvailable(): Result<Boolean> = withClient("isSecureStorageAvailable") { client ->
        client.isSecureStorageAvailable()
    }

    actual suspend fun isConnected(): Result<Boolean> = withClient("isConnected") { client ->
        client.isConnected()
    }

    actual suspend fun logout(): Result<Unit> = withClient("logout") { client ->
        client.logout()
    }

    actual suspend fun isServerReachable(): Result<Boolean> = withClient("isServerReachable") { client ->
        client.isServerReachable()
    }

    actual suspend fun getUsage(): Result<FydePlanUsage> = withClient("getUsage") { client ->
        client.getUsage().toFydePlanUsage()
    }

    actual suspend fun listSessions(): Result<List<FydeOpenSession>> = withClient("listSessions") { client ->
        client.listSessions().map(FfiOpenSession::toFydeOpenSession)
    }

    actual suspend fun revokeSession(sessionId: String): Result<Unit> = withClient("revokeSession") { client ->
        client.revokeSession(sessionId)
    }

    actual suspend fun generateRecoveryKey(): Result<List<String>> = withClient("generateRecoveryKey") { client ->
        client.generateRecoveryKey().split(' ')
    }

    actual suspend fun recoveryKeyStatus(): Result<Long?> = withClient("recoveryKeyStatus") { client ->
        client.recoveryKeyStatus()
    }

    actual suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        withClient("changePassword") { client -> client.changePassword(currentPassword, newPassword) }

    actual suspend fun syncProgress(): Result<FydeSyncProgress> = withClient("syncProgress") { client ->
        client.syncProgress().toFydeSyncProgress()
    }

    actual suspend fun analyzePassword(password: String): Result<FydePasswordAnalysis> =
        withClient("analyzePassword") { client -> client.analyzePassword(password).toFydePasswordAnalysis() }

    actual suspend fun listDocuments(offset: Long, limit: Long): Result<List<FydeDocument>> =
        withClient("listDocuments") { client ->
            client.listDocuments(offset, limit).map(FfiMetadata::toFydeDocument)
        }

    actual suspend fun searchDocuments(
        text: String,
        filters: FydeSearchFilters,
        order: FydeSearchOrder,
        offset: Long,
        limit: Long,
    ): Result<List<FydeDocument>> = withClient("searchDocuments") { client ->
        client.searchDocuments(text, filters.toFfiSearchFilters(), order.toFfiSearchOrder(), offset, limit)
            .map(FfiMetadata::toFydeDocument)
    }

    actual suspend fun searchDocumentFacets(text: String, filters: FydeSearchFilters): Result<FydeSearchFacets> =
        withClient("searchDocumentFacets") { client ->
            client.searchDocumentFacets(text, filters.toFfiSearchFilters()).toFydeSearchFacets()
        }

    actual suspend fun similarDocuments(id: String, limit: Long): Result<List<FydeSimilarDocument>> =
        withClient("similarDocuments") { client ->
            client.similarDocuments(id, limit).map { FydeSimilarDocument(it.metadata.toFydeDocument(), it.confidence) }
        }

    actual suspend fun getDocumentContent(id: String): Result<ByteArray?> = withClient("getDocumentContent") { client ->
        client.getDocument(id)?.content
    }

    actual suspend fun documentThumbnail(id: String, maxWidth: Int, maxHeight: Int): Result<FydeThumbnail?> =
        withClient("documentThumbnail") { client ->
            client.documentThumbnail(id, maxWidth.coerceAtLeast(1).toUInt(), maxHeight.coerceAtLeast(1).toUInt())
                ?.let { FydeThumbnail(it.png, it.width.toInt(), it.height.toInt()) }
        }

    actual suspend fun uploadDocument(path: String): Result<String> = withClient("uploadDocument") { client ->
        client.uploadDocument(path)
    }

    actual suspend fun moveDocument(id: String, folderId: String?): Result<Unit> = withClient("moveDocument") { client ->
        client.moveDocument(id, folderId)
    }

    actual suspend fun listFolders(): Result<List<FydeFolder>> = withClient("listFolders") { client ->
        client.listFolders().map(FfiFolder::toFydeFolder)
    }

    actual suspend fun createFolder(
        parentId: String?,
        name: String,
        appearance: FydeFolderAppearance?,
    ): Result<FydeFolder> = withClient("createFolder") { client ->
        client.createFolder(parentId, name, appearance?.toFfiFolderAppearance()).toFydeFolder()
    }

    actual suspend fun updateFolder(
        id: String,
        name: String?,
        appearance: FydeFolderAppearance?,
    ): Result<Unit> = withClient("updateFolder") { client ->
        client.updateFolder(id, name, appearance?.toFfiFolderAppearance())
    }

    actual suspend fun moveFolder(id: String, parentId: String?): Result<Unit> = withClient("moveFolder") { client ->
        client.moveFolder(id, parentId)
    }

    actual suspend fun deleteFolder(id: String, contents: FydeFolderContents): Result<Unit> =
        withClient("deleteFolder") { client ->
            client.deleteFolder(
                id,
                when (contents) {
                    FydeFolderContents.KEEP -> FfiFolderContents.KEEP
                    FydeFolderContents.DELETE -> FfiFolderContents.DELETE
                },
            )
        }

    actual suspend fun deleteDocument(id: String): Result<Unit> = withClient("deleteDocument") { client ->
        client.deleteDocument(id)
    }

    actual suspend fun setLanguage(language: String): Result<Unit> = withClient("setLanguage") { client ->
        client.setLanguage(language)
    }

    actual suspend fun getLanguage(): Result<String?> = withClient("getLanguage") { client ->
        client.getLanguage()
    }

    actual suspend fun enableScript(scriptId: String): Result<Unit> = withClient("enableScript") { client ->
        client.enableScript(scriptId)
    }

    actual suspend fun disableScript(scriptId: String): Result<Unit> = withClient("disableScript") { client ->
        client.disableScript(scriptId)
    }

    actual suspend fun installScript(
        installationId: String?,
        scriptId: String,
        label: String,
        parameters: Map<String, FydeScriptParameterValue>,
        folderId: String?,
    ): Result<String> = withClient("installScript") { client ->
        client.installScript(installationId, scriptId, label, encodeScriptParametersJson(parameters), folderId)
    }

    actual suspend fun uninstallScript(installationId: String): Result<Unit> = withClient("uninstallScript") { client ->
        client.uninstallScript(installationId)
    }

    actual suspend fun listUserScripts(): Result<List<FydeScript>> = withClient("listUserScripts") { client ->
        client.listUserScripts().map(FfiScriptMetadata::toFydeScript)
    }

    actual suspend fun listPublicScripts(): Result<List<FydeScript>> = withClient("listPublicScripts") { client ->
        client.listPublicScripts().map(FfiScriptMetadata::toFydeScript)
    }

    actual suspend fun fetchIcon(iconId: String): Result<ByteArray> = withClient("fetchIcon") { client ->
        client.fetchIcon(iconId)
    }

    actual fun setScraperHandler(handler: FydeScraperHandler?) {
        scraperHandler = handler
    }

    actual suspend fun runInstalledScript(installationId: String): Result<Unit> =
        withClient("runInstalledScript") { client -> client.runInstalledScript(installationId) }

    actual suspend fun stopScriptRun(installationId: String): Result<Boolean> =
        withClient("stopScriptRun") { client -> client.stopScriptRun(installationId) }

    actual suspend fun listInstallations(): Result<List<FydeInstallation>> =
        withClient("listInstallations") { client ->
            client.listInstalledScripts().map { installed ->
                FydeInstallation(
                    id = installed.installationId,
                    scriptId = installed.script.id,
                    label = installed.label,
                    parameters = decodeScriptParametersJson(installed.parametersJson),
                    folderId = installed.folderId,
                )
            }
        }
}

private fun FfiProgressEvent.toFydeScraperProgress(): FydeScraperProgress = when (this) {
    is FfiProgressEvent.Step -> FydeScraperProgress.Step(step.toFydeScraperStep(), message)
    is FfiProgressEvent.Update -> FydeScraperProgress.Update(current, total)
}

private fun FfiProgressStep.toFydeScraperStep() = when (this) {
    FfiProgressStep.SETTING_UP -> FydeScraperStep.SETTING_UP
    FfiProgressStep.LOGGING_IN -> FydeScraperStep.LOGGING_IN
    FfiProgressStep.CHECKING_SESSION -> FydeScraperStep.CHECKING_SESSION
    FfiProgressStep.WAITING_FOR_VERIFICATION_CODE -> FydeScraperStep.WAITING_FOR_VERIFICATION_CODE
    FfiProgressStep.WAITING_FOR_CAPTCHA -> FydeScraperStep.WAITING_FOR_CAPTCHA
    FfiProgressStep.LISTING_DOCUMENTS -> FydeScraperStep.LISTING_DOCUMENTS
    FfiProgressStep.DOWNLOADING_DOCUMENTS -> FydeScraperStep.DOWNLOADING_DOCUMENTS
    FfiProgressStep.DONE -> FydeScraperStep.DONE
}

private fun FfiDocumentType.toFydeDocumentType() = when (this) {
    FfiDocumentType.INVOICE -> FydeDocumentType.INVOICE
    FfiDocumentType.RECEIPT -> FydeDocumentType.RECEIPT
    FfiDocumentType.STATEMENT -> FydeDocumentType.STATEMENT
    FfiDocumentType.REIMBURSEMENT_STATEMENT -> FydeDocumentType.REIMBURSEMENT_STATEMENT
    FfiDocumentType.PAYSLIP -> FydeDocumentType.PAYSLIP
    FfiDocumentType.CONTRACT -> FydeDocumentType.CONTRACT
    FfiDocumentType.INSURANCE_CERTIFICATE -> FydeDocumentType.INSURANCE_CERTIFICATE
    FfiDocumentType.INSURANCE_CARD -> FydeDocumentType.INSURANCE_CARD
    FfiDocumentType.RIGHTS_CERTIFICATE -> FydeDocumentType.RIGHTS_CERTIFICATE
    FfiDocumentType.PROOF_OF_RESIDENCE -> FydeDocumentType.PROOF_OF_RESIDENCE
    FfiDocumentType.RIB -> FydeDocumentType.RIB
    FfiDocumentType.TAX_NOTICE -> FydeDocumentType.TAX_NOTICE
    FfiDocumentType.TAX_RETURN -> FydeDocumentType.TAX_RETURN
    FfiDocumentType.TAX_CERTIFICATE -> FydeDocumentType.TAX_CERTIFICATE
    FfiDocumentType.EMPLOYMENT_CERTIFICATE -> FydeDocumentType.EMPLOYMENT_CERTIFICATE
    FfiDocumentType.FEE_SUMMARY -> FydeDocumentType.FEE_SUMMARY
    FfiDocumentType.BOOKING -> FydeDocumentType.BOOKING
    FfiDocumentType.MEDICAL_DOCUMENT -> FydeDocumentType.MEDICAL_DOCUMENT
    FfiDocumentType.QUOTE -> FydeDocumentType.QUOTE
    FfiDocumentType.ID_DOCUMENT -> FydeDocumentType.ID_DOCUMENT
    FfiDocumentType.CIVIL_STATUS_RECORD -> FydeDocumentType.CIVIL_STATUS_RECORD
    FfiDocumentType.LETTER -> FydeDocumentType.LETTER
    FfiDocumentType.ACKNOWLEDGEMENT -> FydeDocumentType.ACKNOWLEDGEMENT
    FfiDocumentType.FORM -> FydeDocumentType.FORM
    FfiDocumentType.PAYMENT_SCHEDULE -> FydeDocumentType.PAYMENT_SCHEDULE
    FfiDocumentType.MANDATE -> FydeDocumentType.MANDATE
    FfiDocumentType.TERMS_AND_CONDITIONS -> FydeDocumentType.TERMS_AND_CONDITIONS
    FfiDocumentType.BROCHURE -> FydeDocumentType.BROCHURE
    FfiDocumentType.TIMESHEET -> FydeDocumentType.TIMESHEET
    FfiDocumentType.INSPECTION_REPORT -> FydeDocumentType.INSPECTION_REPORT
    FfiDocumentType.NOTARIAL_DEED -> FydeDocumentType.NOTARIAL_DEED
    FfiDocumentType.ATTESTATION -> FydeDocumentType.ATTESTATION
    FfiDocumentType.PAYMENT_REMINDER -> FydeDocumentType.PAYMENT_REMINDER
    FfiDocumentType.FINE -> FydeDocumentType.FINE
    FfiDocumentType.WARRANTY -> FydeDocumentType.WARRANTY
    FfiDocumentType.RESUME -> FydeDocumentType.RESUME
    FfiDocumentType.MINUTES -> FydeDocumentType.MINUTES
    FfiDocumentType.DIPLOMA -> FydeDocumentType.DIPLOMA
    FfiDocumentType.OTHER -> FydeDocumentType.OTHER
}

private fun FydeDocumentType.toFfiDocumentType() = when (this) {
    FydeDocumentType.INVOICE -> FfiDocumentType.INVOICE
    FydeDocumentType.RECEIPT -> FfiDocumentType.RECEIPT
    FydeDocumentType.STATEMENT -> FfiDocumentType.STATEMENT
    FydeDocumentType.REIMBURSEMENT_STATEMENT -> FfiDocumentType.REIMBURSEMENT_STATEMENT
    FydeDocumentType.PAYSLIP -> FfiDocumentType.PAYSLIP
    FydeDocumentType.CONTRACT -> FfiDocumentType.CONTRACT
    FydeDocumentType.INSURANCE_CERTIFICATE -> FfiDocumentType.INSURANCE_CERTIFICATE
    FydeDocumentType.INSURANCE_CARD -> FfiDocumentType.INSURANCE_CARD
    FydeDocumentType.RIGHTS_CERTIFICATE -> FfiDocumentType.RIGHTS_CERTIFICATE
    FydeDocumentType.PROOF_OF_RESIDENCE -> FfiDocumentType.PROOF_OF_RESIDENCE
    FydeDocumentType.RIB -> FfiDocumentType.RIB
    FydeDocumentType.TAX_NOTICE -> FfiDocumentType.TAX_NOTICE
    FydeDocumentType.TAX_RETURN -> FfiDocumentType.TAX_RETURN
    FydeDocumentType.TAX_CERTIFICATE -> FfiDocumentType.TAX_CERTIFICATE
    FydeDocumentType.EMPLOYMENT_CERTIFICATE -> FfiDocumentType.EMPLOYMENT_CERTIFICATE
    FydeDocumentType.FEE_SUMMARY -> FfiDocumentType.FEE_SUMMARY
    FydeDocumentType.BOOKING -> FfiDocumentType.BOOKING
    FydeDocumentType.MEDICAL_DOCUMENT -> FfiDocumentType.MEDICAL_DOCUMENT
    FydeDocumentType.QUOTE -> FfiDocumentType.QUOTE
    FydeDocumentType.ID_DOCUMENT -> FfiDocumentType.ID_DOCUMENT
    FydeDocumentType.CIVIL_STATUS_RECORD -> FfiDocumentType.CIVIL_STATUS_RECORD
    FydeDocumentType.LETTER -> FfiDocumentType.LETTER
    FydeDocumentType.ACKNOWLEDGEMENT -> FfiDocumentType.ACKNOWLEDGEMENT
    FydeDocumentType.FORM -> FfiDocumentType.FORM
    FydeDocumentType.PAYMENT_SCHEDULE -> FfiDocumentType.PAYMENT_SCHEDULE
    FydeDocumentType.MANDATE -> FfiDocumentType.MANDATE
    FydeDocumentType.TERMS_AND_CONDITIONS -> FfiDocumentType.TERMS_AND_CONDITIONS
    FydeDocumentType.BROCHURE -> FfiDocumentType.BROCHURE
    FydeDocumentType.TIMESHEET -> FfiDocumentType.TIMESHEET
    FydeDocumentType.INSPECTION_REPORT -> FfiDocumentType.INSPECTION_REPORT
    FydeDocumentType.NOTARIAL_DEED -> FfiDocumentType.NOTARIAL_DEED
    FydeDocumentType.ATTESTATION -> FfiDocumentType.ATTESTATION
    FydeDocumentType.PAYMENT_REMINDER -> FfiDocumentType.PAYMENT_REMINDER
    FydeDocumentType.FINE -> FfiDocumentType.FINE
    FydeDocumentType.WARRANTY -> FfiDocumentType.WARRANTY
    FydeDocumentType.RESUME -> FfiDocumentType.RESUME
    FydeDocumentType.MINUTES -> FfiDocumentType.MINUTES
    FydeDocumentType.DIPLOMA -> FfiDocumentType.DIPLOMA
    FydeDocumentType.OTHER -> FfiDocumentType.OTHER
}

private fun FydeSearchFilters.toFfiSearchFilters() = FfiSearchFilters(
    folder = folder?.toFfiFolderFilter(),
    types = types.map { it.toFfiDocumentType() },
    contentTypes = contentTypes,
    createdFrom = createdFrom,
    createdUntil = createdUntil,
)

private fun FfiSearchFacets.toFydeSearchFacets() = FydeSearchFacets(
    folders = folders.map { FydeFacetCount(it.folderId, it.count.toLong()) },
    unfiled = unfiled.toLong(),
    types = types.map { FydeFacetCount(it.value.toFydeDocumentType(), it.count.toLong()) },
    contentTypes = contentTypes.map { FydeFacetCount(it.value, it.count.toLong()) },
    earliestCreatedAt = earliestCreatedAt,
    latestCreatedAt = latestCreatedAt,
)

private fun FydeSearchOrder.toFfiSearchOrder() = when (this) {
    FydeSearchOrder.RELEVANCE -> FfiSearchOrder.RELEVANCE
    FydeSearchOrder.NEWEST -> FfiSearchOrder.NEWEST
    FydeSearchOrder.OLDEST -> FfiSearchOrder.OLDEST
}

private fun FfiScriptParameterType.toFydeScriptParameterType() = when (this) {
    FfiScriptParameterType.STRING -> FydeScriptParameterType.STRING
    FfiScriptParameterType.NUMBER -> FydeScriptParameterType.NUMBER
    FfiScriptParameterType.BOOLEAN -> FydeScriptParameterType.BOOLEAN
}

private fun FfiScriptParameter.toFydeScriptParameter() = FydeScriptParameter(
    label = label,
    placeholder = placeholder,
    type = parameterType.toFydeScriptParameterType(),
    required = required,
    secret = secret,
    choices = choices.map { FydeScriptParameterChoice(value = it.value, label = it.label) },
)

private fun FfiLoginOutcome.toFydeLoginOutcome(): FydeLoginOutcome = when (this) {
    is FfiLoginOutcome.LoggedIn -> FydeLoginOutcome.LoggedIn
    is FfiLoginOutcome.SessionLimitReached -> FydeLoginOutcome.SessionLimitReached(
        maxSessions = maxSessions.toInt(),
        sessions = sessions.map(FfiOpenSession::toFydeOpenSession),
    )
}

private fun FfiUsage.toFydePlanUsage() = FydePlanUsage(
    planId = planId,
    maxScripts = maxScripts.toInt(),
    maxSessions = maxSessions.toInt(),
    maxSize = maxSize.toLong(),
    enabledScripts = enabledScripts.toInt(),
    openSessions = openSessions.toInt(),
    totalSize = totalSize.toLong(),
)

private fun FfiSyncProgress.toFydeSyncProgress() = FydeSyncProgress(
    consumed = consumed.toLong(),
    total = total?.toLong(),
    caughtUp = caughtUp,
)

private fun FfiPasswordAnalysis.toFydePasswordAnalysis() = FydePasswordAnalysis(
    score = score.toInt(),
    strength = when (strength) {
        FfiPasswordStrength.WEAK -> FydePasswordStrength.Weak
        FfiPasswordStrength.FAIR -> FydePasswordStrength.Fair
        FfiPasswordStrength.STRONG -> FydePasswordStrength.Strong
        FfiPasswordStrength.VERY_STRONG -> FydePasswordStrength.VeryStrong
    },
    length = length.toInt(),
    isAccepted = isAccepted,
)

private fun FfiOpenSession.toFydeOpenSession() = FydeOpenSession(
    id = id,
    deviceName = deviceName,
    createdAt = createdAt,
    isCurrent = isCurrent,
)

private fun FfiScript.toFydeScript() = FydeScript(
    id = id,
    name = name,
    description = description,
    shortDescription = shortDescription,
    isPublic = isPublic,
    iconId = iconId,
    icon = ByteArray(0),
    allowedDomains = allowedDomains,
    version = version.toLong(),
    lastUpdated = lastUpdated,
    parameters = parameters.mapValues { (_, parameter) -> parameter.toFydeScriptParameter() },
    type = scriptType.toFydeScriptType(),
)

private fun FfiScriptMetadata.toFydeScript() = FydeScript(
    id = id,
    name = name,
    description = description,
    shortDescription = shortDescription,
    isPublic = isPublic,
    iconId = iconId,
    icon = ByteArray(0),
    allowedDomains = allowedDomains,
    version = version.toLong(),
    lastUpdated = lastUpdated,
    parameters = parameters.mapValues { (_, parameter) -> parameter.toFydeScriptParameter() },
    type = scriptType.toFydeScriptType(),
)

private fun FfiScriptType.toFydeScriptType() = when (this) {
    FfiScriptType.SCRAPER -> FydeScriptType.SCRAPER
    FfiScriptType.PARSER -> FydeScriptType.PARSER
}

private fun FfiMetadata.toFydeDocument() = FydeDocument(
    id = id,
    name = name,
    contentType = contentType,
    createdAt = createdAt,
    size = size.toLong(),
    documentType = type?.toFydeDocumentType(),
    folderId = folderId,
    attributes = attributes.mapValues { (_, attribute) -> attribute.toFydeAttribute() },
)

private fun FfiAttribute.toFydeAttribute(): FydeAttribute = when (this) {
    is FfiAttribute.String -> FydeAttribute.Text(value)
    is FfiAttribute.Integer -> FydeAttribute.Integer(value)
    is FfiAttribute.Float -> FydeAttribute.Decimal(value)
    is FfiAttribute.Boolean -> FydeAttribute.Bool(value)
    is FfiAttribute.Date -> FydeAttribute.Date(value)
    is FfiAttribute.Amount -> FydeAttribute.Amount(minorUnits, currency)
    is FfiAttribute.Select -> FydeAttribute.Select(selected, options)
}

private fun FfiChangelogEvent.toFydeChangelogEvent() = FydeChangelogEvent(
    id = id,
    eventType = when (eventType) {
        FfiEventType.SAVE_DOCUMENT -> FydeEventType.SAVE_DOCUMENT
        FfiEventType.UPDATE_METADATA -> FydeEventType.UPDATE_METADATA
        FfiEventType.DELETED -> FydeEventType.DELETED
        FfiEventType.INSTALL_SCRIPT -> FydeEventType.INSTALL_SCRIPT
        FfiEventType.UNINSTALL_SCRIPT -> FydeEventType.UNINSTALL_SCRIPT
        FfiEventType.SAVE_FOLDER -> FydeEventType.SAVE_FOLDER
        FfiEventType.DELETE_FOLDER -> FydeEventType.DELETE_FOLDER
    },
    subjectId = subjectId,
    document = content?.let { metadata?.toFydeDocument() },
)

private fun FfiFolder.toFydeFolder() = FydeFolder(
    id = id,
    parentId = parentId,
    name = name,
    appearance = appearance?.let {
        FydeFolderAppearance(FydeFolderColor.valueOf(it.color.name), FydeFolderIcon.valueOf(it.icon.name))
    },
)

private fun FydeFolderAppearance.toFfiFolderAppearance() =
    FfiFolderAppearance(FfiFolderColor.valueOf(color.name), FfiFolderIcon.valueOf(icon.name))

private fun FydeFolderFilter.toFfiFolderFilter() = when (this) {
    FydeFolderFilter.Unfiled -> FfiFolderFilter.Unfiled
    is FydeFolderFilter.Within -> FfiFolderFilter.Within(folderId)
}
