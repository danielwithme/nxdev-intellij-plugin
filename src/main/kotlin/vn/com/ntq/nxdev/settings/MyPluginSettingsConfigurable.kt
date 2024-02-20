package vn.com.ntq.nxdev.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class MyPluginSettingsConfigurable : Configurable {

    /**
     * https://plugins.jetbrains.com/docs/intellij/settings-tutorial.html
     */
    private lateinit var mySettingsPanel: JPanel
    private var apiKeyField: JTextField = JBTextField();



    override fun getDisplayName(): String {
        return "NxDev Setting"
    }

    override fun createComponent(): JComponent {
        apiKeyField.text = MyPluginSettings.getInstance().apiKey
        mySettingsPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(JLabel("API Key: "), apiKeyField, 1, false)
                .addComponentFillVertically(JPanel(), 0)
                .panel
        return mySettingsPanel
    }

    override fun isModified(): Boolean {
        return MyPluginSettings.getInstance().apiKey != apiKeyField.text
    }

    override fun apply() {
        MyPluginSettings.getInstance().apiKey = apiKeyField.text
    }

    override fun reset() {
        apiKeyField.text = MyPluginSettings.getInstance().apiKey
    }

    override fun disposeUIResources() {
        // Implement this method if you have any UI resources to dispose
    }
}