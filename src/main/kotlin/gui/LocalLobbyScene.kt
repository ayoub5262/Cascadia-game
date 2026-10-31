package gui

import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.CheckBox
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.MenuScene
import service.RootService
import tools.aqua.bgw.visual.ImageVisual
import entity.BotSpeed

/**
 * The local lobby scene of the game.
 *
 * @constructor Creates a new Local Lobby Scene with the specified rootService.
 *
 * @param rootService The [RootService] that manages the game state.
 */
class LocalLobbyScene(private val rootService: RootService) : MenuScene(1920, 1080) {

    private val maxNameLength = 15
    private val minPlayers = 2
    private val maxPlayers = 4
    private val playerTypeOptions = listOf("Human", "Random Bot", "Strong Bot")
    private val scoringOptions = listOf("Random", "A", "B")

    private val playerRowWidth = 452.0
    private val columnSpacing = 16.0
    private val columnGap = 48.0

    private data class PlayerRow(
        val nameField: TextField,
        val typeBtn: Theme.IconButton,
        val removeBtn: Theme.IconButton,
        val warnLabel: Label
    )

    private val playerRows = mutableListOf<PlayerRow>()

    private val crestLabel = ThemeLayout.crestLabel(Theme.Icons.LOCAL)
    val titleLabel = Label(width = 620, height = 80, text = "LOCAL GAME LOBBY", font = Theme.titleFont,
        alignment = Alignment.CENTER)
    private val titleDivider = ThemeLayout.divider(320)

    val playersTitle = Label(width = 452, height = 34, text = "PLAYERS", font = Theme.headerFont,
        alignment = Alignment.CENTER)

    val addPlayerBtn = Theme.chipButton(width = 190, height = 46, text = "Add",
        icon = Theme.Icons.USERPLUS_CREAM, scene = this)
    val randomizeBtn = Theme.chipButton(width = 190, height = 46, text = "Randomize",
        icon = Theme.Icons.SHUFFLE_CREAM, scene = this)

    val playersBtnRow = Pane<StaticComponentView<*>>(width = 396, height = 46).apply {
        addPlayerBtn.posX = 0.0; addPlayerBtn.posY = 0.0
        randomizeBtn.posX = 206.0; randomizeBtn.posY = 0.0
        add(addPlayerBtn)
        add(randomizeBtn)
    }

    val scoringTitle = Label(width = 714, height = 34, text = "SCORING CARDS",
        font = Theme.headerFont, alignment = Alignment.CENTER)

    val bearLabel = Label(width = 130, height = 30, text = "Bear", font = Theme.bodyFont, alignment = Alignment.CENTER)
    val bearCombo = scoringButton()

    val foxLabel = Label(width = 130, height = 30, text = "Fox", font = Theme.bodyFont, alignment = Alignment.CENTER)
    val foxCombo = scoringButton()

    val hawkLabel = Label(width = 130, height = 30, text = "Hawk", font = Theme.bodyFont, alignment = Alignment.CENTER)
    val hawkCombo = scoringButton()

    val salmonLabel = Label(width = 130, height = 30, text="Salmon", font= Theme.bodyFont, alignment = Alignment.CENTER)
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

        repeat(minPlayers) { addPlayerRow() }
        refreshBotNames()
        rebuild()

        addPlayerBtn.onMouseClicked = {
            if (playerRows.size < maxPlayers) {
                addPlayerRow()
                refreshBotNames()
                rebuild()
            }
        }

        randomizeBtn.onMouseClicked = {
            val names = playerRows.map { it.nameField.text }
            val types = playerRows.map { it.typeBtn.text }
            val zipped = names.zip(types).shuffled()
            playerRows.forEachIndexed { i, row ->
                row.nameField.text = zipped[i].first
                row.typeBtn.text = zipped[i].second
            }
            refreshBotNames()
        }

