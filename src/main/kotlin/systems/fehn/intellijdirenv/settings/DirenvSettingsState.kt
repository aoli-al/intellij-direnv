package systems.fehn.intellijdirenv.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "systems.fehn.intellijdirenv.settings.DirenvSettingsState",
    storages = [Storage("DirenvSettings.xml")],
)
class DirenvSettingsState : PersistentStateComponent<DirenvSettingsState> {
    var direnvSettingsPath: String = ""
    var direnvSettingsImportOnStartup: Boolean = false
    var direnvSettingsImportEveryExecution: Boolean = false

    override fun getState(): DirenvSettingsState = this

    override fun loadState(state: DirenvSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): DirenvSettingsState =
            ApplicationManager.getApplication().getService(DirenvSettingsState::class.java)
    }
}
