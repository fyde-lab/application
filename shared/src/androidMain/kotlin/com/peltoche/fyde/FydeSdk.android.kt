package com.peltoche.fyde

import uniffi.fyde_sdk.FydeClient

actual object FydeSdk {
    private var client: FydeClient? = null

    actual suspend fun initialize(serverUrl: String): Result<Unit> = runCatching {
        client = FydeClient.connect(serverUrl)
    }
}
