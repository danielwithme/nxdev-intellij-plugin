package vn.com.ntq.nxdev.actions

class WriteDocumentationAction : PromptAction() {
    override fun getPrefix(): String{
        return "Write documentation for the following code: "
    }
}