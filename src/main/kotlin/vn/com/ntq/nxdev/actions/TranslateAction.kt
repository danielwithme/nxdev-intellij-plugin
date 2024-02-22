package vn.com.ntq.nxdev.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import vn.com.ntq.nxdev.toolWindow.NxDevWindowFactory
import javax.swing.SwingUtilities

class TranslateAction : PromptAction() {
    private var targetLanguage = "English"
    override fun getPrefix(): String{
        return "translate the text below in $targetLanguage: "
    }

    override fun actionPerformed(e: AnActionEvent) {
        val languages = arrayOf("English", "Vietnamese")
        val targetLangIndex = Messages.showChooseDialog("Choose a language", "Choose Language", languages, "English", null)
        targetLanguage = languages[targetLangIndex];

        val question = getQuestion(e)
        if (question.isNotEmpty()) {
            showResponse(e, question)
        }
    }
}