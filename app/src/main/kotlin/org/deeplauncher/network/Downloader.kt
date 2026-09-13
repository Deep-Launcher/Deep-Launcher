package org.deeplauncher.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import org.deeplauncher.utils.sha1
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class Downloader(private val client: HttpClient) {
    suspend fun download(
        url: String,
        destination: File,
        maxAttempts: Int = 3,
        headers: Map<String, String> = emptyMap(),
        expectedSha1: String? = null
    ) {
        if (destination.exists() && (expectedSha1 == null || destination.sha1() == expectedSha1)) return

        destination.parentFile?.mkdirs()
        val partFile = File(destination.parentFile, "${destination.name}.part")

        DownloadTracker.begin(url, destination.name)

        var lastError: Exception? = null

        repeat(maxAttempts) { attempt ->
            partFile.delete()
            try {
                client.prepareGet(url) {
                    headers.forEach { (name, value) -> header(name, value) }
                }.execute { response ->
                    val status = response.status
                    if (!status.isSuccess()) {
                        throw IllegalStateException("Server returned status ${status.value} when downloading $url")
                    }

                    val expectedLength = response.contentLength()
                    val channel = response.bodyAsChannel()
                    var totalRead = 0L
                    var lastReported = 0L

                    partFile.outputStream().use { fileStream ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (!channel.isClosedForRead) {
                            val read = channel.readAvailable(buffer, 0, buffer.size)
                            if (read > 0) {
                                fileStream.write(buffer, 0, read)
                                totalRead += read
                                if (totalRead - lastReported >= REPORT_INTERVAL) {
                                    lastReported = totalRead
                                    DownloadTracker.update(url, totalRead, expectedLength ?: -1)
                                }
                            }
                        }
                        fileStream.flush()
                    }

                    DownloadTracker.update(url, totalRead, expectedLength ?: -1)

                    if (expectedLength != null && totalRead != expectedLength) {
                        throw IllegalStateException(
                            "Incomplete download: expected $expectedLength bytes, received $totalRead bytes."
                        )
                    }
                    if (totalRead == 0L) {
                        throw IllegalStateException("The download returned 0 bytes.")
                    }
                    if (expectedSha1 != null && partFile.sha1() != expectedSha1) {
                        throw IllegalStateException("Checksum mismatch for $url.")
                    }
                }
                partFile.copyTo(destination, overwrite = true)
                partFile.delete()
                DownloadTracker.end(url)
                return
            } catch (e: Exception) {
                lastError = e
                partFile.delete()
                println("Failed to download $url (attempt ${attempt + 1}/$maxAttempts): ${e.message}")
                DownloadTracker.fail(url, e.message ?: "unknown error")
                if (attempt < maxAttempts - 1) {
                    delay((500L * (attempt + 1)).milliseconds)
                }
            }
        }

        throw IllegalStateException("Failed to download $url after $maxAttempts attempts", lastError)
    }

    private companion object {
        const val REPORT_INTERVAL = 256_000L
    }
}