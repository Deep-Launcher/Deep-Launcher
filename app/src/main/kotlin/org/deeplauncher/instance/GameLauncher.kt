package org.deeplauncher.instance

import org.deeplauncher.core.LauncherFiles
import org.deeplauncher.models.VersionDetail
import java.io.File

class GameLauncher {
    fun launchProcess(
        instanceDir: File,
        versionDetail: VersionDetail,
        javaExecutablePath: String,
        username: String
    ) {
        val separator = File.pathSeparator
        val libraries = versionDetail.libraries
            .mapNotNull { it.downloads?.artifact?.path }
            .map { File(LauncherFiles.librariesDir, it).absolutePath }

        val clientJar = File(LauncherFiles.versionsDir, "${versionDetail.id}/${versionDetail.id}.jar").absolutePath
        val classPath = (libraries + clientJar).joinToString(separator)
        val nativesDir = LauncherFiles.getNativesDir(versionDetail.id)

        val command = mutableListOf(
            javaExecutablePath,
            "-Xmx2G",
            "-Djava.library.path=${nativesDir.absolutePath}",
            "-cp", classPath,
            versionDetail.mainClass,

            "--username", username,
            "--version", versionDetail.id,
            "--gameDir", instanceDir.absolutePath,
            "--assetsDir", LauncherFiles.assetsDir.absolutePath,
            "--assetIndex", versionDetail.assetIndex.id,
            "--uuid", "00000000-0000-0000-0000-000000000000",
            "--accessToken", "0",
            "--userType", "msa"
        )

        val process = ProcessBuilder(command)
            .directory(instanceDir)
            .inheritIO()
            .start()

        process.waitFor()
    }
}