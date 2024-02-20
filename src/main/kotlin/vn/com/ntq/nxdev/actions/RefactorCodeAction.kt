package vn.com.ntq.nxdev.actions

import vn.com.ntq.nxdev.toolWindow.NxDevWindowFactory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import javax.swing.SwingUtilities

class RefactorCodeAction : AskNxDevAction() {
    override fun getPrefix(): String{
        return "Refactor this code and explain what's changed: "
    }
}