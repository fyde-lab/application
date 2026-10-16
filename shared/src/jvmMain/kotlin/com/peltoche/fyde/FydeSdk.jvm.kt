package com.peltoche.fyde

import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.cancellation.CancellationException
import uniffi.fyde_sdk.DocumentChangeListener
import uniffi.fyde_sdk.FfiChangelogEvent
import uniffi.fyde_sdk.FfiClientConfig
import uniffi.fyde_sdk.FfiDocument
import uniffi.fyde_sdk.FfiDocumentType
import uniffi.fyde_sdk.FfiEventType
import uniffi.fyde_sdk.FfiException
import uniffi.fyde_sdk.FfiLogLevel
import uniffi.fyde_sdk.FfiLoginOutcome
import uniffi.fyde_sdk.FfiOpenSession
import uniffi.fyde_sdk.FfiProgressEvent
import uniffi.fyde_sdk.FfiScript
import uniffi.fyde_sdk.FfiScriptParameter
import uniffi.fyde_sdk.FfiScriptParameterType
import uniffi.fyde_sdk.FfiScriptType
import uniffi.fyde_sdk.FfiSearchFacets
import uniffi.fyde_sdk.FfiSearchFilters
import uniffi.fyde_sdk.FfiSearchOrder
import uniffi.fyde_sdk.FfiCategory
import uniffi.fyde_sdk.FfiSubcategory
import uniffi.fyde_sdk.FfiStorage
import uniffi.fyde_sdk.FfiUsage
import uniffi.fyde_sdk.FydeClient
import uniffi.fyde_sdk.LogListener
import uniffi.fyde_sdk.ScraperListener
import kotlin.concurrent.Volatile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val logger = Logger.getLogger("FydeSdk")

/** Forwards every SDK log line to the JVM's `java.util.logging`, mirroring its level. */
private class JvmLogListener : LogListener {
    override fun onLog(level: FfiLogLevel, line: String) {
        val julLevel = when (level) {
            FfiLogLevel.OFF -> return
            FfiLogLevel.ERROR -> Level.SEVERE
            FfiLogLevel.WARN -> Level.WARNING
            FfiLogLevel.INFO -> Level.INFO
            FfiLogLevel.DEBUG -> Level.FINE
            FfiLogLevel.TRACE -> Level.FINEST
        }
        logger.log(julLevel, line)
    }
}

/**
 * The on-disk database path fyde-sdk persists local state to on Desktop
 * (JVM): `fyde.db` under the XDG data directory (`$XDG_DATA_HOME/fyde`,
 * falling back to `~/.local/share/fyde` when unset), mirroring the
 * default `fyde-sdk` itself uses for non-FFI callers (see `sdk/CLAUDE.md`).
 */
private fun defaultStoragePath(): String {
    val dataHome = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
        ?: "${System.getProperty("user.home")}/.local/share"
    val directory = File(dataHome, "fyde").apply { mkdirs() }
    return File(directory, "fyde.db").absolutePath
}

