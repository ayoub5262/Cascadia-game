package gui

import service.RootService
import service.Refreshable
import service.network.ConnectionState
import tools.aqua.bgw.animation.ScaleAnimation
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.CheckBox
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual

/**
 * The join game scene of the game.
 *
 * Lets a player connect to an existing lobby by its game id, configure their own player
 * (human or bot), and then wait for the host to start the game. The scoring cards are not
 * shown here because the joining player receives them from the host via the GameInitMessage.
 *
 * @constructor Creates a new Join Game Scene with the specified rootService.
 *
 * @param rootService The [RootService] that manages the game state.
 */
class JoinGameScene(private val rootService: RootService) : MenuScene(1920, 1080), Refreshable {

    private val playerTypeOptions = listOf("Human", "Random Bot", "Strong Bot")
    private var spinning = false

    private val crestLabel = ThemeLayout.crestLabel(Theme.Icons.JOIN)
    val titleLabel = Label(width = 320, height = 80, text = "JOIN GAME",
        font = Theme.titleFont, alignment = Alignment.CENTER)
    private val titleDivider = ThemeLayout.divider(300)

    val gameIdLabel = Label(width = 300, height = 34, text = "GAME ID", font = Theme.headerFont,
        alignment = Alignment.CENTER_LEFT)
    val gameIdInput = Theme.textField(width = 280, height = 44, text = "").apply { prompt = "#8X2F9A" }
    val connectingSpinner = Label(width = 30, height = 30, text = "",
        visual = ColorVisual(Theme.gold).apply { style.borderRadius = BorderRadius(15.0) }
    ).apply { isVisible = false }

    val connectRow = Pane<StaticComponentView<*>>(width = 320, height = 46).apply {
        gameIdInput.posX = 0.0; gameIdInput.posY = 0.0
        connectingSpinner.posX = 296.0; connectingSpinner.posY = 7.0
        add(gameIdInput); add(connectingSpinner)
    }

    val statusLabel = Label(width = 480, height = 26, text = "",
        font = Theme.bodyFont.copy(size = 15), alignment = Alignment.CENTER_LEFT)

    val playersTitle = Label(width = 300, height = 34, text = "PLAYER", font = Theme.headerFont,
        alignment = Alignment.CENTER_LEFT)

    val p1Name = Theme.textField(width = 280, height = 44, text = "Player 1")
    val p1Type = playerTypeButton()

    val playersGrid = ThemeLayout.createGrid(columns = 2, rows = 1, spacing = 16.0,
        (0 to 0) to p1Name, (1 to 0) to p1Type
    )

    val botSettingsTitle = Label(width = 300, height = 34, text = "BOT SETTINGS",
        font = Theme.headerFont, alignment = Alignment.CENTER_LEFT)
    val simSpeedCheck = CheckBox(width = 250, height = 30, text = "Fast Simulation Speed",
        font = Theme.bodyFont, alignment = Alignment.CENTER_LEFT)

    val botGrid = ThemeLayout.createGrid(columns = 1, rows = 1, spacing = 10.0,
        (0 to 0) to simSpeedCheck
    )

    val backButton = Theme.backButton(width = 230, height = 64, scene = this)
    val connectButton = Theme.primaryButton(width = 230, height = 64, text = "Connect", scene = this)

    val bottomRow = Pane<StaticComponentView<*>>(width = 480, height = 64).apply {
        backButton.posX = 0.0; backButton.posY = 0.0
        connectButton.posX = 250.0; connectButton.posY = 0.0
        add(backButton)
        add(connectButton)
    }

    var menuContainer: Pane<StaticComponentView<*>>? = null

    init {
        background = ImageVisual("background.png")
        backgroundOpacity = 1.0

        rebuild()

        connectButton.onMouseClicked = { connect() }

        rootService.networkService.addRefreshable(this)
    }

    private fun hasBots(): Boolean = p1Type.text != "Human"

