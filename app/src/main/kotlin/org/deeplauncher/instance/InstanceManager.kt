package org.deeplauncher.instance

import io.ktor.client.call.*
import io.ktor.client.request.*
import org.deeplauncher.core.LauncherFiles
import org.deeplauncher.models.MinecraftInstance
import org.deeplauncher.models.VersionDetail
import org.deeplauncher.network.client
import org.deeplauncher.runtime.RuntimeManager
import org.deeplauncher.version.VersionManager
import java.io.File

class InstanceManager(
    private val versionManager: VersionManager,
    private val runtimeManager: RuntimeManager
) {
    private val repository = InstanceRepository()
    private val gameLauncher = GameLauncher()

    suspend fun launchInstance(name: String, username: String) {
        val instanceDir = repository.getInstanceDir(name)
        val instanceConfig = repository.loadInstance(name) ?: return

        if (!versionManager.isAValidVersion(instanceConfig.version))
            throw IllegalArgumentException("Version ${instanceConfig.version} not found in the Mojang manifest")

        val versionInfo = versionManager.getVersionInfo(instanceConfig.version) ?: return
        val versionDetail: VersionDetail = client.get(versionInfo.url).body()

        val requiredJavaVersion = versionManager.getRequiredJavaMajorVersion(versionDetail)
        val javaExecutablePath = LauncherFiles.getJavaPathForMajorVersion(requiredJavaVersion)

        val javaFile = File(javaExecutablePath)
        if (!javaFile.exists()) {
            val runtimeDir = File(LauncherFiles.rootDir, "runtimes/java-$requiredJavaVersion")
            runtimeManager.downloadAndExtractJava(requiredJavaVersion, runtimeDir)
        }

        gameLauncher.launchProcess(instanceDir, versionDetail, javaExecutablePath, username)
    }

    suspend fun createInstance(name: String, version: String): MinecraftInstance {
        val versionInfo = versionManager.getVersionInfo(version)
            ?: throw IllegalArgumentException("Version $version not found in the Mojang manifest")

        versionManager.downloadVersion(versionInfo) { completed, total, url ->
            print("\r$completed / $total downloaded assets ($url)")
            System.out.flush()
        }

        val instance = MinecraftInstance(name, version)
        repository.createInstance(instance)

        return instance
    }

    fun loadInstance(name: String): MinecraftInstance? {
        return repository.loadInstance(name)
    }

    fun saveInstance(instance: MinecraftInstance) {
        repository.saveInstance(instance)
    }

    fun deleteInstance(name: String): Boolean {
        return repository.deleteInstance(name)
    }
}