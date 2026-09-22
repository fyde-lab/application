package com.peltoche.fyde

import uniffi.fyde_sdk.ChangelogListener
import uniffi.fyde_sdk.FfiChangelogEvent
import uniffi.fyde_sdk.FfiDocument
import uniffi.fyde_sdk.FfiEventType
import uniffi.fyde_sdk.FydeClient

actual object FydeSdk {
    private var client: FydeClient? = null

    actual suspend fun initialize(serverUrl: String): Result<Unit> = runCatching {
        client = FydeClient.connect(serverUrl)
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
