package org.deeplauncher.utils

import java.io.File
import java.security.MessageDigest

fun File.sha1(): String {
    val digest = MessageDigest.getInstance("SHA-1")
    inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}