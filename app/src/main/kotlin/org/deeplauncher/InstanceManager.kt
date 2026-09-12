package org.deeplauncher

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class MinecraftInstance(
    val name: String,
    val version: String,
)

class InstanceManager(
    private val instancesDir: File
) {
    init {
        if (!instancesDir.exists()) instancesDir.mkdirs()
    }

    private fun getInstanceDir(name: String): File {
        name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").let { sanitized ->
            return File(instancesDir, sanitized)
        }
    }

    fun createInstance(name: String, version: String): MinecraftInstance {
        val dir = getInstanceDir(name)
        if (dir.exists()) throw IllegalArgumentException("An instance with the name '$name' already exists")

        dir.mkdirs()
        File(dir, "mods").mkdirs()
        File(dir, "saves").mkdirs()
        File(dir, "resourcepacks").mkdirs()

        val instance = MinecraftInstance(name = name, version = version)
        saveInstance(instance)

        return instance
    }

    fun listInstances(): List<MinecraftInstance> {
        val dirs = instancesDir.listFiles { file -> file.isDirectory } ?: return emptyList()
        return dirs.mapNotNull { dir -> loadInstance(dir.name) }
    }

    fun loadInstance(name: String): MinecraftInstance? {
        val dir = getInstanceDir(name)
        val configFile = File(dir, "instance.json")

        if (!configFile.exists() || !configFile.isFile) return null

        return runCatching {
            json.decodeFromString<MinecraftInstance>(configFile.readText())
        }.getOrNull()
    }

    fun saveInstance(instance: MinecraftInstance) {
        val dir = getInstanceDir(instance.name)
        if (!dir.exists()) dir.mkdirs()

        val configFile = File(dir, "instance.json")
        configFile.writeText(json.encodeToString(MinecraftInstance.serializer(), instance))
    }

    fun deleteInstance(name: String): Boolean {
        val dir = getInstanceDir(name)
        return if (dir.exists()) dir.deleteRecursively() else false
    }
}