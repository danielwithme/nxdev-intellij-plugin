package vn.com.ntq.nxdev.toolWindow

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import org.intellij.lang.annotations.Language
import org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil
import org.intellij.plugins.markdown.ui.preview.jcef.MarkdownJCEFHtmlPanel
import org.markdown4j.Markdown4jProcessor
import vn.com.ntq.nxdev.actions.PromptAction
import vn.com.ntq.nxdev.services.MyProjectService
import vn.com.ntq.nxdev.settings.MyPluginSettings
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.IOException
import java.net.URL
import javax.swing.*
import javax.swing.border.LineBorder
import javax.swing.text.StyledDocument


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
//        val requestField = JTextField("What do you want to ask?", 50)
//        val responseArea = JTextArea(5, 50)```
//        val responseArea = JTextPane()
        val processor = Markdown4jProcessor()
        lateinit var panel : MarkdownJCEFHtmlPanel
        val requestField = JTextArea().apply {
            margin = Insets(10,10,5,0)
            isFocusable = true
            wrapStyleWord  = true
            lineWrap = true
        }
        var scroll = JBScrollPane(requestField, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED).apply {
                preferredSize = Dimension(500,100)
        }

        init {
            val requestPanel = JPanel(BorderLayout())
            requestPanel.add(scroll, BorderLayout.CENTER)
            var buttonPanel = JPanel()
            buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.X_AXIS)
            buttonPanel.add(JButton("Send").apply {
                addActionListener(SendRequestActionListener())
            })
            requestPanel.add(buttonPanel, BorderLayout.EAST)

            add(requestPanel, BorderLayout.NORTH)
            add(createPreviewComponent(toolWindow.project, ""), BorderLayout.CENTER)
        }
        fun createPreviewComponent(
            project: Project?,
            @Language("Markdown")
            content: String
//            parentDisposable: Disposable
        ): JComponent {
            val file = LightVirtualFile("content.md", content)
            panel = MarkdownJCEFHtmlPanel(project, file)
//            Disposer.register(parentDisposable, panel)
            val html = runReadAction {
                MarkdownUtil.generateMarkdownHtml(file, content, project)
            }
            panel.setHtml(html, 0)
            return panel.component
        }
        inner class SendRequestActionListener : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                val request = requestField.text
                var responseMarkdown = ""
                GlobalScope.launch(Dispatchers.IO) {
//                    val response = sendEventStreamRequest(request)
                    val url = URL("https://api-nxdev.ntq.ai/api/conversations/stream")
                    val requestBody = RequestBody.create(
                        MediaType.parse("application/json"), Gson().toJson(
                            PromptAction.JsonRequest(
                                model = "ntq-coder",
                                messages = listOf(PromptAction.Message("user", request)),
                                max_tokens = 4096
                            )
                        ))

                    val httpRequest = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", MyPluginSettings.getInstance().apiKey)
                        .addHeader("platform", "Intellij")
                        .build()

                    val response = OkHttpClient.Builder().connectionSpecs(createConnectionSpecs()).build().newCall(httpRequest).execute()
//                    responseArea.text = " ";
//                    val sd = responseArea.styledDocument;

                    response.body()?.source()?.let {source ->
                        while (true) {
                            val event = source.readUtf8Line() ?: break
                            if (event.isBlank())
                                continue;
                            val eventJSON = event.removePrefix("data: data: ")
                            try {
                                val data = Gson().fromJson(eventJSON, ChatCompletion::class.java);
//
//                            SwingUtilities.invokeLater {
////                                val content = processor.process(data?.choices?.getOrNull(0)?.delta?.content?:"")
//                                sd.insertString(sd.length, data?.choices?.getOrNull(0)?.delta?.content?:"", null)
//                            }
                                SwingUtilities.invokeLater {
                                    responseMarkdown += data?.choices?.getOrNull(0)?.delta?.content ?: ""
                                    val file = LightVirtualFile("content.md", responseMarkdown)

                                    val html = runReadAction {
                                        MarkdownUtil.generateMarkdownHtml(file, responseMarkdown, null)
                                    }
                                    panel.setHtml(html, responseMarkdown.length)
                                }
                            } catch (e:Exception) {
                                println(e)
                                continue;
                            }

                        }

                    }
                }
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
//            private fun sendRequest(message: String): ChatCompletion {
//                val url = URL("https://genapi.ntq.ai/v1/chat/completions")
//                val conn = url.openConnection() as HttpURLConnection
//                conn.requestMethod = "POST"
//                conn.setRequestProperty("Content-Type", "application/json")
//                var authorizationKey = "Bearer " + MyPluginSettings.getInstance().apiKey;
//                conn.setRequestProperty("Authorization", authorizationKey)
//                conn.doOutput = true
//
//                val jsonRequest = PromptAction.JsonRequest(
//                        model = "ntq-coder",
//                        messages = listOf(PromptAction.Message("user", message)),
//                        max_tokens = 4096
//                )
//
//                val requestBody = Gson().toJson(jsonRequest)
//                conn.outputStream.write(requestBody.toByteArray())
//
//                val responseCode = conn.responseCode
//                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
//                    conn.inputStream.reader().readText()
//                } else {
//                    "Error: $responseCode"
//                }
//
//                conn.disconnect()
//                return Gson().fromJson(response, ChatCompletion::class.java)
//            }

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
