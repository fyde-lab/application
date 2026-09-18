package com.peltoche.fyde

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
}
