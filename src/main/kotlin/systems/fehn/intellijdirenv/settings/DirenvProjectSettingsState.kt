package systems.fehn.intellijdirenv.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "systems.fehn.intellijdirenv.settings.DirenvProjectSettingsState",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
@Service(Service.Level.PROJECT)
class DirenvProjectSettingsState : PersistentStateComponent<DirenvProjectSettingsState> {
    var importedEnvironment: MutableMap<String, String> = linkedMapOf()
    var unsetEnvironment: MutableList<String> = mutableListOf()

    override fun getState(): DirenvProjectSettingsState = this

    override fun loadState(state: DirenvProjectSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun updateEnvironment(loadedEnvironment: Map<String, String>, unloadedEnvironment: Set<String>): Boolean {
        val normalizedUnloadedEnvironment = unloadedEnvironment.toSortedSet()
        if (importedEnvironment == loadedEnvironment && unsetEnvironment.toSet() == normalizedUnloadedEnvironment) {
            return false
        }

        importedEnvironment.clear()
        importedEnvironment.putAll(loadedEnvironment)

        unsetEnvironment.clear()
        unsetEnvironment.addAll(normalizedUnloadedEnvironment)

        return true
    }

    fun snapshot(): DirenvEnvironmentSnapshot =
        DirenvEnvironmentSnapshot(
            loadedEnvironment = importedEnvironment.toMap(),
            unloadedEnvironment = unsetEnvironment.toSet(),
        )
}

data class DirenvEnvironmentSnapshot(
    val loadedEnvironment: Map<String, String>,
    val unloadedEnvironment: Set<String>,
) {
    fun isEmpty(): Boolean = loadedEnvironment.isEmpty() && unloadedEnvironment.isEmpty()

    fun applyTo(environment: MutableMap<String, String>) {
        environment.putAll(loadedEnvironment)
        unloadedEnvironment.forEach(environment::remove)
    }
}
