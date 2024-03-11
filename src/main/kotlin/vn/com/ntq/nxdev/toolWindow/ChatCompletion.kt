package vn.com.ntq.nxdev.toolWindow

import com.google.gson.annotations.JsonAdapter

data class Data(
        val data: ChatCompletion
)
data class ChatCompletion(
        val id: String,
        val objectName: String,
        val created: Int,
        val model: String,
        val choices: List<Choice>
)
data class Choice(
        val index: Int,
        val delta: Delta,
        val finishReason: String?
)

data class Delta(
        val role: String,
        val content: String
)