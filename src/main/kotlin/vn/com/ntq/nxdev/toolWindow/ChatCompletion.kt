package vn.com.ntq.nxdev.toolWindow

data class ChatCompletion(
        val id: String,
        val objectName: String,
        val created: Int,
        val model: String,
        val choices: List<Choice>,
        val usage: Usage
)

data class Choice(
        val index: Int,
        val message: Message,
        val finishReason: String?
)

data class Message(
        val role: String,
        val content: String
)

data class Usage(
        val promptTokens: Int,
        val completionTokens: Int,
        val totalTokens: Int
)
