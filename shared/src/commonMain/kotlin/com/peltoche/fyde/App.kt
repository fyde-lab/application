package com.peltoche.fyde

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// TODO: replace with documents fetched from fyde-sdk once it exposes a way
// to list/watch received documents (see ../../CLAUDE.md: the server has no
// list RPC yet, so this will go through the changelog service).
private val sampleDocuments = listOf(
    Document("Receipt for Groceries", "50.37 KB", "8 months ago"),
    Document("Blackmail letter from Reginald Thornton-Fiennes", "31.56 KB", "8 months ago"),
    Document("Dr Watson letter - Missing heirloom", "31.31 KB", "8 months ago"),
)

private val FydeDarkColorScheme = darkColorScheme(
    background = Color(0xFF141414),
    surface = Color(0xFF141414),
    surfaceVariant = Color(0xFF242424),
    onBackground = Color(0xFFF2F2F2),
    onSurface = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF8A8A8E),
    outlineVariant = Color(0xFF2A2A2C),
    primary = PdfAccentColor,
)

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = FydeDarkColorScheme) {
        var sdkStatus by remember { mutableStateOf("Initializing fyde-sdk…") }
        var searchQuery by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            sdkStatus = FydeSdk.initialize("http://127.0.0.1:40051")
                .fold(
                    onSuccess = { "fyde-sdk connected" },
                    onFailure = { "fyde-sdk unavailable: ${it.message}" },
                )
        }
        val filteredDocuments = sampleDocuments.filter {
            it.title.contains(searchQuery, ignoreCase = true)
        }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            Text(
                text = sdkStatus,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(
                text = "Latest imported documents",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("File name", color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = MaterialTheme.colorScheme.onBackground,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(top = 4.dp),
            ) {
                items(filteredDocuments) { document ->
                    DocumentRow(document)
                }
            }
        }
    }
}
