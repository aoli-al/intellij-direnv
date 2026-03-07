package systems.fehn.intellijdirenv

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import systems.fehn.intellijdirenv.services.DirenvProjectService

class DirenvImportAction : AnAction(MyBundle.message("importDirenvAction")) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        if (e.project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        when (e.place) {
            ActionPlaces.PROJECT_VIEW_POPUP -> {
                val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
                if (virtualFile == null || virtualFile.isDirectory || !virtualFile.isInLocalFileSystem) {
                    e.presentation.isEnabledAndVisible = false
                } else {
                    e.presentation.isEnabledAndVisible = virtualFile.name.equals(".envrc", ignoreCase = true)
                }
            }

            else -> e.presentation.isEnabledAndVisible = false
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = project.getService(DirenvProjectService::class.java)

        when (e.place) {
            ActionPlaces.PROJECT_VIEW_POPUP -> {
                val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
                service.importDirenv(virtualFile)
            }
        }
    }
}
