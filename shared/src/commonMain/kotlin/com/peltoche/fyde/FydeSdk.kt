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
    val id: String,
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
     * call wins. [onDocumentChange], if given, is invoked once for every
     * changelog event consumed by the background sync job that [login]/
     * [register] start automatically (replaying persisted history, then
     * continuing with the live tail) — or immediately, if a session from a
     * previous run is already persisted. Mirrors `ClientConfig`'s
     * `on_document_change`.
     */
    suspend fun initialize(
        serverUrl: String,
        onDocumentChange: ((FydeChangelogEvent) -> Unit)? = null,
    ): Result<Unit>

    /**
     * Verifies [username]/[password] and opens a session for this device,
     * storing the resulting session token so that subsequent calls can
     * authenticate with the server, and starts the background changelog
     * sync that feeds [initialize]'s `onDocumentChange`. Mirrors
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
     * Returns whether the fyde server currently responds as reachable.
     * Mirrors [`crate::ServerStateService::is_server_reachable`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun isServerReachable(): Result<Boolean>

    /**
     * Lists documents previously cached locally, oldest first, one page at a
     * time. Mirrors [`crate::DocumentsService::list`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun listDocuments(offset: Long, limit: Long): Result<List<FydeDocument>>

    /**
     * Reads the local file at [path], encrypts it and its metadata, then
     * publishes it as a "created" changelog event, returning its generated
     * id. Mirrors [`crate::DocumentsService::upload`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun uploadDocument(path: String): Result<String>

    /**
     * Persists [language] (a short code, e.g. "en", "fr") as this device's
     * UI language preference, read back by [getLanguage]. Mirrors
     * [`crate::UsersService::set_language`]; fails if [initialize] hasn't
     * been called yet.
     */
    suspend fun setLanguage(language: String): Result<Unit>

    /**
     * Returns this device's UI language preference previously persisted by
     * [setLanguage], or `null` if never set. Mirrors
     * [`crate::UsersService::get_language`]; fails if [initialize] hasn't
     * been called yet.
     */
    suspend fun getLanguage(): Result<String?>
}
