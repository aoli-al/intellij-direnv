package systems.fehn.intellijdirenv

import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.diagnostic.logger
import java.lang.reflect.Method

internal sealed interface EnvironmentAccessor {
    fun getEnvs(): Map<String, String>
    fun setEnvs(envs: Map<String, String>)

    companion object {
        fun forRunProfile(runProfile: RunProfile): EnvironmentAccessor? {
            if (runProfile is CommonProgramRunConfigurationParameters) {
                return CommonParamsAccessor(runProfile)
            }

            // Try the run profile object itself
            ReflectiveAccessor.tryCreate(runProfile)?.let { return it }

            // Try nested settings object (e.g. ExternalSystemRunConfiguration.getSettings())
            try {
                val settings = runProfile.javaClass.getMethod("getSettings").invoke(runProfile)
                if (settings != null) {
                    ReflectiveAccessor.tryCreate(settings)?.let { return it }
                }
            } catch (_: Exception) {
            }

            return null
        }
    }
}

private class CommonParamsAccessor(
    private val params: CommonProgramRunConfigurationParameters,
) : EnvironmentAccessor {
    override fun getEnvs(): Map<String, String> = params.envs
    override fun setEnvs(envs: Map<String, String>) { params.envs = envs }
}

private class ReflectiveAccessor(
    private val target: Any,
    private val envGetter: Method,
    private val envSetter: Method,
) : EnvironmentAccessor {

    @Suppress("UNCHECKED_CAST")
    override fun getEnvs(): Map<String, String> = try {
        envGetter.invoke(target) as Map<String, String>
    } catch (e: Exception) {
        LOG.warn("Failed to get environment variables via reflection", e)
        emptyMap()
    }

    override fun setEnvs(envs: Map<String, String>) {
        try {
            envSetter.invoke(target, envs)
        } catch (e: Exception) {
            LOG.warn("Failed to set environment variables via reflection", e)
        }
    }

    companion object {
        private val LOG = logger<ReflectiveAccessor>()

        fun tryCreate(target: Any): ReflectiveAccessor? {
            val clazz = target.javaClass
            return try {
                val getter = findMethod(clazz, "getEnvs") ?: findMethod(clazz, "getEnv") ?: return null
                val setter = findMethod(clazz, "setEnvs", Map::class.java)
                    ?: findMethod(clazz, "setEnv", Map::class.java) ?: return null

                ReflectiveAccessor(target, getter, setter)
            } catch (_: Exception) {
                null
            }
        }

        private fun findMethod(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): Method? = try {
            clazz.getMethod(name, *paramTypes).apply { isAccessible = true }
        } catch (_: NoSuchMethodException) {
            null
        }
    }
}
