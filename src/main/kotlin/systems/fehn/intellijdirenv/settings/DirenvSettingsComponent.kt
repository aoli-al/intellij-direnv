package systems.fehn.intellijdirenv.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class DirenvSettingsComponent {
    private val direnvPathField = TextFieldWithBrowseButton()
    private val importOnStartupCheckBox = JBCheckBox("Automatically import any .envrc in the project root when the project is opened.")
    private val importEveryExecutionCheckBox = JBCheckBox("Automatically import any .envrc in the project root before every run/debug")

    val panel: JPanel
    val preferredFocusedComponent: JComponent get() = direnvPathField

    var direnvPath: String
        get() = direnvPathField.text
        set(value) { direnvPathField.text = value }

    var direnvImportOnStartup: Boolean
        get() = importOnStartupCheckBox.isSelected
        set(value) { importOnStartupCheckBox.isSelected = value }

    var direnvImportEveryExecution: Boolean
        get() = importEveryExecutionCheckBox.isSelected
        set(value) { importEveryExecutionCheckBox.isSelected = value }

    init {
        val descriptor = FileChooserDescriptorFactory.singleFile().apply {
            title = "Choose Directory"
            description = "Choose the path to the direnv file"
        }
        direnvPathField.addBrowseFolderListener(null, descriptor)

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JLabel("DirenvPath: "), direnvPathField, 1, false)
            .addComponent(importOnStartupCheckBox, 1)
            .addComponent(importEveryExecutionCheckBox, 1)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
}
