package vn.com.ntq.nxdev.actions

class RefactorCodeAction : PromptAction() {
    override fun getPrefix(): String{
        return "Refactor this code and explain what's changed: "
    }
}