package Com.fire_AI.`in`.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import Com.fire_AI.`in`.data.FreeTier
import Com.fire_AI.`in`.data.GeminiClient
import Com.fire_AI.`in`.data.GeminiError
import Com.fire_AI.`in`.data.KeyVault
import Com.fire_AI.`in`.data.ModelInfo
import Com.fire_AI.`in`.data.SaveResult
import Com.fire_AI.`in`.data.SettingsStore
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
    val keyboardMode: String = "auto",
    val autoSendVoice: Boolean = false,
    /** Every model the API returned for this key (null until loaded). Eligibility is decided in the UI via FreeTier. */
    val models: List<ModelInfo>? = null,
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
            keyboardMode = settings.keyboardMode,
            autoSendVoice = settings.autoSendVoice,
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun saveKey(input: String) {
        if (input.isBlank()) {
            _state.update { it.copy(status = StatusLine("Enter your Gemini API key first.", true)) }
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { vault.save(input) }
            val text = when (result) {
                SaveResult.Encrypted -> "Key saved. It is stored encrypted on this device."
                SaveResult.SessionOnly ->
                    "This device's secure storage isn't working, so the key is kept in memory only. You'll need to enter it again after the app closes."
            }
            _state.update { it.copy(hasKey = true, models = null, status = StatusLine(text, false)) }
        }
    }

    fun clearKey() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { vault.clear() }
            _state.update { it.copy(hasKey = false, models = null, status = StatusLine("API key removed.", false)) }
        }
    }

    fun loadModelsIfNeeded() {
        val s = _state.value
        if (s.hasKey && s.models == null && !s.busy) testConnection()
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
                val free = models.count { FreeTier.isFree(it.id) }
                _state.update {
                    it.copy(
                        busy = false,
                        models = models,
                        status = StatusLine("Connected. Your key can see ${models.size} models, $free on the free tier list.", false),
                    )
                }
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

    fun setKeyboardMode(mode: String) {
        settings.keyboardMode = mode
        _state.update { it.copy(keyboardMode = mode) }
    }

    fun setAutoSendVoice(on: Boolean) {
        settings.autoSendVoice = on
        _state.update { it.copy(autoSendVoice = on) }
    }
}
