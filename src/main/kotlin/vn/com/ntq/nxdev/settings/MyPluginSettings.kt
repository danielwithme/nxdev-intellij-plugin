package vn.com.ntq.nxdev.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil


@State(name = "MyPluginSettings", storages = [Storage("myPluginSettings.xml")])
class MyPluginSettings : PersistentStateComponent<MyPluginSettings> {

    var apiKey: String = ""
    companion object {
        fun getInstance(): MyPluginSettings {
            return ApplicationManager.getApplication().getService(MyPluginSettings::class.java)
        }
    }
    override fun getState(): MyPluginSettings {
        return this
    }

    override fun loadState(state: MyPluginSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

}