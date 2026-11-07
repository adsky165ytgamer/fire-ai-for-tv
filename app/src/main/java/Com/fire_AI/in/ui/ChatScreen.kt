package Com.fire_AI.`in`.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import Com.fire_AI.`in`.data.Author
import Com.fire_AI.`in`.data.ChatMessage
import Com.fire_AI.`in`.ui.components.MarkdownText
import Com.fire_AI.`in`.ui.components.TextEntry
import Com.fire_AI.`in`.ui.components.focusRing
import Com.fire_AI.`in`.ui.components.rememberVoiceInput
import kotlinx.coroutines.launch

private val PillShape = RoundedCornerShape(50)

/**
 * TV-first layout: a side rail for navigation and a wide conversation area.
 * Only the right-hand column resizes when a system keyboard opens (imePadding is applied once, there).
 */
@Composable
fun ChatScreen(
    state: ChatUiState,
    isTv: Boolean,
    useFireKeyboard: Boolean,
    autoSendVoice: Boolean,
    onSend: (String) -> Boolean,
    onStop: () -> Unit,
    onRegenerate: () -> Unit,
    onClear: () -> Unit,
    onNewChat: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var kbOpen by rememberSaveable { mutableStateOf(false) }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        SideRail(
            state = state,
            isTv = isTv,
            onNewChat = onNewChat,
            onOpenHistory = onOpenHistory,
            onOpenModels = onOpenModels,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.width(if (isTv) 270.dp else 200.dp).fillMaxHeight(),
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ConversationPane(
                state = state,
                keyboardOpen = kbOpen,
                onOpenSettings = onOpenSettings,
                onOpenModels = onOpenModels,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            if (!kbOpen && state.messages.isNotEmpty() && !state.isStreaming) {
                ControlsRow(state = state, onRegenerate = onRegenerate, onClear = onClear)
            }
            InputArea(
                isTv = isTv,
                useFireKeyboard = useFireKeyboard,
                autoSendVoice = autoSendVoice,
                isStreaming = state.isStreaming,
                text = text,
                onTextChange = { text = it },
                kbOpen = kbOpen,
                onKbChange = { kbOpen = it },
                onSend = onSend,
                onStop = onStop,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SideRail(
    state: ChatUiState,
    isTv: Boolean,
    onNewChat: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val h = if (isTv) 60.dp else 48.dp
    Surface(modifier = modifier, shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Fire AI", style = MaterialTheme.typography.headlineLargeEmphasized, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = when {
                    !state.hasKey -> "No API key"
                    state.model.isBlank() -> "No model selected"
                    else -> state.model
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (state.hasKey && state.model.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            RailButton("New Chat", h, onNewChat)
            RailButton("History", h, onOpenHistory)
            RailButton("Models", h, onOpenModels)
            RailButton("Settings", h, onOpenSettings)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RailButton(label: String, minHeight: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight).focusRing(PillShape),
    ) { Text(label) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ConversationPane(
    state: ChatUiState,
    keyboardOpen: Boolean,
    onOpenSettings: () -> Unit,
    onOpenModels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val shape = MaterialTheme.shapes.extraLarge
    val imeVisible = WindowInsets.isImeVisible

    val itemCount = state.messages.size + if (state.isStreaming) 1 else 0
    val lastIndex = itemCount - 1

    // Jump to the newest item when a message is added or streaming starts.
    LaunchedEffect(state.messages.size, state.isStreaming) {
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }
    // The pane shrinks when a keyboard opens; keep the latest message in view.
    LaunchedEffect(keyboardOpen, imeVisible) {
        if (lastIndex >= 0) listState.scrollToItem(lastIndex)
    }
    // Follow the stream, but only while the reader is already at the bottom.
    LaunchedEffect(state.streamingText.length / 60) {
        if (state.isStreaming && listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lastIndex) {
            listState.scrollBy(10_000f)
        }
    }

    Surface(
        modifier = modifier
            .onPreviewKeyEvent { e ->
                // D-pad up/down scrolls the conversation; at either end, focus moves on.
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionDown ->
                        if (listState.canScrollForward) {
                            scope.launch { listState.animateScrollBy(260f) }
                            true
                        } else false
                    Key.DirectionUp ->
                        if (listState.canScrollBackward) {
                            scope.launch { listState.animateScrollBy(-260f) }
                            true
                        } else false
                    else -> false
                }
            }
            .focusRing(shape)
            .focusable(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        if (itemCount == 0) {
            Column(
                Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    !state.hasKey -> {
                        Text("Add your Gemini API key to start.", style = MaterialTheme.typography.titleLargeEmphasized, color = MaterialTheme.colorScheme.onSurface)
                        Button(
                            onClick = onOpenSettings,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.heightIn(min = 56.dp).focusRing(PillShape),
                        ) { Text("Open Settings") }
                    }
                    state.model.isBlank() -> {
                        Text("Choose a model to start.", style = MaterialTheme.typography.titleLargeEmphasized, color = MaterialTheme.colorScheme.onSurface)
                        Button(
                            onClick = onOpenModels,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.heightIn(min = 56.dp).focusRing(PillShape),
                        ) { Text("Open Models") }
                    }
                    else -> Text("Ask anything.", style = MaterialTheme.typography.titleLargeEmphasized, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(state.messages, key = { it.id }) { MessageBubble(it) }
                if (state.isStreaming) item(key = "streaming") { StreamingBubble(state.streamingText) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ControlsRow(state: ChatUiState, onRegenerate: () -> Unit, onClear: () -> Unit) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val lastReply = state.messages.lastOrNull { it.author == Author.Model && !it.isError }?.text
    val hasUserMessage = state.messages.any { it.author == Author.User && !it.isError }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(
            onClick = { confirmClear = false; onRegenerate() },
            enabled = hasUserMessage,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.heightIn(min = 48.dp).focusRing(PillShape),
        ) { Text("Regenerate") }
        FilledTonalButton(
            onClick = {
                confirmClear = false
                if (lastReply != null) {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Fire AI reply", lastReply))
                    copied = true
                }
            },
            enabled = lastReply != null,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.heightIn(min = 48.dp).focusRing(PillShape),
        ) { Text(if (copied) "Copied" else "Copy reply") }
        FilledTonalButton(
            onClick = {
                if (confirmClear) { confirmClear = false; onClear() } else confirmClear = true
            },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.heightIn(min = 48.dp).focusRing(PillShape),
        ) { Text(if (confirmClear) "Select again to clear this chat" else "Clear chat") }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.author == Author.User
    val colors = MaterialTheme.colorScheme
    val container = when {
        message.isError -> colors.errorContainer
        isUser -> colors.primaryContainer
        else -> colors.surfaceContainerHigh
    }
    val content = when {
        message.isError -> colors.onErrorContainer
        isUser -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    val shape = if (isUser) RoundedCornerShape(28.dp, 28.dp, 8.dp, 28.dp) else RoundedCornerShape(28.dp, 28.dp, 28.dp, 8.dp)

    Box(Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(modifier = Modifier.widthIn(max = 860.dp), shape = shape, color = container) {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                if (isUser || message.isError) {
                    Text(message.text, style = MaterialTheme.typography.bodyLarge, color = content)
                } else {
                    MarkdownText(message.text)
                }
            }
        }
    }
}

/** While text is arriving it is shown as plain text (cheap); Markdown is applied once the reply is complete. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StreamingBubble(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            modifier = Modifier.widthIn(max = 860.dp),
            shape = RoundedCornerShape(28.dp, 28.dp, 28.dp, 8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            if (text.isEmpty()) {
                LoadingIndicator(modifier = Modifier.padding(16.dp).size(40.dp))
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InputArea(
    isTv: Boolean,
    useFireKeyboard: Boolean,
    autoSendVoice: Boolean,
    isStreaming: Boolean,
    text: String,
    onTextChange: (String) -> Unit,
    kbOpen: Boolean,
    onKbChange: (Boolean) -> Unit,
    onSend: (String) -> Boolean,
    onStop: () -> Unit,
) {
    val context = LocalContext.current

    fun submit() {
        val t = text.trim()
        if (t.isNotEmpty() && !isStreaming && onSend(t)) {
            onTextChange("")
            onKbChange(false)
        }
    }

    val voice = rememberVoiceInput { result ->
        val merged = (text + " " + result).trim()
        if (autoSendVoice && onSend(merged)) onTextChange("") else onTextChange(merged)
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voice.start() else voice.report("Microphone permission was denied. You can still type your message.")
    }
    val onMic: () -> Unit = {
        when {
            voice.listening -> voice.stop()
            !voice.available -> voice.report("Voice typing isn't available on this device. Use the keyboard instead.")
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> voice.start()
            else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val status = if (voice.listening) "Listening... ${voice.partial}".trim() else voice.message
        if (status != null) {
            Text(status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextEntry(
            value = text,
            onValueChange = onTextChange,
            placeholder = "Message",
            useFireKeyboard = useFireKeyboard,
            isTv = isTv,
            keyboardVisible = kbOpen,
            onKeyboardVisibleChange = onKbChange,
            onSubmit = ::submit,
            submitLabel = "Send",
            trailing = {
                FilledTonalButton(
                    onClick = onMic,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = if (isTv) 60.dp else 52.dp).focusRing(PillShape),
                ) { Text(if (voice.listening) "Stop mic" else "Mic") }
                Button(
                    onClick = { if (isStreaming) onStop() else submit() },
                    enabled = isStreaming || text.isNotBlank(),
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = if (isTv) 60.dp else 52.dp).widthIn(min = 96.dp).focusRing(PillShape),
                ) { Text(if (isStreaming) "Stop" else "Send") }
            },
        )
    }
}
