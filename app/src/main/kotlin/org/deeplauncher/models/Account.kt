package org.deeplauncher.models

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val uuid: String,
    val username: String,
    val type: String = "offline"
)