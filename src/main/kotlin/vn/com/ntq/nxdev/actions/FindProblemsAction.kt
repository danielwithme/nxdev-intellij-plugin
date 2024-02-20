package vn.com.ntq.nxdev.actions

class FindProblemsAction : PromptAction() {
    override fun getPrefix(): String{
        return "Find problems with the following code, fix them and explain what was wrong (Do not change anything else, if there are no problems say so): "
    }
}