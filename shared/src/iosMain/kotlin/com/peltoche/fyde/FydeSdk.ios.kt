package com.peltoche.fyde

import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSLog
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import kotlin.coroutines.cancellation.CancellationException
import uniffi.fyde_sdk.DocumentChangeListener
import uniffi.fyde_sdk.FfiChangelogEvent
import uniffi.fyde_sdk.FfiClientConfig
import uniffi.fyde_sdk.FfiDocument
import uniffi.fyde_sdk.FfiEventType
import uniffi.fyde_sdk.FfiException
import uniffi.fyde_sdk.FfiLogLevel
import uniffi.fyde_sdk.FfiProgressEvent
import uniffi.fyde_sdk.FfiPurpose
import uniffi.fyde_sdk.FfiScript
import uniffi.fyde_sdk.FfiScriptParameter
import uniffi.fyde_sdk.FfiScriptParameterType
import uniffi.fyde_sdk.FfiScriptType
import uniffi.fyde_sdk.FfiSearchFacets
import uniffi.fyde_sdk.FfiSearchFilters
import uniffi.fyde_sdk.FfiSearchOrder
import uniffi.fyde_sdk.FfiSourceCategory
import uniffi.fyde_sdk.FfiSourceSubCategory
import uniffi.fyde_sdk.FfiStorage
import uniffi.fyde_sdk.FydeClient
import uniffi.fyde_sdk.LogListener
import uniffi.fyde_sdk.ScraperListener
import kotlin.concurrent.Volatile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Forwards every SDK log line to iOS's `NSLog`, prefixed with its level. */
private class IosLogListener : LogListener {
    override fun onLog(level: FfiLogLevel, line: String) {
        if (level == FfiLogLevel.OFF) return
        NSLog("[FydeSdk] %@: %@", level.name, line)
    }
}

/**
 * The on-disk database path fyde-sdk persists local state to on iOS:
 * `fyde.db` inside the app's Application Support directory, the
 * Apple-recommended location for app-generated data the user doesn't
 * browse directly (unlike Documents).
 */
