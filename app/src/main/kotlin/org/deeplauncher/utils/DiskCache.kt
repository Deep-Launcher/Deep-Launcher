package org.deeplauncher.utils

import java.io.File

class DiskCache(
    private val cacheDir: File,
    private val defaultTTLMs: Long = 3_600_000
) {
    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    fun get(key: String, ttlMs: Long = defaultTTLMs): String? {
        val file = File(cacheDir, key.hashCode().toString())
        if (!file.exists()) return null

        val lastModified = file.lastModified()
        val isExpired = System.currentTimeMillis() - lastModified > ttlMs

        return if (isExpired) {
            file.delete()
            null
        } else file.readText()
    }

    fun put(key: String, content: String) {
        val file = File(cacheDir, key.hashCode().toString())
        file.writeText(content)
    }
}