/**
 * Runs [block], turning any failure into a [FydeSdkException] whose
 * [FydeSdkException.userMessage] the UI can show as is, and logging it, since
 * the UI never shows the technical description. A fyde-sdk [FfiException]
 * already carries user-facing wording, and fyde-sdk has itself logged its
 * technical description through [JvmLogListener] just before, so only which call
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
    logger.log(Level.WARNING, "FydeSdk.$operation failed: ${e::class.simpleName}")
    if (e is FfiException.SessionExpired) SessionExpiry.report(e.message)
    Result.failure(FydeSdkException(e.message, e))
} catch (e: Throwable) {
    logger.log(Level.SEVERE, "FydeSdk.$operation failed", e)
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
                client = FydeClient.init(config, JvmLogListener(), documentListener, scraperListener)
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

private fun FfiCategory.toFydeCategory() = when (this) {
    FfiCategory.HOUSING -> FydeCategory.HOUSING
    FfiCategory.TELECOM -> FydeCategory.TELECOM
    FfiCategory.MONEY -> FydeCategory.MONEY
    FfiCategory.TAXES -> FydeCategory.TAXES
    FfiCategory.HEALTH -> FydeCategory.HEALTH
    FfiCategory.WORK -> FydeCategory.WORK
    FfiCategory.FAMILY -> FydeCategory.FAMILY
    FfiCategory.TRANSPORT -> FydeCategory.TRANSPORT
    FfiCategory.SHOPPING -> FydeCategory.SHOPPING
    FfiCategory.SUBSCRIPTIONS -> FydeCategory.SUBSCRIPTIONS
    FfiCategory.DONATIONS -> FydeCategory.DONATIONS
}

private fun FfiSubcategory.toFydeSubcategory() = when (this) {
    FfiSubcategory.ENERGY -> FydeSubcategory.ENERGY
    FfiSubcategory.WATER -> FydeSubcategory.WATER
    FfiSubcategory.INTERNET -> FydeSubcategory.INTERNET
    FfiSubcategory.HOME_INSURANCE -> FydeSubcategory.HOME_INSURANCE
    FfiSubcategory.HOME_SERVICES -> FydeSubcategory.HOME_SERVICES
    FfiSubcategory.MOBILE -> FydeSubcategory.MOBILE
    FfiSubcategory.BANKING -> FydeSubcategory.BANKING
    FfiSubcategory.SAVINGS -> FydeSubcategory.SAVINGS
    FfiSubcategory.GAMING -> FydeSubcategory.GAMING
    FfiSubcategory.INCOME_TAX -> FydeSubcategory.INCOME_TAX
    FfiSubcategory.TAX_CERTIFICATES -> FydeSubcategory.TAX_CERTIFICATES
    FfiSubcategory.HEALTH_INSURANCE -> FydeSubcategory.HEALTH_INSURANCE
    FfiSubcategory.COMPLEMENTARY_HEALTH -> FydeSubcategory.COMPLEMENTARY_HEALTH
    FfiSubcategory.CARE -> FydeSubcategory.CARE
    FfiSubcategory.PAYSLIPS -> FydeSubcategory.PAYSLIPS
    FfiSubcategory.EMPLOYMENT -> FydeSubcategory.EMPLOYMENT
    FfiSubcategory.HOUSEHOLD_EMPLOYER -> FydeSubcategory.HOUSEHOLD_EMPLOYER
    FfiSubcategory.BENEFITS -> FydeSubcategory.BENEFITS
    FfiSubcategory.CHILDCARE_SCHOOL -> FydeSubcategory.CHILDCARE_SCHOOL
    FfiSubcategory.TRAIN -> FydeSubcategory.TRAIN
    FfiSubcategory.FLIGHT -> FydeSubcategory.FLIGHT
    FfiSubcategory.ROAD -> FydeSubcategory.ROAD
    FfiSubcategory.RIDE -> FydeSubcategory.RIDE
    FfiSubcategory.TRAVEL -> FydeSubcategory.TRAVEL
    FfiSubcategory.ONLINE -> FydeSubcategory.ONLINE
    FfiSubcategory.PETS -> FydeSubcategory.PETS
    FfiSubcategory.GROCERIES -> FydeSubcategory.GROCERIES
    FfiSubcategory.FOOD_DELIVERY -> FydeSubcategory.FOOD_DELIVERY
    FfiSubcategory.MEDIA -> FydeSubcategory.MEDIA
    FfiSubcategory.CLOUD_SOFTWARE -> FydeSubcategory.CLOUD_SOFTWARE
}

private fun FydeCategory.toFfiCategory() = when (this) {
    FydeCategory.HOUSING -> FfiCategory.HOUSING
    FydeCategory.TELECOM -> FfiCategory.TELECOM
    FydeCategory.MONEY -> FfiCategory.MONEY
    FydeCategory.TAXES -> FfiCategory.TAXES
    FydeCategory.HEALTH -> FfiCategory.HEALTH
    FydeCategory.WORK -> FfiCategory.WORK
    FydeCategory.FAMILY -> FfiCategory.FAMILY
    FydeCategory.TRANSPORT -> FfiCategory.TRANSPORT
    FydeCategory.SHOPPING -> FfiCategory.SHOPPING
    FydeCategory.SUBSCRIPTIONS -> FfiCategory.SUBSCRIPTIONS
    FydeCategory.DONATIONS -> FfiCategory.DONATIONS
}

private fun FydeSubcategory.toFfiSubcategory() = when (this) {
    FydeSubcategory.ENERGY -> FfiSubcategory.ENERGY
    FydeSubcategory.WATER -> FfiSubcategory.WATER
    FydeSubcategory.INTERNET -> FfiSubcategory.INTERNET
    FydeSubcategory.HOME_INSURANCE -> FfiSubcategory.HOME_INSURANCE
    FydeSubcategory.HOME_SERVICES -> FfiSubcategory.HOME_SERVICES
    FydeSubcategory.MOBILE -> FfiSubcategory.MOBILE
    FydeSubcategory.BANKING -> FfiSubcategory.BANKING
    FydeSubcategory.SAVINGS -> FfiSubcategory.SAVINGS
    FydeSubcategory.GAMING -> FfiSubcategory.GAMING
    FydeSubcategory.INCOME_TAX -> FfiSubcategory.INCOME_TAX
    FydeSubcategory.TAX_CERTIFICATES -> FfiSubcategory.TAX_CERTIFICATES
    FydeSubcategory.HEALTH_INSURANCE -> FfiSubcategory.HEALTH_INSURANCE
    FydeSubcategory.COMPLEMENTARY_HEALTH -> FfiSubcategory.COMPLEMENTARY_HEALTH
    FydeSubcategory.CARE -> FfiSubcategory.CARE
    FydeSubcategory.PAYSLIPS -> FfiSubcategory.PAYSLIPS
    FydeSubcategory.EMPLOYMENT -> FfiSubcategory.EMPLOYMENT
    FydeSubcategory.HOUSEHOLD_EMPLOYER -> FfiSubcategory.HOUSEHOLD_EMPLOYER
    FydeSubcategory.BENEFITS -> FfiSubcategory.BENEFITS
    FydeSubcategory.CHILDCARE_SCHOOL -> FfiSubcategory.CHILDCARE_SCHOOL
    FydeSubcategory.TRAIN -> FfiSubcategory.TRAIN
    FydeSubcategory.FLIGHT -> FfiSubcategory.FLIGHT
    FydeSubcategory.ROAD -> FfiSubcategory.ROAD
    FydeSubcategory.RIDE -> FfiSubcategory.RIDE
    FydeSubcategory.TRAVEL -> FfiSubcategory.TRAVEL
    FydeSubcategory.ONLINE -> FfiSubcategory.ONLINE
    FydeSubcategory.PETS -> FfiSubcategory.PETS
    FydeSubcategory.GROCERIES -> FfiSubcategory.GROCERIES
    FydeSubcategory.FOOD_DELIVERY -> FfiSubcategory.FOOD_DELIVERY
    FydeSubcategory.MEDIA -> FfiSubcategory.MEDIA
    FydeSubcategory.CLOUD_SOFTWARE -> FfiSubcategory.CLOUD_SOFTWARE
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
    FydeDocumentType.OTHER -> FfiDocumentType.OTHER
}

private fun FydeSearchFilters.toFfiSearchFilters() = FfiSearchFilters(
    categories = categories.map { it.toFfiCategory() },
    subcategories = subcategories.map { it.toFfiSubcategory() },
    types = types.map { it.toFfiDocumentType() },
    contentTypes = contentTypes,
    createdFrom = createdFrom,
    createdUntil = createdUntil,
)

private fun FfiSearchFacets.toFydeSearchFacets() = FydeSearchFacets(
    categories = categories.map { FydeFacetCount(it.value.toFydeCategory(), it.count.toLong()) },
    subcategories = subcategories.map { FydeFacetCount(it.value.toFydeSubcategory(), it.count.toLong()) },
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
    documentType = metadata.type?.toFydeDocumentType(),
    category = metadata.category?.toFydeCategory(),
    subcategory = metadata.subcategory?.toFydeSubcategory(),
    content = content,
)

private fun FfiChangelogEvent.toFydeChangelogEvent() = FydeChangelogEvent(
    id = id,
    eventType = when (eventType) {
        FfiEventType.SAVE_DOCUMENT -> FydeEventType.SAVE_DOCUMENT
        FfiEventType.UPDATE_METADATA -> FydeEventType.UPDATE_METADATA
        FfiEventType.DELETED -> FydeEventType.DELETED
        FfiEventType.INSTALL_SCRIPT -> FydeEventType.INSTALL_SCRIPT
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
                documentType = eventMetadata.type?.toFydeDocumentType(),
                category = eventMetadata.category?.toFydeCategory(),
                subcategory = eventMetadata.subcategory?.toFydeSubcategory(),
                content = eventContent,
            )
        }
    },
)
