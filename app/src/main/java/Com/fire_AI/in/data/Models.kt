package Com.fire_AI.`in`.data

enum class Author { User, Model }

data class ChatMessage(
    val id: Long,
    val author: Author,
    val text: String,
    val isError: Boolean = false,
)

data class Conversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<ChatMessage>,
)

data class ConversationSummary(val id: String, val title: String, val updatedAt: Long)

data class ModelInfo(
    val id: String,
    val displayName: String,
    val inputTokenLimit: Int = 0,
    val outputTokenLimit: Int = 0,
    val thinking: Boolean = false,
)
