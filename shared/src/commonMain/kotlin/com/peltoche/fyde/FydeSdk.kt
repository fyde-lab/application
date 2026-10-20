package com.peltoche.fyde

/**
 * The metadata of a document previously cached locally by `fyde-sdk`,
 * mirroring the SDK's `Ffi*` UniFFI types without leaking `:sdkBridge`'s
 * generated bindings into `commonMain` (see `../../shared/build.gradle.kts`).
 * Listings carry no content; fetch it with [FydeSdk.getDocumentContent] once
 * the document is shown.
 */
data class FydeDocument(
    val id: String,
    val name: String,
    val contentType: String,
    val createdAt: Long,
    val size: Long,
    val documentType: FydeDocumentType?,
    val category: FydeCategory?,
    val subcategory: FydeSubcategory?,
)

/**
 * The part of life a document is about (housing, money, taxes...),
 * mirroring the SDK's `FfiCategory`. `null` on [FydeDocument] means not yet
 * classified.
 */
enum class FydeCategory {
    HOUSING,
    TELECOM,
    MONEY,
    TAXES,
    HEALTH,
    WORK,
    FAMILY,
    TRANSPORT,
    SHOPPING,
    SUBSCRIPTIONS,
    DONATIONS,
}

/**
 * A finer-grained classification within one [FydeCategory], mirroring the
 * SDK's `FfiSubcategory`.
 */
enum class FydeSubcategory {
    ENERGY,
    WATER,
    INTERNET,
    HOME_INSURANCE,
    HOME_SERVICES,
    MOBILE,
    BANKING,
    SAVINGS,
    GAMING,
    INCOME_TAX,
    TAX_CERTIFICATES,
    HEALTH_INSURANCE,
    COMPLEMENTARY_HEALTH,
    CARE,
    PAYSLIPS,
    EMPLOYMENT,
    HOUSEHOLD_EMPLOYER,
    BENEFITS,
    CHILDCARE_SCHOOL,
    TRAIN,
    FLIGHT,
    ROAD,
    RIDE,
    TRAVEL,
    ONLINE,
    PETS,
    GROCERIES,
    FOOD_DELIVERY,
    MEDIA,
    CLOUD_SOFTWARE,
}

/**
 * What kind of paper a document is (invoice, payslip, tax notice...),
 * mirroring the SDK's `FfiDocumentType`. `null` on [FydeDocument] means not
 * yet typed.
 */
enum class FydeDocumentType {
    INVOICE,
    RECEIPT,
    STATEMENT,
    REIMBURSEMENT_STATEMENT,
    PAYSLIP,
    CONTRACT,
    INSURANCE_CERTIFICATE,
    INSURANCE_CARD,
    RIGHTS_CERTIFICATE,
    PROOF_OF_RESIDENCE,
    RIB,
    TAX_NOTICE,
    TAX_RETURN,
    TAX_CERTIFICATE,
    EMPLOYMENT_CERTIFICATE,
    FEE_SUMMARY,
    BOOKING,
    MEDICAL_DOCUMENT,
    OTHER,
}

/**
 * Restricts a [FydeSdk.searchDocuments] to documents whose fields match,
 * mirroring the SDK's `FfiSearchFilters`: any of the values within one list,
 * every list that's non-empty, and every bound that's set. The default
 * matches every document.
 */
data class FydeSearchFilters(
    val categories: List<FydeCategory> = emptyList(),
    val subcategories: List<FydeSubcategory> = emptyList(),
    val types: List<FydeDocumentType> = emptyList(),
    /** Compared exactly against [FydeDocument.contentType]. */
    val contentTypes: List<String> = emptyList(),
    /** Inclusive lower bound on [FydeDocument.createdAt], in epoch seconds. */
    val createdFrom: Long? = null,
    /** Inclusive upper bound on [FydeDocument.createdAt], in epoch seconds. */
    val createdUntil: Long? = null,
)

/** How many documents carry one [value] of a field, mirroring the SDK's `Ffi*Count` records. */
data class FydeFacetCount<T>(val value: T, val count: Long)

