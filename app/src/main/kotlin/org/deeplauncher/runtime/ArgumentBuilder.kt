package org.deeplauncher.runtime

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.deeplauncher.core.LauncherFiles
import org.deeplauncher.models.VersionDetail
import org.deeplauncher.utils.OsUtils
import java.io.File

class ArgumentBuilder(
    private val version: VersionDetail,
    gameDir: File,
    username: String,
    uuid: String,
    private val activeFeatures: Map<String, Boolean> = emptyMap()
) {

    private val placeholders = mapOf(
        "auth_player_name" to username,
        "version_name" to version.id,
        "game_directory" to gameDir.absolutePath,
        "assets_root" to LauncherFiles.assetsDir.absolutePath,
        "assets_index_name" to (version.assetIndex.id),
        "auth_uuid" to uuid,
        "auth_access_token" to uuid,
        "--accessToken" to uuid,
        "--userType" to "legacy",
        "user_properties" to "{}"
    )

    fun buildGameArguments(): List<String> {
        val gameArgs = mutableListOf<String>()

        if (version.arguments?.game != null) {
            version.arguments.game.forEach { element ->
                parseArgumentElement(element, gameArgs)
            }
        } else if (!version.minecraftArguments.isNullOrBlank()) {
            val replaced = replacePlaceholders(version.minecraftArguments)
            gameArgs.addAll(replaced.split(" "))
        }

        return gameArgs
    }

    private fun parseArgumentElement(element: JsonElement, targetList: MutableList<String>) {
        when (element) {
            is JsonPrimitive -> {
                targetList.add(replacePlaceholders(element.content))
            }

            is JsonObject -> {
                if (evaluateRules(element)) {
                    when (val value = element["value"]) {
                        is JsonPrimitive -> targetList.add(replacePlaceholders(value.content))
                        is JsonArray -> value.forEach {
                            if (it is JsonPrimitive) {
                                targetList.add(replacePlaceholders(it.content))
                            }
                        }

                        else -> Unit
                    }
                }
            }

            else -> Unit
        }
    }

    private fun evaluateRules(obj: JsonObject): Boolean {
        val rules = obj["rules"] as? JsonArray ?: return true
        var allow = false

        for (ruleElement in rules) {
            val rule = ruleElement as? JsonObject ?: continue
            val action = (rule["action"] as? JsonPrimitive)?.content ?: continue
            var matches = true

            if (rule["os"] is JsonObject) {
                val osName = ((rule["os"] as JsonObject)["name"] as? JsonPrimitive)?.content
                if (osName != OsUtils.getOSName()) {
                    matches = false
                }
            }

            if (rule["features"] is JsonObject) {
                val ruleFeatures = rule["features"] as JsonObject
                for ((featureKey, featureVal) in ruleFeatures) {
                    val expected = (featureVal as? JsonPrimitive)?.content?.toBoolean() ?: false
                    val actual = activeFeatures[featureKey] ?: false
                    if (actual != expected) {
                        matches = false
                    }
                }
            }

            if (matches) {
                allow = (action == "allow")
            }
        }
        return allow
    }

    private fun replacePlaceholders(text: String): String {
        var result = text
        placeholders.forEach { (key, value) ->
            result = result.replace($$"${$$key}", value)
        }
        return result
    }
}