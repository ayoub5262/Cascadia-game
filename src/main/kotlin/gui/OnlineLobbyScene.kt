package gui

import entity.BotSpeed
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.CheckBox
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.MenuScene
import service.RootService
import tools.aqua.bgw.visual.ImageVisual
import service.Refreshable
import service.network.ConnectionState
import kotlin.random.Random


/**
 * The online lobby scene of the game.
 *
 * @constructor Creates a new Online Lobby Scene with the specified rootService.
 *
 * @param rootService The [RootService] that manages the game state.
 */
class OnlineLobbyScene(private val rootService: RootService) : MenuScene(1920, 1080), Refreshable {

    private val playerTypeOptions = listOf("Human", "Random Bot", "Strong Bot")
    private val scoringOptions = listOf("Random", "A", "B")
    private val maxBots = 3

    private val playerRowWidth = 452.0
    private val columnSpacing = 16.0
    private val columnGap = 48.0

    private data class BotRow(
        val nameField: TextField,
        val typeBtn: Theme.IconButton,
        val removeBtn: Theme.IconButton
    )

    private val botRows = mutableListOf<BotRow>()

    private val crestLabel = ThemeLayout.crestLabel(Theme.Icons.ONLINE)
    val titleLabel = Label(width = 400, height = 80, text = "ONLINE GAME",
        font = Theme.titleFont, alignment = Alignment.CENTER)
    private val titleDivider = ThemeLayout.divider(320)
    val subtitleLabel = Label(width = 480, height = 44, text = "",
        font = Theme.bodyFont, alignment = Alignment.CENTER)

    val playersTitle = Label(width = 452, height = 34, text = "PLAYERS", font = Theme.headerFont,
        alignment = Alignment.CENTER)

    val p1Name = Theme.textField(width = 230, height = 42, text = "Player 1")
    val p1Type = hostTypeButton()
    val p1HostLabel = Label(
        width = 70, height = 42, text = "Host", font = Theme.headerFont.copy(size = 18),
        alignment = Alignment.CENTER_LEFT
    )

    val invitePlayerBtn = Theme.chipButton(
        width = 150, height = 46, text = "Add Bot", icon = Theme.Icons.USERPLUS_CREAM, scene = this
    )

    val scoringTitle = Label(width = 714, height = 34, text = "SCORING CARDS", font = Theme.headerFont,
        alignment = Alignment.CENTER)

    val bearLabel = Label(width = 130, height = 30, text = "Bear", font = Theme.bodyFont, alignment = Alignment.CENTER)
    val bearCombo = scoringButton()

    val foxLabel = Label(width = 130, height = 30, text = "Fox", font = Theme.bodyFont, alignment = Alignment.CENTER)
    val foxCombo = scoringButton()

    val hawkLabel = Label(width = 130, height = 30, text = "Hawk", font = Theme.bodyFont, alignment = Alignment.CENTER)
    val hawkCombo = scoringButton()

    val salmonLabel = Label(width = 130, height=30, text="Salmon", font = Theme.bodyFont, alignment = Alignment.CENTER)
    val salmonCombo = scoringButton()

    val elkLabel = Label(width = 130, height = 30, text = "Elk", font = Theme.bodyFont, alignment = Alignment.CENTER)
    val elkCombo = scoringButton()

    val scoringGrid = ThemeLayout.createGrid(columns = 5, rows = 2, spacing = 16.0,
        (0 to 0) to bearLabel, (0 to 1) to bearCombo,
        (1 to 0) to foxLabel, (1 to 1) to foxCombo,
        (2 to 0) to hawkLabel, (2 to 1) to hawkCombo,
        (3 to 0) to salmonLabel, (3 to 1) to salmonCombo,
        (4 to 0) to elkLabel, (4 to 1) to elkCombo
    )

    val botSettingsTitle = Label(width = 452, height = 34, text = "BOT SETTINGS",
        font = Theme.headerFont, alignment = Alignment.CENTER)
    val simSpeedCheck = CheckBox(width = 250, height = 30, text = "Fast Simulation Speed",
        font = Theme.bodyFont, alignment = Alignment.CENTER_LEFT)

    val botGrid = ThemeLayout.createGrid(columns = 1, rows = 1, spacing = 10.0,
        (0 to 0) to simSpeedCheck
    )

    val backButton = Theme.backButton(width = 230, height = 64, scene = this)
    val startButton = Theme.primaryButton(width = 230, height = 64, text = "Start Game",
        icon = Theme.Icons.PLAY_CREAM, scene = this)

    val bottomRow = Pane<StaticComponentView<*>>(width = 700, height = 64).apply {
        backButton.posX = 0.0; backButton.posY = 0.0
        add(backButton)
        add(startButton)
    }

