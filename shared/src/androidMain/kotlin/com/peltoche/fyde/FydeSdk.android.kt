package com.peltoche.fyde

import android.util.Log
import java.io.File
import uniffi.fyde_sdk.DocumentChangeListener
import uniffi.fyde_sdk.FfiChangelogEvent
import uniffi.fyde_sdk.FfiClientConfig
import uniffi.fyde_sdk.FfiDocument
import uniffi.fyde_sdk.FfiEventType
import uniffi.fyde_sdk.FfiLogLevel
import uniffi.fyde_sdk.FfiStorage
import uniffi.fyde_sdk.FydeClient
import uniffi.fyde_sdk.LogListener

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
 * The on-disk database path fyde-sdk persists local state to on Android:
 * `fyde.db` inside the app's private, per-app storage directory (backed up
 * on app uninstall/data-clear, and off-limits to other apps).
 */
private fun defaultStoragePath(): String = File(appContext.filesDir, "fyde.db").absolutePath

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
        client = FydeClient.init(config, AndroidLogListener(), listener)
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
