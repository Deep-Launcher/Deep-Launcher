package org.deeplauncher.utils

import java.io.File
import java.util.zip.ZipInputStream

object ArchiveUtils {
    fun extractNativeLibraries(jarFile: File, nativesDir: File) {
        nativesDir.mkdirs()

        ZipInputStream(jarFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                val isNativeLib = !entry.isDirectory && (
                        name.endsWith(".so") || name.endsWith(".dll") ||
                                name.endsWith(".dylib") || name.endsWith(".jnilib")
                        )

                if (isNativeLib) {
                    val outFile = File(nativesDir, File(name).name)
                    outFile.outputStream().use { fos -> zis.copyTo(fos) }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    fun unzipTarGz(tarGzFile: File, targetDir: File) {
        if (!tarGzFile.exists() || tarGzFile.length() == 0L) {
            throw IllegalStateException("Missing or empty tar.gz file: ${tarGzFile.absolutePath}")
        }

        val process = ProcessBuilder("tar", "-xzf", tarGzFile.absolutePath, "-C", targetDir.absolutePath)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw IllegalStateException("Failed to extract tar.gz (exit code $exitCode): $output")
        }
    }

    fun unzip(zipFile: File, targetDir: File) {
        if (!zipFile.exists() || zipFile.length() == 0L) {
            throw IllegalStateException("Missing or empty zip file: ${zipFile.absolutePath}")
        }

        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    newFile.outputStream().use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}