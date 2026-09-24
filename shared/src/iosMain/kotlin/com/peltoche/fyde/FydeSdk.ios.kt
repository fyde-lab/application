package com.peltoche.fyde

import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSLog
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import uniffi.fyde_sdk.ChangelogListener
import uniffi.fyde_sdk.FfiChangelogEvent
import uniffi.fyde_sdk.FfiClientConfig
import uniffi.fyde_sdk.FfiDocument
import uniffi.fyde_sdk.FfiEventType
import uniffi.fyde_sdk.FfiLogLevel
import uniffi.fyde_sdk.FfiStorage
import uniffi.fyde_sdk.FydeClient
import uniffi.fyde_sdk.LogListener

/** Forwards every SDK log line to iOS's `NSLog`, prefixed with its level. */
private class IosLogListener : LogListener {
    override fun onLog(level: FfiLogLevel, line: String) {
        if (level == FfiLogLevel.OFF) return
        NSLog("[FydeSdk] %s: %s", level.name, line)
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

actual object FydeSdk {
    private var client: FydeClient? = null

    actual suspend fun initialize(serverUrl: String): Result<Unit> = runCatching {
        val config = FfiClientConfig(
            url = serverUrl,
            storage = FfiStorage.Disk(path = defaultStoragePath()),
            logLevel = FfiLogLevel.INFO,
        )
        client = FydeClient.init(config, IosLogListener())
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

    actual suspend fun consume(onEvent: (FydeChangelogEvent) -> Unit): Result<Unit> = runCatching {
        val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
        val listener = onEvent
        client.consume(object : ChangelogListener {
            override fun onEvent(event: FfiChangelogEvent) = listener(event.toFydeChangelogEvent())
        })
    }
}

private fun FfiDocument.toFydeDocument() = FydeDocument(
    id = id,
    name = metadata.name,
    contentType = metadata.contentType,
    createdAt = metadata.createdAt,
    size = metadata.size.toLong(),
    checksum = metadata.checksum,
    transcript = metadata.transcript,
    content = content,
)

private fun FfiChangelogEvent.toFydeChangelogEvent() = FydeChangelogEvent(
    id = id,
    eventType = when (eventType) {
        FfiEventType.CREATED -> FydeEventType.CREATED
        FfiEventType.UPDATE_METADATA -> FydeEventType.UPDATE_METADATA
        FfiEventType.DELETED -> FydeEventType.DELETED
    },
    documentId = documentId,
    document = content?.let { eventContent ->
        metadata?.let { eventMetadata ->
            FydeDocument(
                id = documentId,
                name = eventMetadata.name,
                contentType = eventMetadata.contentType,
                createdAt = eventMetadata.createdAt,
                size = eventMetadata.size.toLong(),
                checksum = eventMetadata.checksum,
                transcript = eventMetadata.transcript,
                content = eventContent,
            )
        }
    },
)
