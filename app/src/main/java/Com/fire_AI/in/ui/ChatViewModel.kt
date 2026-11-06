package Com.fire_AI.`in`.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import Com.fire_AI.`in`.data.Author
import Com.fire_AI.`in`.data.ChatMessage
import Com.fire_AI.`in`.data.Conversation
import Com.fire_AI.`in`.data.ConversationStore
import Com.fire_AI.`in`.data.ConversationSummary
import Com.fire_AI.`in`.data.GeminiClient
import Com.fire_AI.`in`.data.GeminiError
import Com.fire_AI.`in`.data.KeyVault
import Com.fire_AI.`in`.data.SettingsStore
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    /** Text of the reply that is still arriving. Kept apart so only one list item changes per update. */
    val streamingText: String = "",
    val isStreaming: Boolean = false,
    val hasKey: Boolean = false,
    val model: String = "",
    val history: List<ConversationSummary> = emptyList(),
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val client = GeminiClient()
    private val vault = KeyVault(app)
    private val settings = SettingsStore(app)
    private val store = ConversationStore(app)

    private val ids = AtomicLong(0)
    private var conversationId = UUID.randomUUID().toString()
    private var streamJob: Job? = null
    private var epoch = 0

    private val _state = MutableStateFlow(ChatUiState(hasKey = vault.hasKey(), model = settings.model))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    /** Call when returning from Settings. */
    fun refreshSettings() {
        _state.update { it.copy(hasKey = vault.hasKey(), model = settings.model) }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            val list = runCatching { store.list() }.getOrDefault(emptyList())
            _state.update { it.copy(history = list) }
        }
    }

    /** Returns true when the message was accepted, so the UI can clear its input box. */
    fun send(text: String): Boolean {
        val prompt = text.trim()
        if (prompt.isEmpty() || _state.value.isStreaming) return false
        if (!vault.hasKey()) {
            _state.update { it.copy(hasKey = false, messages = (it.messages + msg(Author.Model, GeminiError.MissingKey().message.orEmpty(), true)).takeLast(MAX_MESSAGES)) }
            return false
        }

        val userMsg = msg(Author.User, prompt)
        val requestMessages = (_state.value.messages.filter { !it.isError } + userMsg).takeLast(HISTORY_MESSAGES)
        val myEpoch = epoch
        _state.update {
            it.copy(
                messages = (it.messages + userMsg).takeLast(MAX_MESSAGES),
                streamingText = "",
                isStreaming = true,
                hasKey = true,
                model = settings.model,
            )
        }

        streamJob = viewModelScope.launch {
            val sb = StringBuilder()
            var lastEmit = 0L
            try {
                val key = withContext(Dispatchers.IO) { vault.load() } ?: throw GeminiError.MissingKey()
                val model = settings.model
                val temperature = settings.temperature
                val replies = if (settings.streaming) {
                    client.streamReply(key, model, requestMessages, temperature)
                } else {
                    flow { emit(client.generateReply(key, model, requestMessages, temperature)) }
                }
                replies.collect { chunk ->
                    sb.append(chunk)
                    // Publish at most ~20 times a second so weak GPUs are not flooded.
                    val now = SystemClock.uptimeMillis()
                    if (now - lastEmit >= EMIT_INTERVAL_MS) {
                        lastEmit = now
                        val snapshot = sb.toString()
                        _state.update { it.copy(streamingText = snapshot) }
                    }
                }
                commit(sb.toString(), null, myEpoch)
            } catch (e: CancellationException) {
                commit(sb.toString(), null, myEpoch) // keep whatever arrived before Stop
                throw e
            } catch (e: GeminiError) {
                commit(sb.toString(), e.message, myEpoch)
            } catch (e: Exception) {
                commit(sb.toString(), GeminiError.Other("").message, myEpoch)
            }
        }
        return true
    }

    fun stop() {
        streamJob?.cancel()
    }

    fun newChat() {
        epoch++
        streamJob?.cancel()
        streamJob = null
        conversationId = UUID.randomUUID().toString()
        _state.update { it.copy(messages = emptyList(), streamingText = "", isStreaming = false) }
    }

    fun openConversation(id: String) {
        viewModelScope.launch {
            val c = store.load(id) ?: return@launch
            epoch++
            streamJob?.cancel()
            streamJob = null
            conversationId = c.id
            _state.update {
                it.copy(
                    messages = c.messages.map { m -> m.copy(id = ids.incrementAndGet()) }.takeLast(MAX_MESSAGES),
                    streamingText = "",
                    isStreaming = false,
                )
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            runCatching { store.delete(id) }
            if (id == conversationId) newChat()
            refreshHistory()
        }
    }

    private fun commit(replyRaw: String, errorText: String?, myEpoch: Int) {
        if (myEpoch != epoch) return
        val reply = replyRaw.trim()
        _state.update { s ->
            var list = s.messages
            if (reply.isNotEmpty()) list = list + msg(Author.Model, reply)
            if (errorText != null) list = list + msg(Author.Model, errorText, isError = true)
            s.copy(messages = list.takeLast(MAX_MESSAGES), streamingText = "", isStreaming = false)
        }
        persist()
    }

    private fun persist() {
        val saved = _state.value.messages.filter { !it.isError }
        val first = saved.firstOrNull { it.author == Author.User } ?: return
        val conv = Conversation(conversationId, first.text.lineSequence().first().take(60), System.currentTimeMillis(), saved)
        viewModelScope.launch { runCatching { store.save(conv) } }
    }

    private fun msg(author: Author, text: String, isError: Boolean = false) =
        ChatMessage(ids.incrementAndGet(), author, text, isError)

    private companion object {
        const val MAX_MESSAGES = 60     // hard cap on what the UI keeps in memory
        const val HISTORY_MESSAGES = 20 // context sent to Gemini with each prompt
        const val EMIT_INTERVAL_MS = 50L
    }
}
