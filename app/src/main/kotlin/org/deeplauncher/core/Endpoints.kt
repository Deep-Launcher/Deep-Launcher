package org.deeplauncher.core

object Endpoints {
    const val VERSIONS_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    const val MINECRAFT_RESOURCES = "https://resources.download.minecraft.net"

    fun adoptium(javaVersion: Int, os: String, arch: String) =
        "https://api.adoptium.net/v3/binary/latest/$javaVersion/ga/$os/$arch/jdk/hotspot/normal/eclipse"
}