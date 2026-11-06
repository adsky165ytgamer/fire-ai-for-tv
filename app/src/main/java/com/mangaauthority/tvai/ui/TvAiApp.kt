package com.mangaauthority.tvai.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

enum class Screen { Chat, History, Settings }

@Composable
fun TvAiApp(chatVm: ChatViewModel, settingsVm: SettingsViewModel, isTv: Boolean) {
    var screen by rememberSaveable { mutableStateOf(Screen.Chat) }
    val chat by chatVm.state.collectAsStateWithLifecycle()
    val settings by settingsVm.state.collectAsStateWithLifecycle()

    // Back returns to the chat first; on the chat screen it keeps its normal system behaviour.
    BackHandler(enabled = screen != Screen.Chat) { screen = Screen.Chat }

    LaunchedEffect(screen) {
        when (screen) {
            Screen.Chat -> chatVm.refreshSettings()
            Screen.History -> chatVm.refreshHistory()
            Screen.Settings -> Unit
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        // Extra margin on TV keeps content inside the overscan-safe area.
        val content = Modifier.fillMaxSize().padding(if (isTv) 40.dp else 16.dp)
        when (screen) {
            Screen.Chat -> ChatScreen(
                state = chat,
                isTv = isTv,
                onSend = chatVm::send,
                onStop = chatVm::stop,
                onNewChat = chatVm::newChat,
                onOpenHistory = { screen = Screen.History },
                onOpenSettings = { screen = Screen.Settings },
                modifier = content,
            )
            Screen.History -> HistoryScreen(
                items = chat.history,
                onBack = { screen = Screen.Chat },
                onOpen = { id ->
                    chatVm.openConversation(id)
                    screen = Screen.Chat
                },
                onDelete = chatVm::deleteConversation,
                modifier = content,
            )
            Screen.Settings -> SettingsScreen(
                state = settings,
                onBack = { screen = Screen.Chat },
                onSaveKey = settingsVm::saveKey,
                onClearKey = settingsVm::clearKey,
                onTest = settingsVm::testConnection,
                onSelectModel = settingsVm::selectModel,
                onTemperature = settingsVm::changeTemperature,
                onStreaming = settingsVm::setStreaming,
                modifier = content,
            )
        }
    }
}