        startButton.onMouseClicked = {
            val names = playerRows.map { it.nameField.text.take(maxNameLength) }
            val scoringCards = listOf(bearCombo, foxCombo, hawkCombo, salmonCombo, elkCombo)
                .map { combo ->
                    when (combo.text) {
                        "A" -> true
                        "B" -> false
                        else -> kotlin.random.Random.nextBoolean()
                    }
                }
            var simSpeed: BotSpeed
            if (simSpeedCheck.isChecked){
                simSpeed = BotSpeed.FAST
            }
            else{
                simSpeed = BotSpeed.NORMAL
            }
            if (names.size in minPlayers..maxPlayers) {
                rootService.gameActionService.startNewGame(names, false, scoringCards, simSpeed)
            }
        }
    }

    private fun addPlayerRow() {
        val index = playerRows.size
        val nameField = Theme.textField(width = 230, height = 42, text = "Player ${index + 1}")
        val warnLabel = Label(width = 230, height = 16, text = "", font = Theme.warnFont,
            alignment = Alignment.CENTER_LEFT)
        var lastValidName = nameField.text
        nameField.onTextChanged = { newText ->
            if (newText.length > maxNameLength) {
                nameField.text = lastValidName
                warnLabel.text = "Max $maxNameLength characters"
            } else {
                lastValidName = newText
                warnLabel.text = ""
            }
        }
        val typeBtn = playerTypeButton()
        val removeBtn = Theme.removeIconButton(scene = this)
        removeBtn.onMouseClicked = {
            val i = playerRows.indexOfFirst { it.removeBtn === removeBtn }
            if (i >= 0 && playerRows.size > minPlayers) {
                playerRows.removeAt(i)
                refreshBotNames()
                rebuild()
            }
        }
        playerRows.add(PlayerRow(nameField, typeBtn, removeBtn, warnLabel))
    }

    private fun hasBots(): Boolean = playerRows.any { it.typeBtn.text != "Human" }

    private fun buildPlayersBlock(): Pane<StaticComponentView<*>> {
        val rowHeight = 42.0
        val warnHeight = 16.0
        val spacing = 6.0
        val showRemove = playerRows.size > minPlayers
        val stride = rowHeight + warnHeight + spacing

        val block = Pane<StaticComponentView<*>>(
            width = playerRowWidth, height = playerRows.size * stride - spacing
        )
        playerRows.forEachIndexed { i, row ->
            val y = i * stride
            row.nameField.posX = 0.0; row.nameField.posY = y
            row.typeBtn.posX = 246.0; row.typeBtn.posY = y
            block.add(row.nameField)
            block.add(row.typeBtn)
            if (showRemove) {
                row.removeBtn.posX = 412.0; row.removeBtn.posY = y + 1.0
                block.add(row.removeBtn)
            }
            row.warnLabel.posX = 0.0; row.warnLabel.posY = y + rowHeight
            block.add(row.warnLabel)
        }
        return block
    }

    private fun rebuild() {
        addPlayerBtn.isDisabled = playerRows.size >= maxPlayers

        playersBlock?.clear()
        leftColumn?.clear()
        rightColumn?.clear()
        columnsRow?.clear()

        val newPlayersBlock = buildPlayersBlock()

        val leftComponents = mutableListOf<StaticComponentView<*>>(playersTitle, newPlayersBlock, playersBtnRow)
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

        val newContainer = ThemeLayout.cardContainer(
            posX = 0, posY = 0, spacing = 16.0,
            fill = Theme.greenPanel,
            components = listOf(crestLabel, titleLabel, titleDivider, newColumnsRow, bottomRow),
            style = ThemeLayout.CardStyle(
                centered = setOf(crestLabel, titleLabel, titleDivider),
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

    private fun refreshBotNames() {
        var randomCount = 0
        var strongCount = 0
        playerRows.forEachIndexed { i, row ->
            when (row.typeBtn.text) {
                "Random Bot" -> {
                    randomCount++
                    row.nameField.text = "RANDOMBOT $randomCount"
                    row.nameField.isDisabled = true
                }
                "Strong Bot" -> {
                    strongCount++
                    row.nameField.text = "STRONGBOT $strongCount"
                    row.nameField.isDisabled = true
                }
                else -> {
                    row.nameField.isDisabled = false
                    if (row.nameField.text.startsWith("RANDOMBOT") || row.nameField.text.startsWith("STRONGBOT")) {
                        row.nameField.text = "Player ${i + 1}"
                    }
                }
            }
        }
    }

    private fun playerTypeButton() = Theme.chipButton(
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
}