private fun defaultStoragePath(): String {
    val directoryPath = (
        NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String
        ) ?: NSHomeDirectory()
    NSFileManager.defaultManager.createDirectoryAtPath(
        directoryPath,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    return "$directoryPath/fyde.db"
}

/**
 * Runs [block], turning any failure into a [FydeSdkException] whose
 * [FydeSdkException.userMessage] the UI can show as is, and logging it, since
 * the UI never shows the technical description. A fyde-sdk [FfiException]
 * already carries user-facing wording, and fyde-sdk has itself logged its
 * technical description through [IosLogListener] just before, so only which call
 * failed and with which error (the exception's variant) are logged here;
 * anything else (e.g. the native library failing to load) is logged in
 * full. Cancellation is rethrown rather than reported as a failure.
 */
private inline fun <T> sdkCall(operation: String, block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: FfiException) {
    NSLog("[FydeSdk] WARN: FydeSdk.%@ failed: %@", operation, e::class.simpleName ?: "FfiException")
    Result.failure(FydeSdkException(e.message, e))
} catch (e: Throwable) {
    NSLog("[FydeSdk] ERROR: FydeSdk.%@ failed: %@", operation, e.stackTraceToString())
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
                    storage = FfiStorage.Disk(path = defaultStoragePath()),
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
                    override fun onProgress(event: FfiProgressEvent) {
                        scraperHandler?.onProgress(event.toFydeScraperProgress())
                    }

                    // Called on the scraper's own blocking thread, so it may
                    // block until the user answers.
                    override fun onQuestion(question: String): String =
                        scraperHandler?.let { handler -> runBlocking { handler.onQuestion(question) } } ?: ""
                }
                client = FydeClient.init(config, IosLogListener(), documentListener, scraperListener)
            }
        }
    }

    actual suspend fun login(username: String, password: String): Result<Unit> = withClient("login") { client ->
        client.login(username, password, getPlatform().name)
    }.map { }

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

    actual suspend fun listDocuments(offset: Long, limit: Long): Result<List<FydeDocument>> =
        withClient("listDocuments") { client ->
            client.listDocuments(offset, limit).map(FfiDocument::toFydeDocument)
        }

    actual suspend fun searchDocuments(
        text: String,
        filters: FydeSearchFilters,
        order: FydeSearchOrder,
        offset: Long,
        limit: Long,
    ): Result<List<FydeDocument>> = withClient("searchDocuments") { client ->
        client.searchDocuments(text, filters.toFfiSearchFilters(), order.toFfiSearchOrder(), offset, limit)
            .map(FfiDocument::toFydeDocument)
    }

    actual suspend fun searchDocumentFacets(text: String, filters: FydeSearchFilters): Result<FydeSearchFacets> =
        withClient("searchDocumentFacets") { client ->
            client.searchDocumentFacets(text, filters.toFfiSearchFilters()).toFydeSearchFacets()
        }

    actual suspend fun uploadDocument(path: String): Result<String> = withClient("uploadDocument") { client ->
        client.uploadDocument(path)
    }

    actual suspend fun setLanguage(language: String): Result<Unit> = withClient("setLanguage") { client ->
        client.setLanguage(language)
    }

    actual suspend fun getLanguage(): Result<String?> = withClient("getLanguage") { client ->
        client.getLanguage()
    }

    actual suspend fun createScript(
        name: String,
        isPublic: Boolean,
        icon: ByteArray,
        script: String,
        description: String,
        shortDescription: String,
        allowedDomains: List<String>,
        parameters: Map<String, FydeScriptParameter>,
    ): Result<FydeScript> =
        withClient("createScript") { client ->
            client.createScript(
                name,
                isPublic,
                icon,
                script,
                description,
                shortDescription,
                allowedDomains,
                FfiScriptType.PARSER,
                parameters.mapValues { (_, parameter) -> parameter.toFfiScriptParameter() },
            ).toFydeScript()
        }

    actual suspend fun enableScript(scriptId: String): Result<Unit> = withClient("enableScript") { client ->
        client.enableScript(scriptId)
    }

    actual suspend fun disableScript(scriptId: String): Result<Unit> = withClient("disableScript") { client ->
        client.disableScript(scriptId)
    }

    actual suspend fun installScript(
        scriptId: String,
        parameters: Map<String, FydeScriptParameterValue>,
    ): Result<Unit> = withClient("installScript") { client ->
        client.installScript(scriptId, encodeScriptParametersJson(parameters))
    }

    actual suspend fun listUserScripts(): Result<List<FydeScript>> = withClient("listUserScripts") { client ->
        client.listUserScripts().map(FfiScript::toFydeScript)
    }

    actual suspend fun listPublicScripts(): Result<List<FydeScript>> = withClient("listPublicScripts") { client ->
        client.listPublicScripts().map(FfiScript::toFydeScript)
    }

    actual fun setScraperHandler(handler: FydeScraperHandler?) {
        scraperHandler = handler
    }

    actual suspend fun runInstalledScript(scriptId: String): Result<Unit> = withClient("runInstalledScript") { client ->
        client.runInstalledScript(scriptId)
    }

    actual suspend fun installedScriptParameters(): Result<Map<String, Map<String, FydeScriptParameterValue>>> =
        withClient("installedScriptParameters") { client ->
            client.listInstalledScripts().associate { installed ->
                installed.script.id to decodeScriptParametersJson(installed.parametersJson)
            }
        }
}

private fun FfiProgressEvent.toFydeScraperProgress(): FydeScraperProgress = when (this) {
    is FfiProgressEvent.Step -> FydeScraperProgress.Step(name)
    is FfiProgressEvent.Update -> FydeScraperProgress.Update(current, total, message)
}

