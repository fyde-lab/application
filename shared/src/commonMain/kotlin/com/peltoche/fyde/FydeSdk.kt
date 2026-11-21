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
    /** The folder holding the document, `null` for one at the root (À classer). */
    val folderId: String?,
    /** Typed values by attribute name (`issuer`, `total_amount`...), mirroring the SDK's `Metadata::attributes`. */
    val attributes: Map<String, FydeAttribute> = emptyMap(),
)

/**
 * A document [FydeSdk.similarDocuments] found close in meaning to another,
 * mirroring the SDK's `SimilarDocument`. [confidence] is how close, from 0
 * (the model's "unrelated" floor) to 1 (identical text): the SDK's own
 * scale, shown as a percentage, never re-ranked or filtered in the app.
 */
data class FydeSimilarDocument(val document: FydeDocument, val confidence: Float)

/**
 * A document's first page as an image, from [FydeSdk.documentThumbnail],
 * mirroring the SDK's `Thumbnail`: [png] is PNG-encoded, [width] x [height]
 * pixels. Not a data class, so its content never lands in a `toString`.
 */
class FydeThumbnail(val png: ByteArray, val width: Int, val height: Int)

/**
 * One typed value of a document's attributes, mirroring the SDK's
 * `FfiAttribute`. Amounts are minor units of their ISO 4217 currency (cents
 * for `"EUR"`), never floats; dates are ISO 8601 `YYYY-MM-DD`.
 */
sealed interface FydeAttribute {
    data class Text(val value: String) : FydeAttribute
    data class Integer(val value: Long) : FydeAttribute
    data class Decimal(val value: Double) : FydeAttribute
    data class Bool(val value: Boolean) : FydeAttribute
    data class Date(val value: String) : FydeAttribute
    data class Amount(val minorUnits: Long, val currency: String) : FydeAttribute
    data class Select(val selected: String, val options: List<String>) : FydeAttribute
}

/** A top-level folder's colour, mirroring the SDK's `FfiFolderColor` (each a fill and an ink of the design's folder palette). */
enum class FydeFolderColor { SAND, GREEN, TEAL, BRICK, ROSE, BLUE, INDIGO, AMBER, OCHRE, CYAN, SLATE, LEAF, RUST, PLUM, VIOLET, MOSS }

/** A top-level folder's icon, mirroring the SDK's `FfiFolderIcon`. */
enum class FydeFolderIcon {
    FOLDER, HOME, WALLET, BRIEFCASE, LANDMARK, HEART, PHONE, TRAIN, BAG, USERS,
    ID_CARD, SCALE, GRADUATION_CAP, PAW, TICKET, REPEAT, GIFT, CAR, KEY, CALENDAR,
}

/** The colour and icon a top-level folder shows, mirroring the SDK's `FfiFolderAppearance`. */
data class FydeFolderAppearance(val color: FydeFolderColor, val icon: FydeFolderIcon)

/**
 * A folder of the vault, mirroring the SDK's `FfiFolder`: it holds documents,
 * subfolders, or both. [appearance] is set on top-level folders only; a
 * subfolder shows its top-level folder's.
 */
data class FydeFolder(
    val id: String,
    val parentId: String?,
    val name: String,
    val appearance: FydeFolderAppearance?,
)

/** What deleting a folder does with what it holds, mirroring the SDK's `FfiFolderContents`. */
enum class FydeFolderContents {
    /** Its subfolders and documents move up to its parent. */
    KEEP,

    /** They're deleted with it, from every device. */
    DELETE,
}

/** Where a search is restricted to, mirroring the SDK's `FfiFolderFilter`. */
sealed interface FydeFolderFilter {
    /** The documents at the root: À classer. */
    data object Unfiled : FydeFolderFilter

