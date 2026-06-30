package com.picpulse.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.picpulse.app.data.model.ProcessingState
import com.picpulse.app.ui.HomeScreen
import com.picpulse.app.ui.ProcessingScreen
import com.picpulse.app.ui.ResultsScreen
import com.picpulse.app.ui.theme.PicPulseTheme
import com.picpulse.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDark by remember { mutableStateOf(true) }
            PicPulseTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PicPulseApp(
                        isDark = isDark,
                        onToggleTheme = { isDark = !isDark }
                    )
                }
            }
        }
    }
}

@Composable
fun PicPulseApp(
    vm: MainViewModel = viewModel(),
    isDark: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val state by vm.state.collectAsStateWithLifecycle()

    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = "Toggle theme",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        val folderPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                pendingUri = uri
                vm.startProcessing(uri)
            }
        }

        when (val s = state) {
            is ProcessingState.Idle -> {
                HomeScreen(
                    vm = vm,
                    onSelectFolder = { folderPicker.launch(null) }
                )
            }
            is ProcessingState.Scanning,
            is ProcessingState.Processing -> {
                ProcessingScreen(
                    state = s,
                    onBack = { vm.reset() }
                )
            }
            is ProcessingState.Complete -> {
                ResultsScreen(
                    state = s,
                    onBack = { vm.reset() }
                )
            }
            is ProcessingState.Error -> {
                ProcessingScreen(
                    state = s,
                    onBack = { vm.reset() }
                )
            }
        }
    }
}
