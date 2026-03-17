package systems.fehn.intellijdirenv.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class DirenvSettingsConfigurable : Configurable {
    private var component: DirenvSettingsComponent? = null

    override fun getDisplayName(): String = "DirEnv Settings"

    override fun getPreferredFocusedComponent(): JComponent? = component?.preferredFocusedComponent

    override fun createComponent(): JComponent {
        val c = DirenvSettingsComponent()
        component = c
        return c.panel
    }

    override fun isModified(): Boolean {
        val c = component ?: return false
        val settings = DirenvSettingsState.getInstance()
        return c.direnvPath != settings.direnvSettingsPath ||
                c.direnvImportOnStartup != settings.direnvSettingsImportOnStartup ||
                c.direnvImportEveryExecution != settings.direnvSettingsImportEveryExecution
    }

    override fun apply() {
        val c = component ?: return
        val settings = DirenvSettingsState.getInstance()
        settings.direnvSettingsPath = c.direnvPath
        settings.direnvSettingsImportOnStartup = c.direnvImportOnStartup
        settings.direnvSettingsImportEveryExecution = c.direnvImportEveryExecution
    }

    override fun reset() {
        val c = component ?: return
        val settings = DirenvSettingsState.getInstance()
        c.direnvPath = settings.direnvSettingsPath
        c.direnvImportOnStartup = settings.direnvSettingsImportOnStartup
        c.direnvImportEveryExecution = settings.direnvSettingsImportEveryExecution
    }

    override fun disposeUIResources() {
        component = null
    }
}
