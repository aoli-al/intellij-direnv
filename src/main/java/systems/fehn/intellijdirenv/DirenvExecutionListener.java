package systems.fehn.intellijdirenv;

import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import systems.fehn.intellijdirenv.services.DirenvProjectService;
import systems.fehn.intellijdirenv.settings.DirenvSettingsState;

import java.util.LinkedHashMap;
import java.util.Map;

class DirenvExecutionListener implements com.intellij.execution.ExecutionListener {
    private static final Key<RunConfigurationEnvironmentState> RUN_CONFIGURATION_ENVIRONMENT_STATE_KEY =
            Key.create("systems.fehn.intellijdirenv.runConfigurationEnvironmentState");

    public DirenvExecutionListener() {
    }

    @Override
    public void processStarting(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
        if ( !DirenvSettingsState.getInstance().direnvSettingsImportEveryExecution ) {
            com.intellij.execution.ExecutionListener.super.processStarting(executorId, env);
            return;
        }

        Project project = env.getProject();
        DirenvProjectService service = project.getService(DirenvProjectService.class);
        VirtualFile envrcFile = service.getProjectEnvrcFile();
        if (envrcFile != null) {
            service.importDirenv(envrcFile, false);
        }

        RunProfile runProfile = env.getRunProfile();
        if (!(runProfile instanceof CommonProgramRunConfigurationParameters configuration)) {
            com.intellij.execution.ExecutionListener.super.processStarting(executorId, env);
            return;
        }

        Map<String, String> effectiveEnvironment = new LinkedHashMap<>(System.getenv());
        service.currentImportedEnvironment().applyTo(effectiveEnvironment);
        effectiveEnvironment.putAll(configuration.getEnvs());

        env.putUserData(
                RUN_CONFIGURATION_ENVIRONMENT_STATE_KEY,
                new RunConfigurationEnvironmentState(new LinkedHashMap<>(configuration.getEnvs()), configuration.isPassParentEnvs())
        );
        configuration.setEnvs(effectiveEnvironment);
        configuration.setPassParentEnvs(false);
        com.intellij.execution.ExecutionListener.super.processStarting(executorId, env);
    }

    @Override
    public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull com.intellij.execution.process.ProcessHandler handler) {
        restoreRunConfigurationEnvironment(env);
        com.intellij.execution.ExecutionListener.super.processStarted(executorId, env, handler);
    }

    @Override
    public void processNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
        restoreRunConfigurationEnvironment(env);
        com.intellij.execution.ExecutionListener.super.processNotStarted(executorId, env);
    }

    private static void restoreRunConfigurationEnvironment(@NotNull ExecutionEnvironment env) {
        RunConfigurationEnvironmentState state = env.getUserData(RUN_CONFIGURATION_ENVIRONMENT_STATE_KEY);
        if (state == null) {
            return;
        }

        RunProfile runProfile = env.getRunProfile();
        if (runProfile instanceof CommonProgramRunConfigurationParameters configuration) {
            configuration.setEnvs(state.envs());
            configuration.setPassParentEnvs(state.passParentEnvs());
        }

        env.putUserData(RUN_CONFIGURATION_ENVIRONMENT_STATE_KEY, null);
    }

    private record RunConfigurationEnvironmentState(Map<String, String> envs, boolean passParentEnvs) {
    }
}