    private fun rebuild() {
        menuContainer?.let {
            ThemeLayout.clearCard(it)
            removeComponents(it)
        }

        val components = mutableListOf<StaticComponentView<*>>(
            crestLabel, titleLabel, titleDivider, gameIdLabel, connectRow
        )
        if (statusLabel.text.isNotEmpty()) components.add(statusLabel)
        components.add(playersTitle)
        components.add(playersGrid)
        if (hasBots()) {
            components.add(botSettingsTitle)
            components.add(botGrid)
        }
        components.add(bottomRow)

        val newContainer = ThemeLayout.cardContainer(
            posX = 0, posY = 0, spacing = 16.0,
            fill = Theme.greenPanel,
            components = components,
            style = ThemeLayout.CardStyle(
                centered = setOf(crestLabel, titleLabel, titleDivider),
                topGap = 26.0, bottomGap = 32.0
            )
        )
        newContainer.posX = (1920.0 - newContainer.width) / 2.0
        newContainer.posY = (1080.0 - newContainer.height) / 2.0

        menuContainer = newContainer
        addComponents(newContainer)
    }

    private fun connect() {
        val sessionID = gameIdInput.text.trim()
        if (sessionID.isBlank()) {
            statusLabel.text = "Bitte eine Game-ID eingeben"
            rebuild()
            return
        }

        val name = resolveJoinName()
        setConnecting(true)
        statusLabel.text = "Verbinde mit $sessionID..."
        rebuild()

        try {
            rootService.networkService.joinGame(secret = "wildlife", name = name, sessionID = sessionID)
        } catch (e: IllegalStateException) {
            statusLabel.text = "Verbindung fehlgeschlagen: ${e.message}"
            setConnecting(false)
            rebuild()
        } catch (e: IllegalArgumentException) {
            statusLabel.text = "Verbindung fehlgeschlagen: ${e.message}"
            setConnecting(false)
            rebuild()
        }
    }

    private fun resolveJoinName(): String = when (p1Type.text) {
        "Random Bot" -> "RANDOMBOT 1"
        "Strong Bot" -> "STRONGBOT 1"
        else -> p1Name.text.ifBlank { "Guest" }
    }

    override fun refreshConnectionState(state: ConnectionState) {
        when (state) {
            ConnectionState.WAITING_FOR_INIT -> {
                setConnecting(false)
                connectButton.isDisabled = true
                statusLabel.text = "Verbunden! Warte auf den Host..."
                rebuild()
            }
            ConnectionState.DISCONNECTED -> setConnecting(false)
            else -> {}
        }
    }

    private fun setConnecting(connecting: Boolean) {
        connectButton.isDisabled = connecting
        gameIdInput.isDisabled = connecting
        p1Name.isDisabled = connecting
        p1Type.isDisabled = connecting
        connectingSpinner.isVisible = connecting
        if (connecting) startSpinner()
    }

    private fun startSpinner() {
        if (spinning) return
        spinning = true
        pulse()
    }

    private fun pulse() {
        if (!connectingSpinner.isVisible) {
            spinning = false
            return
        }
        val up = ScaleAnimation(
            componentView = connectingSpinner, toScaleX = 1.3, toScaleY = 1.3, duration = 400
        ).apply {
            onFinished = {
                val down = ScaleAnimation(
                    componentView = connectingSpinner, toScaleX = 1.0, toScaleY = 1.0, duration = 400
                ).apply {
                    onFinished = { pulse() }
                }
                playAnimation(down)
            }
        }
        playAnimation(up)
    }

    private fun playerTypeButton() = Theme.chipButton(
        width = 150, height = 42, text = "Human", scene = this
    ).apply {
        onMouseClicked = {
            text = playerTypeOptions[(playerTypeOptions.indexOf(text) + 1) % playerTypeOptions.size]
            p1Name.isDisabled = text != "Human"
            rebuild()
        }
    }
}
