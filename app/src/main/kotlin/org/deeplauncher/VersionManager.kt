package org.deeplauncher

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.deeplauncher.utils.DiskCache
import java.io.File

@Serializable
enum class VersionType {
    @SerialName("release")
    RELEASE,

    @SerialName("snapshot")
    SNAPSHOT,

    @SerialName("old_beta")
    OLD_BETA,

    @SerialName("old_alpha")
    OLD_ALPHA
}

@Serializable
data class LatestVersion(
    val release: String,
    val snapshot: String
)

@Serializable
data class VersionInfo(
    val id: String,
    val type: VersionType,
    val url: String
)

@Serializable
data class VersionManifest(
    val latest: LatestVersion,
    val versions: List<VersionInfo>
)

@Serializable
data class VersionDetail(
    val id: String,
    val mainClass: String,
    val downloads: VersionDownloads,
    val libraries: List<Library>,
    val assetIndex: AssetIndexInfo
)

@Serializable
data class VersionDownloads(
    val client: DownloadArtifact
)

@Serializable
data class Library(
    val downloads: LibraryDownloads? = null,
    val name: String
)

@Serializable
data class LibraryDownloads(
    val artifact: DownloadArtifact? = null
)

@Serializable
data class AssetIndexInfo(
    val id: String,
    val sha1: String,
    val url: String
)

@Serializable
data class DownloadArtifact(
    val sha1: String,
    val size: Long,
    val url: String,
    val path: String? = null
)

@Serializable
data class AssetIndex(
    val objects: Map<String, AssetObject>
)

@Serializable
data class AssetObject(
    val hash: String,
    val size: Long
)

class VersionManager(
    private val client: HttpClient,
    private val launcherFiles: LauncherFiles,
    private val cache: DiskCache
) {
    private val manifestUrl = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

    suspend fun listVersions(useCache: Boolean = true): Result<VersionManifest> = runCatching {
        if (useCache) {
            val cachedJson = cache.get("version_manifest")
            if (cachedJson != null) {
                return@runCatching json.decodeFromString<VersionManifest>(cachedJson)
            }
        }

        val responseJson: String = client.get(manifestUrl).body()
        cache.put("version_manifest", responseJson)

        json.decodeFromString<VersionManifest>(responseJson)
    }

    suspend fun listVersionsByType(type: VersionType, useCache: Boolean = true): List<VersionInfo> {
        return listVersions(useCache).getOrNull()?.versions?.filter { version -> version.type == type } ?: emptyList()
    }

    suspend fun downloadVersion(versionInfo: VersionInfo) {
        val versionDetail: VersionDetail = client.get(versionInfo.url).body()

        val clientFile = File(launcherFiles.rootDir, "versions/${versionDetail.id}/${versionDetail.id}.jar")
        downloadFile(versionDetail.downloads.client.url, clientFile)

        for ((downloads) in versionDetail.libraries) {
            val artifact = downloads?.artifact ?: continue
            val libPath = artifact.path ?: continue
            val libFile = File(launcherFiles.librariesDir, libPath)

            downloadFile(artifact.url, libFile)
        }

        downloadAssets(versionDetail.assetIndex)
    }

    private suspend fun downloadAssets(assetIndexInfo: AssetIndexInfo) {
        val indexFile = File(launcherFiles.assetsDir, "indexes/${assetIndexInfo.id}.json")
        downloadFile(assetIndexInfo.url, indexFile)

        val assetIndex: AssetIndex = client.get(assetIndexInfo.url).body()
        val semaphore = Semaphore(16)

        coroutineScope {
            assetIndex.objects.values.map { asset ->
                launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        val hashPrefix = asset.hash.substring(0, 2)
                        val assetUrl = "https://resources.download.minecraft.net/$hashPrefix/${asset.hash}"
                        val destFile = File(launcherFiles.assetsDir, "objects/$hashPrefix/${asset.hash}")

                        println(assetUrl)
                        downloadFile(assetUrl, destFile)
                    }
                }
            }.joinAll()
        }
    }

    private suspend fun downloadFile(url: String, destination: File) {
        if (destination.exists()) return
        destination.parentFile?.mkdirs()

        client.prepareGet(url).execute { response ->
            val channel = response.bodyAsChannel()
            destination.outputStream().use { stream ->
                channel.toInputStream().copyTo(stream)
            }
        }
    }
}