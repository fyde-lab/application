package com.peltoche.fyde

import uniffi.fyde_sdk.FfiDocument
import uniffi.fyde_sdk.FydeClient

actual object FydeSdk {
    private var client: FydeClient? = null

    actual suspend fun initialize(serverUrl: String): Result<Unit> = runCatching {
        client = FydeClient.connect(serverUrl)
    }

    actual suspend fun listDocuments(offset: Long, limit: Long): Result<List<FydeDocument>> =
        runCatching {
            val client = checkNotNull(client) { "FydeSdk.initialize must be called first" }
            client.listDocuments(offset, limit).map(FfiDocument::toFydeDocument)
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
