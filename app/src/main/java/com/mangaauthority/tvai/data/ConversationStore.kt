package com.mangaauthority.tvai.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Saves chats as small JSON files in app storage. Nothing is loaded until the user opens it. */
class ConversationStore(context: Context) {

    private val dir = File(context.applicationContext.filesDir, "conversations")
    private val mutex = Mutex()

    private fun file(id: String) = File(dir, "$id.json")
    private fun indexFile() = File(dir, "index.json")

    suspend fun save(c: Conversation) = withContext(Dispatchers.IO) {
        mutex.withLock {
            dir.mkdirs()
            val msgs = JSONArray()
            c.messages.takeLast(MAX_STORED_MESSAGES).forEach {
                msgs.put(JSONObject().put("a", if (it.author == Author.User) "u" else "m").put("t", it.text))
            }
            writeAtomic(
                file(c.id),
                JSONObject().put("id", c.id).put("title", c.title).put("updatedAt", c.updatedAt)
                    .put("messages", msgs).toString()
            )
            val index = (readIndex().filter { it.id != c.id } + ConversationSummary(c.id, c.title, c.updatedAt))
                .sortedByDescending { it.updatedAt }
            index.drop(MAX_CONVERSATIONS).forEach { file(it.id).delete() }
            writeIndex(index.take(MAX_CONVERSATIONS))
        }
    }

    suspend fun list(): List<ConversationSummary> = withContext(Dispatchers.IO) {
        mutex.withLock { readIndex().sortedByDescending { it.updatedAt } }
    }

    suspend fun load(id: String): Conversation? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val f = file(id)
                if (!f.exists()) return@withLock null
                val o = JSONObject(f.readText())
                val arr = o.getJSONArray("messages")
                val msgs = (0 until arr.length()).map { i ->
                    val m = arr.getJSONObject(i)
                    ChatMessage(0, if (m.getString("a") == "u") Author.User else Author.Model, m.getString("t"))
                }
                Conversation(id, o.optString("title"), o.optLong("updatedAt"), msgs)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            file(id).delete()
            writeIndex(readIndex().filter { it.id != id })
        }
    }

    private fun readIndex(): List<ConversationSummary> = try {
        val arr = JSONArray(indexFile().takeIf { it.exists() }?.readText() ?: "[]")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ConversationSummary(o.getString("id"), o.optString("title"), o.optLong("updatedAt"))
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun writeIndex(items: List<ConversationSummary>) {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("id", it.id).put("title", it.title).put("updatedAt", it.updatedAt)) }
        dir.mkdirs()
        writeAtomic(indexFile(), arr.toString())
    }

    private fun writeAtomic(target: File, text: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(text)
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    }

    private companion object {
        const val MAX_CONVERSATIONS = 30
        const val MAX_STORED_MESSAGES = 200
    }
}
