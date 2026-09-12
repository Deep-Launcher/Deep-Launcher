package org.deeplauncher.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class Downloader(private val client: HttpClient) {
    suspend fun download(
        url: String,
        destination: File,
        maxAttempts: Int = 3,
        headers: Map<String, String> = emptyMap()
    ) {
        if (destination.exists()) return
        destination.parentFile?.mkdirs()

        var lastError: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                client.prepareGet(url) {
                    headers.forEach { (name, value) -> header(name, value) }
                }.execute { response ->
                    if (!response.status.isSuccess()) {
                        throw IllegalStateException("Server returned status ${response.status.value} when downloading $url")
                    }

                    val expectedLength = response.contentLength()
                    val channel = response.bodyAsChannel()
                    var totalRead = 0L

                    destination.outputStream().use { fileStream ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (!channel.isClosedForRead) {
                            val read = channel.readAvailable(buffer, 0, buffer.size)
                            if (read > 0) {
                                fileStream.write(buffer, 0, read)
                                totalRead += read
                            }
                        }
                        fileStream.flush()
                    }

                    if (expectedLength != null && totalRead != expectedLength) {
                        throw IllegalStateException(
                            "Incomplete download: expected $expectedLength bytes, received $totalRead bytes."
                        )
                    }
                    if (totalRead == 0L) {
                        throw IllegalStateException("The download returned 0 bytes.")
                    }
                }
                return
            } catch (e: Exception) {
                lastError = e
                destination.delete()
                println("Failed to download $url (attempt ${attempt + 1}/$maxAttempts): ${e.message}")
                if (attempt < maxAttempts - 1) {
                    delay((500L * (attempt + 1)).milliseconds)
                }
            }
        }

        throw IllegalStateException("Failed to download $url after $maxAttempts attempts", lastError)
    }
}