    /** The documents of [folderId] and every folder below it. */
    data class Within(val folderId: String) : FydeFolderFilter
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
    QUOTE,
    ID_DOCUMENT,
    CIVIL_STATUS_RECORD,
    LETTER,
    ACKNOWLEDGEMENT,
    FORM,
    PAYMENT_SCHEDULE,
    MANDATE,
    TERMS_AND_CONDITIONS,
    BROCHURE,
    TIMESHEET,
    INSPECTION_REPORT,
    NOTARIAL_DEED,
    ATTESTATION,
    PAYMENT_REMINDER,
    FINE,
    WARRANTY,
    RESUME,
    MINUTES,
    DIPLOMA,
    OTHER,
}

/**
 * Restricts a [FydeSdk.searchDocuments] to documents whose fields match,
 * mirroring the SDK's `FfiSearchFilters`: any of the values within one list,
 * every list that's non-empty, and every bound that's set. The default
 * matches every document.
 */
data class FydeSearchFilters(
    /** A folder (subfolders included) or À classer; `null` for the whole vault. */
    val folder: FydeFolderFilter? = null,
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
 * filter but honor every other one — so picking a type narrows the folders
 * on offer to those holding it. Computed by `fyde-sdk`, never here.
 */
data class FydeSearchFacets(
    /** Each folder holding a matching document, itself or below, by id, with that count (its subfolders' included). */
    val folders: List<FydeFacetCount<String>> = emptyList(),
    /** How many matching documents are in À classer. */
    val unfiled: Long = 0,
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
    /** A document was uploaded, by the user or a source's run. */
    DOCUMENT_UPLOADED,

    /** A document's name changed. */
    DOCUMENT_RENAMED,

    /** A document's type or attributes changed. */
    DOCUMENT_ATTRIBUTES_UPDATED,

    /** A document moved to another folder, or to À classer. */
    DOCUMENT_MOVED,

    /** A document was deleted. */
    DOCUMENT_DELETED,

    /** A folder was created. */
    FOLDER_CREATED,

    /** A folder's name changed. */
    FOLDER_RENAMED,

    /** A top-level folder's colour or icon changed. */
    FOLDER_APPEARANCE_CHANGED,

    /** A folder moved under another one, or to the top level. */
    FOLDER_MOVED,

    /** A folder was deleted, keeping or deleting what it held. */
    FOLDER_DELETED,

    /** A script was installed: a new installation, see [FydeSdk.installScript]. */
    SCRIPT_INSTALLED,

    /** An installation's parameters (its credentials) changed. */
    INSTALLATION_CREDENTIALS_UPDATED,

    /** An installation's label changed. */
    INSTALLATION_RENAMED,

    /** An installation now files its documents into another folder. */
    INSTALLATION_FOLDER_CHANGED,

    /** An installation moved to another version of its script. */
    INSTALLATION_UPGRADED,

    /** An installation was removed, see [FydeSdk.uninstallScript]. */
    SCRIPT_UNINSTALLED,
}

/**
 * A single recorded write, mirroring the SDK's `FfiChangelogEvent` without
 * leaking `:sdkBridge`'s generated bindings into `commonMain`. [subjectId]
 * is the id of whatever the write is about: a document for the `DOCUMENT_*`
 * events, a folder for the `FOLDER_*` ones, an installation for the others.
 * [document] (metadata only) is only present for
 * [FydeEventType.DOCUMENT_UPLOADED] events, the only ones that carry document
 * content.
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
    /**
     * The only values a [FydeScriptParameterType.STRING] parameter accepts,
     * offered as a select; empty for free input.
     */
    val choices: List<FydeScriptParameterChoice> = emptyList(),
)

/**
 * One of the values a [FydeScriptParameter] accepts, mirroring the SDK's
 * `FfiScriptParameterChoice`: [value] is what the script receives, [label]
 * what the form shows.
 */
data class FydeScriptParameterChoice(val value: String, val label: String)

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
 * and nothing in the app reads it. Nor its [icon] image, only its [iconId]
 * (`null` if it has none): [icon] stays empty until filled from
 * [FydeSdk.fetchIcon].
 */
