package com.peltoche.fyde

import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import uniffi.fyde_sdk.DocumentChangeListener
import uniffi.fyde_sdk.FfiChangelogEvent
import uniffi.fyde_sdk.FfiClientConfig
import uniffi.fyde_sdk.FfiDocument
import uniffi.fyde_sdk.FfiEventType
import uniffi.fyde_sdk.FfiLogLevel
import uniffi.fyde_sdk.FfiPurpose
import uniffi.fyde_sdk.FfiScript
import uniffi.fyde_sdk.FfiScriptParameter
import uniffi.fyde_sdk.FfiScriptParameterType
import uniffi.fyde_sdk.FfiScriptType
import uniffi.fyde_sdk.FfiSourceCategory
import uniffi.fyde_sdk.FfiSourceSubCategory
import uniffi.fyde_sdk.FfiStorage
import uniffi.fyde_sdk.FydeClient
import uniffi.fyde_sdk.LogListener

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

actual object FydeSdk {
    private var client: FydeClient? = null

    actual suspend fun initialize(
        serverUrl: String,
        onDocumentChange: ((FydeChangelogEvent) -> Unit)?,
    ): Result<Unit> = runCatching {
        val config = FfiClientConfig(
            url = serverUrl,
            storage = FfiStorage.Disk(path = defaultStoragePath()),
            logLevel = FfiLogLevel.INFO,
        )
        val listener = onDocumentChange?.let { callback ->
            object : DocumentChangeListener {
                override fun onEvent(event: FfiChangelogEvent) = callback(event.toFydeChangelogEvent())
            }
        }
        client = FydeClient.init(config, JvmLogListener(), listener)
    }

    actual suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
        client.login(username, password, getPlatform().name)
    }.map { }

    actual suspend fun register(username: String, password: String): Result<Unit> = runCatching {
        val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
        client.createUser(username, password, getPlatform().name)
    }.map { }

    actual suspend fun isConnected(): Result<Boolean> = runCatching {
        val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
        client.isConnected()
    }

    actual suspend fun isServerReachable(): Result<Boolean> = runCatching {
        val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
        client.isServerReachable()
    }

    actual suspend fun listDocuments(offset: Long, limit: Long): Result<List<FydeDocument>> =
        runCatching {
            val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
            client.listDocuments(offset, limit).map(FfiDocument::toFydeDocument)
        }

    actual suspend fun uploadDocument(path: String): Result<String> = runCatching {
        val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
        client.uploadDocument(path)
    }

    actual suspend fun setLanguage(language: String): Result<Unit> = runCatching {
        val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
        client.setLanguage(language)
    }

    actual suspend fun getLanguage(): Result<String?> = runCatching {
        val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
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
        runCatching {
            val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
            client.createScript(
                name,
                isPublic,
                icon,
                script,
                description,
                shortDescription,
                allowedDomains,
                FfiScriptType.SCRAPER,
                parameters.mapValues { (_, parameter) -> parameter.toFfiScriptParameter() },
            ).toFydeScript()
        }

    actual suspend fun enableScript(scriptId: String): Result<Unit> = runCatching {
        val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
        client.enableScript(scriptId)
    }

    actual suspend fun disableScript(scriptId: String): Result<Unit> = runCatching {
        val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
        client.disableScript(scriptId)
    }

    actual suspend fun installScript(
        scriptId: String,
        parameters: Map<String, FydeScriptParameterValue>,
    ): Result<Unit> = runCatching {
        val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
        client.installScript(scriptId, encodeScriptParametersJson(parameters))
    }

    actual suspend fun listUserScripts(): Result<List<FydeScript>> = runCatching {
        val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
        client.listUserScripts().map(FfiScript::toFydeScript)
    }

    actual suspend fun listPublicScripts(): Result<List<FydeScript>> = runCatching {
        val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
        client.listPublicScripts().map(FfiScript::toFydeScript)
    }
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
