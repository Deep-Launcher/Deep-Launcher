package org.deeplauncher

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import org.deeplauncher.core.LauncherFiles
import org.deeplauncher.instance.InstanceManager
import org.deeplauncher.network.Downloader
import org.deeplauncher.network.client
import org.deeplauncher.runtime.RuntimeManager
import org.deeplauncher.version.VersionManager

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
    val downloader = Downloader(client)
    val versionManager = VersionManager(
        downloader = downloader,
        client = client,
        launcherFiles = LauncherFiles,
    )
    val runtimeManager = RuntimeManager(downloader)
    val instanceManager = InstanceManager(versionManager, runtimeManager)

    Application.launch(App::class.java)
}