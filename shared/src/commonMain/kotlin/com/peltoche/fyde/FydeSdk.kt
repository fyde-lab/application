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
    val sourceCategory: FydeSourceCategory?,
    val sourceSubCategory: FydeSourceSubCategory?,
    val subjects: List<String>,
    val purpose: FydePurpose?,
    val content: ByteArray,
)

/**
 * Who/what a document originated from, mirroring the SDK's
 * `FfiSourceCategory`. `null` on [FydeDocument] means not yet classified.
 */
enum class FydeSourceCategory {
    BANK,
    INSURANCE,
    RETAIL,
    TELECOM,
    ENERGY,
    WATER,
    HEALTH,
    GOV,
    ASSOCIATION,
    EDUCATION,
    EMPLOYER,
    TRANSPORT,
    GOODS,
    ALIMENTATION,
    BUILDING,
    REAL_ESTATE,
    WEB,
    INDIVIDUAL,
    SHOPPING,
}

/**
 * A finer-grained classification within a [FydeSourceCategory], mirroring
 * the SDK's `FfiSourceSubCategory`.
 */
enum class FydeSourceSubCategory {
    CIVIL_REGISTRATION,
    IMMIGRATION,
    TRANSPORT,
    FAMILY,
    TAX,
    HEALTH,
    REAL_ESTATE,
    MOBILE,
    INTERNET,
    CITIZEN,
    SPORT,
}

/**
 * What a document is for, mirroring the SDK's `FfiPurpose`.
 */
enum class FydePurpose {
    ATTESTATION,
    CONTRACT,
    INVOICE,
    REPORT,
    DESCRIPTION,
    EVALUATION,
    EMPLOYMENT,
}

/**
 * The kind of write recorded by a [FydeChangelogEvent], mirroring the SDK's
 * `FfiEventType`.
 */
enum class FydeEventType {
    CREATED,
    UPDATE_METADATA,
    DELETED,

    /** A script was installed for the account, see [FydeSdk.installScript]. */
    SCRIPT_INSTALLED,
}

/**
 * A single recorded write, mirroring the SDK's `FfiChangelogEvent` without
 * leaking `:sdkBridge`'s generated bindings into `commonMain`. [subjectId]
 * is the id of whatever the write is about: a document for
 * [FydeEventType.CREATED]/[FydeEventType.UPDATE_METADATA]/
 * [FydeEventType.DELETED], a script for [FydeEventType.SCRIPT_INSTALLED].
 * [document] is only present for [FydeEventType.CREATED] events, which are
 * the only ones that carry document content and metadata today.
 */
data class FydeChangelogEvent(
    val id: String,
    val eventType: FydeEventType,
    val subjectId: String,
    val document: FydeDocument?,
)

/**
 * What kind of value a [FydeScriptParameter] expects, mirroring the SDK's
 * `FfiScriptParameterType`, so a form can render the right control (text
 * field, number field, toggle) and coerce the entered value before sending
 * it back to the script.
 */
enum class FydeScriptParameterType {
    STRING,
    NUMBER,
    BOOLEAN,
}

/**
 * A single configurable parameter a script declares it needs from the user
 * (e.g. a login username, an API key) before it can run, mirroring the
 * SDK's `FfiScriptParameter`. A script's full set of these is carried on
 * [FydeScript.parameters], keyed by the parameter's machine name (e.g.
 * "username", "password") as the script itself refers to it.
 */
data class FydeScriptParameter(
    val label: String,
    val placeholder: String,
    val type: FydeScriptParameterType,
    val required: Boolean,
    val secret: Boolean,
)

/**
 * A value entered for one of a script's [FydeScriptParameter]s, typed per
 * [FydeScriptParameter.type] so it reaches the script as the matching Lua
 * type (string, number, boolean) rather than always as a string.
 */
sealed interface FydeScriptParameterValue {
    data class Text(val value: String) : FydeScriptParameterValue

    /** Must be finite: JSON (and so the script) has no NaN or infinity. */
    data class Number(val value: Double) : FydeScriptParameterValue

    data class Bool(val value: Boolean) : FydeScriptParameterValue
}

