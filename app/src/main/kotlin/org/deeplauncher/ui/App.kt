package org.deeplauncher.ui

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label

class App {

    @FXML
    private lateinit var playBtn: Button

    @FXML
    private lateinit var downloadsBtn: Button

    @FXML
    private lateinit var settingsBtn: Button

    @FXML
    private lateinit var accountBtn: Button

    @FXML
    private lateinit var topbarTitle: Label

    @FXML
    private lateinit var accountStatus: Label

    @FXML
    private lateinit var instanceName: Label

    @FXML
    private lateinit var instanceMeta: Label

    @FXML
    private lateinit var navTip: Label

    @FXML
    private fun initialize() {
        instanceName.text = "Base Version"
        instanceMeta.text = "1.21 Saga · Java Edition"
        accountStatus.text = "No Account"
    }

    @FXML
    fun onPlayClicked() {
        topbarTitle.text = "Play"
    }

    @FXML
    fun onDownloadsClicked() {
        topbarTitle.text = "Downloads"
    }

    @FXML
    fun onSettingsClicked() {
        topbarTitle.text = "Settings"
    }

    @FXML
    fun onAccountClicked() {
        topbarTitle.text = "Account"
    }

    @FXML
    fun showPlayTip() = showTip(playBtn, "Play")

    @FXML
    fun showDownloadsTip() = showTip(downloadsBtn, "Downloads")

    @FXML
    fun showSettingsTip() = showTip(settingsBtn, "Settings")

    @FXML
    fun showAccountTip() = showTip(accountBtn, "Account")

    @FXML
    fun hideTip() {
        navTip.isVisible = false
    }

    private fun showTip(button: Button, text: String) {
        navTip.text = text
        navTip.applyCss()
        navTip.autosize()

        val point = button.localToScene(0.0, 0.0)
        val x = point.x + button.width + 8.0
        val y = point.y + (button.height - navTip.height) / 2.0

        navTip.isVisible = true
        navTip.relocate(x, y)
        navTip.toFront()
    }
}