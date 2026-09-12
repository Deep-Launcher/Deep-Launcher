package org.deeplauncher.version

import io.ktor.client.call.*
import io.ktor.client.request.*
import org.deeplauncher.core.Endpoints
import org.deeplauncher.core.json
import org.deeplauncher.models.VersionManifest
import org.deeplauncher.network.client
import org.deeplauncher.utils.DiskCache

class VersionRepository(
    private val cache: DiskCache
) {
    suspend fun listVersions(useCache: Boolean = true): Result<VersionManifest> = runCatching {
        if (useCache) {
            val cachedJson = cache.get("version_manifest")
            if (cachedJson != null) {
                return@runCatching json.decodeFromString<VersionManifest>(cachedJson)
            }
        }

        val responseJson: String = client.get(Endpoints.VERSIONS_MANIFEST).body()
        cache.put("version_manifest", responseJson)

        json.decodeFromString<VersionManifest>(responseJson)
    }
}