/**
 * The filter values still worth offering for a search, mirroring the SDK's
 * `FfiSearchFacets`: per field, every value at least one matching document
 * carries, most common first. Each field's values ignore that field's own
 * filter but honor every other one — so picking a category narrows the
 * subcategories and types on offer to that category's, while the other
 * categories compatible with the rest of the search stay available. Computed by
 * `fyde-sdk`, never here.
 */
data class FydeSearchFacets(
    val categories: List<FydeFacetCount<FydeCategory>> = emptyList(),
    val subcategories: List<FydeFacetCount<FydeSubcategory>> = emptyList(),
    val types: List<FydeFacetCount<FydeDocumentType>> = emptyList(),
    val contentTypes: List<FydeFacetCount<String>> = emptyList(),
    /** The oldest matching [FydeDocument.createdAt], ignoring the date filters; `null` when nothing matches. */
    val earliestCreatedAt: Long? = null,
    /** The newest matching [FydeDocument.createdAt], likewise. */
    val latestCreatedAt: Long? = null,
)

/** The order [FydeSdk.searchDocuments] returns its matches in, mirroring the SDK's `FfiSearchOrder`. */
enum class FydeSearchOrder {
    /** Best text match first, newest first among equals; the same as [NEWEST] without search text. */
    RELEVANCE,
    NEWEST,
    OLDEST,
}

/**
 * The kind of write recorded by a [FydeChangelogEvent], mirroring the SDK's
 * `FfiEventType`.
 */
enum class FydeEventType {
    SAVE_DOCUMENT,
    UPDATE_METADATA,
    DELETED,

    /** A script was installed for the account, see [FydeSdk.installScript]. */
    INSTALL_SCRIPT,
}

/**
 * A single recorded write, mirroring the SDK's `FfiChangelogEvent` without
 * leaking `:sdkBridge`'s generated bindings into `commonMain`. [subjectId]
 * is the id of whatever the write is about: a document for
 * [FydeEventType.SAVE_DOCUMENT]/[FydeEventType.UPDATE_METADATA]/
 * [FydeEventType.DELETED], a script for [FydeEventType.INSTALL_SCRIPT].
 * [document] (metadata only) is only present for [FydeEventType.SAVE_DOCUMENT]
 * events, which are the only ones that carry document content and metadata today.
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

/**
 * Decodes the parameter values `FfiClient::list_installed_scripts` returns
 * (`parameters_json`) — the inverse of [encodeScriptParametersJson]: a flat
 * JSON object of strings, numbers and booleans, keyed by parameter machine
 * name. A `null` value is skipped, as an unset optional parameter. Throws
 * [IllegalArgumentException] on anything else (nested values, malformed
 * JSON), which only a fyde-sdk bug could produce.
 */
internal fun decodeScriptParametersJson(json: String): Map<String, FydeScriptParameterValue> =
    FlatJsonObjectParser(json).parse()

private class FlatJsonObjectParser(private val json: String) {
    private var index = 0

    fun parse(): Map<String, FydeScriptParameterValue> {
        val values = linkedMapOf<String, FydeScriptParameterValue>()
        expect('{')
        if (peek() == '}') {
            index++
        } else {
            while (true) {
                val key = string()
                expect(':')
                value()?.let { values[key] = it }
                when (next()) {
                    ',' -> continue
                    '}' -> break
                    else -> fail("expected ',' or '}'")
                }
            }
        }
        skipWhitespace()
        require(index == json.length) { "unexpected trailing content at $index" }
        return values
    }

    private fun value(): FydeScriptParameterValue? = when (val char = peek()) {
        '"' -> FydeScriptParameterValue.Text(string())
        't' -> literal("true", FydeScriptParameterValue.Bool(true))
        'f' -> literal("false", FydeScriptParameterValue.Bool(false))
        'n' -> literal("null", null)
        else -> if (char == '-' || char.isDigit()) number() else fail("unexpected '$char'")
    }

    private fun <T> literal(word: String, result: T): T {
        require(json.startsWith(word, index)) { "expected $word at $index" }
        index += word.length
        return result
    }

    private fun number(): FydeScriptParameterValue {
        val start = index
        while (index < json.length && (json[index].isDigit() || json[index] in "+-.eE")) index++
        val number = json.substring(start, index).toDoubleOrNull() ?: fail("invalid number")
        return FydeScriptParameterValue.Number(number)
    }

