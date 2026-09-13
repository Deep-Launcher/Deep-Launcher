package org.deeplauncher.instance

import org.deeplauncher.core.LauncherFiles
import org.deeplauncher.models.Account
import org.deeplauncher.models.VersionDetail
import org.deeplauncher.runtime.RuntimeManager
import java.io.File

class GameLauncher(
    private val runtimeManager: RuntimeManager
) {
    fun launchProcess(
        instanceDir: File,
        versionDetail: VersionDetail,
        javaExecutablePath: String,
        account: Account
    ) {
        val separator = File.pathSeparator
        val libraries = versionDetail.libraries
            .mapNotNull { it.downloads?.artifact?.path }
            .map { File(LauncherFiles.librariesDir, it).absolutePath }

        val clientJar = File(LauncherFiles.versionsDir, "${versionDetail.id}/${versionDetail.id}.jar").absolutePath
        val classPath = (libraries + clientJar).joinToString(separator)
        val nativesDir = LauncherFiles.getNativesDir(versionDetail.id)

        val gameArgs = runtimeManager.buildGameArguments(versionDetail, instanceDir, account.username, account.uuid)

        val command = mutableListOf<String>().apply {
            add(javaExecutablePath)
            add("-Xmx4G")
            add("-Djava.library.path=${nativesDir.absolutePath}")
            add("-cp")
            add(classPath)
            add(versionDetail.mainClass)
            addAll(gameArgs)
        }

        val process = ProcessBuilder(command)
            .directory(instanceDir)
            .inheritIO()
            .start()

        process.waitFor()
    }
}