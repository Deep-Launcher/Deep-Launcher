package org.deeplauncher.core

import java.io.File

object LauncherFiles {
    val rootDir = File(System.getProperty("user.home"), ".deeplauncher")
    val cacheDir = File(rootDir, "cache")
    val instancesDir = File(rootDir, "instances")
    val librariesDir = File(rootDir, "libraries")
    val assetsDir = File(rootDir, "assets")

    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
        if (!instancesDir.exists()) instancesDir.mkdirs()
        if (!librariesDir.exists()) librariesDir.mkdirs()
        if (!assetsDir.exists()) assetsDir.mkdirs()
    }

    fun getNativesDir(versionId: String): File = File(rootDir, "versions/$versionId/natives")

    fun getJavaPathForMajorVersion(majorVersion: Int): String {
        val javaBinaryName = if (System.getProperty("os.name").lowercase().contains("win")) "java.exe" else "java"
        return File(rootDir, "runtimes/java-$majorVersion/bin/$javaBinaryName").absolutePath
    }
}