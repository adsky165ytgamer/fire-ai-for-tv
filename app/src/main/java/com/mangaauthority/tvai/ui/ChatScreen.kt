package com.mangaauthority.tvai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mangaauthority.tvai.data.Author
import com.mangaauthority.tvai.data.ChatMessage
import com.mangaauthority.tvai.ui.components.MarkdownText
import com.mangaauthority.tvai.ui.components.focusRing
import kotlinx.coroutines.launch

private val PillShape = RoundedCornerShape(50)

/**
 * TV-first layout: a side rail for actions and a wide conversation area.
 * Every control is reachable by touch, air-mouse, keyboard and D-pad.
 */
@Composable
fun ChatScreen(
    state: ChatUiState,
    isTv: Boolean,
    onSend: (String) -> Boolean,
    onStop: () -> Unit,
    onNewChat: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        SideRail(
            state = state,
            isTv = isTv,
            onNewChat = onNewChat,
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.width(if (isTv) 280.dp else 210.dp).fillMaxHeight(),
        )
        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ConversationPane(
                state = state,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            InputRow(isTv = isTv, isStreaming = state.isStreaming, onSend = onSend, onStop = onStop)
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
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonHeight = if (isTv) 64.dp else 52.dp
    Surface(modifier = modifier, shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("TV AI", style = MaterialTheme.typography.headlineLargeEmphasized, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = if (state.hasKey) state.model else "No API key",
                style = MaterialTheme.typography.labelLarge,
                color = if (state.hasKey) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            FilledTonalButton(
                onClick = onNewChat,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().heightIn(min = buttonHeight).focusRing(PillShape),
            ) { Text("New chat") }
            FilledTonalButton(
                onClick = onOpenHistory,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().heightIn(min = buttonHeight).focusRing(PillShape),
            ) { Text("History") }
            FilledTonalButton(
                onClick = onOpenSettings,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().heightIn(min = buttonHeight).focusRing(PillShape),
            ) { Text("Settings") }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConversationPane(state: ChatUiState, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val shape = MaterialTheme.shapes.extraLarge

    val itemCount = state.messages.size + if (state.isStreaming) 1 else 0
    val lastIndex = itemCount - 1

    // Jump to the newest item when a message is added or streaming starts.
    LaunchedEffect(state.messages.size, state.isStreaming) {
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
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
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.hasKey) {
                    Text(
                        "Ask anything. Select the message box to type.",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Add your Gemini API key to start chatting.",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Button(
                        onClick = onOpenSettings,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.heightIn(min = 56.dp).focusRing(PillShape),
                    ) { Text("Open Settings") }
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

@Composable
private fun InputRow(isTv: Boolean, isStreaming: Boolean, onSend: (String) -> Boolean, onStop: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val requester = remember { FocusRequester() }
    val fieldShape = MaterialTheme.shapes.extraLarge

    // On TV, start with the input focused so OK on the remote opens the keyboard.
    LaunchedEffect(Unit) {
        if (isTv) runCatching { requester.requestFocus() }
    }

    fun submit() {
        val t = text.trim()
        if (t.isNotEmpty() && !isStreaming && onSend(t)) text = ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { e ->
                    // Physical keyboard: Enter sends, Shift+Enter adds a new line.
                    if (e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.NumPadEnter) && !e.isShiftPressed) {
                        submit()
                        true
                    } else false
                }
                .focusRing(fieldShape)
                .focusRequester(requester),
            placeholder = { Text("Message") },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = fieldShape,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        )
        Button(
            onClick = { if (isStreaming) onStop() else submit() },
            enabled = isStreaming || text.isNotBlank(),
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.heightIn(min = if (isTv) 64.dp else 56.dp).widthIn(min = 104.dp).focusRing(PillShape),
        ) { Text(if (isStreaming) "Stop" else "Send") }
    }
}
