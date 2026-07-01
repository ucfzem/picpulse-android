package com.picpulse.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.picpulse.app.data.model.ImageResult
import com.picpulse.app.data.model.ProcessingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    state: ProcessingState.Complete,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Card(modifier = Modifier.padding(16.dp)) {
                Text("Total: ${state.total} | Renamed: ${state.renamed} | Flare: ${state.flared} | Errors: ${state.errors}")
            }

            LazyColumn {
                items(state.results) { result ->
                    ResultCard(result)
                }
            }
        }
    }
}

@Composable
private fun ResultCard(result: ImageResult) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(result.originalName)
        if (result.renamed) {
            Text("→ ${result.newName}")
        }
        if (result.hasFlare) {
            Text("Flare: ${"%.1f".format(result.flareScore * 100)}%")
        }
        if (result.error != null) {
            Text("Error: ${result.error}")
        }
    }
}
