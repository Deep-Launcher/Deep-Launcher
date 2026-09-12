package org.deeplauncher.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssetIndexInfo(
    val id: String,
    val sha1: String,
    val url: String
)

@Serializable
data class AssetIndex(
    val objects: Map<String, AssetObject>,
    val virtual: Boolean = false,
    @SerialName("map_to_resources")
    val mapToResources: Boolean = false
)

@Serializable
data class Assets(
    val objects: Map<String, AssetObject>,
    val virtual: Boolean = false,
    @SerialName("map_to_resources")
    val mapToResources: Boolean = false
)

@Serializable
data class AssetObject(
    val hash: String,
    val size: Long
)