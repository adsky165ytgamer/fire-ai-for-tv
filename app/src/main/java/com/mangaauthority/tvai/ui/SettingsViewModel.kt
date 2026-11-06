package com.mangaauthority.tvai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangaauthority.tvai.data.GeminiClient
import com.mangaauthority.tvai.data.GeminiError
import com.mangaauthority.tvai.data.KeyVault
import com.mangaauthority.tvai.data.ModelInfo
import com.mangaauthority.tvai.data.SaveResult
import com.mangaauthority.tvai.data.SettingsStore
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StatusLine(val text: String, val isError: Boolean)

data class SettingsUiState(
    val hasKey: Boolean = false,
    val model: String = "",
    val temperature: Float = 0.7f,
    val streaming: Boolean = true,
    val models: List<ModelInfo> = emptyList(),
    val busy: Boolean = false,
    val status: StatusLine? = null,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val vault = KeyVault(app)
    private val settings = SettingsStore(app)
    private val client = GeminiClient()

    private val _state = MutableStateFlow(
        SettingsUiState(
            hasKey = vault.hasKey(),
            model = settings.model,
            temperature = settings.temperature,
            streaming = settings.streaming,
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun saveKey(input: String) {
        if (input.isBlank()) {
            _state.update { it.copy(status = StatusLine("Paste your Gemini API key first.", true)) }
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { vault.save(input) }
            val text = when (result) {
                SaveResult.Encrypted -> "Key saved. It is stored encrypted on this device."
                SaveResult.SessionOnly ->
                    "This device's secure storage isn't working, so the key is kept in memory only. You'll need to enter it again after the app closes."
            }
            _state.update { it.copy(hasKey = true, status = StatusLine(text, false)) }
        }
    }

    fun clearKey() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { vault.clear() }
            _state.update { it.copy(hasKey = false, models = emptyList(), status = StatusLine("API key removed.", false)) }
        }
    }

    /** Fetches the model list from the API. A successful call proves the key works. */
    fun testConnection() {
        viewModelScope.launch {
            val key = withContext(Dispatchers.IO) { vault.load() }
            if (key == null) {
                _state.update { it.copy(status = StatusLine(GeminiError.MissingKey().message.orEmpty(), true)) }
                return@launch
            }
            _state.update { it.copy(busy = true, status = StatusLine("Contacting Gemini...", false)) }
            try {
                val models = client.listModels(key)
                val selected = _state.value.model
                val text = when {
                    models.isEmpty() -> "Connected, but Gemini returned no usable models."
                    models.any { it.id == selected } -> "Connected. ${models.size} models available."
                    else -> "Connected, but \"$selected\" isn't in your model list. Choose another model below."
                }
                _state.update { it.copy(busy = false, models = models, status = StatusLine(text, false)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GeminiError) {
                _state.update { it.copy(busy = false, status = StatusLine(e.message.orEmpty(), true)) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, status = StatusLine(GeminiError.Other("").message.orEmpty(), true)) }
            }
        }
    }

    fun selectModel(id: String) {
        settings.model = id
        _state.update { it.copy(model = id, status = StatusLine("Model set to $id.", false)) }
    }

    fun changeTemperature(delta: Float) {
        val t = (((_state.value.temperature + delta) * 10).roundToInt() / 10f).coerceIn(0f, 2f)
        settings.temperature = t
        _state.update { it.copy(temperature = t) }
    }

    fun setStreaming(on: Boolean) {
        settings.streaming = on
        _state.update { it.copy(streaming = on) }
    }
}
