package org.deeplauncher

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import kotlinx.serialization.json.Json
import org.deeplauncher.utils.DiskCache
import java.io.File

val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

data class LauncherFiles(
    val rootDir: File,
    val cacheDir: File,
    val instancesDir: File,
    val librariesDir: File,
    val assetsDir: File
)

fun setupFiles(): LauncherFiles {
    val root = File(System.getProperty("user.home"), ".deeplauncher")

    val cache = File(root, "cache")
    val assets = File(root, "assets")
    val assetsIndexes = File(assets, "indexes")
    val assetsObjects = File(assets, "objects")
    val libraries = File(root, "libraries")
    val instances = File(root, "instances")

    assetsIndexes.mkdirs()
    assetsObjects.mkdirs()
    libraries.mkdirs()
    instances.mkdirs()

    return LauncherFiles(
        rootDir = root,
        cacheDir = cache,
        instancesDir = instances,
        librariesDir = libraries,
        assetsDir = assets
    )
}

class App : Application() {

    override fun start(primaryStage: Stage) {
        val root = FXMLLoader.load<Parent>(javaClass.getResource("/ui/launcher.fxml"))
        val scene = Scene(root, 1000.0, 640.0)

        scene.stylesheets.add(javaClass.getResource("/ui/style.css").toExternalForm())

        primaryStage.icons.add(Image(javaClass.getResource("/ui/sculk.png").toExternalForm()))
        primaryStage.title = "Deep Launcher"
        primaryStage.minWidth = 860.0
        primaryStage.minHeight = 540.0
        primaryStage.scene = scene
        primaryStage.show()
    }
}

fun main() {
    val client = HttpClient(CIO) { install(ContentNegotiation) { json } }

    val launcherFiles = setupFiles()
    val versionManager = VersionManager(client = client, cache = DiskCache(launcherFiles.cacheDir))

    Application.launch(App::class.java)
}