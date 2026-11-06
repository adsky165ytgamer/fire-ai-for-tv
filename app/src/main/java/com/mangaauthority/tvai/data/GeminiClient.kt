package com.mangaauthority.tvai.data

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** User-facing failures. The message is safe to show on screen as is. */
sealed class GeminiError(message: String) : Exception(message) {
    class MissingKey : GeminiError("No API key is set. Open Settings and add your Gemini API key.")
    class InvalidKey : GeminiError("Gemini rejected the API key. Check the key in Settings, make sure it is enabled in Google AI Studio, then save it again.")
    class Network : GeminiError("Can't reach Gemini. Check the internet connection and try again.")
    class Timeout : GeminiError("The request timed out. Try again in a moment.")
    class Quota : GeminiError("Quota or rate limit reached for this API key. Wait a bit, or check your usage and billing in Google AI Studio. You can also pick a different model in Settings.")
    class Server : GeminiError("Gemini had a server problem. Try again shortly.")
    class UnsupportedModel(model: String) : GeminiError("The model \"$model\" isn't available for this key. Open Settings and choose another model.")
    class Malformed : GeminiError("Gemini sent a reply the app couldn't read.")
    class Empty : GeminiError("Gemini returned an empty reply. Try rephrasing your message.")
    class Blocked(reason: String) : GeminiError("Gemini declined to answer ($reason). Try rephrasing your message.")
    class Other(detail: String) : GeminiError(if (detail.isBlank()) "Something went wrong. Try again." else "Request failed: $detail")
}

/**
 * Talks to the Gemini Developer API (REST, v1beta). The API key goes in the
 * x-goog-api-key header, never in the URL, and is never logged.
 */
class GeminiClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // max silence between streamed chunks
        .build(),
) {

    /** Real server-sent-event streaming: each emitted String is newly arrived text. */
    fun streamReply(apiKey: String, model: String, history: List<ChatMessage>, temperature: Float): Flow<String> = flow {
        val request = post("$BASE/models/$model:streamGenerateContent?alt=sse", apiKey, buildBody(history, temperature))
        val call = http.newCall(request)
        val handle = currentCoroutineContext().job.invokeOnCompletion { call.cancel() }
        try {
            val response = (try { call.execute() } catch (e: IOException) { throw mapIo(e, call) })
            response.use { r ->
                if (!r.isSuccessful) throw httpError(r.code, r.body?.string(), model)
                val source = r.body?.source() ?: throw GeminiError.Malformed()
                var gotText = false
                var finish: String? = null
                while (true) {
                    val line = (try { source.readUtf8Line() } catch (e: IOException) { throw mapIo(e, call) }) ?: break
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isEmpty() || payload == "[DONE]") continue
                    val chunk = parseChunk(payload, model)
                    if (chunk.finish != null) finish = chunk.finish
                    if (chunk.text.isNotEmpty()) {
                        gotText = true
                        emit(chunk.text)
                    }
                }
                if (!gotText) {
                    if (finish != null && finish != "STOP" && finish != "MAX_TOKENS") throw GeminiError.Blocked(finish)
                    throw GeminiError.Empty()
                }
            }
        } finally {
            handle.dispose()
        }
    }.flowOn(Dispatchers.IO)

    /** Non-streaming fallback (used when streaming is switched off in Settings). */
    suspend fun generateReply(apiKey: String, model: String, history: List<ChatMessage>, temperature: Float): String {
        val request = post("$BASE/models/$model:generateContent", apiKey, buildBody(history, temperature))
        return blocking(request, model) { r ->
            val body = r.body?.string() ?: throw GeminiError.Malformed()
            val chunk = parseChunk(body, model)
            if (chunk.text.isEmpty()) {
                val f = chunk.finish
                if (f != null && f != "STOP" && f != "MAX_TOKENS") throw GeminiError.Blocked(f)
                throw GeminiError.Empty()
            }
            chunk.text
        }
    }

    /** Lists models that support generateContent. Also works as a cheap key check. */
    suspend fun listModels(apiKey: String): List<ModelInfo> {
        val request = Request.Builder()
            .url("$BASE/models?pageSize=1000")
            .header("x-goog-api-key", apiKey)
            .get()
            .build()
        return blocking(request, null) { r ->
            val root = try { JSONObject(r.body?.string().orEmpty()) } catch (e: JSONException) { throw GeminiError.Malformed() }
            val arr = root.optJSONArray("models") ?: JSONArray()
            buildList {
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i) ?: continue
                    val id = m.optString("name").removePrefix("models/")
                    if (!id.startsWith("gemini")) continue
                    val methods = m.optJSONArray("supportedGenerationMethods") ?: continue
                    var ok = false
                    for (j in 0 until methods.length()) if (methods.optString(j) == "generateContent") ok = true
                    if (!ok) continue
                    add(ModelInfo(id, m.optString("displayName").ifBlank { id }))
                }
            }.sortedBy { it.id }
        }
    }

    // ---- internals ----

    private suspend fun <T> blocking(request: Request, model: String?, parse: (Response) -> T): T =
        withContext(Dispatchers.IO) {
            val call = http.newCall(request)
            val handle = currentCoroutineContext().job.invokeOnCompletion { call.cancel() }
            try {
                val response = (try { call.execute() } catch (e: IOException) { throw mapIo(e, call) })
                response.use { r ->
                    if (!r.isSuccessful) throw httpError(r.code, r.body?.string(), model)
                    parse(r)
                }
            } finally {
                handle.dispose()
            }
        }

    private fun post(url: String, apiKey: String, json: String): Request =
        Request.Builder()
            .url(url)
            .header("x-goog-api-key", apiKey)
            .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

    /** Gemini wants roles user/model; merge neighbours with the same role and start with a user turn. */
    private fun buildBody(history: List<ChatMessage>, temperature: Float): String {
        val turns = ArrayList<Pair<String, String>>()
        for (m in history) {
            val role = if (m.author == Author.User) "user" else "model"
            val last = turns.lastOrNull()
            if (last != null && last.first == role) turns[turns.size - 1] = role to (last.second + "\n\n" + m.text)
            else turns.add(role to m.text)
        }
        while (turns.isNotEmpty() && turns.first().first != "user") turns.removeAt(0)
        val contents = JSONArray()
        for ((role, text) in turns) {
            contents.put(
                JSONObject().put("role", role)
                    .put("parts", JSONArray().put(JSONObject().put("text", text)))
            )
        }
        return JSONObject()
            .put("contents", contents)
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))
            .put("generationConfig", JSONObject().put("temperature", temperature.toDouble()))
            .toString()
    }

    private class Chunk(val text: String, val finish: String?)

    private fun parseChunk(json: String, model: String): Chunk {
        val root = try { JSONObject(json) } catch (e: JSONException) { throw GeminiError.Malformed() }
        root.optJSONObject("error")?.let { err ->
            throw apiError(err.optInt("code"), err.optString("status"), err.optString("message"), model)
        }
        val block = root.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
        if (block.isNotEmpty()) throw GeminiError.Blocked(block)
        val cand = root.optJSONArray("candidates")?.optJSONObject(0) ?: return Chunk("", null)
        val finish = cand.optString("finishReason").ifEmpty { null }
        val parts = cand.optJSONObject("content")?.optJSONArray("parts") ?: return Chunk("", finish)
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val p = parts.optJSONObject(i) ?: continue
            if (p.optBoolean("thought")) continue
            sb.append(p.optString("text", ""))
        }
        return Chunk(sb.toString(), finish)
    }

    private fun httpError(code: Int, body: String?, model: String?): GeminiError {
        val err = try { JSONObject(body.orEmpty()).optJSONObject("error") } catch (e: JSONException) { null }
        return apiError(code, err?.optString("status").orEmpty(), err?.optString("message").orEmpty(), model)
    }

    private fun apiError(code: Int, status: String, message: String, model: String?): GeminiError = when {
        code == 429 || status == "RESOURCE_EXHAUSTED" -> GeminiError.Quota()
        code == 401 || code == 403 || status == "UNAUTHENTICATED" || status == "PERMISSION_DENIED" ||
            message.contains("API key not valid", ignoreCase = true) ||
            message.contains("API_KEY_INVALID", ignoreCase = true) -> GeminiError.InvalidKey()
        code == 404 || status == "NOT_FOUND" ->
            if (model != null) GeminiError.UnsupportedModel(model) else GeminiError.Other(message.take(160))
        code >= 500 || status == "INTERNAL" || status == "UNAVAILABLE" || status == "DEADLINE_EXCEEDED" -> GeminiError.Server()
        else -> GeminiError.Other(message.take(160))
    }

    private fun mapIo(e: IOException, call: Call): Throwable = when {
        call.isCanceled() -> CancellationException("Request cancelled")
        e is SocketTimeoutException -> GeminiError.Timeout()
        else -> GeminiError.Network()
    }

    companion object {
        const val BASE = "https://generativelanguage.googleapis.com/v1beta"
        const val SYSTEM_PROMPT =
            "You are a helpful assistant shown on a TV or phone screen. Keep answers clear and compact. " +
                "You may use Markdown (headings, lists, bold, code blocks, simple tables)."
    }
}