    private fun string(): String {
        expect('"')
        return buildString {
            while (true) {
                require(index < json.length) { "unterminated string" }
                when (val char = json[index++]) {
                    '"' -> break
                    '\\' -> when (val escaped = json[index++]) {
                        '"', '\\', '/' -> append(escaped)
                        'n' -> append('\n')
                        'r' -> append('\r')
                        't' -> append('\t')
                        'b' -> append('\b')
                        'f' -> append('\u000C')
                        'u' -> {
                            append(json.substring(index, index + 4).toInt(16).toChar())
                            index += 4
                        }
                        else -> fail("invalid escape '\\$escaped'")
                    }
                    else -> append(char)
                }
            }
        }
    }

    private fun expect(char: Char) {
        if (next() != char) fail("expected '$char'")
    }

    private fun next(): Char {
        skipWhitespace()
        require(index < json.length) { "unexpected end of input" }
        return json[index++]
    }

    private fun peek(): Char {
        skipWhitespace()
        require(index < json.length) { "unexpected end of input" }
        return json[index]
    }

    private fun skipWhitespace() {
        while (index < json.length && json[index].isWhitespace()) index++
    }

    private fun fail(message: String): Nothing = throw IllegalArgumentException("$message at $index in parameters json")
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
 * What a script does, mirroring the SDK's `FfiScriptType`. The server only
 * accepts [PARSER] scripts today; [SCRAPER] is what `fyde-scripts`'
 * `scrapers/` are, run by [FydeSdk.runInstalledScript].
 */
enum class FydeScriptType {
    /** Logs into a third-party site and imports documents from it. */
    SCRAPER,

    /** Classifies each document as it's uploaded; no network access, no parameters. */
    PARSER,
}

/**
 * A user-authored script's metadata, mirroring the SDK's `FfiScript` and
 * `FfiScriptMetadata` without leaking `:sdkBridge`'s generated bindings into
 * `commonMain`. Never carries the script's content: the listings don't
 * return it (see `sdk/src/domains/scripts/models.rs`'s `ScriptMetadata`),
 * and nothing in the app reads it. They don't return its [icon] either: it's
 * empty in a listed script until filled from [FydeSdk.fetchScriptIcon].
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
    val lastUpdated: Long,
    val parameters: Map<String, FydeScriptParameter> = emptyMap(),
    val type: FydeScriptType = FydeScriptType.PARSER,
)

/** A running scraper's progress, mirroring the SDK's `FfiProgressEvent`. */
sealed interface FydeScraperProgress {
    /** A step started; [message] is its wording in the user's language (e.g. "Connexion en cours"). */
    data class Step(val message: String) : FydeScraperProgress

    /** How far the current step has got; [total] is 0 when unknown. */
    data class Update(val current: Long, val total: Long) : FydeScraperProgress
}

/**
 * Receives a [FydeSdk.runInstalledScript] run's progress, and answers the
 * questions its script asks (a one-time code, ...). Called from the run's
 * own background thread, never the UI's: [onQuestion] suspends until the
 * user answers, and its result goes back to the script.
 */
interface FydeScraperHandler {
    fun onProgress(progress: FydeScraperProgress)

    suspend fun onQuestion(question: String): String
}

/**
 * One of an account's open sessions (a device logged into it), mirroring
 * `fyde-sdk`'s `FfiOpenSession`. [createdAt] is in Unix seconds.
 * [isCurrent] flags the session this device is signed in with, in
 * [FydeSdk.listSessions]; it's always `false` in
 * [FydeLoginOutcome.SessionLimitReached], when no session is open here yet.
 */
data class FydeOpenSession(
    val id: String,
    val deviceName: String,
    val createdAt: Long,
    val isCurrent: Boolean = false,
)

/**
 * The plan the signed-in account is on, its limits and what the account
 * currently uses of each, mirroring the SDK's `FfiUsage`. Every figure is
 * shared by all the account's devices. A usage figure can exceed its limit
 * after the account moved to a smaller plan: what it stores is kept, only
 * further writes are refused.
 */
data class FydePlanUsage(
    /** A stable identifier, e.g. "free", "basic" — not display text. */
    val planId: String,
    val maxScripts: Int,
    val maxSessions: Int,
    /** In bytes. */
    val maxSize: Long,
    val enabledScripts: Int,
    /** This device's session included. */
    val openSessions: Int,
    /** In bytes. */
    val totalSize: Long,
)

/** What [FydeSdk.login] did, once the credentials checked out. Mirrors `fyde-sdk`'s `FfiLoginOutcome`. */
sealed interface FydeLoginOutcome {
    /** A session was opened for this device. */
    data object LoggedIn : FydeLoginOutcome

