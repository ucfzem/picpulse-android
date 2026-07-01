package com.picpulse.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.picpulse.app.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onSelectFolder: () -> Unit
) {
    val downloadProgress by vm.modelDownloadProgress.collectAsState()
    var modelLoaded by remember { mutableStateOf(vm.loadModel()) }

    LaunchedEffect(Unit) {
        modelLoaded = vm.loadModel()
    }

    LaunchedEffect(downloadProgress) {
        if (downloadProgress == 100) {
            modelLoaded = vm.loadModel()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PicPulse",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "AI Image Analyzer & Renamer",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        if (downloadProgress in 0..99) {
            LinearProgressIndicator(
                progress = downloadProgress / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("Downloading AI model... $downloadProgress%")
        } else if (modelLoaded) {
            Text(
                "Model loaded",
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Button(onClick = { vm.downloadModel() }) {
                Text("Download AI Model")
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Add flare tag")
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = vm.addFlareTag,
                onCheckedChange = { vm.addFlareTag = it }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Use OCR (text detection)")
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = vm.useOcr,
                onCheckedChange = { vm.useOcr = it }
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSelectFolder,
            enabled = modelLoaded
        ) {
            Text("Select Folder & Process")
        }
    }
}
