package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class FydeSdkJvmTest {
    @Test
    fun `initialize reaches the native fyde-sdk client and surfaces connection failures`() = runBlocking {
        // The underlying gRPC channel connects lazily, so `initialize`
        // itself always succeeds without dialing the server; the login
        // call below is what actually exercises the real JNI call into
        // fyde-sdk (via :sdkBridge) end-to-end, and, since no server is
        // listening on this port, expects to fail with a connection error
        // rather than a linkage error.
        FydeSdk.initialize("http://127.0.0.1:1").getOrThrow()

        val result = FydeSdk.login("username", "password")

        assertTrue(result.isFailure, "expected a connection failure, got $result")
    }

    @Test
    fun `setLanguage persists and getLanguage reads it back through the native fyde-sdk client`() = runBlocking {
        // Purely local (no gRPC round trip), unlike login/register above, so
        // this doesn't need a live server to exercise the real JNI call into
        // fyde-sdk end-to-end. Doesn't assert an initial `null` value: the
        // default storage path is real, on-disk state shared across test
        // runs on this machine, so a prior run may already have set one.
        FydeSdk.initialize("http://127.0.0.1:1").getOrThrow()

        FydeSdk.setLanguage("fr").getOrThrow()
        assertEquals("fr", FydeSdk.getLanguage().getOrThrow())

        FydeSdk.setLanguage("en").getOrThrow()
        assertEquals("en", FydeSdk.getLanguage().getOrThrow())
    }
}
