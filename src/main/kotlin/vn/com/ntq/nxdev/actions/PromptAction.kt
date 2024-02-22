package vn.com.ntq.nxdev.actions

import vn.com.ntq.nxdev.toolWindow.ChatCompletion
import vn.com.ntq.nxdev.toolWindow.NxDevWindowFactory
import com.google.gson.Gson
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import org.markdown4j.Markdown4jProcessor
import vn.com.ntq.nxdev.settings.MyPluginSettings
import java.net.HttpURLConnection
import java.net.URL
import javax.swing.SwingUtilities

open class PromptAction : AnAction() {

    val processor = Markdown4jProcessor()
    override fun actionPerformed(e: AnActionEvent) {
        val question = getQuestion(e)
        if (question.isNotEmpty()) {
            showResponse(e, question)
        }
    }
     protected open fun getQuestion(e: AnActionEvent): String {
        val project = e.project
        val editor = e.getData(PlatformDataKeys.EDITOR)
        if (project != null && editor != null) {
            val selectionModel = editor.selectionModel
            val selectedText = selectionModel.selectedText
            if (!selectedText.isNullOrEmpty()) {
                return getPrefix() + selectedText
            } else {
                Messages.showMessageDialog(project, "No text selected", "Error", Messages.getErrorIcon())
            }
        }
        return ""
    }

    protected open fun showResponse(e: AnActionEvent, question: String) {
        val project = e.project
        if (project != null) {
            // Call to external API
            val response = sendRequest(question)
            SwingUtilities.invokeLater {
                val toolWindowManager = ToolWindowManager.getInstance(project)
                val toolWindow = toolWindowManager.getToolWindow("NxDev") // replace with your tool window id
                val contentManager = toolWindow?.contentManager
                val content = contentManager?.getContent(0)?.component
                if (content is NxDevWindowFactory.NxDevWindows) {
                    toolWindow.show();
                    content.requestField.text = getPrefix()
                    val markdownContent = processor.process(response.choices[0].message.content)
                    content.responseArea.text = markdownContent
                }
            }
        }
    }

    protected open fun getPrefix(): String{
        return "";
    }
    private fun sendRequest(message: String): ChatCompletion {
        val url = URL("https://genapi.ntq.ai/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        var authorizationKey = "Bearer " + MyPluginSettings.getInstance().apiKey;
        conn.setRequestProperty("Authorization", authorizationKey)
        conn.doOutput = true

        val jsonRequest = JsonRequest(
                model = "ntq-coder",
                messages = listOf(Message("user", message)),
                max_tokens = 4096
        )

        val requestBody = Gson().toJson(jsonRequest)
        conn.outputStream.write(requestBody.toByteArray())

        val responseCode = conn.responseCode
        val response = if (responseCode == HttpURLConnection.HTTP_OK) {
            conn.inputStream.reader().readText()
        } else {
            "Error: $responseCode"
        }

        conn.disconnect()
        return Gson().fromJson(response, ChatCompletion::class.java)
    }

    data class JsonRequest(
            val model: String,
            val messages: List<Message>,
            val max_tokens: Int
    )

    data class Message(
            val role: String,
            val content: String
    )
}