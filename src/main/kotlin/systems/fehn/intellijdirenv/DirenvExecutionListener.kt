package systems.fehn.intellijdirenv

import com.intellij.execution.ExecutionListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.logger
import systems.fehn.intellijdirenv.services.DirenvProjectService

class DirenvExecutionListener : ExecutionListener {
    override fun processStarting(executorId: String, env: ExecutionEnvironment) {
        val project = env.project
        val service = project.getService(DirenvProjectService::class.java)

        // Re-import direnv if an .envrc exists (applies to all run configs)
        service.projectEnvrcFile?.let { service.importDirenv(it, false) }

        // Also apply to the current run profile in case it was created after the last import
        val accessor = EnvironmentAccessor.forRunProfile(env.runProfile) ?: run {
            LOG.debug("Run profile ${env.runProfile.javaClass.name} does not support environment variables")
            return
        }
        service.applyDirenvTo(accessor)
    }

    companion object {
        private val LOG = logger<DirenvExecutionListener>()
    }
}
