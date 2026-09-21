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
 * The kind of write recorded by a [FydeChangelogEvent], mirroring the SDK's
 * `FfiEventType`.
 */
enum class FydeEventType {
    CREATED,
    UPDATE_METADATA,
    DELETED,
}

/**
 * A single recorded write against a document, mirroring the SDK's
 * `FfiChangelogEvent` without leaking `:sdkBridge`'s generated bindings into
 * `commonMain`. [document] is only present for [FydeEventType.CREATED]
 * events, which are the only ones that carry content and metadata today.
 */
data class FydeChangelogEvent(
    val offset: Long,
    val eventType: FydeEventType,
    val documentId: String,
    val document: FydeDocument?,
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
     * Verifies [username]/[password] and opens a session for this device,
     * storing the resulting session token so that subsequent calls (like
     * [consume]) can authenticate with the server. Mirrors
     * [`crate::UsersService::login`]; fails if [initialize] hasn't been
     * called yet.
     */
    suspend fun login(username: String, password: String): Result<Unit>

    /**
     * Creates a new account for [username] and opens a session for this
     * device, the same as a subsequent [login] would. Mirrors
     * [`crate::UsersService::create`]; fails if [initialize] hasn't been
     * called yet.
     */
    suspend fun register(username: String, password: String): Result<Unit>

    /**
     * Returns whether a session token is currently persisted, i.e. a prior
     * [login]/[register] call succeeded and no session-clearing call has
     * happened since. Mirrors `sessions::Service::is_connected`; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun isConnected(): Result<Boolean>

    /**
     * Lists documents previously cached locally, oldest first, one page at a
     * time. Mirrors [`crate::DocumentsService::list`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun listDocuments(offset: Long, limit: Long): Result<List<FydeDocument>>

    /**
     * Streams and decrypts every changelog event since the last consumed
     * offset, invoking [onEvent] once for each (replaying history, then
     * continuing with the live tail). Resumes from the cursor persisted
     * locally by a previous call, or from the beginning of the changelog if
     * there is none. Suspends until the server closes the stream or an
     * error occurs, so run it from a background coroutine. Mirrors
     * [`crate::ChangelogService::consume`]; fails if [initialize] hasn't
     * been called yet.
     */
    suspend fun consume(onEvent: (FydeChangelogEvent) -> Unit): Result<Unit>
}
