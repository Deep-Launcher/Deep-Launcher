package org.deeplauncher

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.deeplauncher.utils.DiskCache

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

class VersionManager(
    private val client: HttpClient,
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
}