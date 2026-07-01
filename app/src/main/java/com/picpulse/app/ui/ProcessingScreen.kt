package com.picpulse.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.picpulse.app.data.model.ProcessingState

@Composable
fun ProcessingScreen(
    state: ProcessingState,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Processing",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(24.dp))

        when (val s = state) {
            is ProcessingState.Scanning -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                if (s.discovered > 0) {
                    Text("Scanning: ${s.discovered} images found")
                } else {
                    Text("Scanning folder...")
                }
            }
            is ProcessingState.Processing -> {
                LinearProgressIndicator(
                    progress = s.current.toFloat() / s.total,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("${s.current} / ${s.total}")
                Spacer(Modifier.height(4.dp))
                Text(s.fileName, style = MaterialTheme.typography.bodyMedium)
                if (s.keyword.isNotEmpty()) {
                    Text("Keyword: ${s.keyword}")
                }
                if (s.confidence > 0) {
                    Text("Confidence: ${"%.1f".format(s.confidence * 100)}%")
                }
                if (s.hasFlare) {
                    Text("Lens flare detected", color = MaterialTheme.colorScheme.error)
                }
            }
            is ProcessingState.Complete -> {}
            is ProcessingState.Error -> {}
            is ProcessingState.Idle -> {}
        }

        if (state is ProcessingState.Complete || state is ProcessingState.Error) {
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }
}
