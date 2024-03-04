package vn.com.ntq.nxdev.toolWindow

import vn.com.ntq.nxdev.actions.PromptAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import vn.com.ntq.nxdev.services.MyProjectService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.markdown4j.Markdown4jProcessor
import vn.com.ntq.nxdev.settings.MyPluginSettings
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.net.HttpURLConnection
import java.net.URL
import javax.swing.*


class NxDevWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val nxDevWindows = NxDevWindows(toolWindow)
        val content = ContentFactory.SERVICE.getInstance().createContent(nxDevWindows, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class NxDevWindows(toolWindow: ToolWindow) : JPanel(BorderLayout()) {

        private val service = toolWindow.project.service<MyProjectService>()
        val requestField = JTextField("What do you want to ask?", 50)
//        val responseArea = JTextArea(5, 50)
        val responseArea = JTextPane()
        val processor = Markdown4jProcessor()

        init {
            val requestPanel = JPanel(BorderLayout())
            requestPanel.add(requestField, BorderLayout.CENTER)
            requestPanel.add(JButton("Send").apply {
                addActionListener(SendRequestActionListener())
            } , BorderLayout.EAST)

            add(requestPanel, BorderLayout.NORTH)
            responseArea.contentType ="text/html"
            add(JScrollPane(responseArea), BorderLayout.CENTER)
        }

        inner class SendRequestActionListener : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                val request = requestField.text
                GlobalScope.launch(Dispatchers.IO) {
                    val response = sendRequest(request)
                    SwingUtilities.invokeLater {
                        val content = processor.process(response.choices[0].message.content)
                        responseArea.text =  content
                    }
                }
            }

            private fun sendRequest(message: String): ChatCompletion {
                val url = URL("https://genapi.ntq.ai/v1/chat/completions")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                var authorizationKey = "Bearer " + MyPluginSettings.getInstance().apiKey;
                conn.setRequestProperty("Authorization", authorizationKey)
                conn.doOutput = true

                val jsonRequest = PromptAction.JsonRequest(
                        model = "ntq-coder",
                        messages = listOf(PromptAction.Message("user", message)),
                        max_tokens = 4096,
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

        }
    }

//
//    class NTQCoderWindow(toolWindow: ToolWindow) {
//
//        private val service = toolWindow.project.service<MyProjectService>()
//
//        fun getContent() = JBPanel<JBPanel<*>>().apply {
////            val label = JBLabel(MyBundle.message("randomLabel", "?"))
////
////            add(label)
////            add(JButton(MyBundle.message("shuffle")).apply {
////                addActionListener {
////                    label.text = MyBundle.message("randomLabel", service.getRandomNumber())
////                }
////            })
//            var textField = JTextField("What do you want to ask?",50);
//            add(textField);
//            add(JButton(MyBundle.message("ButtonMessage")).apply {
//            addActionListener {
////                label.text = MyBundle.message("randomLabel", service.getRandomNumber())
//            }
//            })
//        }
//    }
}
