package org.deeplauncher.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JavaVersionInfo(
    val component: String? = null,
    val majorVersion: Int
)

@Serializable
data class AdoptiumRelease(
    val binaries: List<Binary>
)

@Serializable
data class Binary(
    @SerialName("package")
    val pkg: PackageInfo,
    val os: String,
    val architecture: String
)

@Serializable
data class PackageInfo(
    val link: String,
    val name: String
)