/**
 * Encodes [parameters] as the JSON object `FfiClient::install_script`
 * takes, keyed by parameter machine name. Whole numbers are written without
 * a fractional part (`42`, not `42.0`), so a script reading an integer
 * parameter gets an integer. Hand-rolled since `shared` has no JSON library
 * and this is its only JSON output.
 */
internal fun encodeScriptParametersJson(parameters: Map<String, FydeScriptParameterValue>): String =
    parameters.entries.joinToString(separator = ",", prefix = "{", postfix = "}") { (key, value) ->
        val encoded = when (value) {
            is FydeScriptParameterValue.Text -> encodeJsonString(value.value)
            is FydeScriptParameterValue.Bool -> value.value.toString()
            is FydeScriptParameterValue.Number -> {
                require(value.value.isFinite()) { "parameter $key is not a finite number: ${value.value}" }
                val asLong = value.value.toLong()
                if (asLong.toDouble() == value.value) asLong.toString() else value.value.toString()
            }
        }
        "${encodeJsonString(key)}:$encoded"
    }

private fun encodeJsonString(value: String): String = buildString {
    append('"')
    for (char in value) {
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (char < ' ') {
                append("\\u")
                append(char.code.toString(16).padStart(4, '0'))
            } else {
                append(char)
            }
        }
    }
    append('"')
}

/**
 * A user-authored script, mirroring the SDK's `FfiScript` without leaking
 * `:sdkBridge`'s generated bindings into `commonMain`. Unlike [FydeDocument],
 * [script] is plain text, not ciphertext — see `sdk/src/domains/scripts/
 * models.rs`'s doc comment for why.
 */
data class FydeScript(
    val id: String,
    val name: String,
    val description: String,
    val shortDescription: String,
    val isPublic: Boolean,
    val icon: ByteArray,
    val allowedDomains: List<String>,
    val version: Long,
    val script: String,
    val lastUpdated: Long,
    val parameters: Map<String, FydeScriptParameter> = emptyMap(),
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

    /**
     * Creates a new script owned by the authenticated user, at version 1.
     * Mirrors [`crate::ScriptsService::create_script`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun createScript(
        name: String,
        isPublic: Boolean,
        icon: ByteArray,
        script: String,
        description: String,
        shortDescription: String,
        allowedDomains: List<String>,
        parameters: Map<String, FydeScriptParameter> = emptyMap(),
    ): Result<FydeScript>

    /**
     * Enables [scriptId] for the authenticated user, so it starts appearing
     * in [listUserScripts]. Mirrors [`crate::ScriptsService::enable_script`];
     * fails if [initialize] hasn't been called yet.
     */
    suspend fun enableScript(scriptId: String): Result<Unit>

    /**
     * Disables [scriptId] for the authenticated user, so it stops appearing
     * in [listUserScripts]. Mirrors
     * [`crate::ScriptsService::disable_script`]; fails if [initialize]
     * hasn't been called yet.
     */
    suspend fun disableScript(scriptId: String): Result<Unit>

    /**
     * Installs [scriptId] for the authenticated user, to be run with
     * [parameters] (keyed by parameter machine name, see
     * [FydeScript.parameters]): publishes the script and those values,
     * encrypted, as a [FydeEventType.SCRIPT_INSTALLED] changelog event.
     * Fails if a parameter the script marks required is missing. Mirrors
     * [`crate::ScriptsService::install_script`]; fails if [initialize]
     * hasn't been called yet.
     */
    suspend fun installScript(
        scriptId: String,
        parameters: Map<String, FydeScriptParameterValue>,
    ): Result<Unit>

    /**
     * Lists the scripts currently enabled for the authenticated user.
     * Mirrors [`crate::ScriptsService::list_user_scripts`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun listUserScripts(): Result<List<FydeScript>>

    /**
     * Lists every script marked public, regardless of who owns it or
     * whether the authenticated user has it enabled. Mirrors
     * [`crate::ScriptsService::list_public_scripts`]; fails if [initialize]
     * hasn't been called yet.
     */
    suspend fun listPublicScripts(): Result<List<FydeScript>>
}
