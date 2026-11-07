package Com.fire_AI.`in`.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import Com.fire_AI.`in`.ui.components.LocalShowFocus

enum class Screen { Chat, History, Models, Settings }

@Composable
fun FireAiApp(chatVm: ChatViewModel, settingsVm: SettingsViewModel, isTv: Boolean) {
    var screen by rememberSaveable { mutableStateOf(Screen.Chat) }
    val chat by chatVm.state.collectAsStateWithLifecycle()
    val settings by settingsVm.state.collectAsStateWithLifecycle()

    // Focus outlines are for remotes and keyboards: hidden after a touch or mouse press, shown again on the next key press.
    var showFocus by remember { mutableStateOf(isTv) }

    val useFireKeyboard = when (settings.keyboardMode) {
        "fire" -> true
        "system" -> false
        else -> isTv
    }

    // Back order: keyboard (handled inside TextEntry) -> rename panel -> sub-screen -> chat -> system.
    BackHandler(enabled = screen != Screen.Chat) { screen = Screen.Chat }

    LaunchedEffect(screen) {
        when (screen) {
            Screen.Chat -> chatVm.refreshSettings()
            Screen.History -> chatVm.refreshHistory()
            Screen.Models -> settingsVm.loadModelsIfNeeded()
            Screen.Settings -> Unit
        }
    }

    CompositionLocalProvider(LocalShowFocus provides showFocus) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { showFocus = true; false }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val e = awaitPointerEvent(PointerEventPass.Initial)
                                if (e.changes.any { it.pressed }) showFocus = false
                            }
                        }
                    }
                    // Safe areas without the keyboard: the keyboard inset is applied once, on the chat column only.
                    .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
                    .padding(if (isTv) 40.dp else 16.dp),
            ) {
                val content = Modifier.fillMaxSize()
                when (screen) {
                    Screen.Chat -> ChatScreen(
                        state = chat,
                        isTv = isTv,
                        useFireKeyboard = useFireKeyboard,
                        autoSendVoice = settings.autoSendVoice,
                        onSend = chatVm::send,
                        onStop = chatVm::stop,
                        onRegenerate = chatVm::regenerate,
                        onClear = chatVm::clearConversation,
                        onNewChat = chatVm::newChat,
                        onOpenHistory = { screen = Screen.History },
                        onOpenModels = { screen = Screen.Models },
                        onOpenSettings = { screen = Screen.Settings },
                        modifier = content,
                    )
                    Screen.History -> HistoryScreen(
                        items = chat.history,
                        isTv = isTv,
                        useFireKeyboard = useFireKeyboard,
                        onBack = { screen = Screen.Chat },
                        onOpen = { id ->
                            chatVm.openConversation(id)
                            screen = Screen.Chat
                        },
                        onRename = chatVm::renameConversation,
                        onDelete = chatVm::deleteConversation,
                        modifier = content,
                    )
                    Screen.Models -> ModelsScreen(
                        state = settings,
                        onBack = { screen = Screen.Chat },
                        onRefresh = settingsVm::testConnection,
                        onSelect = settingsVm::selectModel,
                        onOpenSettings = { screen = Screen.Settings },
                        modifier = content,
                    )
                    Screen.Settings -> SettingsScreen(
                        state = settings,
                        isTv = isTv,
                        useFireKeyboard = useFireKeyboard,
                        onBack = { screen = Screen.Chat },
                        onSaveKey = settingsVm::saveKey,
                        onClearKey = settingsVm::clearKey,
                        onTest = settingsVm::testConnection,
                        onKeyboardMode = settingsVm::setKeyboardMode,
                        onAutoSendVoice = settingsVm::setAutoSendVoice,
                        onTemperature = settingsVm::changeTemperature,
                        onStreaming = settingsVm::setStreaming,
                        modifier = content,
                    )
                }
            }
        }
    }
}
