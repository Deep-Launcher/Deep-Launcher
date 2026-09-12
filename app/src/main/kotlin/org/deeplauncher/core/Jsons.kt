package org.deeplauncher.core

import kotlinx.serialization.json.Json

val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}