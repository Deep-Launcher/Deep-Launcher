package org.deeplauncher.utils

import org.deeplauncher.models.Library

object OsUtils {
    fun isLibraryAllowedOnCurrentOs(library: Library): Boolean {
        val rules = library.rules ?: return true
        var allowed = false

        for ((action, os) in rules) {
            val osName = os?.name
            val matchesOs = osName == null || osName == currentOsKey()
            if (matchesOs) {
                allowed = action == "allow"
            }
        }

        return allowed
    }

    fun currentOsKey(): String {
        val name = System.getProperty("os.name").lowercase()
        return when {
            name.contains("win") -> "windows"
            name.contains("mac") -> "osx"
            else -> "linux"
        }
    }

    fun getOSName(): String {
        val name = System.getProperty("os.name").lowercase()
        return when {
            name.contains("win") -> "windows"
            name.contains("mac") -> "mac"
            else -> "linux"
        }
    }

    fun getArchName(): String {
        val arch = System.getProperty("os.arch").lowercase()
        return if (arch.contains("aarch64") || arch.contains("arm64")) "aarch64" else "x64"
    }
}