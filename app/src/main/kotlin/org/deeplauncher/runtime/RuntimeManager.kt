package org.deeplauncher.runtime

import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.deeplauncher.core.Endpoints
import org.deeplauncher.network.Downloader
import org.deeplauncher.utils.ArchiveUtils
import org.deeplauncher.utils.OsUtils
import java.io.File

class RuntimeManager(private val downloader: Downloader) {

    suspend fun downloadAndExtractJava(
        javaVersion: Int,
        outputDir: File
    ): File = withContext(Dispatchers.IO) {
        val os = OsUtils.getOSName()
        val arch = OsUtils.getArchName()
        val extension = if (os == "windows") "zip" else "tar.gz"

        val binaryUrl = Endpoints.adoptium(javaVersion, os, arch)
        outputDir.mkdirs()
        val archiveFile = File(outputDir, "openjdk-$javaVersion.$extension")

        downloader.download(
            url = binaryUrl,
            destination = archiveFile,
            headers = mapOf(
                HttpHeaders.Accept to "*/*",
                HttpHeaders.UserAgent to "DeepLauncher/1.0 (+https://github.com/deeplauncher)"
            )
        )

        println("Extracting Java runtime...")
        try {
            if (archiveFile.name.endsWith(".zip")) ArchiveUtils.unzip(archiveFile, outputDir)
            else ArchiveUtils.unzipTarGz(archiveFile, outputDir)
        } catch (e: Exception) {
            archiveFile.delete()
            throw e
        }

        val jdkDir = findJdkDirectory(outputDir)
            ?: throw IllegalStateException("Failed to locate binaries in: ${outputDir.absolutePath}.")

        val normalizedJdkDir = if (jdkDir != outputDir) {
            flattenDirectory(jdkDir, outputDir)
            outputDir
        } else jdkDir

        archiveFile.delete()
        return@withContext normalizedJdkDir
    }

    private fun flattenDirectory(source: File, target: File) {
        source.listFiles()?.forEach { file ->
            val destination = File(target, file.name)
            if (destination.exists()) destination.deleteRecursively()
            if (!file.renameTo(destination)) {
                file.copyRecursively(destination, overwrite = true)
                file.deleteRecursively()
            }
        }
        var current = source
        while (current != target && current.exists()) {
            val parent = current.parentFile
            current.deleteRecursively()
            if (parent == null || parent == target) break
            current = parent
        }
    }

    private fun findJdkDirectory(dir: File): File? =
        if (File(dir, "bin").exists()) dir
        else dir.listFiles()?.firstOrNull { file -> file.isDirectory && File(file, "bin").exists() }
}