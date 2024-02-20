package vn.com.ntq.nxdev.actions

import vn.com.ntq.nxdev.toolWindow.NxDevWindowFactory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import javax.swing.SwingUtilities

class FindProblemsAction : AskNxDevAction() {
    override fun getPrefix(): String{
        return "Find problems with the following code, fix them and explain what was wrong (Do not change anything else, if there are no problems say so): "
    }
}