data class FydeScript(
    val id: String,
    val name: String,
    val description: String,
    val shortDescription: String,
    val isPublic: Boolean,
    val iconId: String? = null,
    val icon: ByteArray,
    val allowedDomains: List<String>,
    val version: Long,
    val lastUpdated: Long,
    val parameters: Map<String, FydeScriptParameter> = emptyMap(),
    val type: FydeScriptType = FydeScriptType.PARSER,
)

/**
 * One installation of a script for the authenticated user: one account on
 * its site, mirroring the SDK's `FfiInstalledScript`. A user can install the
 * same script several times, once per account; each has its own [id] (what
 * [FydeSdk.runInstalledScript], [FydeSdk.stopScriptRun],
 * [FydeSdk.uninstallScript] and a reinstall take) and [label] (never blank),
 * and runs, signs in and fails on its own. [parameters] are the values it
 * was installed with, keyed by parameter machine name. [folderId] is the
 * folder every document it fetches is filed in, `null` for automatic filing;
 * it may name a folder deleted since.
 */
data class FydeInstallation(
    val id: String,
    val scriptId: String,
    val label: String,
    val parameters: Map<String, FydeScriptParameterValue>,
    val folderId: String? = null,
)

/** A high-level stage of a scraper run, mirroring the SDK's `FfiProgressStep`. */
enum class FydeScraperStep {
    /** The SDK preparing the run (on-device model, browser) before its script starts. */
    SETTING_UP,
    LOGGING_IN,
    CHECKING_SESSION,
    WAITING_FOR_VERIFICATION_CODE,
    WAITING_FOR_CAPTCHA,
    LISTING_DOCUMENTS,
    DOWNLOADING_DOCUMENTS,
    DONE,
}

/** A running scraper's progress, mirroring the SDK's `FfiProgressEvent`. */
sealed interface FydeScraperProgress {
    /** [step] started; [message] is its wording in the user's language (e.g. "Connexion en cours"). */
    data class Step(val step: FydeScraperStep, val message: String) : FydeScraperProgress

    /** How far the current step has got; [total] is 0 when unknown. */
    data class Update(val current: Long, val total: Long) : FydeScraperProgress
}

/**
 * Receives a [FydeSdk.runInstalledScript] run's progress, and answers the
 * questions its script asks (a one-time code, ...). Called from the run's
 * own background thread, never the UI's: [onQuestion] suspends until the
 * user answers, and its result goes back to the script. Several
 * installations can run at once, so both name the [installationId] whose
 * run it is.
 */
interface FydeScraperHandler {
    fun onProgress(installationId: String, progress: FydeScraperProgress)

