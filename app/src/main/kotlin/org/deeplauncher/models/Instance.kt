package org.deeplauncher.models

import kotlinx.serialization.Serializable

@Serializable
data class MinecraftInstance(
    val name: String,
    val version: String
)