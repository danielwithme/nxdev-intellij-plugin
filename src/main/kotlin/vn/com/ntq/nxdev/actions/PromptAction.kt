package vn.com.ntq.nxdev.actions

import com.google.gson.Gson
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import org.markdown4j.Markdown4jProcessor
import vn.com.ntq.nxdev.settings.MyPluginSettings
import vn.com.ntq.nxdev.toolWindow.NxDevWindowFactory
import java.net.URL
import java.time.Instant
import java.util.*
import javax.swing.SwingUtilities

open class PromptAction : AnAction() {

    val processor = Markdown4jProcessor()
    override fun actionPerformed(e: AnActionEvent) {
        GlobalScope.launch(Dispatchers.IO) {
            val question = getQuestion(e)
            if (question.isNotEmpty()) {
                showResponse(e, question)
            }
        }
    }
     protected open fun getQuestion(e: AnActionEvent): String {
        val project = e.project
        val editor = e.getData(PlatformDataKeys.EDITOR)
        if (project != null && editor != null) {
            val selectionModel = editor.selectionModel
            var selectedText = "";
            ApplicationManager.getApplication().runReadAction {
                selectedText = selectionModel.selectedText.toString()
            }
            if (!selectedText.isNullOrEmpty()) {
                return getPrefix() +
                        "\n```Java\n$selectedText\n```\n"
            } else {
                Messages.showMessageDialog(project, "No text selected", "Error", Messages.getErrorIcon())
            }
        }
        return ""
    }

    protected open fun showResponse(e: AnActionEvent, question: String) {
            SwingUtilities.invokeLater {
                val project = e.project
                if (project != null) {
                    val toolWindowManager = ToolWindowManager.getInstance(project)
                    val toolWindow = toolWindowManager.getToolWindow("NxDev") // replace with your tool window id
                    val contentManager = toolWindow?.contentManager
                    val content = contentManager?.getContent(0)?.component
                    if (content is NxDevWindowFactory.NxDevWindows) {
                        toolWindow.show();
                        content.requestField.text = getPrefix()
                        GlobalScope.launch(Dispatchers.IO) {
                            content.requestField.isEnabled = false
                            content.requestField.text = ""
                            content.addQuestion(question)
                            content.addResponse(question)
                            content.addToConversation(question, content.responseMessage)
                            content.responseMessage = ""
                            content.requestField.isEnabled = true
                        }
                    }
                }
        }
    }

    protected open fun getPrefix(): String{
        return "";
    }

    val ConversationId = UUID.randomUUID().toString()
    fun messageQuestion(request: String): PromptAction.Message {
        val messageID: String = UUID.randomUUID().toString()
        val createdAt = Instant.now().toString()
        return PromptAction.Message("user", request, messageID, createdAt)
    }
    fun messageAnswer(message: String): PromptAction.Message {
        val messageID: String = UUID.randomUUID().toString()
        val createdAt = Instant.now().toString()
        return PromptAction.Message("assistant", message, messageID, createdAt)
    }
    private fun sendEventStreamRequest(message: String): Response? {
        val url = URL("https://api-nxdev.ntq.ai/api/conversations/stream")
        val requestBody = RequestBody.create(
            MediaType.parse("application/json"), Gson().toJson(
                PromptAction.JsonRequest(
                    messages = listOf(PromptAction.Message("user", message, messageQuestion(message).id, messageQuestion(message).createAt)),
                    max_tokens = 4096,
                    conversationId = ConversationId
                )
            ))

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", MyPluginSettings.getInstance().apiKey)
            .addHeader("platform", "Intellij")
            .build()

        return OkHttpClient.Builder().connectionSpecs(createConnectionSpecs()).build().newCall(request).execute()
    }

    private fun createConnectionSpecs(): List<ConnectionSpec>? {
        val spec = ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
            .tlsVersions(TlsVersion.TLS_1_2)
            .cipherSuites(
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
            )
            .build()
        return listOf<ConnectionSpec>(spec, ConnectionSpec.CLEARTEXT)
    }

    data class JsonRequest(
            val model: String = "1",
            val messages: List<Message>,
            val max_tokens: Int,
            val stream: Boolean = true,
            val conversationId : String
    )

    data class Message(
            val role: String,
            val content: String,
            val id: String,
            val createAt: String
    )
}