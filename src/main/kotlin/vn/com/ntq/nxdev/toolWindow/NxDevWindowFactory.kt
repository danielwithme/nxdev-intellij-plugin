package vn.com.ntq.nxdev.toolWindow

import com.google.gson.Gson
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.DelicateCoroutinesApi
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
import util.TextPrompt
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.net.URL
import javax.swing.*
import java.awt.event.AdjustmentEvent

import java.awt.event.AdjustmentListener





class NxDevWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        runWriteAction {
            val nxDevWindows = NxDevWindows(toolWindow)
            val content = ContentFactory.SERVICE.getInstance().createContent(nxDevWindows, null, false)
            toolWindow.contentManager.addContent(content)
        }
    }

    override fun shouldBeAvailable(project: Project) = true

    class NxDevWindows(toolWindow: ToolWindow) : JPanel(BorderLayout()) {

        val ROLE_NXDEV_TEXT = "**NX_DEV**: "
        val ROLE_YOU_TEXT = "**YOU**: "
        var conversation = ""

        private val service = toolWindow.project.service<MyProjectService>()
        val processor = Markdown4jProcessor()
        lateinit var panel : MarkdownJCEFHtmlPanel
        val button = JButton("Send")
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
            val tp1 = TextPrompt("What do you want to ask?", requestField, TextPrompt.Show.FOCUS_LOST)
            tp1.changeStyle(Font.ITALIC)

            tp1.changeAlpha(0.5f)
            tp1.verticalAlignment = SwingConstants.TOP

            val requestPanel = JPanel(BorderLayout())
            requestPanel.add(scroll, BorderLayout.CENTER)
            var buttonPanel = JPanel()
            buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.X_AXIS)
            button.isEnabled=false
            requestField.addKeyListener(object : KeyAdapter() {
                override fun keyReleased(e: KeyEvent?) {
                    super.keyReleased(e)
                    button.isEnabled = requestField.text.isNotEmpty()
                }
            })
            buttonPanel.add(button.apply {
                addActionListener(SendRequestActionListener())
            })
            requestPanel.add(buttonPanel, BorderLayout.EAST)

            add(requestPanel, BorderLayout.SOUTH)
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
        fun displayConversation(){
            SwingUtilities.invokeLater {
                val file = LightVirtualFile("content.md", conversation)

                val html = runReadAction {
                    MarkdownUtil.generateMarkdownHtml(file, conversation, null)
                }
                panel.setHtml(html, conversation.length)

                //Scroll to bottom
                panel.scrollBy(0, 100)
            }
        }
        fun callAI(request: String): Response? {
//                    val response = sendEventStreamRequest(request)
            val url = URL("https://api-nxdev.ntq.ai/api/conversations/stream")
            val requestBody = RequestBody.create(
                MediaType.parse("application/json"), Gson().toJson(
                    PromptAction.JsonRequest(
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

            return OkHttpClient.Builder().connectionSpecs(createConnectionSpecs()).build().newCall(httpRequest).execute()

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
        inner class SendRequestActionListener : ActionListener {
            @OptIn(DelicateCoroutinesApi::class)
            override fun actionPerformed(e: ActionEvent) {
                val request = requestField.text
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        addQuestion(request)
                        addResponse(request)

                    }catch(e: Exception){
                        SwingUtilities.invokeLater {

                            var responseMarkdown = ""
                            responseMarkdown = "There is a lot of traffic at the moment, please try again later."
                            val file = LightVirtualFile("content.md", responseMarkdown)
                            val html = runReadAction {
                                MarkdownUtil.generateMarkdownHtml(file, responseMarkdown, null)
                            }
                            panel.setHtml(html, responseMarkdown.length)
                        }
                    }finally {
                        SwingUtilities.invokeLater {
                            requestField.text = ""
                            button.isEnabled = false
                        }
                    }
                }
            }

        }

        private fun extractResponseToConversation(response: Response?) {

            response?.body()?.source()?.let { source ->

                while (true) {
                    val event = source.readUtf8Line() ?: break
                    if (event.isBlank())
                        continue;
                    val eventJSON = event.removePrefix("data: data: ")
                    try {
                        val data = Gson().fromJson(eventJSON, ChatCompletion::class.java);
                        SwingUtilities.invokeLater {
                            conversation += data?.choices?.getOrNull(0)?.delta?.content ?: ""
                            displayConversation()
                        }
                    } catch (e: Exception) {
                        println(e)
                        continue;
                    }

                }
            }
        }

        fun addQuestion(question: String) {
            conversation = ensureLineBreakAtEnd(conversation);
            conversation += ROLE_YOU_TEXT
            conversation += question
            displayConversation()
        }

        fun addResponse(question: String) {
            val response = callAI(question)
            conversation = ensureLineBreakAtEnd(conversation);
            conversation += ROLE_NXDEV_TEXT
            extractResponseToConversation(response)

        }
        fun ensureLineBreakAtEnd(input: String): String {
            return if (input.endsWith("\n")) {
                if (input.endsWith("\n\n")) input else "$input\n"
            } else "$input\n\n"
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
