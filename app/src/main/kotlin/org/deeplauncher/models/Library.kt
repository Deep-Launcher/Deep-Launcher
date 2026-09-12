package org.deeplauncher.models

import kotlinx.serialization.Serializable

@Serializable
data class Library(
    val downloads: LibraryDownloads? = null,
    val name: String,
    val natives: Map<String, String>? = null,
    val rules: List<LibraryRule>? = null
)

@Serializable
data class LibraryDownloads(
    val artifact: DownloadArtifact? = null,
    val classifiers: Map<String, DownloadArtifact>? = null
)

@Serializable
data class LibraryRule(
    val action: String,
    val os: LibraryOsRule? = null
)

@Serializable
data class LibraryOsRule(
    val name: String? = null
)