    suspend fun onQuestion(installationId: String, question: String): String
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
 * How far this device's sync has got through the account's changelog since
 * it started (at login, or at launch with a session open), mirroring the
 * SDK's `FfiSyncProgress`: what a device just signed into shows while the
 * account's history arrives (O9, DO9). Counts changelog events (a
 * document's, a folder's, a source's), not documents: the server can't tell
 * them apart.
 */
data class FydeSyncProgress(
    /** Events gone through so far. */
    val consumed: Long,
    /** Events to go through to catch up; null until the server has counted them, or if it can't. Never below [consumed]. */
    val total: Long?,
    /** Whether every event the server counted has been gone through: what arrives from then on is live. */
    val caughtUp: Boolean,
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

/** How hard a password is to guess, weakest first. Mirrors `fyde-sdk`'s `FfiPasswordStrength`. */
enum class FydePasswordStrength { Weak, Fair, Strong, VeryStrong }

/**
 * The SDK's verdict on a candidate password, mirroring `FfiPasswordAnalysis`:
 * [score] from 0 to 4, [length] in characters. A password that isn't
 * [isAccepted] (one rated [FydePasswordStrength.Weak]) is refused when an
 * account is created or a password changed.
 */
data class FydePasswordAnalysis(
    val score: Int,
    val strength: FydePasswordStrength,
    val length: Int,
    val isAccepted: Boolean,
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
     * Whether this platform shows documents' first pages: desktop, Android
     * and iOS. Starts the SDK's thumbnail job (`FfiBackgroundJobs.thumbnails`),
     * the only thing that draws them; off (Web), none is ever drawn and the
     * app never asks for one ([documentThumbnail]).
     */
    val showsThumbnails: Boolean

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
     * How far sync has got through the account's changelog (see
     * [FydeSyncProgress]), counted afresh at each login. Local: poll it.
     * Mirrors `DocumentsService::sync_progress`; fails if [initialize]
     * hasn't been called yet.
     */
    suspend fun syncProgress(): Result<FydeSyncProgress>

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
     * Generates a new recovery key for the signed-in account, replacing any
     * previous one, and returns its words (24 English BIP39 words, in
     * order) for the user to write down: they're stored nowhere, so they
     * can't be shown again. Mirrors
     * [`crate::UsersService::generate_recovery_key`].
     */
    suspend fun generateRecoveryKey(): Result<List<String>>

    /**
     * When the signed-in account's current recovery key was generated, in
     * Unix seconds, or `null` if it has none, for offering to create or
     * replace it. Asks the server each time. Mirrors
     * [`crate::UsersService::recovery_key_status`].
     */
    suspend fun recoveryKeyStatus(): Result<Long?>

    /**
     * Changes the signed-in account's password. Documents stay readable, the
     * recovery key stays valid and every device stays signed in. Fails if
     * [currentPassword] is wrong, if [newPassword] is too weak, or if this
     * device's session predates the SDK remembering the username (log out
     * and back in). Mirrors [`crate::UsersService::change_password`].
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>

    /** Rates a candidate password, as account creation and [changePassword] will. Mirrors `fyde_sdk::analyze_password`. */
    suspend fun analyzePassword(password: String): Result<FydePasswordAnalysis>

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
     * The other documents cached locally whose text is close in meaning to
     * that of the document [id], most similar first, at most [limit], each
     * with its [FydeSimilarDocument.confidence]; none for a document without
     * text or not indexed yet. Mirrors [`crate::DocumentsService::similar`].
     */
    suspend fun similarDocuments(id: String, limit: Long): Result<List<FydeSimilarDocument>>

    /**
     * The content of the document [id] previously cached locally, or `null`
     * if there's none. Mirrors [`crate::DocumentsService::get`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun getDocumentContent(id: String): Result<ByteArray?>

    /**
     * The first page of the PDF document [id] as stored in the SDK's local
     * database, as large as fits the SDK's fixed square (512 px a side), its
     * aspect ratio kept, on white; `null` if the document isn't cached here
     * or no page is stored for it. Never drawn on request: only the SDK's
     * background job draws them (`FfiBackgroundJobs.thumbnails`). Fails for
     * content that isn't a readable PDF. Mirrors
     * [`crate::DocumentsService::thumbnail`].
     */
    suspend fun documentThumbnail(id: String): Result<FydeThumbnail?>

    /**
     * Reads the local file at [path], encrypts it and its metadata, then
     * publishes it as a "created" changelog event, returning its generated
     * id. Mirrors [`crate::DocumentsService::upload`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun uploadDocument(path: String): Result<String>

    /**
     * Moves the document [id] into [folderId] (`null` for the root, À
     * classer). Mirrors [`crate::DocumentsService::move_document`].
     */
    suspend fun moveDocument(id: String, folderId: String?): Result<Unit>

    /** Every folder synced to this device, by name. Mirrors [`crate::FoldersService::list`]. */
    suspend fun listFolders(): Result<List<FydeFolder>>

    /**
     * Publishes a new folder under [parentId] (`null` for the top level, which
     * needs an [appearance]) and returns it; it shows up in [listFolders] once
     * synced. Mirrors [`crate::FoldersService::create`].
     */
    suspend fun createFolder(
        parentId: String?,
        name: String,
        appearance: FydeFolderAppearance?,
    ): Result<FydeFolder>

    /** Renames or recolours the folder [id]; each `null` keeps its value. Mirrors [`crate::FoldersService::update`]. */
    suspend fun updateFolder(
        id: String,
        name: String? = null,
        appearance: FydeFolderAppearance? = null,
    ): Result<Unit>

    /** Moves the folder [id] under [parentId] (`null` for the top level). Mirrors [`crate::FoldersService::move_folder`]. */
    suspend fun moveFolder(id: String, parentId: String?): Result<Unit>

    /** Deletes the folder [id], keeping or deleting what it holds. Mirrors [`crate::FoldersService::delete`]. */
    suspend fun deleteFolder(id: String, contents: FydeFolderContents): Result<Unit>

    /**
     * Publishes the deletion of the document [id] as a changelog event; it
     * leaves the local cache, and [initialize]'s callback fires, once sync
     * consumes it. Mirrors [`crate::DocumentsService::delete`].
     */
    suspend fun deleteDocument(id: String): Result<Unit>

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
     * [FydeScript.parameters]): as a new installation (one more account on
     * its site) when [installationId] is `null`, else replacing that one.
     * [label] tells it apart from the user's other installations of the
     * script; a blank one defaults to its username. Every document it
     * fetches is filed in [folderId], or automatically when `null`; a
     * reinstall passes the installation's [FydeInstallation.folderId] to keep
     * it. Publishes the installation, encrypted, as a
     * [FydeEventType.SCRIPT_INSTALLED] changelog event (for a reinstall, one
     * event per change: [FydeEventType.INSTALLATION_CREDENTIALS_UPDATED],
     * [FydeEventType.INSTALLATION_RENAMED]...), and returns its id.
     * Fails if a parameter the script marks required is missing, the label is
     * too long, or [folderId] isn't a folder synced here. Mirrors
     * [`crate::ScriptsService::install_script`]; doesn't enable the script
     * ([enableScript]); fails if [initialize] hasn't been called yet.
     */
    suspend fun installScript(
        installationId: String?,
        scriptId: String,
        label: String,
        parameters: Map<String, FydeScriptParameterValue>,
        folderId: String?,
    ): Result<String>

