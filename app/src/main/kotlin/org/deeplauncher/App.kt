package org.deeplauncher

import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import javafx.stage.WindowEvent
import org.deeplauncher.account.AccountManager
import org.deeplauncher.core.LauncherFiles
import org.deeplauncher.instance.InstanceManager
import org.deeplauncher.network.Downloader
import org.deeplauncher.network.client
import org.deeplauncher.runtime.RuntimeManager
import org.deeplauncher.version.VersionManager

class App : Application() {

    private val versionManager by lazy {
        VersionManager(
            downloader = Downloader(client),
            client = client,
            launcherFiles = LauncherFiles
        )
    }

    private val instanceManager by lazy {
        InstanceManager(versionManager, RuntimeManager(Downloader(client)))
    }

    private val accountManager by lazy { AccountManager() }

    override fun start(primaryStage: Stage) {
        val loader = FXMLLoader(javaClass.getResource("/ui/launcher.fxml"))
        loader.setControllerFactory { type ->
            if (type == org.deeplauncher.ui.App::class.java) {
                org.deeplauncher.ui.App(versionManager, instanceManager, accountManager)
            } else {
                type.getDeclaredConstructor().newInstance()
            }
        }

        val root = loader.load<Parent>()
        val scene = Scene(root, 1360.0, 768.0)

        scene.stylesheets.add(javaClass.getResource("/ui/style.css").toExternalForm())

        primaryStage.icons.add(Image(javaClass.getResource("/ui/sculk.png").toExternalForm()))
        primaryStage.title = "Deep Launcher"
        primaryStage.minWidth = 860.0
        primaryStage.minHeight = 540.0
        primaryStage.scene = scene

        // The stage is hidden while a game is running; keep the JVM alive so the
        // window can reappear when the game closes
        Platform.setImplicitExit(false)
        primaryStage.onCloseRequest = EventHandler { Platform.exit() }

        primaryStage.show()
    }
}

fun main() {
    Application.launch(App::class.java)
}