package systems.fehn.intellijdirenv.services

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import systems.fehn.intellijdirenv.EnvironmentAccessor
import systems.fehn.intellijdirenv.MyBundle
import systems.fehn.intellijdirenv.notificationGroup
import systems.fehn.intellijdirenv.settings.DirenvProjectSettingsState
import systems.fehn.intellijdirenv.settings.DirenvSettingsState
import systems.fehn.intellijdirenv.switchNull

@Service(Service.Level.PROJECT)
class DirenvProjectService(private val project: Project) {
    private val logger by lazy { logger<DirenvProjectService>() }

    private val projectDir = project.guessProjectDir()
        .switchNull(
            onNull = { logger.warn("Could not determine project dir of project ${project.name}") },
        )

    val projectEnvrcFile: VirtualFile?
        get() = projectDir?.findChild(".envrc")?.takeUnless { it.isDirectory }
            .switchNull(
                onNull = { logger.trace { "Project ${project.name} contains no .envrc file" } },
                onNonNull = { logger.trace { "Project ${project.name} has .envrc file ${it.path}" } },
            )

    private val jsonFactory by lazy { JsonFactory() }
    private val projectSettings by lazy { project.getService(DirenvProjectSettingsState::class.java) }

    fun importDirenv(envrcFile: VirtualFile, notifyNoChange: Boolean = true) {
        val process = executeDirenv(envrcFile, "export", "json")

        if (process.waitFor() != 0) {
            handleDirenvError(process, envrcFile)
            return
        }

        val sb = StringBuilder()
        process.inputStream.bufferedReader().forEachLine {
            sb.appendLine(it)
        }
        logger.info("Received direnv output: ${sb.toString()}")
        jsonFactory.createParser(sb.toString()).use { parser ->
            val didWork = handleDirenvOutput(parser)
            applyToAllRunConfigurations()
            notificationGroup
                .createNotification(
                    MyBundle.message("executedSuccessfully"),
                    "",
                    NotificationType.INFORMATION,
                ).notify(project)
        }
    }

    private fun handleDirenvOutput(parser: JsonParser): Boolean {
        val loadedEnvironment = linkedMapOf<String, String>()
        val unloadedEnvironment = linkedSetOf<String>()

        while (parser.nextToken() != null) {
            if (parser.currentToken == JsonToken.FIELD_NAME) {
                val variableName = parser.currentName()
                when (parser.nextToken()) {
                    JsonToken.VALUE_NULL -> {
                        unloadedEnvironment += variableName
                        loadedEnvironment.remove(variableName)
                        logger.trace { "Unset variable $variableName" }
                    }

                    JsonToken.VALUE_STRING -> {
                        loadedEnvironment[variableName] = parser.valueAsString
                        unloadedEnvironment.remove(variableName)
                        logger.trace { "Set variable $variableName to ${parser.valueAsString}" }
                    }

                    else -> continue
                }
            }
        }

        return projectSettings.updateEnvironment(loadedEnvironment, unloadedEnvironment)
    }

    internal fun applyDirenvTo(accessor: EnvironmentAccessor) {
        val snapshot = projectSettings.snapshot()
        if (snapshot.isEmpty()) return

        val merged = LinkedHashMap(snapshot.loadedEnvironment)
        // User-configured envs take priority over direnv
        merged.putAll(accessor.getEnvs())
        accessor.setEnvs(merged)
    }

    private fun applyToAllRunConfigurations() {
        val runManager = RunManager.getInstance(project)
        for (settings in runManager.allSettings) {
            val accessor = EnvironmentAccessor.forRunProfile(settings.configuration) ?: continue
            applyDirenvTo(accessor)
        }
    }

    private fun handleDirenvError(process: Process, envrcFile: VirtualFile) {
        val error = process.errorStream.bufferedReader().readText()

        val notification = if (error.contains(" is blocked")) {
            notificationGroup
                .createNotification(
                    MyBundle.message("envrcNotYetAllowed"),
                    "",
                    NotificationType.WARNING,
                )
                .addAction(
                    NotificationAction.create(MyBundle.message("allow")) { _, notification ->
                        notification.hideBalloon()
                        executeDirenv(envrcFile, "allow").waitFor()

                        importDirenv(envrcFile)
                    },
                )
        } else {
            logger.error(error)

            notificationGroup
                .createNotification(
                    MyBundle.message("errorDuringDirenv"),
                    "",
                    NotificationType.ERROR,
                )
        }

        notification
            .addAction(
                NotificationAction.create(MyBundle.message("openEnvrc")) { _, it ->
                    it.hideBalloon()

                    FileEditorManager.getInstance(project).openFile(envrcFile, true, true)
                },
            )
            .notify(project)
    }

    private fun executeDirenv(envrcFile: VirtualFile, vararg args: String): Process {
        val workingDir = envrcFile.parent.path

        val cli = GeneralCommandLine("direnv", *args)
            .withWorkDirectory(workingDir)

        val appSettings = DirenvSettingsState.getInstance()
        if (appSettings.direnvSettingsPath.isNotEmpty()) {
            cli.withExePath(appSettings.direnvSettingsPath)
        }

        return cli.createProcess()
    }
}