    /**
     * Uninstalls [installationId]: publishes a
     * [FydeEventType.SCRIPT_UNINSTALLED] event, which erases its credentials
     * and saved sign-in from every device once synced. Doesn't disable its
     * script ([disableScript]): callers do once none of its installations
     * remains. Mirrors [`crate::ScriptsService::uninstall_script`].
     */
    suspend fun uninstallScript(installationId: String): Result<Unit>

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
     * The icon [iconId] (an encoded image, e.g. a [FydeScript.iconId]), which
     * [listUserScripts]/[listPublicScripts] leave out. Mirrors
     * [`crate::IconsService::fetch_icon`], which caches icons on the device;
     * fails if [initialize] hasn't been called yet.
     */
    suspend fun fetchIcon(iconId: String): Result<ByteArray>

    /**
     * Every installation this device has synced so far, oldest first, as
     * [installScript] last published it. Mirrors
     * [`crate::ScriptsService::list_installed_scripts`]; fails if
     * [initialize] hasn't been called yet.
     */
    suspend fun listInstallations(): Result<List<FydeInstallation>>

    /** Where [runInstalledScript] reports progress and asks questions; `null` answers every question with "". */
    fun setScraperHandler(handler: FydeScraperHandler?)

    /**
     * Runs installation [installationId] with the values it was installed
     * with, until it finishes; documents it saves are uploaded like any
     * other. Mirrors `FfiClient::run_installed_script`; fails if the
     * installation hasn't synced to this device yet.
     */
    suspend fun runInstalledScript(installationId: String): Result<Unit>

    /**
     * Stops [installationId]'s run that [runInstalledScript] started, returning
     * whether there was one. Returns at once: that [runInstalledScript] call
     * then fails with [FydeSdkException.isRunStopped] once its script has
     * really stopped (unless it finished first). A question it was waiting
     * on is abandoned, its answer dropped. Mirrors `FfiClient::stop_script_run`.
     */
    suspend fun stopScriptRun(installationId: String): Result<Boolean>
}