    var menuContainer: Pane<StaticComponentView<*>>? = null
    private var playersBlock: Pane<StaticComponentView<*>>? = null
    private var leftColumn: Pane<StaticComponentView<*>>? = null
    private var rightColumn: Pane<StaticComponentView<*>>? = null
    private var columnsRow: Pane<StaticComponentView<*>>? = null

    init {
        background = ImageVisual("background.png")
        backgroundOpacity = 1.0

        refreshBotNames()
        rebuild()

        invitePlayerBtn.onMouseClicked = {
            if (botRows.size < maxBots) {
                addBot()
                refreshBotNames()
                rebuild()
            }
        }

        startButton.onMouseClicked = {

            refreshBotNames()
            val playerName = p1Name.text.ifBlank { "Host" }
            val scoringCards = selectedScoringCards()
            val botPlayers = selectedBotNames()

            val botSpeed = if (simSpeedCheck.isChecked) {
                BotSpeed.FAST
            } else {
                BotSpeed.SLOW
            }


            when (rootService.networkService.connectionState) {
                ConnectionState.DISCONNECTED -> {
                    rootService.networkService.hostGame(
                        secret = "wildlife",
                        name = playerName,
                        sessionID = null,
                        scoringCards = scoringCards,
                        botSpeed = botSpeed,
                        botPlayers = botPlayers
                    )

                    subtitleLabel.text = "Lobby wird erstellt..."
                    startButton.text = "Start Game"
                    rebuild()
                }

                ConnectionState.WAITING_FOR_GUEST -> {
                    rootService.networkService.startNewHostedGame(
                        botPlayers = botPlayers,
                        botSpeed = botSpeed
                    )
                }

                else -> {
                    subtitleLabel.text = "Das Game läuft schon"
                    rebuild()
                }
            }
        }

        rootService.networkService.addRefreshable(this)
    }

    private fun addBot() {
        val nameField = Theme.textField(width = 230, height = 42, text = "RANDOMBOT ${botRows.size + 1}").apply {
            isDisabled = true
        }
        val typeBtn = playerTypeButton()
        val removeBtn = Theme.removeIconButton(scene = this)
        removeBtn.onMouseClicked = {
            val i = botRows.indexOfFirst { it.removeBtn === removeBtn }
            if (i >= 0) {
                botRows.removeAt(i)
                refreshBotNames()
                rebuild()
            }
        }
        botRows.add(BotRow(nameField, typeBtn, removeBtn))
    }

    private fun hasBots(): Boolean = botRows.isNotEmpty() || p1Type.text != "Human"

    private fun buildPlayersBlock(): Pane<StaticComponentView<*>> {
        val rowHeight = 42.0
        val spacing = 22.0
        val stride = rowHeight + spacing

        val block = Pane<StaticComponentView<*>>(
            width = playerRowWidth, height = (botRows.size + 1) * stride - spacing + 16.0
        )

        p1Name.posX = 0.0; p1Name.posY = 0.0
        p1Type.posX = 246.0; p1Type.posY = 0.0
        p1HostLabel.posX = 412.0; p1HostLabel.posY = 0.0
        block.add(p1Name); block.add(p1Type); block.add(p1HostLabel)

        botRows.forEachIndexed { i, row ->
            val y = (i + 1) * stride
            row.nameField.posX = 0.0; row.nameField.posY = y
            row.typeBtn.posX = 246.0; row.typeBtn.posY = y
            row.removeBtn.posX = 412.0; row.removeBtn.posY = y + 1.0
            block.add(row.nameField); block.add(row.typeBtn); block.add(row.removeBtn)
        }

        return block
    }

