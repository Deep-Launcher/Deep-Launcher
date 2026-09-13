package org.deeplauncher.version

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.deeplauncher.core.LauncherFiles
import org.deeplauncher.core.json
import org.deeplauncher.models.*
import org.deeplauncher.network.Downloader
import org.deeplauncher.utils.ArchiveUtils
import org.deeplauncher.utils.DiskCache
import org.deeplauncher.utils.OsUtils
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class VersionManager(
    private val downloader: Downloader,
    private val client: HttpClient,
    private val launcherFiles: LauncherFiles
) {
    private val repository = VersionRepository(DiskCache(launcherFiles.cacheDir))

    suspend fun getVersionInfo(id: String, useCache: Boolean = true): VersionInfo? {
        return repository.listVersions(useCache).getOrNull()?.versions?.find { it.id == id }
    }

    suspend fun listVersions(useCache: Boolean = true): List<VersionInfo> {
        val manifest = repository.listVersions(useCache).getOrNull() ?: return emptyList()
        return manifest.versions
    }

    suspend fun isAValidVersion(id: String, useCache: Boolean = true): Boolean {
        return getVersionInfo(id, useCache) != null
    }

    suspend fun ensureLegacyResources(versionDetail: VersionDetail, gameDir: File) {
        val assetIndexInfo = versionDetail.assetIndex
        val cachedIndexFile = File(launcherFiles.assetsDir, "indexes/${assetIndexInfo.id}.json")

        val assetIndex: AssetIndex = if (cachedIndexFile.exists()) {
            json.decodeFromString(cachedIndexFile.readText())
        } else {
            client.get(assetIndexInfo.url).body()
        }

        if (!assetIndex.virtual && !assetIndex.mapToResources) return

        val targetDir = if (assetIndex.mapToResources) {
            File(gameDir, "resources")
        } else {
            File(launcherFiles.assetsDir, "virtual/${assetIndexInfo.id}")
        }

        for ((originalPath, asset) in assetIndex.objects) {
            val hashPrefix = asset.hash.substring(0, 2)
            val sourceFile = File(LauncherFiles.assetsDir, "objects/$hashPrefix/${asset.hash}")
            val targetFile = File(targetDir, originalPath)

            if (!sourceFile.exists() || targetFile.exists()) continue

            targetFile.parentFile?.mkdirs()
            sourceFile.copyTo(targetFile, overwrite = true)
        }
    }

    fun getRequiredJavaMajorVersion(versionDetail: VersionDetail): Int {
        return versionDetail.javaVersion?.majorVersion ?: extractMajorVersion(versionDetail.id)
    }

    private fun extractMajorVersion(versionId: String): Int {
        if (!versionId.first().isDigit()) return 8

        val parts = versionId.split(".")
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: return 8
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0

        return when {
            minor < 17 -> 8
            minor in 17..19 -> 17
            minor == 20 && patch < 5 -> 17
            else -> 21
        }
    }

    suspend fun ensureLibraryFiles(versionDetail: VersionDetail) {
        val nativesDir = LauncherFiles.getNativesDir(versionDetail.id)

        for (library in versionDetail.libraries) {
            if (!OsUtils.isLibraryAllowedOnCurrentOs(library)) continue

            val artifact = library.downloads?.artifact
            if (artifact != null && artifact.path != null) {
                downloader.download(
                    artifact.url,
                    File(launcherFiles.librariesDir, artifact.path),
                    expectedSha1 = artifact.sha1
                )
            }

            downloadAndExtractNativesIfPresent(library, versionDetail.id, nativesDir)
        }
    }

    suspend fun downloadVersion(
        versionInfo: VersionInfo,
        onProgress: ((completed: Int, total: Int, url: String) -> Unit)? = null
    ) {
        val versionDetail: VersionDetail = client.get(versionInfo.url).body()

        val clientFile = File(launcherFiles.versionsDir, "${versionDetail.id}/${versionDetail.id}.jar")
        downloader.download(
            versionDetail.downloads.client.url,
            clientFile,
            expectedSha1 = versionDetail.downloads.client.sha1
        )

        val nativesDir = LauncherFiles.getNativesDir(versionDetail.id)

        for (library in versionDetail.libraries) {
            if (!OsUtils.isLibraryAllowedOnCurrentOs(library)) continue

            val artifact = library.downloads?.artifact
            val libPath = artifact?.path
            if (artifact != null && libPath != null) {
                val libFile = File(launcherFiles.librariesDir, libPath)
                downloader.download(artifact.url, libFile, expectedSha1 = artifact.sha1)
            }

            downloadAndExtractNativesIfPresent(library, versionDetail.id, nativesDir)
        }

        downloadAssets(versionDetail.assetIndex, onProgress)
    }

    private suspend fun downloadAndExtractNativesIfPresent(
        library: Library,
        versionId: String,
        nativesDir: File
    ) {
        val classifierKey = library.natives?.get(OsUtils.currentOsKey()) ?: return
        val nativeArtifact = library.downloads?.classifiers?.get(classifierKey) ?: return

        val jarName = nativeArtifact.path?.substringAfterLast("/")
            ?: "${library.name.replace(":", "-")}-$classifierKey.jar"

        val nativeJarFile = File(launcherFiles.cacheDir, "natives-jars/$versionId/$jarName")
        downloader.download(nativeArtifact.url, nativeJarFile, expectedSha1 = nativeArtifact.sha1)
        ArchiveUtils.extractNativeLibraries(nativeJarFile, nativesDir)
    }

    private suspend fun downloadAssets(
        assetIndexInfo: AssetIndexInfo,
        onProgress: ((completed: Int, total: Int, url: String) -> Unit)? = null
    ) {
        val indexFile = File(launcherFiles.assetsDir, "indexes/${assetIndexInfo.id}.json")
        downloader.download(assetIndexInfo.url, indexFile, expectedSha1 = assetIndexInfo.sha1)

        val assetIndex: AssetIndex = client.get(assetIndexInfo.url).body()
        val totalAssets = assetIndex.objects.size
        val completedCount = AtomicInteger(0)
        val semaphore = Semaphore(16)
        val failedAssets = java.util.concurrent.ConcurrentLinkedQueue<String>()

        coroutineScope {
            assetIndex.objects.values.map { asset ->
                launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        val hashPrefix = asset.hash.substring(0, 2)
                        val assetUrl = "https://resources.download.minecraft.net/$hashPrefix/${asset.hash}"
                        val destFile = File(launcherFiles.assetsDir, "objects/$hashPrefix/${asset.hash}")

                        try {
                            downloader.download(assetUrl, destFile, expectedSha1 = asset.hash)
                        } catch (e: Exception) {
                            failedAssets.add(assetUrl)
                            println("Warning: failed to download asset $assetUrl (${e.message})")
                        }

                        val current = completedCount.incrementAndGet()
                        if (onProgress != null) onProgress(current, totalAssets, assetUrl)
                    }
                }
            }.joinAll()
        }

        if (failedAssets.isNotEmpty()) {
            throw IllegalStateException(
                "${failedAssets.size} out of $totalAssets assets could not be downloaded after retry attempts. " +
                        "Run the download again: assets that have already finished will not be re-downloaded."
            )
        }
    }
}