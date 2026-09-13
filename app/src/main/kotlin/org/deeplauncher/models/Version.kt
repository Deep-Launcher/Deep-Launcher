package org.deeplauncher.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ArgumentsDetail(
    val game: List<JsonElement>? = null,
    val jvm: List<JsonElement>? = null
)

@Serializable
data class VersionDetail(
    val id: String,
    val mainClass: String,
    val downloads: VersionDownloads,
    val libraries: List<Library>,
    val assetIndex: AssetIndexInfo,
    val javaVersion: JavaVersionInfo? = null,
    val minecraftArguments: String? = null,
    val arguments: ArgumentsDetail? = null
)

@Serializable
data class VersionDownloads(
    val client: DownloadArtifact
)

@Serializable
data class DownloadArtifact(
    val sha1: String,
    val size: Long,
    val url: String,
    val path: String? = null
)

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