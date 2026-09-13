package org.deeplauncher.ui

import javafx.animation.FadeTransition
import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.ParallelTransition
import javafx.animation.PauseTransition
import javafx.animation.ScaleTransition
import javafx.animation.Timeline
import javafx.animation.TranslateTransition
import javafx.application.Platform
import javafx.css.PseudoClass
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.CacheHint
import javafx.scene.Node
import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.shape.Polyline
import javafx.scene.shape.SVGPath
import javafx.stage.Modality
import javafx.stage.Popup
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.deeplauncher.account.AccountManager
import org.deeplauncher.core.LauncherFiles
import org.deeplauncher.instance.InstanceManager
import org.deeplauncher.network.DownloadEntry
import org.deeplauncher.network.DownloadTracker
import org.deeplauncher.version.VersionManager

class App(
    private val versionManager: VersionManager,
    private val instanceManager: InstanceManager,
    private val accountManager: AccountManager
) {

    @FXML
    private lateinit var settingsBtn: Button

    @FXML
    private lateinit var minimizeBtn: Button

    @FXML
    private lateinit var maximizeBtn: Button

    @FXML
    private lateinit var closeBtn: Button

    @FXML
    private lateinit var navHome: Button

    @FXML
    private lateinit var navDownloads: Button

    @FXML
    private lateinit var navLibrary: Button

    @FXML
    private lateinit var navFiles: Button

    @FXML
    private lateinit var addButton: Button

    @FXML
    private lateinit var accountBtn: Button

    @FXML
    private lateinit var libraryBtn: Button

    @FXML
    private lateinit var heroTitle: Label

    @FXML
    private lateinit var heroSub: Label

    @FXML
    private lateinit var quickThumbs: HBox

    @FXML
    private lateinit var seamLine: Polyline

    @FXML
    private lateinit var mainScroll: ScrollPane

    @FXML
    private lateinit var homePage: VBox

    @FXML
    private lateinit var thumb1: Button

    @FXML
    private lateinit var thumb2: Button

    @FXML
    private lateinit var thumb3: Button

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val navButtons by lazy { listOf(navHome, navDownloads, navLibrary, navFiles) }
    private var currentNav: Button? = null

    private val favorites = mutableSetOf<String>()
    private var searchField: TextField? = null
    private var cardsBox: FlowPane? = null

    // Caches populated off the FX thread, pages read from these instead of hitting disk.
    private var instancesCache: List<org.deeplauncher.models.MinecraftInstance> = emptyList()
    private var accountsCache: List<org.deeplauncher.models.Account> = emptyList()
    private var librarySearchDebounce: PauseTransition? = null

    private val downloadsPage: VBox by lazy { buildDownloadsPage() }
    private val libraryPage: VBox by lazy { buildLibraryPage() }
    private val accountsPage: VBox by lazy { buildAccountsPage() }
    private var accountsList: VBox? = null
    private var downloadsVisible = false

    // Shared popup for instance hover cards, avoids creating a new Popup on every refresh.
    private val instancePopupNameLabel = Label().apply {
        style = "-fx-font-family: 'Teko Semibold'; -fx-font-size: 17; -fx-text-fill: #d7e4e0;"
    }
    private val instancePopupVersionLabel = Label().apply {
        style = "-fx-font-family: 'Inter'; -fx-font-size: 10.5; -fx-text-fill: #9dbab5; " +
                "-fx-background-color: #071f29; -fx-border-color: #0a2731; -fx-border-radius: 9; " +
                "-fx-background-radius: 9; -fx-padding: 2 8 2 8;"
    }
    private val instancePopupCard = VBox(instancePopupNameLabel, instancePopupVersionLabel).apply {
        spacing = 3.0
        style = "-fx-background-color: #020c12; -fx-background-radius: 10; -fx-border-color: #0a2430; " +
                "-fx-border-radius: 10; -fx-padding: 9 12 9 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 12, 0, 0, 4);"
    }
    private val instancePopup = Popup().apply {
        content.add(instancePopupCard)
        isAutoHide = false
    }

    @FXML
    private fun initialize() {
        seamLine.points.setAll(
            0.0, 3.0, 90.0, 2.0, 140.0, 4.0, 220.0, 1.0, 300.0, 3.0, 410.0, 2.0,
            520.0, 4.0, 610.0, 1.0, 700.0, 3.0, 800.0, 2.0, 900.0, 4.0, 1000.0, 1.0,
            1100.0, 3.0, 1200.0, 2.0, 1290.0, 4.0, 1360.0, 3.0
        )

        navButtons.forEach { button -> button.pseudoClassStateChanged(SELECTED, false) }
        select(navHome)

        installTip(navHome, "Home")
        installTip(navDownloads, "Downloads")
        installTip(navLibrary, "Library")
        installTip(navFiles, "Files")
        installTip(addButton, "New Instance")
        installTip(accountBtn, "Account")

        listOf(thumb1, thumb2, thumb3).forEach { it.isFocusTraversable = false }

        // Subtle pop effect on hover/click for UI buttons
        listOf(
            navHome, navDownloads, navLibrary, navFiles,
            addButton, accountBtn, settingsBtn, libraryBtn
        ).forEach { installPop(it) }

        DownloadTracker.entries.addListener(ListChangeListener { _ ->
            if (downloadsVisible) refreshDownloadsBody()
        })

        // Pré-carrega as outras páginas e busca instâncias/contas fora da FX thread.
        downloadsPage
        libraryPage
        accountsPage
        reloadInstances()
        reloadAccounts()
    }

    // EFFECTS

    private fun animateScale(node: Node, target: Double, ms: Double) {
        ScaleTransition(Duration.millis(ms), node).apply {
            toX = target
            toY = target
            interpolator = Interpolator.EASE_BOTH
            play()
        }
    }

    /** Subtle scale pop */
    private fun installPop(node: Node, hoverScale: Double = 1.05, pressScale: Double = 0.93) {
        node.isCache = true
        node.cacheHint = CacheHint.SPEED
        node.onMouseEntered = EventHandler { animateScale(node, hoverScale, 140.0) }
        node.onMouseExited = EventHandler { animateScale(node, 1.0, 160.0) }
        node.onMousePressed = EventHandler { animateScale(node, pressScale, 70.0) }
        node.onMouseReleased = EventHandler { animateScale(node, hoverScale, 110.0) }
    }

    /** Gentle vertical lift */
    private fun installLift(node: Node, liftY: Double = -4.0, ms: Double = 160.0) {
        node.isCache = true
        node.cacheHint = CacheHint.SPEED
        node.onMouseEntered = EventHandler {
            TranslateTransition(Duration.millis(ms), node).apply { toY = liftY; interpolator = Interpolator.EASE_OUT; play() }
        }
        node.onMouseExited = EventHandler {
            TranslateTransition(Duration.millis(ms), node).apply { toY = 0.0; interpolator = Interpolator.EASE_OUT; play() }
        }
    }

    /** Tooltip with a short delay, anchored to the icon side instead of following the mouse */
    private fun installTip(node: Node, text: String) {
        val tip = Tooltip(text).apply {
            showDelay = Duration.millis(160.0)
            hideDelay = Duration.millis(120.0)
            isAutoFix = true
        }
        node.addEventHandler(MouseEvent.MOUSE_ENTERED) {
            val screen = node.localToScreen(0.0, 0.0) ?: return@addEventHandler
            tip.show(node, screen.x + node.boundsInLocal.width + 10, screen.y + node.boundsInLocal.height / 2 - 14)
        }
        node.addEventHandler(MouseEvent.MOUSE_EXITED) { tip.hide() }
    }

    /** Page switch with a crossfade instead of an instant swap */
    private fun switchPage(page: Region) {
        val current = mainScroll.content as? Region
        if (current === page) return
        if (current == null) {
            mainScroll.content = page
            page.opacity = 0.0
            FadeTransition(Duration.millis(180.0), page).apply { toValue = 1.0; play() }
            return
        }
        FadeTransition(Duration.millis(110.0), current).apply {
            toValue = 0.0
            setOnFinished {
                mainScroll.content = page
                page.opacity = 0.0
                FadeTransition(Duration.millis(200.0), page).apply { toValue = 1.0; play() }
            }
            play()
        }
    }

    /** Cascading entrance for the library cards */
    private fun animateCardEntrance(node: Node, index: Int) {
        node.opacity = 0.0
        node.translateY = 14.0
        val fade = FadeTransition(Duration.millis(260.0), node).apply { toValue = 1.0 }
        val slide = TranslateTransition(Duration.millis(260.0), node).apply { toY = 0.0 }
        ParallelTransition(fade, slide).apply {
            delay = Duration.millis(index * 45.0)
            interpolator = Interpolator.EASE_OUT
            play()
        }
    }

    private fun applyTexture(region: Region, name: String) {
        region.styleClass.removeIf { it.startsWith("tex-") }
        region.styleClass.add(textureClass(name))
    }

    private fun textureClass(name: String): String {
        return when (name.length % 3) {
            0 -> "tex-grass"
            1 -> "tex-dirt"
            else -> "tex-sculk"
        }
    }

    /** Reuses a single shared Popup instead of building a new one per instance/per refresh. */
    private fun installInstanceCardPopup(host: Region, instance: org.deeplauncher.models.MinecraftInstance) {
        host.onMouseEntered = EventHandler<MouseEvent> {
            animateScale(host, 1.06, 140.0)
            instancePopupNameLabel.text = instance.name
            instancePopupVersionLabel.text = instance.version
            val screen = host.localToScreen(0.0, 0.0) ?: return@EventHandler
            instancePopup.show(host.scene.window, screen.x + host.width + 8, screen.y - 2)
        }
        host.onMouseExited = EventHandler {
            animateScale(host, 1.0, 160.0)
            instancePopup.hide()
        }
    }

    // ============ CACHE RELOAD (off the FX thread) ============

    private fun reloadInstances(onLoaded: (() -> Unit)? = null) {
        scope.launch {
            instancesCache = withContext(Dispatchers.IO) { instanceManager.listInstances() }
            refreshHero()
            refreshQuickPlay()
            refreshThumbs()
            if (mainScroll.content === libraryPage) refreshLibraryBody()
            onLoaded?.invoke()
        }
    }

    private fun reloadAccounts(onLoaded: (() -> Unit)? = null) {
        scope.launch {
            accountsCache = withContext(Dispatchers.IO) { accountManager.listAccounts() }
            if (mainScroll.content === accountsPage) refreshAccounts()
            onLoaded?.invoke()
        }
    }

    private fun refreshThumbs() {
        val thumbs = listOf(thumb1, thumb2, thumb3)
        thumbs.forEachIndexed { index, thumb ->
            val instance = instancesCache.getOrNull(index)
            if (instance != null) {
                applyTexture(thumb, instance.name)
                installInstanceCardPopup(thumb, instance)
                thumb.cursor = Cursor.HAND
                thumb.setOnAction { showLibrary(instance.name) }
            } else {
                thumb.styleClass.removeIf { it.startsWith("tex-") }
                thumb.onMouseEntered = null
                thumb.onMouseExited = null
                thumb.cursor = Cursor.DEFAULT
                thumb.setOnAction(null)
            }
        }
    }

    private fun refreshHero() {
        val instances = instancesCache
        if (instances.isEmpty()) {
            heroTitle.text = "Welcome to Deep Launcher"
            heroSub.text = "Create your first instance and press play."
        } else {
            val first = instances.first()
            heroTitle.text = first.name
            heroSub.text = "${first.version}, Ready to launch!"
        }
    }

    private fun refreshQuickPlay() {
        val tiles = quickThumbs.children
        tiles.clear()
        instancesCache.take(MAX_QUICK_PLAY).forEach { tiles.add(quickTile(it)) }
        repeat((MAX_QUICK_PLAY - instancesCache.size).coerceAtLeast(0)) { tiles.add(emptyTile()) }
    }

    private fun quickTile(instance: org.deeplauncher.models.MinecraftInstance): Button {
        return Button().apply {
            styleClass.setAll("qp-thumb")
            applyTexture(this, instance.name)
            isFocusTraversable = false
            setOnAction { launchInstance(instance, this) }
        }.also { installInstanceCardPopup(it, instance) }
    }

    private fun emptyTile(): Button {
        return Button("+").apply {
            styleClass.setAll("qp-thumb", "empty")
            isFocusTraversable = false
            setOnAction { onAddInstance() }
            installPop(this, hoverScale = 1.08, pressScale = 0.9)
        }
    }

    @FXML
    fun onNavEntered() = Unit

    @FXML
    fun onNavExited() = Unit

    @FXML
    fun onNavHome() {
        select(navHome)
        downloadsVisible = false
        switchPage(homePage)
    }

    @FXML
    fun onNavDownloads() {
        select(navDownloads)
        showDownloads()
    }

    @FXML
    fun onNavLibrary() = showLibrary()

    @FXML
    fun onNavFiles() = showComingSoon("Files")

    @FXML
    fun onBrowseLibrary() = showLibrary()

    private fun select(button: Button) {
        if (button !== currentNav) {
            navButtons.forEach { it.pseudoClassStateChanged(SELECTED, it === button) }
            accountBtn.pseudoClassStateChanged(SELECTED, button === accountBtn)
            currentNav = button
        }
    }

    private fun showDownloads() {
        downloadsVisible = true
        switchPage(downloadsPage)
        refreshDownloadsBody()
    }

    private fun buildDownloadsPage(): VBox {
        return VBox(
            VBox(
                Label("Downloads").apply { styleClass.setAll("lib-title") },
                Label("Files being fetched for your instances.").apply { styleClass.setAll("lib-sub") }
            ).apply { spacing = 2.0 },
            VBox().apply { styleClass.setAll("dl-list") }
        ).apply {
            styleClass.setAll("dl-page")
        }
    }

    private fun refreshDownloadsBody() {
        val listBox = (downloadsPage.children[1] as VBox)
        listBox.children.clear()

        val active = DownloadTracker.entries
        if (active.isEmpty()) {
            listBox.children.add(Label("No downloads yet. Start or update an instance and progress will show here.").apply {
                styleClass.setAll("lib-empty")
            })
            return
        }

        active.take(30).forEach { entry ->
            listBox.children.add(downloadRow(entry))
        }
    }

    private fun downloadRow(entry: DownloadEntry): HBox {
        val arrow = SVGPath().apply {
            styleClass.setAll("dl-icon")
            content = "M12,4v12M6,10 12,16 18,10M4,20h16"
        }

        val name = Label(entry.fileName).apply { styleClass.setAll("dl-name") }
        val status = Label().apply {
            styleClass.setAll("dl-status")
            textProperty().bind(entry.status)
        }

        val bar = ProgressBar().apply {
            styleClass.setAll("dl-bar")
            progressProperty().bind(entry.progress)
        }

        return HBox(
            arrow,
            VBox(name, status).apply { styleClass.setAll("dl-info") },
            Region().apply { HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS) },
            bar
        ).apply {
            styleClass.setAll("dl-item")
        }
    }

    private fun buildLibraryPage(): VBox {
        val title = Label("Library").apply { styleClass.setAll("lib-title") }
        val sub = Label().apply { styleClass.setAll("lib-sub") }

        val search = TextField().apply {
            styleClass.setAll("lib-search")
            promptText = "Search instances…"
            textProperty().addListener { _, _, _ -> debounceLibrarySearch() }
        }
        searchField = search

        val header = HBox(
            VBox(title, sub).apply { spacing = 2.0 },
            Region().apply { HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS) },
            search
        ).apply {
            alignment = Pos.CENTER_LEFT
            spacing = 18.0
        }

        val grid = FlowPane().apply {
            hgap = 18.0
            vgap = 18.0
            styleClass.setAll("lib-grid")
        }
        cardsBox = grid

        val page = VBox(header, grid).apply {
            styleClass.setAll("lib-page")
        }
        return page
    }

    /** Delays the search refresh until typing pauses, instead of recomputing on every keystroke. */
    private fun debounceLibrarySearch() {
        librarySearchDebounce?.stop()
        librarySearchDebounce = PauseTransition(Duration.millis(250.0)).apply {
            setOnFinished { refreshLibraryBody() }
            play()
        }
    }

    private fun showLibrary(highlightName: String? = null) {
        select(navLibrary)
        downloadsVisible = false
        switchPage(libraryPage)
        if (highlightName != null) searchField?.text = highlightName else searchField?.text = ""
        refreshLibraryBody()
        if (highlightName != null) highlightCard(highlightName)
    }

    private fun refreshLibraryBody() {
        val instances = instancesCache
        val filter = searchField?.text?.trim()?.lowercase().orEmpty()

        (libraryPage.children[0] as HBox).let { header ->
            (header.children[0] as VBox).children[1].let { it as Label }.text =
                if (filter.isEmpty()) "${instances.size} instance(s)"
                else "${instances.count { it.name.lowercase().contains(filter) }} of ${instances.size} instance(s)"
        }

        cardsBox?.children?.clear()

        val shown = instances.filter { filter.isEmpty() || it.name.lowercase().contains(filter) }
        if (shown.isEmpty()) {
            cardsBox?.children?.add(Label("Nothing here yet. Press + to create your first instance.").apply {
                styleClass.setAll("lib-empty")
            })
            return
        }

        // Cascading entrance for the cards
        shown.forEachIndexed { index, instance ->
            val card = libraryCard(instance)
            cardsBox?.children?.add(card)
            animateCardEntrance(card, index)
        }
    }

    private fun libraryCard(instance: org.deeplauncher.models.MinecraftInstance): VBox {
        val icon = Region().apply {
            styleClass.setAll("lib-icon")
            applyTexture(this, instance.name)
        }

        val name = Label(instance.name).apply {
            styleClass.setAll("lib-name")
            maxWidth = 190.0
            isWrapText = true
        }
        val version = Label(instance.version).apply { styleClass.setAll("lib-version") }

        val heading = HBox(
            icon,
            VBox(name, version).apply { spacing = 6.0 }
        ).apply {
            alignment = Pos.CENTER_LEFT
            spacing = 12.0
        }
        installInstanceCardPopup(icon, instance)

        val fav = Button().apply {
            styleClass.setAll("lib-fav")
            isFocusTraversable = false
            graphic = SVGPath().apply {
                styleClass.setAll("lib-fav-glyph")
                content = "M12,17.27 18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z"
            }
            setOnAction {
                val nowSelected = pseudoClassStates.contains(SELECTED)
                pseudoClassStateChanged(SELECTED, !nowSelected)
                if (nowSelected) favorites.remove(instance.name) else favorites.add(instance.name)
            }
            if (instance.name in favorites) pseudoClassStateChanged(SELECTED, true)
        }

        val play = Button("Play").apply {
            styleClass.setAll("lib-play")
            setOnAction { launchInstance(instance, this) }
        }

        val delete = Button().apply {
            styleClass.setAll("lib-fav")
            isFocusTraversable = false
            graphic = SVGPath().apply {
                styleClass.setAll("lib-fav-glyph")
                content = "M2,6h20M9,6V3h6v3M6,6h12v16H6zM10,10v6M14,10v6"
            }
            setOnAction { confirmDeleteInstance(instance) }
        }

        // Pop effect on hover/click for the card buttons
        installPop(play, hoverScale = 1.04, pressScale = 0.94)
        installPop(fav, hoverScale = 1.12, pressScale = 0.88)
        installPop(delete, hoverScale = 1.12, pressScale = 0.88)

        val actions = HBox(fav, play, delete).apply { styleClass.setAll("lib-actions") }

        val card = VBox(heading, actions).apply {
            styleClass.setAll("lib-card")
        }
        // Animated lift of the whole card on hover
        installLift(card)
        return card
    }

    private fun launchInstance(
        instance: org.deeplauncher.models.MinecraftInstance,
        button: Button? = null
    ) {
        if (button?.isDisable == true) return

        val account = accountManager.getActiveAccount()
        if (account == null) {
            Alert(Alert.AlertType.INFORMATION).apply {
                title = "Account required"
                headerText = "No account selected"
                contentText = "Go to Accounts and create or select an account before playing."
                initOwner(stage())
            }.show()
            return
        }

        val label = button?.takeIf { !it.text.isNullOrEmpty() }
        val labelText = label?.text
        button?.isDisable = true
        if (label != null) label.text = "Launching…"
        val window = stage()
        window?.hide()
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    instanceManager.launchInstance(instance.name, account)
                }
            } catch (e: Exception) {
                Platform.runLater { showLaunchError(instance, e) }
            } finally {
                window?.show()
                window?.toFront()
                button?.let {
                    it.isDisable = false
                    if (label != null) label.text = labelText
                }
            }
        }
    }

    private fun showLaunchError(instance: org.deeplauncher.models.MinecraftInstance, e: Exception) {
        Alert(Alert.AlertType.ERROR).apply {
            title = "Launch failed"
            headerText = "Could not launch ${instance.name}"
            contentText = e.message ?: "Unknown error."
        }.show()
    }

    private fun confirmDeleteInstance(instance: org.deeplauncher.models.MinecraftInstance) {
        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "Delete instance"
            headerText = "Delete ${instance.name}?"
            contentText = "This will permanently remove the instance and all of its files."
            initOwner(stage())
            alertType = Alert.AlertType.CONFIRMATION
            buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)
        }
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            scope.launch {
                withContext(Dispatchers.IO) { instanceManager.deleteInstance(instance.name) }
                favorites.remove(instance.name)
                reloadInstances()
            }
        }
    }

    private fun highlightCard(name: String) {
        cardsBox?.children?.forEach { node ->
            if (node is VBox && node.children.size > 1) {
                val label = node.children[1] as? Label ?: return@forEach
                if (label.styleClass.contains("lib-name") && label.text == name) {
                    node.pseudoClassStateChanged(SELECTED, true)
                    Timeline(
                        KeyFrame(Duration.seconds(2.5), EventHandler { node.pseudoClassStateChanged(SELECTED, false) })
                    ).play()
                }
            }
        }
    }

    private fun buildAccountsPage(): VBox {
        val header = VBox(
            Label("Accounts").apply { styleClass.setAll("lib-title") },
            Label("Offline accounts used to join the game.").apply { styleClass.setAll("lib-sub") }
        ).apply { spacing = 2.0 }

        val listBox = VBox().apply { styleClass.setAll("acc-list") }
        accountsList = listBox

        val nickField = TextField().apply {
            styleClass.setAll("acc-field")
            promptText = "Offline nickname"
        }

        val status = Label().apply { styleClass.setAll("acc-status") }

        val addBtn = Button("Add Account").apply {
            styleClass.setAll("acc-add")
            setOnAction { addAccount(nickField, status) }
        }
        nickField.setOnAction { addBtn.fire() }

        val form = HBox(nickField, addBtn).apply { styleClass.setAll("acc-form") }

        return VBox(header, listBox, form, status).apply {
            styleClass.setAll("acc-page")
        }
    }

    private fun refreshAccounts() {
        val accounts = accountsCache
        val active = accountManager.getActiveAccount()

        (accountsPage.children[0] as VBox).let { header ->
            (header.children[1] as Label).text =
                if (accounts.isEmpty()) "Offline accounts used to join the game."
                else "${accounts.size} account(s) · active: ${active?.username ?: "none"}"
        }

        accountsList?.children?.clear()

        if (accounts.isEmpty()) {
            accountsList?.children?.add(Label("No accounts yet. Add an offline account below.").apply {
                styleClass.setAll("lib-empty")
            })
            return
        }

        accounts.forEach { account ->
            accountsList?.children?.add(accountRow(account, active?.uuid == account.uuid))
        }
    }

    private fun accountRow(account: org.deeplauncher.models.Account, isActive: Boolean): HBox {
        val avatar = Region().apply {
            styleClass.setAll("acc-avatar")
            applyTexture(this, account.username)
        }

        val name = Label(account.username).apply { styleClass.setAll("acc-name") }
        val uuid = Label(account.uuid).apply { styleClass.setAll("acc-uuid") }
        val type = Label("OFFLINE").apply { styleClass.setAll("acc-badge") }

        val activeBadge = Label(if (isActive) "ACTIVE" else "NOT ACTIVE").apply {
            styleClass.setAll("acc-badge")
            if (isActive) styleClass.add("acc-badge-active")
        }

        val select = Button().apply {
            styleClass.setAll("lib-fav")
            isFocusTraversable = false
            graphic = SVGPath().apply {
                styleClass.setAll("lib-fav-glyph")
                content = "M5,12l5,5L20,7"
            }
            setOnAction {
                accountManager.selectAccount(account.uuid)
                refreshAccounts()
            }
        }

        val delete = Button().apply {
            styleClass.setAll("lib-fav")
            isFocusTraversable = false
            graphic = SVGPath().apply {
                styleClass.setAll("lib-fav-glyph")
                content = "M2,6h20M9,6V3h6v3M6,6h12v16H6zM10,10v6M14,10v6"
            }
            setOnAction { confirmDeleteAccount(account) }
        }

        installPop(select, hoverScale = 1.12, pressScale = 0.88)
        installPop(delete, hoverScale = 1.12, pressScale = 0.88)

        val actions = HBox(select, delete).apply { styleClass.setAll("lib-actions") }

        return HBox(
            avatar,
            VBox(name, HBox(uuid, type).apply { spacing = 8.0 }).apply { spacing = 4.0 },
            Region().apply { HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS) },
            activeBadge,
            actions
        ).apply {
            styleClass.setAll("acc-item")
            pseudoClassStateChanged(SELECTED, isActive)
        }
    }

    private fun addAccount(nickField: TextField, status: Label) {
        val nick = nickField.text.trim()
        if (nick.isEmpty()) {
            status.text = "Nickname cannot be empty."
            return
        }
        scope.launch {
            try {
                val account = withContext(Dispatchers.IO) { accountManager.createAccount(nick) }
                nickField.text = ""
                status.text = "Account '${account.username}' created."
                reloadAccounts()
            } catch (e: IllegalArgumentException) {
                status.text = e.message
            }
        }
    }

    private fun confirmDeleteAccount(account: org.deeplauncher.models.Account) {
        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "Delete account"
            headerText = "Delete ${account.username}?"
            contentText = "This account will be removed from the launcher."
            initOwner(stage())
            alertType = Alert.AlertType.CONFIRMATION
            buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)
        }
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            scope.launch {
                withContext(Dispatchers.IO) { accountManager.deleteAccount(account.uuid) }
                reloadAccounts()
            }
        }
    }

    private val settingsOverlay: StackPane by lazy { buildSettingsOverlay() }
    private val settingsPanel: VBox by lazy { buildSettingsPanel() }
    private var settingsEscapeWired = false

    @FXML
    fun onSettingsClicked() = openSettings()

    private fun openSettings() {
        val scene = stage()?.scene ?: return
        val root = scene.root as? StackPane ?: return

        if (settingsOverlay.parent !== root) root.children.add(settingsOverlay)
        settingsOverlay.isVisible = true
        settingsOverlay.isManaged = true

        if (!settingsEscapeWired) {
            settingsEscapeWired = true
            scene.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
                if (settingsOverlay.isVisible && event.code == KeyCode.ESCAPE) {
                    closeSettings()
                    event.consume()
                }
            }
        }

        val dim = FadeTransition(Duration.millis(170.0), settingsOverlay).apply {
            fromValue = 0.0
            toValue = 1.0
        }
        dim.play()
    }

    private fun closeSettings() {
        settingsOverlay.isVisible = false
        settingsOverlay.isManaged = false
    }

    private fun buildSettingsOverlay(): StackPane {
        val backdrop = Region().apply {
            styleClass.setAll("ov-backdrop")
            onMouseClicked = EventHandler { closeSettings() }
        }
        val overlay = StackPane(backdrop, settingsPanel).apply {
            StackPane.setAlignment(settingsPanel, Pos.CENTER)
            isVisible = false
            isManaged = false
        }
        return overlay
    }

    private fun buildSettingsPanel(): VBox {
        // The SVGPath's raw shape bounds are slightly asymmetric with its stroke, which used to
        // throw off centering inside the button. Wrapping it in a fixed-size StackPane and
        // centering *that* instead fixes it for good.
        val closeGlyph = SVGPath().apply {
            styleClass.setAll("win-glyph")
            content = "M0,0l12,12M12,0L0,12"
        }
        val closeGraphic = StackPane(closeGlyph).apply {
            styleClass.setAll("ov-close-glyph-box")
        }

        val closeBtn = Button().apply {
            styleClass.setAll("ov-close")
            isFocusTraversable = false
            graphic = closeGraphic
            setOnAction { closeSettings() }
            installPop(this, hoverScale = 1.1, pressScale = 0.92)
        }

        val header = HBox(
            Label("Settings").apply { styleClass.setAll("ov-title") },
            Region().apply { HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS) },
            closeBtn
        ).apply {
            styleClass.setAll("ov-header")
            alignment = Pos.CENTER_LEFT
        }

        val content = VBox().apply { styleClass.setAll("set-content") }

        val navGeneral = sectionButton("General")
        val navDefaults = sectionButton("Default Options")
        val navDownloads = sectionButton("Downloads")
        val navAbout = sectionButton("About")
        val navButtons = listOf(navGeneral, navDefaults, navDownloads, navAbout)
        val pages = listOf(
            buildGeneralSection(),
            buildDefaultOptionsSection(),
            buildDownloadsSection(),
            buildAboutSection()
        )

        fun show(button: Button, page: VBox) {
            navButtons.forEach { it.pseudoClassStateChanged(SELECTED, it === button) }
            content.children.clear()
            content.children.add(page)
            page.opacity = 0.0
            FadeTransition(Duration.millis(160.0), page).apply { toValue = 1.0; play() }
        }

        navButtons.forEachIndexed { index, button ->
            button.setOnAction { show(button, pages[index]) }
        }
        show(navGeneral, pages[0])

        val sidebar = VBox().apply {
            styleClass.setAll("set-sidebar")
            alignment = Pos.TOP_CENTER
            children.addAll(navButtons)
        }

        val body = HBox(sidebar, content).apply {
            styleClass.setAll("set-body")
            alignment = Pos.TOP_LEFT
        }
        VBox.setVgrow(body, javafx.scene.layout.Priority.ALWAYS)
        HBox.setHgrow(content, javafx.scene.layout.Priority.ALWAYS)

        return VBox(header, body).apply {
            styleClass.setAll("ov-panel")
            prefWidth = 900.0
            prefHeight = 560.0
            maxWidth = 900.0
            maxHeight = 560.0
            minWidth = 680.0
            minHeight = 440.0
        }
    }

    private fun sectionButton(label: String): Button {
        return Button(label).apply {
            styleClass.setAll("set-nav")
            isFocusTraversable = false
            maxWidth = Double.MAX_VALUE
            alignment = Pos.CENTER_LEFT
        }
    }

    private fun settingRow(labelText: String, control: Node): HBox {
        return HBox(
            Label(labelText).apply { styleClass.setAll("set-label") },
            Region().apply { HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS) },
            control
        ).apply {
            styleClass.setAll("set-row")
            alignment = Pos.CENTER_LEFT
            spacing = 12.0
        }
    }

    private fun buildGeneralSection(): VBox {
        val language = ComboBox<String>().apply {
            styleClass.setAll("dialog-combo")
            items.addAll("English", "Português do Brasil")
            value = "English"
        }
        val animations = CheckBox("Interface animations").apply {
            styleClass.setAll("set-check")
            isSelected = true
        }
        val notifications = CheckBox("Notifications").apply {
            styleClass.setAll("set-check")
            isSelected = true
        }

        return VBox(
            Label("General").apply { styleClass.setAll("set-title") },
            Label("Appearance and language preferences.").apply { styleClass.setAll("set-sub") },
            settingRow("Language", language),
            settingRow("Animations", animations),
            settingRow("Notifications", notifications)
        ).apply {
            styleClass.setAll("set-page")
            spacing = 14.0
        }
    }

    private fun buildDefaultOptionsSection(): VBox {
        val jvm = TextArea(" -Xmx2G\n -XX:+UseG1GC").apply {
            styleClass.setAll("set-textarea")
            prefRowCount = 4
            prefColumnCount = 36
        }
        val maxRam = ComboBox<String>().apply {
            styleClass.setAll("dialog-combo")
            items.addAll((1..8).map { "$it GB" })
            value = "4 GB"
        }
        val javaPath = TextField().apply {
            styleClass.setAll("dialog-field")
            promptText = "Auto detect"
        }

        return VBox(
            Label("Default Options").apply { styleClass.setAll("set-title") },
            Label("Memory and JVM settings applied to every instance.").apply { styleClass.setAll("set-sub") },
            Label("JVM arguments").apply { styleClass.setAll("set-label") },
            jvm,
            settingRow("Max memory", maxRam),
            settingRow("Java executable", javaPath)
        ).apply {
            styleClass.setAll("set-page")
            spacing = 14.0
        }
    }

    private fun buildDownloadsSection(): VBox {
        val parallel = ComboBox<String>().apply {
            styleClass.setAll("dialog-combo")
            items.addAll((1..6).map { "$it parallel" })
            value = "4 parallel"
        }
        val resume = CheckBox("Resume interrupted downloads").apply {
            styleClass.setAll("set-check")
            isSelected = true
        }
        val dir = TextField().apply {
            styleClass.setAll("dialog-field")
            isEditable = false
            text = LauncherFiles.rootDir.absolutePath
        }

        return VBox(
            Label("Downloads").apply { styleClass.setAll("set-title") },
            Label("How files are fetched for your instances.").apply { styleClass.setAll("set-sub") },
            settingRow("Parallel downloads", parallel),
            settingRow("Resume", resume),
            settingRow("Storage folder", dir)
        ).apply {
            styleClass.setAll("set-page")
            spacing = 14.0
        }
    }

    private fun buildAboutSection(): VBox {
        val brand = Region().apply {
            styleClass.setAll("brand-mark-sculk")
        }
        return VBox(
            HBox(
                brand,
                VBox(
                    Label("Deep Launcher").apply { styleClass.setAll("set-title") },
                    Label("Version 0.1").apply { styleClass.setAll("set-sub") }
                ).apply {
                    spacing = 2.0
                    alignment = Pos.CENTER_LEFT
                }
            ).apply {
                styleClass.setAll("set-row")
                alignment = Pos.CENTER_LEFT
                spacing = 12.0
            },
            Label("A modern Minecraft launcher built with JavaFX and Kotlin.").apply {
                styleClass.setAll("set-muted")
            },
            Label("Join the community on Discord.").apply {
                styleClass.setAll("set-muted")
            }
        ).apply {
            styleClass.setAll("set-page")
            spacing = 14.0
        }
    }

    @FXML
    fun onAccountClicked() {
        select(accountBtn)
        downloadsVisible = false
        switchPage(accountsPage)
        refreshAccounts()
    }

    private fun showComingSoon(feature: String) {
        val alert = Alert(Alert.AlertType.INFORMATION).apply {
            title = feature
            headerText = "$feature is coming soon"
            contentText = "This area of the launcher is not implemented yet."
            initOwner(stage())
        }
        alert.show()
    }

    @FXML
    fun onMinimize() {
        stage()?.isIconified = true
    }

    @FXML
    fun onMaximize() {
        stage()?.let { it.isMaximized = !it.isMaximized }
    }

    @FXML
    fun onClose() {
        Platform.exit()
    }

    private fun stage(): Stage? {
        return settingsBtn.scene?.window as? Stage
    }

    @FXML
    fun onAddInstance() {
        val dialog = Stage(StageStyle.UTILITY).apply {
            initModality(Modality.WINDOW_MODAL)
            initOwner(stage())
            title = "New Instance"
            isResizable = false
        }

        val nameField = TextField().apply {
            text = "New Instance"
            styleClass.setAll("dialog-field")
        }

        val versionBox = ComboBox<String>().apply {
            styleClass.setAll("dialog-combo")
            promptText = "Select a version"
        }

        val progressBar = ProgressBar().apply {
            styleClass.setAll("dialog-progress")
            isVisible = false
            isManaged = false
        }

        val statusLabel = Label("Loading versions…").apply {
            styleClass.setAll("dialog-status")
            isWrapText = true
        }

        val cancelBtn = Button("Cancel").apply {
            styleClass.setAll("dialog-button")
            setOnAction { dialog.close() }
        }

        val createBtn = Button("Create").apply {
            styleClass.setAll("dialog-primary")
            isDefaultButton = true
            setOnAction {
                val name = nameField.text.trim()
                val version = versionBox.value
                if (name.isEmpty()) {
                    statusLabel.text = "Instance name cannot be empty."
                    return@setOnAction
                }
                if (version == null) {
                    statusLabel.text = "Select a version first."
                    return@setOnAction
                }
                isDisable = true
                text = "Creating…"
                progressBar.isVisible = true
                progressBar.isManaged = true
                progressBar.progress = -1.0
                statusLabel.text = "Checking existing instances…"
                scope.launch {
                    try {
                        val exists = withContext(Dispatchers.IO) {
                            instanceManager.listInstances().any { it.name == name }
                        }
                        if (exists) {
                            isDisable = false
                            text = "Create"
                            progressBar.isVisible = false
                            progressBar.isManaged = false
                            statusLabel.text = "An instance with the name '$name' already exists."
                            return@launch
                        }
                        statusLabel.text = "Downloading version files…"
                        withContext(Dispatchers.IO) {
                            instanceManager.createInstance(name, version) { completed, total, _ ->
                                Platform.runLater {
                                    progressBar.progress = if (total > 0) completed.toDouble() / total else 0.0
                                    statusLabel.text = "Downloading assets $completed/$total"
                                }
                            }
                        }
                        dialog.close()
                        reloadInstances()
                    } catch (e: Exception) {
                        isDisable = false
                        text = "Create"
                        progressBar.isVisible = false
                        progressBar.isManaged = false
                        statusLabel.text = e.message ?: "Failed to create the instance."
                    }
                }
            }
        }

        val buttons = HBox(cancelBtn, createBtn).apply {
            spacing = 10.0
            alignment = Pos.CENTER_RIGHT
        }

        val form = VBox(
            Label("Name").apply { styleClass.setAll("dialog-label") },
            nameField,
            Label("Version").apply { styleClass.setAll("dialog-label", "dialog-label-spaced") },
            versionBox,
            statusLabel,
            progressBar,
            buttons
        ).apply {
            spacing = 6.0
            padding = Insets(18.0)
            styleClass.setAll("dialog-root")
        }

        val scene = Scene(form, 380.0, 320.0)
        scene.stylesheets.add(javaClass.getResource("/ui/style.css").toExternalForm())
        dialog.scene = scene

        scope.launch {
            val versions = withContext(Dispatchers.IO) {
                versionManager.listVersions()
            }
            versionBox.items.addAll(versions.map { it.id })
            statusLabel.text = "${versionBox.items.size} versions available."
        }

        dialog.showAndWait()
    }

    private companion object {
        val SELECTED = PseudoClass.getPseudoClass("selected")
        const val MAX_QUICK_PLAY = 7
    }
}