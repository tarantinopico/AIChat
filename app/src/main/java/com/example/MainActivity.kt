package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.VertexAiTheme

enum class AppScreen {
    CHAT,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VertexAiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VertexAiApp()
                }
            }
        }
    }
}

@Composable
fun VertexAiApp(viewModel: ChatViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }

    when (currentScreen) {
        AppScreen.CHAT -> {
            ChatScreen(
                viewModel = viewModel,
                onNavigateToSettings = { currentScreen = AppScreen.SETTINGS }
            )
        }
        AppScreen.SETTINGS -> {
            BackHandler {
                currentScreen = AppScreen.CHAT
            }
            SettingsScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.CHAT }
            )
        }
    }
}
