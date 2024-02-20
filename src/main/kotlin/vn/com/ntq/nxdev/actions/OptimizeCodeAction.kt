package vn.com.ntq.nxdev.actions

class OptimizeCodeAction : PromptAction() {
    override fun getPrefix(): String{
        return "Optimize the following code if there is anything to improve, if not say so: "
    }
}