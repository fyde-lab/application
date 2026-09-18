package com.peltoche.fyde

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class FydeSdkJvmTest {
    @Test
    fun `initialize reaches the native fyde-sdk client and surfaces connection failures`() = runBlocking {
        // No server is listening on this port, so this exercises the real
        // JNI call into fyde-sdk (via :sdkBridge) end-to-end and expects it
        // to fail with a connection error, rather than a linkage error.
        val result = FydeSdk.initialize("http://127.0.0.1:1")

        assertTrue(result.isFailure, "expected a connection failure, got $result")
    }
}
