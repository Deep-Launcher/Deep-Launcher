package org.deeplauncher.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    install(HttpRedirect)
    install(HttpTimeout) {
        requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 60_000
    }
    install(HttpRequestRetry) {
        maxRetries = 3
        retryOnExceptionIf { _, cause -> true }
        delayMillis { retry -> retry * 1000L }
    }
}