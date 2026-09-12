package org.deeplauncher.instance

import org.deeplauncher.core.LauncherFiles
import org.deeplauncher.core.json
import org.deeplauncher.models.MinecraftInstance
import java.io.File

class InstanceRepository {
    fun getInstanceDir(name: String): File {
        name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").let { sanitized ->
            return File(LauncherFiles.instancesDir, sanitized)
        }
    }

    fun createInstance(instance: MinecraftInstance) {
        val dir = getInstanceDir(instance.name)
        if (dir.exists()) throw IllegalArgumentException("An instance with the name '${instance.name}' already exists")

        dir.mkdirs()
        File(dir, "mods").mkdirs()
        File(dir, "saves").mkdirs()
        File(dir, "resourcepacks").mkdirs()

        saveInstance(instance)
    }

    fun loadInstance(name: String): MinecraftInstance? {
        val dir = getInstanceDir(name)
        val configFile = File(dir, "instance.json")

        if (!configFile.exists() || !configFile.isFile) return null

        return runCatching { json.decodeFromString<MinecraftInstance>(configFile.readText()) }.getOrNull()
    }

    fun saveInstance(instance: MinecraftInstance) {
        val dir = getInstanceDir(instance.name)
        if (!dir.exists()) dir.mkdirs()

        File(dir, "instance.json").writeText(json.encodeToString(MinecraftInstance.serializer(), instance))
    }

    fun deleteInstance(name: String): Boolean =
        getInstanceDir(name).let { if (it.exists()) it.deleteRecursively() else false }
}