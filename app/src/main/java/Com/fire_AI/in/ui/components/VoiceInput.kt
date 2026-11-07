package Com.fire_AI.`in`.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Voice typing through Android's SpeechRecognizer. The recognizer exists only while listening
 * and is destroyed afterwards, so nothing keeps the microphone open.
 * Recognition depends on a speech service being installed (Google's app on most TVs; often missing on AOSP boxes).
 */
class VoiceInput(private val context: Context, private val onFinal: (String) -> Unit) {

    var listening by mutableStateOf(false)
        private set
    var partial by mutableStateOf("")
        private set
    var message by mutableStateOf<String?>(null)
        private set

    private var recognizer: SpeechRecognizer? = null

    val available: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun report(text: String) { message = text }

    fun start() {
        message = null
        partial = ""
        if (!available) {
            message = "Voice typing isn't available on this device. Use the keyboard instead."
            return
        }
        release()
        try {
            val r = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = r
            r.setRecognitionListener(listener)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            listening = true
            r.startListening(intent)
        } catch (e: Exception) {
            release()
            message = "Couldn't start voice recognition on this device."
        }
    }

    /** Ends recording; the recognizer still delivers what it heard so far. */
    fun stop() {
        recognizer?.stopListening()
    }

    fun release() {
        recognizer?.let {
            it.setRecognitionListener(null)
            it.destroy()
        }
        recognizer = null
        listening = false
        partial = ""
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        }

        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
            release()
            if (text.isEmpty()) message = "Didn't catch that. Try again." else onFinal(text)
        }

        override fun onError(error: Int) {
            release()
            message = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Try again."
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
                    "Voice recognition needs an internet connection."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for voice typing."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognition is busy. Try again in a moment."
                SpeechRecognizer.ERROR_AUDIO -> "Couldn't use the microphone."
                else -> "Voice recognition failed. Try again or use the keyboard."
            }
        }
    }
}

@Composable
fun rememberVoiceInput(onFinal: (String) -> Unit): VoiceInput {
    val context = LocalContext.current.applicationContext
    val latest by rememberUpdatedState(onFinal)
    val voice = remember { VoiceInput(context) { latest(it) } }
    DisposableEffect(voice) { onDispose { voice.release() } }
    return voice
}
