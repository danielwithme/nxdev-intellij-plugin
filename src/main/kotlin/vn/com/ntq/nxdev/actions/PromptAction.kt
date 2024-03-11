package vn.com.ntq.nxdev.actions

import vn.com.ntq.nxdev.toolWindow.ChatCompletion
import vn.com.ntq.nxdev.toolWindow.NxDevWindowFactory
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
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
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
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("NxDev") // replace with your tool window id
            val contentManager = toolWindow?.contentManager
            val content = contentManager?.getContent(0)?.component
            if (content is NxDevWindowFactory.NxDevWindows) {
                SwingUtilities.invokeLater {
                    toolWindow.show();
                    content.requestField.text = getPrefix()
                    content.responseArea.text = " "
                }
            }
            // Call to external API
            val response = sendEventStreamRequest(question)
            response?.body()?.source()?.let {source ->
                while (true) {
                    val event = source.readUtf8Line() ?: break
                    if (event.isBlank())
                        continue;
                    val eventJSON = event.removePrefix("data:")
                    val data = Gson().fromJson(eventJSON, ChatCompletion::class.java);

                    SwingUtilities.invokeLater {
                        if (content is NxDevWindowFactory.NxDevWindows) {
                            val sd = content.responseArea.styledDocument;
                            sd.insertString(sd.length, data?.choices?.getOrNull(0)?.delta?.content?:"", null)
                        }
                    }

                }

            }
        }
    }

    protected open fun getPrefix(): String{
        return "";
    }


    private fun sendEventStreamRequest(message: String): Response? {
        val url = URL("https://genapi.ntq.ai/v1/chat/completions")
        val requestBody = RequestBody.create(
            MediaType.parse("application/json"), Gson().toJson(
                PromptAction.JsonRequest(
                    model = "ntq-coder",
                    messages = listOf(PromptAction.Message("user", message)),
                    max_tokens = 4096
                )
            ))

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${MyPluginSettings.getInstance().apiKey}")
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
        return listOf<ConnectionSpec>(spec)
    }

    data class JsonRequest(
            val model: String,
            val messages: List<Message>,
            val max_tokens: Int,
            val platform: String = "Intellij",
            val stream: Boolean = true
    )

    data class Message(
            val role: String,
            val content: String
    )
}