private fun FfiSourceCategory.toFydeSourceCategory() = when (this) {
    FfiSourceCategory.BANK -> FydeSourceCategory.BANK
    FfiSourceCategory.INSURANCE -> FydeSourceCategory.INSURANCE
    FfiSourceCategory.RETAIL -> FydeSourceCategory.RETAIL
    FfiSourceCategory.TELECOM -> FydeSourceCategory.TELECOM
    FfiSourceCategory.ENERGY -> FydeSourceCategory.ENERGY
    FfiSourceCategory.WATER -> FydeSourceCategory.WATER
    FfiSourceCategory.HEALTH -> FydeSourceCategory.HEALTH
    FfiSourceCategory.GOV -> FydeSourceCategory.GOV
    FfiSourceCategory.ASSOCIATION -> FydeSourceCategory.ASSOCIATION
    FfiSourceCategory.EDUCATION -> FydeSourceCategory.EDUCATION
    FfiSourceCategory.EMPLOYER -> FydeSourceCategory.EMPLOYER
    FfiSourceCategory.TRANSPORT -> FydeSourceCategory.TRANSPORT
    FfiSourceCategory.GOODS -> FydeSourceCategory.GOODS
    FfiSourceCategory.ALIMENTATION -> FydeSourceCategory.ALIMENTATION
    FfiSourceCategory.BUILDING -> FydeSourceCategory.BUILDING
    FfiSourceCategory.REAL_ESTATE -> FydeSourceCategory.REAL_ESTATE
    FfiSourceCategory.WEB -> FydeSourceCategory.WEB
    FfiSourceCategory.INDIVIDUAL -> FydeSourceCategory.INDIVIDUAL
    FfiSourceCategory.SHOPPING -> FydeSourceCategory.SHOPPING
}

private fun FfiSourceSubCategory.toFydeSourceSubCategory() = when (this) {
    FfiSourceSubCategory.CIVIL_REGISTRATION -> FydeSourceSubCategory.CIVIL_REGISTRATION
    FfiSourceSubCategory.IMMIGRATION -> FydeSourceSubCategory.IMMIGRATION
    FfiSourceSubCategory.TRANSPORT -> FydeSourceSubCategory.TRANSPORT
    FfiSourceSubCategory.FAMILY -> FydeSourceSubCategory.FAMILY
    FfiSourceSubCategory.TAX -> FydeSourceSubCategory.TAX
    FfiSourceSubCategory.HEALTH -> FydeSourceSubCategory.HEALTH
    FfiSourceSubCategory.REAL_ESTATE -> FydeSourceSubCategory.REAL_ESTATE
    FfiSourceSubCategory.MOBILE -> FydeSourceSubCategory.MOBILE
    FfiSourceSubCategory.INTERNET -> FydeSourceSubCategory.INTERNET
    FfiSourceSubCategory.CITIZEN -> FydeSourceSubCategory.CITIZEN
    FfiSourceSubCategory.SPORT -> FydeSourceSubCategory.SPORT
}

private fun FfiPurpose.toFydePurpose() = when (this) {
    FfiPurpose.ATTESTATION -> FydePurpose.ATTESTATION
    FfiPurpose.CONTRACT -> FydePurpose.CONTRACT
    FfiPurpose.INVOICE -> FydePurpose.INVOICE
    FfiPurpose.REPORT -> FydePurpose.REPORT
    FfiPurpose.DESCRIPTION -> FydePurpose.DESCRIPTION
    FfiPurpose.EVALUATION -> FydePurpose.EVALUATION
    FfiPurpose.EMPLOYMENT -> FydePurpose.EMPLOYMENT
}

