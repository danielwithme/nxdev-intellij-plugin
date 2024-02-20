package vn.com.ntq.nxdev.actions

class ExplainCodeAction : PromptAction() {

     override fun getPrefix(): String{
        return "Explain what this code does: "
    }
}