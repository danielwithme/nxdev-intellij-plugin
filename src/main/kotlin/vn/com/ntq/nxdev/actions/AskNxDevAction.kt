package vn.com.ntq.nxdev.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import vn.com.ntq.nxdev.toolWindow.NxDevWindowFactory
import javax.swing.SwingUtilities

class AskNxDevAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(PlatformDataKeys.EDITOR)
        if (project != null && editor != null) {
            val selectionModel = editor.selectionModel
            val selectedText = selectionModel.selectedText
            if (!selectedText.isNullOrEmpty()) {
                SwingUtilities.invokeLater {
                    val toolWindowManager = ToolWindowManager.getInstance(project)
                    val toolWindow = toolWindowManager.getToolWindow("NxDev") // replace with your tool window id
                    toolWindow?.show();
                }
            } else {
                Messages.showMessageDialog(project, "No text selected", "Error", Messages.getErrorIcon())
            }
        }
    }
}