private fun FydeSourceCategory.toFfiSourceCategory() = when (this) {
    FydeSourceCategory.BANK -> FfiSourceCategory.BANK
    FydeSourceCategory.INSURANCE -> FfiSourceCategory.INSURANCE
    FydeSourceCategory.RETAIL -> FfiSourceCategory.RETAIL
    FydeSourceCategory.TELECOM -> FfiSourceCategory.TELECOM
    FydeSourceCategory.ENERGY -> FfiSourceCategory.ENERGY
    FydeSourceCategory.WATER -> FfiSourceCategory.WATER
    FydeSourceCategory.HEALTH -> FfiSourceCategory.HEALTH
    FydeSourceCategory.GOV -> FfiSourceCategory.GOV
    FydeSourceCategory.ASSOCIATION -> FfiSourceCategory.ASSOCIATION
    FydeSourceCategory.EDUCATION -> FfiSourceCategory.EDUCATION
    FydeSourceCategory.EMPLOYER -> FfiSourceCategory.EMPLOYER
    FydeSourceCategory.TRANSPORT -> FfiSourceCategory.TRANSPORT
    FydeSourceCategory.GOODS -> FfiSourceCategory.GOODS
    FydeSourceCategory.ALIMENTATION -> FfiSourceCategory.ALIMENTATION
    FydeSourceCategory.BUILDING -> FfiSourceCategory.BUILDING
    FydeSourceCategory.REAL_ESTATE -> FfiSourceCategory.REAL_ESTATE
    FydeSourceCategory.WEB -> FfiSourceCategory.WEB
    FydeSourceCategory.INDIVIDUAL -> FfiSourceCategory.INDIVIDUAL
    FydeSourceCategory.SHOPPING -> FfiSourceCategory.SHOPPING
}

private fun FydeSourceSubCategory.toFfiSourceSubCategory() = when (this) {
    FydeSourceSubCategory.CIVIL_REGISTRATION -> FfiSourceSubCategory.CIVIL_REGISTRATION
    FydeSourceSubCategory.IMMIGRATION -> FfiSourceSubCategory.IMMIGRATION
    FydeSourceSubCategory.TRANSPORT -> FfiSourceSubCategory.TRANSPORT
    FydeSourceSubCategory.FAMILY -> FfiSourceSubCategory.FAMILY
    FydeSourceSubCategory.TAX -> FfiSourceSubCategory.TAX
    FydeSourceSubCategory.HEALTH -> FfiSourceSubCategory.HEALTH
    FydeSourceSubCategory.REAL_ESTATE -> FfiSourceSubCategory.REAL_ESTATE
    FydeSourceSubCategory.MOBILE -> FfiSourceSubCategory.MOBILE
    FydeSourceSubCategory.INTERNET -> FfiSourceSubCategory.INTERNET
    FydeSourceSubCategory.CITIZEN -> FfiSourceSubCategory.CITIZEN
    FydeSourceSubCategory.SPORT -> FfiSourceSubCategory.SPORT
}

private fun FydePurpose.toFfiPurpose() = when (this) {
    FydePurpose.ATTESTATION -> FfiPurpose.ATTESTATION
    FydePurpose.CONTRACT -> FfiPurpose.CONTRACT
    FydePurpose.INVOICE -> FfiPurpose.INVOICE
    FydePurpose.REPORT -> FfiPurpose.REPORT
    FydePurpose.DESCRIPTION -> FfiPurpose.DESCRIPTION
    FydePurpose.EVALUATION -> FfiPurpose.EVALUATION
    FydePurpose.EMPLOYMENT -> FfiPurpose.EMPLOYMENT
}

private fun FydeSearchFilters.toFfiSearchFilters() = FfiSearchFilters(
    sourceCategories = sourceCategories.map { it.toFfiSourceCategory() },
    sourceSubCategories = sourceSubCategories.map { it.toFfiSourceSubCategory() },
    purposes = purposes.map { it.toFfiPurpose() },
    types = types,
    subjects = subjects,
    contentTypes = contentTypes,
    createdFrom = createdFrom,
    createdUntil = createdUntil,
)

