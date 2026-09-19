package com.peltoche.fyde

/**
 * A document previously cached locally by `fyde-sdk`, mirroring the SDK's
 * `Ffi*` UniFFI types without leaking `:sdkBridge`'s generated bindings into
 * `commonMain` (see `../../shared/build.gradle.kts`).
 */
data class FydeDocument(
    val id: String,
    val name: String,
    val contentType: String,
    val createdAt: Long,
    val size: Long,
    val checksum: String,
    val transcript: String,
    val content: ByteArray,
)

/**
 * Cross-platform entry point to `fyde-sdk` (`../../sdk`), the Rust client
 * library for talking to a fyde server. Implemented for Desktop (JVM),
 * Android and iOS via the UniFFI-generated bindings in `:sdkBridge`; not
 * yet wired for Web, see `../../../CLAUDE.md`.
 */
expect object FydeSdk {
    /**
     * Connects to a fyde server at [serverUrl], initializing the SDK client
     * for the lifetime of the process. Safe to call more than once; the last
     * call wins.
     */
    suspend fun initialize(serverUrl: String): Result<Unit>

    /**
     * Lists documents previously cached locally, oldest first, one page at a
     * time. Mirrors [`crate::DocumentsService::list`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun listDocuments(offset: Long, limit: Long): Result<List<FydeDocument>>
}
