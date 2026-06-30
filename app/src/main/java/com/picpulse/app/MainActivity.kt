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
            PicPulseTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PicPulseApp()
                }
            }
        }
    }
}

@Composable
fun PicPulseApp(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    var pendingUri by remember { mutableStateOf<Uri?>(null) }

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