private fun FfiSearchFacets.toFydeSearchFacets() = FydeSearchFacets(
    sourceCategories = sourceCategories.map { FydeFacetCount(it.value.toFydeSourceCategory(), it.count.toLong()) },
    sourceSubCategories = sourceSubCategories.map { FydeFacetCount(it.value.toFydeSourceSubCategory(), it.count.toLong()) },
    purposes = purposes.map { FydeFacetCount(it.value.toFydePurpose(), it.count.toLong()) },
    types = types.map { FydeFacetCount(it.value, it.count.toLong()) },
    contentTypes = contentTypes.map { FydeFacetCount(it.value, it.count.toLong()) },
    subjects = subjects.map { FydeFacetCount(it.value, it.count.toLong()) },
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

private fun FydeScriptParameterType.toFfiScriptParameterType() = when (this) {
    FydeScriptParameterType.STRING -> FfiScriptParameterType.STRING
    FydeScriptParameterType.NUMBER -> FfiScriptParameterType.NUMBER
    FydeScriptParameterType.BOOLEAN -> FfiScriptParameterType.BOOLEAN
}

private fun FfiScriptParameter.toFydeScriptParameter() = FydeScriptParameter(
    label = label,
    placeholder = placeholder,
    type = parameterType.toFydeScriptParameterType(),
    required = required,
    secret = secret,
)

private fun FydeScriptParameter.toFfiScriptParameter() = FfiScriptParameter(
    label = label,
    placeholder = placeholder,
    parameterType = type.toFfiScriptParameterType(),
    required = required,
    secret = secret,
)

private fun FfiScript.toFydeScript() = FydeScript(
    id = id,
    name = name,
    description = description,
    shortDescription = shortDescription,
    isPublic = isPublic,
    icon = icon,
    allowedDomains = allowedDomains,
    version = version.toLong(),
    script = script,
    lastUpdated = lastUpdated,
    parameters = parameters.mapValues { (_, parameter) -> parameter.toFydeScriptParameter() },
    type = when (scriptType) {
        FfiScriptType.SCRAPER -> FydeScriptType.SCRAPER
        FfiScriptType.PARSER -> FydeScriptType.PARSER
    },
)

private fun FfiDocument.toFydeDocument() = FydeDocument(
    id = id,
    name = metadata.name,
    contentType = metadata.contentType,
    createdAt = metadata.createdAt,
    size = metadata.size.toLong(),
    sourceCategory = metadata.sourceCategory?.toFydeSourceCategory(),
    sourceSubCategory = metadata.sourceSubCategory?.toFydeSourceSubCategory(),
    subjects = metadata.subjects,
    purpose = metadata.purpose?.toFydePurpose(),
    content = content,
)

private fun FfiChangelogEvent.toFydeChangelogEvent() = FydeChangelogEvent(
    id = id,
    eventType = when (eventType) {
        FfiEventType.CREATED -> FydeEventType.CREATED
        FfiEventType.UPDATE_METADATA -> FydeEventType.UPDATE_METADATA
        FfiEventType.DELETED -> FydeEventType.DELETED
        FfiEventType.SCRIPT_INSTALLED -> FydeEventType.SCRIPT_INSTALLED
    },
    subjectId = subjectId,
    document = content?.let { eventContent ->
        metadata?.let { eventMetadata ->
            FydeDocument(
                id = subjectId,
                name = eventMetadata.name,
                contentType = eventMetadata.contentType,
                createdAt = eventMetadata.createdAt,
                size = eventMetadata.size.toLong(),
                sourceCategory = eventMetadata.sourceCategory?.toFydeSourceCategory(),
                sourceSubCategory = eventMetadata.sourceSubCategory?.toFydeSourceSubCategory(),
                subjects = eventMetadata.subjects,
                purpose = eventMetadata.purpose?.toFydePurpose(),
                content = eventContent,
            )
        }
    },
)