    /**
     * The account already has as many open sessions as its plan allows
     * ([maxSessions]), so none was opened and nothing was stored. Log in
     * again with one of [sessions]' ids as `replaceSessionId` to close it in
     * favor of this device.
     */
    data class SessionLimitReached(
        val maxSessions: Int,
        val sessions: List<FydeOpenSession>,
    ) : FydeLoginOutcome
}

/**
 * Cross-platform entry point to `fyde-sdk` (the `sdk` repo), the Rust client
 * library for talking to a fyde server. Implemented for Desktop (JVM),
 * Android and iOS via the UniFFI-generated bindings in `:sdkBridge`; not
 * yet wired for Web, see `../../../CLAUDE.md`.
 */
expect object FydeSdk {
    /**
     * Connects to a fyde server at [serverUrl], creating the SDK client the
     * first time it succeeds; every later call (from any screen) reuses that
     * one client, only replacing [onDocumentChange] when one is given.
     * Found via review: each call used to create another client, while the
     * previous one kept running and holding the changelog write lock — so
     * every later client fell back to read-only sync, and documents never
     * came back after logging out and in again. [onDocumentChange], if
     * given, is invoked once for every changelog event consumed by the
     * background sync job that [login]/[register] start automatically
     * (replaying persisted history, then continuing with the live tail) — or
     * immediately, if a session from a previous run is already persisted.
     * Mirrors `ClientConfig`'s `on_document_change`.
     */
    suspend fun initialize(
        serverUrl: String,
        onDocumentChange: ((FydeChangelogEvent) -> Unit)? = null,
    ): Result<Unit>

    /**
     * Verifies [username]/[password] and opens a session for this device,
     * storing the resulting session token so that subsequent calls can
     * authenticate with the server, and starts the background changelog
     * sync that feeds [initialize]'s `onDocumentChange`. On a device without
     * secure storage (see [isSecureStorageAvailable]),
     * [allowInsecureStorage] lets the session's keys be kept unencrypted in
     * the local database instead, as for [register] — only pass `true` once
     * the user explicitly accepted that. Mirrors
     * [`crate::UsersService::login`]; fails if [initialize] hasn't been
     * called yet.
     *
     * Succeeds with [FydeLoginOutcome.SessionLimitReached] instead, opening
     * no session, when the account's plan allows no more open sessions:
     * calling this again with one of the listed sessions' ids as
     * [replaceSessionId] closes that session in favor of this device.
     */
    suspend fun login(
        username: String,
        password: String,
        allowInsecureStorage: Boolean = false,
        replaceSessionId: String? = null,
    ): Result<FydeLoginOutcome>

    /**
     * Creates a new account for [username] and opens a session for this
     * device, the same as a subsequent [login] would. On a device without
     * secure storage (see [isSecureStorageAvailable]), [allowInsecureStorage]
     * lets the account's keys be kept unencrypted in the local database
     * instead — only pass `true` once the user explicitly accepted that.
     * Mirrors [`crate::UsersService::create`]; fails if [initialize] hasn't
     * been called yet.
     */
    suspend fun register(username: String, password: String, allowInsecureStorage: Boolean = false): Result<Unit>

    /**
     * Returns whether this device has secure storage (Keychain, Secret
     * Service, ...) for the account's keys; when it doesn't, [register]
     * needs `allowInsecureStorage`. Mirrors
     * [`crate::UsersService::is_secure_storage_available`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun isSecureStorageAvailable(): Result<Boolean>

    /**
     * Returns whether a session token is currently persisted, i.e. a prior
     * [login]/[register] call succeeded and no session-clearing call has
     * happened since. Mirrors `sessions::Service::is_connected`; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun isConnected(): Result<Boolean>

    /**
     * Ends the session on this device: tells the server, then always clears
     * every local trace of the account — the master key and session token in
     * the OS credential store, and the local database with its decrypted
     * document cache — even if the server can't be reached. Fails only if
     * clearing local state itself failed. Mirrors
     * [`crate::UsersService::logout`].
     */
    suspend fun logout(): Result<Unit>

    /**
     * Returns whether the fyde server currently responds as reachable.
     * Mirrors [`crate::ServerStateService::is_server_reachable`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun isServerReachable(): Result<Boolean>

    /**
     * Fetches the signed-in account's plan, limits and current usage from
     * the server. Mirrors [`crate::UsageService::get_usage`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun getUsage(): Result<FydePlanUsage>

    /**
     * Lists every session open on the signed-in account, across all of its
     * users, oldest first, with this device's flagged
     * [FydeOpenSession.isCurrent]. Always asks the server: the account's
     * other devices open and close sessions too. Mirrors
     * [`crate::UsersService::list_sessions`].
     */
    suspend fun listSessions(): Result<List<FydeOpenSession>>

    /**
     * Closes [sessionId], one of [listSessions]' sessions, logging its device
     * out on its next call. Use [logout] for this device's own session, which
     * also clears local state. Mirrors [`crate::UsersService::revoke_session`].
     */
    suspend fun revokeSession(sessionId: String): Result<Unit>

    /**
     * Lists documents previously cached locally, oldest first, one page at a
     * time. Mirrors [`crate::DocumentsService::list`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun listDocuments(offset: Long, limit: Long): Result<List<FydeDocument>>

    /**
     * Searches documents previously cached locally for [text] — every word
     * must match, as a case- and accent-insensitive prefix, the document's
     * name, original name, text content, type or classification
     * (blank matches every document) — keeping only those matching
     * [filters], one page at a time in [order]. Mirrors
     * [`crate::DocumentsService::search`]; fails if [initialize] hasn't been
     * called yet.
     */
    suspend fun searchDocuments(
        text: String,
        filters: FydeSearchFilters,
        order: FydeSearchOrder,
        offset: Long,
        limit: Long,
    ): Result<List<FydeDocument>>

    /**
     * The filter values still worth offering for a [searchDocuments] of
     * [text] narrowed by [filters] (see [FydeSearchFacets]). Mirrors
     * [`crate::DocumentsService::search_facets`]; fails if [initialize]
     * hasn't been called yet.
     */
    suspend fun searchDocumentFacets(text: String, filters: FydeSearchFilters): Result<FydeSearchFacets>

    /**
     * The content of the document [id] previously cached locally, or `null`
     * if there's none. Mirrors [`crate::DocumentsService::get`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun getDocumentContent(id: String): Result<ByteArray?>

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
     * Creates a new [FydeScriptType.PARSER] script owned by the authenticated
     * user, at version 1 — the only type the server accepts.
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
     * encrypted, as a [FydeEventType.INSTALL_SCRIPT] changelog event.
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

    /**
     * The icon of the script [scriptId] (an encoded image, empty if it has
     * none), which [listUserScripts]/[listPublicScripts] leave out. Mirrors
     * [`crate::ScriptsService::fetch_script_icon`]; fails if [initialize]
     * hasn't been called yet.
     */
    suspend fun fetchScriptIcon(scriptId: String): Result<ByteArray>

    /**
     * The parameter values each installed script was last installed with,
     * keyed by script id, for every installation this device has synced so
     * far — what [installScript] last published for it. Mirrors
     * [`crate::ScriptsService::list_installed_scripts`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun installedScriptParameters(): Result<Map<String, Map<String, FydeScriptParameterValue>>>

    /** Where [runInstalledScript] reports progress and asks questions; `null` answers every question with "". */
    fun setScraperHandler(handler: FydeScraperHandler?)

    /**
     * Runs the installed script [scriptId] with the values it was installed
     * with, until it finishes; documents it saves are uploaded like any
     * other. Mirrors `FfiClient::run_installed_script`; fails if the
     * installation hasn't synced to this device yet.
     */
    suspend fun runInstalledScript(scriptId: String): Result<Unit>
}
