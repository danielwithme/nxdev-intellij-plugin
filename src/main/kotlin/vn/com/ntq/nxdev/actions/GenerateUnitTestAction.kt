package vn.com.ntq.nxdev.actions

class GenerateUnitTestAction : PromptAction() {
    override fun getPrefix(): String{
        return "Generate a unit test for this code: "
    }
}