    private fun rebuild() {
        invitePlayerBtn.isDisabled = botRows.size >= maxBots

        playersBlock?.clear()
        leftColumn?.clear()
        rightColumn?.clear()
        columnsRow?.clear()

        val newPlayersBlock = buildPlayersBlock()

        val leftComponents = mutableListOf<StaticComponentView<*>>(playersTitle, newPlayersBlock, invitePlayerBtn)
        if (hasBots()) {
            leftComponents.add(botSettingsTitle)
            leftComponents.add(botGrid)
        }
        val newLeftColumn = ThemeLayout.stackVertical(columnSpacing, leftComponents)
        val newRightColumn = ThemeLayout.stackVertical(columnSpacing, listOf(scoringTitle, scoringGrid))

        val newColumnsRow = Pane<StaticComponentView<*>>(
            width = newLeftColumn.width + columnGap + newRightColumn.width,
            height = maxOf(newLeftColumn.height, newRightColumn.height)
        )
        newLeftColumn.posX = 0.0; newLeftColumn.posY = 0.0
        newRightColumn.posX = newLeftColumn.width + columnGap; newRightColumn.posY = 0.0
        newColumnsRow.add(newLeftColumn)
        newColumnsRow.add(newRightColumn)

        bottomRow.width = newColumnsRow.width
        startButton.posX = newColumnsRow.width - startButton.width

        menuContainer?.let {
            ThemeLayout.clearCard(it)
            removeComponents(it)
        }

        val components = mutableListOf<StaticComponentView<*>>(crestLabel, titleLabel, titleDivider)
        if (subtitleLabel.text.isNotEmpty()) components.add(subtitleLabel)
        components.add(newColumnsRow)
        components.add(bottomRow)

        val newContainer = ThemeLayout.cardContainer(
            posX = 0, posY = 0, spacing = 16.0,
            fill = Theme.greenPanel,
            components = components,
            style = ThemeLayout.CardStyle(
                centered = setOf(crestLabel, titleLabel, titleDivider, subtitleLabel),
                topGap = 26.0, bottomGap = 32.0
            )
        )
        newContainer.posX = (1920.0 - newContainer.width) / 2.0
        newContainer.posY = (1080.0 - newContainer.height) / 2.0

        playersBlock = newPlayersBlock
        leftColumn = newLeftColumn
        rightColumn = newRightColumn
        columnsRow = newColumnsRow
        menuContainer = newContainer
        addComponents(newContainer)
    }

    /**
     * Das wird benötigt um den network später die scoringcards zu geben
     */
    private fun selectedScoringCards(): List<Boolean> {
        fun comboToBoolean(value: String): Boolean {
            return when (value) {
                "A" -> true
                "B" -> false
                else -> Random.nextBoolean()
            }
        }

        return listOf(
            comboToBoolean(bearCombo.text),
            comboToBoolean(elkCombo.text),
            comboToBoolean(foxCombo.text),
            comboToBoolean(hawkCombo.text),
            comboToBoolean(salmonCombo.text)
        )
    }

    /**
     * Brauchen wir damit wir im hosten die id kriegen zum inviten
     */
    override fun refreshConnectionState(state: ConnectionState) {
        if (state == ConnectionState.WAITING_FOR_GUEST) {
            val sessionID = rootService.networkService.client?.sessionID ?: "..."
            val players = rootService.networkService.getLobbyPlayers().joinToString(", ")

            subtitleLabel.text = "Lobby ID: $sessionID\nPlayers: $players"
            startButton.text = "Start Game"
            rebuild()
        }
    }


    // Locks bot name fields and assigns default names ("RANDOMBOT n" / "STRONGBOT n") based on
    // slot type and position. GameActionService.startNewGame identifies player type by these
    // exact name prefixes, so the numbering must produce unique names per bot type.
    private fun refreshBotNames() {
        val all = listOf(p1Name to p1Type) + botRows.map { it.nameField to it.typeBtn }

        var randomCount = 0
        var strongCount = 0
        all.forEachIndexed { i, (name, type) ->
            when (type.text) {
                "Random Bot" -> {
                    randomCount++
                    name.text = "RANDOMBOT $randomCount"
                    name.isDisabled = true
                }
                "Strong Bot" -> {
                    strongCount++
                    name.text = "STRONGBOT $strongCount"
                    name.isDisabled = true
                }
                else -> {
                    name.isDisabled = false
                    if (name.text.startsWith("RANDOMBOT") || name.text.startsWith("STRONGBOT")) {
                        name.text = "Player ${i + 1}"
                    }
                }
            }
        }
    }


    private fun playerTypeButton() = Theme.chipButton(
        width = 150, height = 42, text = "Random Bot", scene = this
    ).apply {
        onMouseClicked = {
            text = if (text == "Random Bot") {
                "Strong Bot"
            } else {
                "Random Bot"
            }

            refreshBotNames()
            rebuild()
        }
    }

    private fun hostTypeButton() = Theme.chipButton(
        width = 150, height = 42, text = "Human", scene = this
    ).apply {
        onMouseClicked = {
            text = playerTypeOptions[(playerTypeOptions.indexOf(text) + 1) % playerTypeOptions.size]
            refreshBotNames()
            rebuild()
        }
    }

    private fun scoringButton() = Theme.chipButton(
        width = 130, height = 42, text = "Random", scene = this
    ).apply {
        onMouseClicked = {
            text = scoringOptions[(scoringOptions.indexOf(text) + 1) % scoringOptions.size]
        }
    }

    private fun selectedBotNames(): List<String> {
        return botRows.map { it.nameField.text }
    }

}
