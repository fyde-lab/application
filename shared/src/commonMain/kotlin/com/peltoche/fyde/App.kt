package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        var sdkStatus by remember { mutableStateOf("Initializing fyde-sdk…") }
        LaunchedEffect(Unit) {
            sdkStatus = FydeSdk.initialize("http://127.0.0.1:40051")
                .fold(
                    onSuccess = { "fyde-sdk connected" },
                    onFailure = { "fyde-sdk unavailable: ${it.message}" },
                )
        }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(sdkStatus)
        }
    }
}