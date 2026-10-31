package gui

import entity.CascadiaGame
import entity.GameState
import entity.Habitat
import entity.HabitatTile
import entity.Player
import entity.PlayerType
import entity.Wildlife
import entity.WildlifeToken
import kotlin.math.sqrt
import service.Refreshable
import service.RootService
import tools.aqua.bgw.animation.DelayAnimation
import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.components.gamecomponentviews.HexagonView
import tools.aqua.bgw.components.gamecomponentviews.TokenView
import tools.aqua.bgw.components.layoutviews.CameraPane
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.HexOrientation
import tools.aqua.bgw.event.KeyCode
import tools.aqua.bgw.event.MouseButtonType
import tools.aqua.bgw.style.BorderRadius
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual

/**
* The GameScene class is a BoardGameScene that displays the game board and all game components.
*
* @constructor Creates a new Game Scene with the specified rootService.
*
* @param rootService The [RootService] that manages the game state.
*/
class GameScene(private val rootService: RootService) :
    BoardGameScene(1920, 1080, background = ImageVisual("gameSceneBackground.jpg")) {

    val lifecycleRefresher = LifecycleRefresher()
    val actionRefresher = ActionRefresher()

    private val titleFont = Font(26, Color.WHITE, fontWeight = Font.FontWeight.BOLD)
    private val buttonFont = Font(18, Color.WHITE, fontWeight = Font.FontWeight.SEMI_BOLD)

    private var viewedPlayer = 0
    private var chosenTokenTile: Int? = null
    private var selectedWildlifeCol: Int? = null
    private var lockedInCoord: Pair<Int, Int>? = null
    private var placingWildlifeToken = false
    private var redrawUsedThisTurn = false
    private var previewRotation = 0
    private var habitatConfirmed = false
    private var usedHabitatTile: HabitatTile? = null
    private var usedWildlifeToken: WildlifeToken? = null
    private var panJitter = 0.0
    private var natureAction: NatureAction? = null
    private var localSeatIndex: Int? = null

    private enum class NatureAction { CHOOSING, SWAP, FREEPAIR }

    private var lastSavedHash: Int = -1

    private val selectedForRedraw = mutableSetOf<Int>()
    private val boardHexViews = mutableListOf<HexagonView>()
    private val shopViews = mutableListOf<ComponentView>()
    private val tileTokenViews = mutableListOf<ComponentView>()

    val helpButton = Theme.backButton(
        width = 110, height = 44, text = "Help", icon = Theme.Icons.RULES, scene = this
    ).apply { posX = 30.0; posY = 24.0 }

    private val saveButton = Theme.primaryButton(
        width = 130, height = 44, text = "Save", icon = Theme.Icons.SAVE_CREAM, fontSize = 16, scene = this
    ).apply { posX = 150.0; posY = 24.0; onMouseClicked = { rootService.gameActionService.saveGame() } }

    val quitButton = Theme.dangerButton(
        width = 150, height = 44, text = "Quit Game", fontSize = 16, scene = this
    ).apply { posX = 1740.0; posY = 24.0 }

    private val topBarBg = Label(
        posX = 290, posY = 20, width = 1440, height = 52,
        visual = ColorVisual(Color(255, 255, 255, 120)).apply { style.borderRadius = BorderRadius(10.0) }
    )

    private val turnsLabel = Label(
        posX = 300, posY = 26, width = 180, height = 40,
        text = "Turn: 1", font = titleFont.copy(color = Color(85, 65, 50)), alignment = Alignment.CENTER,
        visual = ColorVisual(Color(225, 215, 205)).apply { style.borderRadius = BorderRadius(10.0) }
    )

    private val natureTokenBg = Label(
        posX = 1570, posY = 174, width = 230, height = 60,
        visual = ColorVisual(Color(60, 40, 20)).apply { style.borderRadius = BorderRadius(30.0) }
    )

    private val natureLabel = Label(
        posX = 1690, posY = 174, width = 70, height = 60,
        text = "0", font = titleFont.copy(color = Color(240, 220, 200)), alignment = Alignment.CENTER_LEFT
    )

    private val playerLabel = Label(
        posX = 1400, posY = 26, width = 300, height = 40,
        text = "Player 1", font = titleFont.copy(color = Color(85, 65, 50)), alignment = Alignment.CENTER,
        visual = ColorVisual(Color(225, 215, 205)).apply { style.borderRadius = BorderRadius(10.0) }
    )

    private val prevPlayerButton = Theme.modernButton(
        width = 44, height = 40, text = "<", scene = this,
        buttonStyle = Theme.ButtonStyle(
            font = buttonFont.copy(color = Color(85, 65, 50), fontWeight = Font.FontWeight.BOLD),
            baseColor = Color(225, 215, 205),
            radius = BorderRadius(topLeft = 20.0, topRight = 0.0, bottomRight = 0.0, bottomLeft = 20.0)
        )
    ).apply { posX = 1240.0; posY = 162.0; onMouseClicked = { showPreviousPlayer() } }

    private val viewedLabel = Label(
        posX = 1284, posY = 162, width = 180, height = 40,
        text = "Player 1",
        font = buttonFont.copy(color = Color(85, 65, 50), fontWeight = Font.FontWeight.BOLD),
        alignment = Alignment.CENTER, visual = ColorVisual(Color(225, 215, 205))
    )

    private val nextPlayerButton = Theme.modernButton(
        width = 44, height = 40, text = ">", scene = this,
        buttonStyle = Theme.ButtonStyle(
            font = buttonFont.copy(color = Color(85, 65, 50), fontWeight = Font.FontWeight.BOLD),
            baseColor = Color(225, 215, 205),
            radius = BorderRadius(topLeft = 0.0, topRight = 20.0, bottomRight = 20.0, bottomLeft = 0.0)
        )
    ).apply { posX = 1464.0; posY = 162.0; onMouseClicked = { showNextPlayer() } }

    private val boardContentPane = Pane<ComponentView>(width = CONTENT_SIZE, height = CONTENT_SIZE)

    private val boardBackground = Label(
        posX = 280, posY = 150, width = 1232, height = 700,
        visual = ImageVisual("blank-map.jpg").apply { style.borderRadius = BorderRadius(18) }
    )

    private val boardCameraPane = CameraPane(
        posX = 280, posY = 150, width = 1232, height = 700,
        target = boardContentPane, limitBounds = false
    ).apply {
        interactive = true
        panMouseButton = MouseButtonType.LEFT_BUTTON
    }

    private val zoomInButton = Button(
        posX = 300, posY = 770, width = 60, height = 60,
        visual = ImageVisual("zoomIn-inactive.png")
    ).apply {
        onMouseClicked = {
            if (boardCameraPane.zoom < 3.0) boardCameraPane.zoom += 0.25
        }
        onMouseEntered = { visual = ImageVisual("zoomIn.png") }
        onMouseExited = { visual = ImageVisual("zoomIn-inactive.png") }
    }

    private val zoomOutButton = Button(
        posX = 380, posY = 770, width = 60, height = 60,
        visual = ImageVisual("zoomOut-inactive.png")
    ).apply {
        onMouseClicked = {
            if (boardCameraPane.zoom > 0.3) boardCameraPane.zoom -= 0.25
        }
        onMouseEntered = { visual = ImageVisual("zoomOut.png") }
        onMouseExited = { visual = ImageVisual("zoomOut-inactive.png") }
    }

    private val scoringPanel = Label(
        posX = 20, posY = 150, width = 240, height = 712,
        visual = ColorVisual(Color(248, 246, 240, 120)).apply { style.borderRadius = BorderRadius(18) }
    )

    private val scoringThumbs = Wildlife.entries.mapIndexed { i, animal ->
        Label(
            posX = 48, posY = 164 + i * 135, width = 185, height = 124,
            visual = ImageVisual(scoringImagePath(animal))
        ).apply { onMouseClicked = { showScoringCard(animal) } }
    }

    private val scoringTokens = Wildlife.entries.mapIndexed { i, animal ->
        Label(
            posX = 32, posY = 158 + i * 135, width = 58, height = 58,
            visual = ImageVisual("tokens/${animalCode(animal)}.png")
        ).apply { onMouseClicked = { showScoringCard(animal) } }
    }

    private val shopBackground = Label(
        posX = 1532, posY = 150, width = 368, height = 700,
        visual = ImageVisual("blank-map2.jpg").apply { style.borderRadius = BorderRadius(18) }
    )

    private val cancelButton = Theme.modernButton(
        width = 150, height = 56, text = "Cancel", scene = this,
        buttonStyle = Theme.ButtonStyle(
            font = buttonFont.copy(color = Color(80, 50, 40)), baseColor = Color(200, 190, 180)
        )
    ).apply { posX = 560.0; posY = 880.0; isVisible = false; onMouseClicked = { cancelPlacement() } }

    private val confirmButton = Theme.modernButton(
        width = 150, height = 56, text = "Confirm", scene = this,
        buttonStyle = Theme.ButtonStyle(
            font = buttonFont.copy(color = Color(80, 50, 40)), baseColor = Color(200, 190, 180)
        )
    ).apply { posX = 720.0; posY = 880.0; isVisible = false; onMouseClicked = { confirmPlacement() } }

    private val rotateCcwButton = Theme.modernButton(
        width = 250, height = 56, text = "Rotate Counterclockwise", scene = this,
        buttonStyle = Theme.ButtonStyle(
            font = buttonFont.copy(color = Color(80, 50, 40)), baseColor = Color(200, 190, 180)
        )
    ).apply { posX = 880.0; posY = 880.0; isVisible = false; onMouseClicked = { rotateHabitat(true) } }

    private val rotateCwButton = Theme.modernButton(
        width = 200, height = 56, text = "Rotate Clockwise", scene = this,
        buttonStyle = Theme.ButtonStyle(
            font = buttonFont.copy(color = Color(80, 50, 40)), baseColor = Color(200, 190, 180)
        )
    ).apply { posX = 1140.0; posY = 880.0; isVisible = false; onMouseClicked = { rotateHabitat(false) } }

    private val confirmRedrawButton = Theme.modernButton(
        width = 220, height = 56, text = "Confirm Redraw", scene = this,
        buttonStyle = Theme.ButtonStyle(
            font = buttonFont.copy(color = Color(80, 50, 40)), baseColor = Color(200, 190, 180)
        )
    ).apply { posX = 880.0; posY = 880.0; isVisible = false; onMouseClicked = { confirmRedraw() } }

    private val swapModeButton = Theme.modernButton(
        width = 320, height = 56, text = "Swap wildlife tokens", scene = this,
        buttonStyle = Theme.ButtonStyle(
            font = buttonFont.copy(color = Color(80, 50, 40)), baseColor = Color(200, 190, 180)
        )
    ).apply {
        posX = 600.0; posY = 880.0; isVisible = false
        onMouseClicked = { chooseNatureAction(NatureAction.SWAP) }
    }

    private val freePairButton = Theme.modernButton(
        width = 320, height = 56, text = "Free combination", scene = this,
        buttonStyle = Theme.ButtonStyle(
            font = buttonFont.copy(color = Color(80, 50, 40)), baseColor = Color(200, 190, 180)
        )
    ).apply {
        posX = 960.0; posY = 880.0; isVisible = false
        onMouseClicked = { chooseNatureAction(NatureAction.FREEPAIR) }
    }

    private val undoButton = Button(
        posX = 305, posY = 166, width = 56, height = 40, text = "", visual = ImageVisual("arrowleft.png")
    ).apply { onMouseClicked = { log("undo()"); rootService.playerActionService.undo() } }

    private val redoButton = Button(
        posX = 369, posY = 166, width = 56, height = 40, text = "", visual = ImageVisual("arrowright.png")
    ).apply { onMouseClicked = { log("redo()"); rootService.playerActionService.redo() } }

    private val natureButton = Button(
        posX = 1555, posY = 156, width = 96, height = 96, text = "",
        visual = ImageVisual("tokens/nature-token.png")
    ).apply {
        rotation = 180.0
        onMouseClicked = { log("useNatureToken()"); rootService.playerActionService.useNatureToken() }
    }

    private val shopSlots = List(4) { i ->
        Label(
            posX = 1556, posY = 262.0 + 130.0 * i, width = 320, height = 125,
            visual = ColorVisual(Color(0, 0, 0, 80)).apply { style.borderRadius = BorderRadius(15.0) }
        )
    }

    private val shopPlusLabels = List(4) { i ->
        Label(
            posX = 1696, posY = 300.0 + 130.0 * i, width = 40, height = 40,
            text = "+", font = Font(48, Color(220, 200, 180), fontWeight = Font.FontWeight.BOLD),
            alignment = Alignment.CENTER
        )
    }

    private val redrawButton = Theme.modernButton(
        width = 320, height = 52, text = "Redraw 3 wildlife tokens", scene = this,
        buttonStyle = Theme.ButtonStyle(
            font = buttonFont.copy(color = Color(80, 50, 40)), baseColor = Color(200, 190, 180)
        )
    ).apply {
        posX = 1556.0; posY = 786.0
        onMouseClicked = {
            redrawUsedThisTurn = true
            log("redrawOn3Wildlife()")
            rootService.playerActionService.redrawOn3Wildlife()
        }
    }

    private val scoringOverlayImage = Label(posX = 510, posY = 238, width = 900, height = 604)
    private val scoringOverlay = overlay(scoringOverlayImage)

    private val tokenToast = Label(
        posX = 646, posY = 88, width = 500, height = 52, text = "",
        font = titleFont, alignment = Alignment.CENTER,
        visual = ColorVisual(Color(0x2E5E3A)).apply { style.borderRadius = BorderRadius(12) }
    ).apply { isVisible = false }

    val showResultButton = Theme.primaryButton(
        width = 200, height = 44, text = "Show Results", icon = Theme.Icons.GRID_CREAM, fontSize = 16, scene = this
    ).apply { posX = 840.0; posY = 24.0; isVisible = false }

    init {
        panToStart()
        onSceneRescaled = { panToStart() }
        addComponents(boardBackground)
        addComponents(boardCameraPane)
        addComponents(scoringPanel)
        scoringThumbs.forEach { addComponents(it) }
        scoringTokens.forEach { addComponents(it) }
        addComponents(
            topBarBg,
            shopBackground,
            natureTokenBg,
            zoomInButton,
            zoomOutButton
        )
        shopSlots.forEach { addComponents(it) }
        shopPlusLabels.forEach { addComponents(it) }
        addComponents(
            turnsLabel,
            natureLabel,
            helpButton,
            saveButton,
            quitButton,
            playerLabel,
            undoButton,
            redoButton,
            prevPlayerButton,
            viewedLabel,
            nextPlayerButton,
            cancelButton,
            confirmButton,
            rotateCcwButton,
            rotateCwButton,
            confirmRedrawButton,
            swapModeButton,
            freePairButton,
            redrawButton,
            natureButton,
        )
        addComponents(tokenToast)
        addComponents(scoringOverlay, showResultButton)
        onKeyPressed = { event ->
            when (event.keyCode) {
                KeyCode.Q -> rotateHabitat(true)
                KeyCode.E, KeyCode.R -> rotateHabitat(false)
                KeyCode.ENTER -> endTurn()
                KeyCode.ESCAPE -> hideOverlays()
                else -> Unit
            }
        }
    }

    private fun stateHash(game: CascadiaGame?): Int {
        if (game == null) return -1
        val save = rootService.currentGame ?: return -1
        return save.history.size * 31 +
                save.undoes.size * 17 +
                game.gameState.ordinal * 13 +
                game.roundNumber * 7 +
                game.currentPlayer
    }

    private fun triggerHotseatBotTurnIfNeeded(game: CascadiaGame) {
        val currentPlayer = game.players.getOrNull(game.currentPlayer) ?: return
        if (!game.isHotseatMode || currentPlayer.playMode == PlayerType.HUMAN) return

        val botAnim = DelayAnimation(duration = game.simulationSpeed.toMillieSeconds().toInt()).apply {
            onFinished = { rootService.botActionService.botTurn() }
        }
        playAnimation(botAnim)
    }
    private fun resetTurnState() {
        chosenTokenTile = null
        selectedWildlifeCol = null
        lockedInCoord = null
        placingWildlifeToken = false
        previewRotation = 0
        habitatConfirmed = false
        usedHabitatTile = null
        usedWildlifeToken = null
        natureAction = null
    }
    /**
     * Handles the refreshes for the game itself: starting, loading and saving a game, turn
     * changes, undo and redo, and the end of the game.
     */
    inner class LifecycleRefresher : Refreshable {
        override fun refreshAfterStartNewGame() {
            resolveLocalSeat()
            hideOverlays()
            showResultButton.isVisible = false
            updateScoringThumbnails()
            lastSavedHash = -1
            resetTurnState()
            showActivePlayer()
        }

        override fun refreshAfterStartTurn() {
            val game = rootService.currentGame?.currentState ?: return

            if (game.roundNumber == 1 && game.currentPlayer == 0) {
                showActivePlayer()
            } else {
                val anim = DelayAnimation(duration = 1500).apply {
                    onFinished = { showActivePlayer() }
                }
                playAnimation(anim)
            }

            triggerHotseatBotTurnIfNeeded(game)
        }

        override fun refreshAfterLoadGame() {
            resolveLocalSeat()
            hideOverlays()
            showResultButton.isVisible = false
            updateScoringThumbnails()
            lastSavedHash = stateHash(rootService.currentGame?.currentState)
            resetTurnState()
            showActivePlayer()
        }

        override fun refreshAfterSaveGame() {
            lastSavedHash = stateHash(rootService.currentGame?.currentState)
            val game = rootService.currentGame?.currentState ?: return
            updateHistoryButtons(game)
        }

        override fun refreshAfterUndo() { resetTurnState(); showActivePlayer() }

        override fun refreshAfterRedo() { resetTurnState(); showActivePlayer() }

        override fun refreshAfterEndGame(scores: List<Pair<Player, Map<String, Int>>>) {
            showResultButton.isVisible = true
        }
    }

    /**
     * Handles the refreshes for the actions a player takes during a turn and updates the board,
     * the shop and the buttons.
     */
    inner class ActionRefresher : Refreshable {
        override fun refreshAfterChooseCombination() {
            val game = rootService.currentGame?.currentState ?: return
            if (isViewingActivePlayer(game)) updateBoard(game)
            updateShop(game)
            updateButtons(game)
        }

        override fun refreshAfterPlaceHabitatTile() {
            val game = rootService.currentGame?.currentState ?: return
            updateTopBar(game)
            if (isViewingActivePlayer(game)) updateBoard(game)
            updateShop(game)
            updateButtons(game)
        }

        override fun refreshAfterPlaceWildlifeToken() {
            val game = rootService.currentGame?.currentState ?: return
            updateTopBar(game)
            if (isViewingActivePlayer(game)) updateBoard(game)
            updateShop(game)
            updateButtons(game)
        }

        override fun refreshAfterRedrawWildlifeTokens() {
            val game = rootService.currentGame?.currentState ?: return
            updateShop(game)
        }

        override fun refreshAfterRedrawOn3Wildlife() {
            val game = rootService.currentGame?.currentState ?: return
            updateShop(game)
            updateButtons(game)
        }

        override fun refreshAfterRotateTile() {
            val game = rootService.currentGame?.currentState ?: return
            if (isViewingActivePlayer(game)) updateBoard(game)
            updateShop(game)
        }

        override fun refreshAfterDiscardWildlifeToken() {
            val game = rootService.currentGame?.currentState ?: return
            if (isViewingActivePlayer(game)) updateBoard(game)
            updateShop(game)
            updateButtons(game)
        }

        override fun refreshAfterUseNatureToken() {
            val game = rootService.currentGame?.currentState ?: return
            natureAction = NatureAction.CHOOSING
            chosenTokenTile = null
            selectedWildlifeCol = null
            selectedForRedraw.clear()
            lockedInCoord = null
            updateTopBar(game)
            if (isViewingActivePlayer(game)) updateBoard(game)
            updateShop(game)
            updateButtons(game)
        }

        override fun refreshAfterRestockShops() {
            val game = rootService.currentGame?.currentState ?: return
            updateShop(game)
        }

        override fun refreshAfterCheckOverpopulation() {
            val game = rootService.currentGame?.currentState ?: return
            updateShop(game)
        }
    }

    private fun showActivePlayer() {
        selectedWildlifeCol = null
        selectedForRedraw.clear()
        natureAction = null
        redrawUsedThisTurn = false
        previewRotation = 0
        habitatConfirmed = false
        panToStart()
        viewedPlayer = rootService.currentGame?.currentState?.currentPlayer ?: 0
        updateView()
    }

    private fun panToStart() {
        panJitter = if (panJitter == 0.0) 0.001 else 0.0
        boardCameraPane.pan(BASE_X + HEX_WIDTH / 2 + panJitter, BASE_Y + HEX_SIZE, smooth = false)
    }

    private fun updateView() {
        val game = rootService.currentGame?.currentState ?: return
        updateTopBar(game)
        updateBoard(game)
        updateShop(game)
        updateButtons(game)
    }

    private fun updateButtons(game: CascadiaGame) {
        updateHistoryButtons(game)
        updateActionButtons(game)
        updatePlacementButtons(game)
    }

    private fun updateHistoryButtons(game: CascadiaGame) {
        val isNetwork = !game.isHotseatMode
        val isAllBots = game.players.all { it.playMode != PlayerType.HUMAN }
        val undoRedoEnabled = !isNetwork && !isAllBots
        val save = rootService.currentGame
        val canUndo = undoRedoEnabled && (save?.history?.size ?: 0) > 1
        val canRedo = undoRedoEnabled && !(save?.undoes?.isEmpty() ?: true)
        undoButton.isDisabled = !canUndo
        undoButton.opacity = if (canUndo) 1.0 else 0.4
        redoButton.isDisabled = !canRedo
        redoButton.opacity = if (canRedo) 1.0 else 0.4
        
        val hasUnsavedChanges = stateHash(game) != lastSavedHash
        saveButton.isDisabled = isNetwork || !hasUnsavedChanges
        saveButton.opacity = if (saveButton.isDisabled) 0.4 else 1.0
    }

    private fun updateActionButtons(game: CascadiaGame) {
        val redrawUsable = game.threeWildlifeIdentical && isHumanTurn(game) && !redrawUsedThisTurn
        redrawButton.isDisabled = !redrawUsable
        redrawButton.opacity = if (redrawUsable) 1.0 else 0.4

        val activeTokens = game.players.getOrNull(game.currentPlayer)?.natureTokens ?: 0
        val natureUsable = activeTokens > 0 && isHumanTurn(game) &&
            (game.gameState == GameState.CHOOSEPAIR || game.gameState == GameState.USENATURETOKEN)
        natureButton.isDisabled = !natureUsable
        natureButton.opacity = 1.0
    }

    private fun updatePlacementButtons(game: CascadiaGame) {
        val inNature = game.gameState == GameState.USENATURETOKEN && isHumanTurn(game)
        val choosingNature = inNature && natureAction == NatureAction.CHOOSING
        val inRedrawSelection = inNature && natureAction == NatureAction.SWAP && chosenTokenTile == null
        swapModeButton.isVisible = choosingNature
        freePairButton.isVisible = choosingNature
        confirmButton.isVisible = isCommittedPlacing(game) || isPreviewReady(game)
        val canRotate = isCommittedPlacing(game) || isPreviewRotatable(game)
        listOf(rotateCcwButton, rotateCwButton).forEach { it.isVisible = canRotate }
        confirmRedrawButton.isVisible = inRedrawSelection
        cancelButton.text = if (placingWildlifeToken) "Discard" else "Cancel"
        cancelButton.isVisible = chosenTokenTile != null || inRedrawSelection
    }

    private fun isCommittedPlacing(game: CascadiaGame): Boolean =
        chosenTokenTile != null && !placingWildlifeToken && game.drawnCombination != null

    /*

    Moin aus dem network also anscheinend stirbt das game weil wenn man auf cancel drückt
    nicht ind er service gecancelt wird musste deswegen bissl daran rum schrauben
    ~Kaan~
    private fun isPreviewReady(game: CascadiaGame): Boolean =
        isFreePairPreview(game) && chosenTokenTile != null && lockedInCoord != null && !habitatConfirmed

    private fun isPreviewRotatable(game: CascadiaGame): Boolean =
        isFreePairPreview(game) && chosenTokenTile != null && !habitatConfirmed
     */

    private fun isPreviewReady(game: CascadiaGame): Boolean =
        chosenTokenTile != null &&
                lockedInCoord != null &&
                !habitatConfirmed &&
                game.drawnCombination == null

    private fun isPreviewRotatable(game: CascadiaGame): Boolean =
        chosenTokenTile != null &&
                !habitatConfirmed &&
                game.drawnCombination == null

    private fun updateTopBar(game: CascadiaGame) {
        val viewed = currentPlayer(game)
        val active = game.players.getOrNull(game.currentPlayer)
        turnsLabel.text = "Turn: ${game.roundNumber}"
        natureLabel.text = "${viewed?.natureTokens ?: 0}"
        playerLabel.text = active?.name ?: "Player ${game.currentPlayer + 1}"
        viewedLabel.text = viewed?.name ?: "Player ${viewedPlayer + 1}"
    }

    private fun updateBoard(game: CascadiaGame) {
        tileTokenViews.forEach { boardContentPane.remove(it) }
        tileTokenViews.clear()
        boardHexViews.forEach { boardContentPane.remove(it) }
        boardHexViews.clear()
        val board = currentPlayer(game)?.board.orEmpty()
        renderPlacedTiles(game, board)
        renderPlacementPreview(game, board)
        renderTileTokens(game, board)
    }

    private fun renderPlacedTiles(game: CascadiaGame, board: Map<Pair<Int, Int>, HabitatTile>) {
        board.forEach { (coord, tile) ->
            val hex = habitatHex(tile)
            if (placingWildlifeToken && isViewingActivePlayer(game)) {
                val valid = isWildlifeTarget(game, tile)
                hex.opacity = if (valid) 1.0 else 0.4
                if (valid) hex.onMouseClicked = { placeWildlifeTokenAt(coord) }
            }
            placeHex(coord, hex)
        }
    }

    private fun isWildlifeTarget(game: CascadiaGame, tile: HabitatTile): Boolean {
        val drawnAnimal = game.drawnCombination?.second?.animal ?: return false
        return placingWildlifeToken && isViewingActivePlayer(game) &&
            tile.placedWildlifeToken == null && tile.possibleAnimals.contains(drawnAnimal)
    }

    private fun renderPlacementPreview(game: CascadiaGame, board: Map<Pair<Int, Int>, HabitatTile>) {
        if (chosenTokenTile == null || placingWildlifeToken || !isViewingActivePlayer(game)) return
        if (!habitatConfirmed) {
            validPlacements(board.keys).forEach { coord ->
                placeHex(coord, validPlacementHex(coord))
            }
        }
        val tile = chosenTokenTile?.let { game.habitatShop.getOrNull(it) }
        val cell = lockedInCoord
        //val extra = if (isFreePairPreview(game)) previewRotation else 0
        val extra = if (game.drawnCombination == null) previewRotation else 0
        if (cell != null && tile != null) placeHex(cell, habitatHex(tile, highlight = true, extraRotation = extra))
    }

    private fun renderTileTokens(game: CascadiaGame, board: Map<Pair<Int, Int>, HabitatTile>) {
        board.forEach { (coord, tile) ->
            val (rawX, rawY) = rawHexPos(coord)
            val centerX = BASE_X + rawX + HEX_SIZE
            val centerY = BASE_Y + rawY + HEX_SIZE
            if (isWildlifeTarget(game, tile)) {
                addTileTokens(tile, centerX, centerY) { placeWildlifeTokenAt(coord) }
            } else {
                addTileTokens(tile, centerX, centerY)
            }
        }
        val previewTile = chosenTokenTile?.let { game.habitatShop.getOrNull(it) }
        val previewCell = lockedInCoord
        if (previewTile != null && previewCell != null) {
            val (rawX, rawY) = rawHexPos(previewCell)
            addTileTokens(previewTile, BASE_X + rawX + HEX_SIZE, BASE_Y + rawY + HEX_SIZE)
        }
    }

    private fun addTileTokens(
        tile: HabitatTile, centerX: Double, centerY: Double, onClick: (() -> Unit)? = null
    ) {
        tileTokens(tile, centerX, centerY).forEach {
            if (onClick != null) it.onMouseClicked = { onClick() }
            tileTokenViews += it
            boardContentPane.add(it)
        }
    }

    private fun updateShop(game: CascadiaGame) {
        shopViews.forEach { removeComponents(it) }
        shopViews.clear()
        renderHabitatShop(game)
        renderWildlifeShop(game)
        shopViews.forEach { addComponents(it) }
    }

    private fun renderHabitatShop(game: CascadiaGame) {
        game.habitatShop.forEachIndexed { i, tile ->
            if (tile == null) return@forEachIndexed
            if (tile === usedHabitatTile) return@forEachIndexed
            val hex = habitatHex(tile, highlight = chosenTokenTile == i)
            hex.posX = 1588.0
            hex.posY = 326.0 + 130.0 * i - 55.0
            val dimmed = if (chosenTokenTile == null || chosenTokenTile == i) 1.0 else 0.35
            hex.opacity = dimmed
            hex.onMouseClicked = { selectColumn(i) }
            shopViews += hex

            val centerX = hex.posX + HEX_SIZE
            val centerY = hex.posY + HEX_SIZE
            tileTokens(tile, centerX, centerY).forEach {
                it.opacity = dimmed
                it.onMouseClicked = { selectColumn(i) }
                shopViews += it
            }
        }
    }

    private fun renderWildlifeShop(game: CascadiaGame) {
        val natureMode = game.gameState == GameState.USENATURETOKEN && isHumanTurn(game)
        game.wildlifeShop.forEachIndexed { i, token ->
            if (token == null) return@forEachIndexed
            if (token === usedWildlifeToken) return@forEachIndexed
            val isSelectedForRedraw = i in selectedForRedraw
            val tv = TokenView(
                posX = 1748.0, posY = 326.0 + 130.0 * i - 48.0, width = 96, height = 96,
                visual = ImageVisual("tokens/${animalCode(token.animal)}.png")
            ).apply {
                opacity = when {
                    isSelectedForRedraw -> 0.55
                    selectedWildlifeCol != null && selectedWildlifeCol != i -> 0.35
                    else -> 1.0
                }
            }
            if (natureMode) {
                if (natureAction == NatureAction.FREEPAIR && chosenTokenTile != null) {
                    tv.onMouseClicked = { selectWildlifeColumn(i) }
                } else if (natureAction == NatureAction.SWAP && chosenTokenTile == null) {
                    tv.onMouseClicked = { toggleRedrawSelection(i) }
                }
            }
            shopViews += tv
        }
    }

    private fun selectColumn(i: Int) {
        if (placingWildlifeToken || habitatConfirmed) return
        val game = rootService.currentGame?.currentState ?: return
        if (!isViewingActivePlayer(game)) return
        if (!isHumanTurn(game)) return
        if (natureAction == NatureAction.CHOOSING || natureAction == NatureAction.SWAP) return
        val freePair = isFreePairPreview(game)
        if (game.habitatShop.getOrNull(i) == null) return
        if (!freePair && game.wildlifeShop.getOrNull(i) == null) return

        chosenTokenTile = i
        selectedForRedraw.clear()
        lockedInCoord = null
        previewRotation = 0

        selectedWildlifeCol = if (freePair) null else i

        updateBoard(game)
        updateShop(game)
        updateButtons(game)

        /*
        Moin aus dem network also anscheinend stirbt das game weil wenn man auf cancel drückt
        nicht ind er service gecancelt wird musste deswegen bissl daran rum schrauben
        ~Kaan~
        chosenTokenTile = i
        selectedForRedraw.clear()
        lockedInCoord = null
        previewRotation = 0
        if (freePair) {
            selectedWildlifeCol = null
            updateBoard(game)
            updateShop(game)
            updateButtons(game)
        } else {
            selectedWildlifeCol = i
            rootService.playerActionService.chooseCombination(i to i)
            // refreshAfterChooseCombination handles update
        }

         */
    }

    private fun chooseNatureAction(action: NatureAction) {
        natureAction = action
        val game = rootService.currentGame?.currentState ?: return
        updateShop(game)
        updateButtons(game)
    }

    private fun selectWildlifeColumn(j: Int) {
        val game = rootService.currentGame?.currentState ?: return
        if (!isHumanTurn(game)) return
        if (chosenTokenTile == null) return
        if (game.wildlifeShop.getOrNull(j) == null) return
        selectedWildlifeCol = j
        val cell = lockedInCoord
        if (habitatConfirmed && cell != null) {
            commitFreePairPlacement(game, cell)
        } else {
            updateShop(game)
            updateButtons(game)
        }
    }

    private fun cancelPlacement() {
        val game = rootService.currentGame?.currentState ?: return
        if (placingWildlifeToken) {
            usedWildlifeToken = (selectedWildlifeCol ?: chosenTokenTile)?.let { game.wildlifeShop.getOrNull(it) }
            chosenTokenTile = null
            selectedWildlifeCol = null
            lockedInCoord = null
            placingWildlifeToken = false
            log("discardWildlifeToken()")
            rootService.playerActionService.discardWildlifeToken()
            // refreshAfterDiscardWildlifeToken handles update
        } else {
            chosenTokenTile = null
            selectedWildlifeCol = null
            selectedForRedraw.clear()
            lockedInCoord = null
            previewRotation = 0
            habitatConfirmed = false
            if (natureAction != null) natureAction = NatureAction.CHOOSING
            updateBoard(game)
            updateShop(game)
            updateButtons(game)
        }
    }

    /**
        Moin aus dem network also anscheinend stirbt das game weil wenn man auf cancel drückt
        nicht ind er service gecancelt wird musste deswegen bissl daran rum schrauben
        ~Kaan~
     */
    private fun confirmPlacement() {

        val game = rootService.currentGame?.currentState ?: return
        val cell = lockedInCoord ?: return

        if (isFreePairPreview(game)) {
            if (chosenTokenTile == null) return

            if (selectedWildlifeCol != null) {
                commitFreePairPlacement(game, cell)
            } else {
                habitatConfirmed = true
                updateBoard(game)
                updateShop(game)
                updateButtons(game)
            }

            return
        }

        val habitatIndex = chosenTokenTile ?: return
        val wildlifeIndex = selectedWildlifeCol ?: habitatIndex

        if (game.drawnCombination == null) {
            rootService.playerActionService.chooseCombination(
                habitatIndex to wildlifeIndex
            )
        }

        placeHabitatCommitted(game, cell)
    }

    private fun commitFreePairPlacement(game: CascadiaGame, cell: Pair<Int, Int>) {
        val habitatCol = chosenTokenTile ?: return
        val wildlifeCol = selectedWildlifeCol ?: return
        log("chooseCombination($habitatCol to $wildlifeCol)")
        rootService.playerActionService.chooseCombination(habitatCol to wildlifeCol)
        repeat(((previewRotation % 6) + 6) % 6) {
            log("rotateHabitat(false)")
            rootService.playerActionService.rotateHabitat(false)
        }
        previewRotation = 0
        habitatConfirmed = false
        placeHabitatCommitted(game, cell)
    }

    private fun placeHabitatCommitted(game: CascadiaGame, cell: Pair<Int, Int>) {
        if (game.drawnCombination == null) return
        usedHabitatTile = chosenTokenTile?.let { game.habitatShop.getOrNull(it) }
        lockedInCoord = null
        placingWildlifeToken = true
        log("placeHabitat($cell)")
        rootService.playerActionService.placeHabitat(cell)
        // refreshAfterPlaceHabitatTile fires here with placingWildlifeToken=true, usedHabitatTile set
        if (!hasWildlifeTokenPotential(game)) {
            usedWildlifeToken = (selectedWildlifeCol ?: chosenTokenTile)?.let { game.wildlifeShop.getOrNull(it) }
            chosenTokenTile = null
            selectedWildlifeCol = null
            placingWildlifeToken = false
            log("discardWildlifeToken()")
            rootService.playerActionService.discardWildlifeToken()
            // refreshAfterDiscardWildlifeToken handles update
        }
    }

    private fun placeWildlifeTokenAt(coord: Pair<Int, Int>) {
        val game = rootService.currentGame?.currentState ?: return
        if (!isHumanTurn(game)) return
        usedWildlifeToken = (selectedWildlifeCol ?: chosenTokenTile)?.let { game.wildlifeShop.getOrNull(it) }
        chosenTokenTile = null
        selectedWildlifeCol = null
        lockedInCoord = null
        placingWildlifeToken = false
        val activePlayer = game.players.getOrNull(game.currentPlayer)
        val tokensBefore = activePlayer?.natureTokens ?: 0
        log("placeWildlifeTokens($coord)")
        rootService.playerActionService.placeWildlifeTokens(coord)
        if ((activePlayer?.natureTokens ?: 0) > tokensBefore) {
            showTokenToast("+1 Nature Token for ${activePlayer?.name}")
        }
    }

    private fun showTokenToast(text: String) {
        tokenToast.text = text
        tokenToast.isVisible = true
        playAnimation(DelayAnimation(2000).apply {
            onFinished = { tokenToast.isVisible = false }
        })
    }

    private fun hasWildlifeTokenPotential(game: CascadiaGame): Boolean {
        val animal = game.drawnCombination?.second?.animal ?: return false
        val board = game.players.getOrNull(game.currentPlayer)?.board.orEmpty()
        return board.values.any { it.placedWildlifeToken == null && it.possibleAnimals.contains(animal) }
    }

    private fun toggleRedrawSelection(i: Int) {
        val game = rootService.currentGame?.currentState ?: return
        if (!isHumanTurn(game)) return
        if (i in selectedForRedraw) selectedForRedraw.remove(i) else selectedForRedraw.add(i)
        updateShop(game)
        updateButtons(game)
    }

    private fun confirmRedraw() {
        val indices = selectedForRedraw.sorted()
        selectedForRedraw.clear()
        natureAction = null
        log("redrawWildlifeTokens($indices)")
        rootService.playerActionService.redrawWildlifeTokens(indices)
        val game = rootService.currentGame?.currentState ?: return
        updateShop(game)
        updateButtons(game)
    }
    /*

        Moin aus dem network also anscheinend stirbt das game weil wenn man auf cancel drückt
        nicht ind er service gecancelt wird musste deswegen bissl daran rum schrauben
        ~Kaan~

    private fun rotateHabitat(isLeftRotation: Boolean) {
        val game = rootService.currentGame?.currentState ?: return
        if (isFreePairPreview(game)) {
            if (chosenTokenTile == null || habitatConfirmed) return
            previewRotation += if (isLeftRotation) -1 else 1
            updateBoard(game)
        } else {
            if (game.gameState != GameState.PLACEHABITAT) return
            log("rotateHabitat($isLeftRotation)")
            rootService.playerActionService.rotateHabitat(isLeftRotation)
            // refreshAfterRotateTile handles update
        }
    }

     */

    private fun rotateHabitat(isLeftRotation: Boolean) {
        val game = rootService.currentGame?.currentState ?: return

        val isUncommittedPreview =
            chosenTokenTile != null &&
                    game.drawnCombination == null &&
                    !habitatConfirmed

        if (isUncommittedPreview) {
            previewRotation += if (isLeftRotation) -1 else 1
            updateBoard(game)
            return
        }

        if (game.gameState != GameState.PLACEHABITAT) return

        log("rotateHabitat($isLeftRotation)")
        rootService.playerActionService.rotateHabitat(isLeftRotation)
    }

    private fun endTurn() {
        val game = rootService.currentGame?.currentState ?: return
        if (!isHumanTurn(game)) return
        if (chosenTokenTile != null || placingWildlifeToken) return
        lockedInCoord = null
        log("endTurn()")
        rootService.gameActionService.endTurn()
        // refreshAfterEndTurn (empty) → refreshAfterStartTurn → showActivePlayer handles update
    }

    private fun showPreviousPlayer() {
        val count = rootService.currentGame?.currentState?.players?.size ?: return
        viewedPlayer = (viewedPlayer - 1 + count) % count
        updateView()
    }

    private fun showNextPlayer() {
        val count = rootService.currentGame?.currentState?.players?.size ?: return
        viewedPlayer = (viewedPlayer + 1) % count
        updateView()
    }

    private fun showScoringCard(animal: Wildlife) {
        scoringOverlayImage.visual = ImageVisual(scoringImagePath(animal))
        showOverlay(scoringOverlay)
    }

    private fun scoringImagePath(animal: Wildlife): String {
        val isCardTypeA = rootService.currentGame?.currentState?.scoringCards
            ?.find { it.wildlife == animal }?.isCardTypeA ?: true
        val variant = if (isCardTypeA) "A" else "B"
        return "scoring-goals/${animal.name.lowercase()}-$variant.png"
    }

    private fun updateScoringThumbnails() {
        Wildlife.entries.forEachIndexed { i, animal ->
            scoringThumbs[i].visual = ImageVisual(scoringImagePath(animal))
        }
    }

    private fun showOverlay(pane: Pane<ComponentView>) {
        removeComponents(pane)
        addComponents(pane)
        pane.isVisible = true
    }

    private fun hideOverlays() {
        scoringOverlay.isVisible = false
    }

    private fun placeHex(coord: Pair<Int, Int>, hex: HexagonView) {
        val (rawX, rawY) = rawHexPos(coord)
        hex.posX = BASE_X + rawX
        hex.posY = BASE_Y + rawY
        boardHexViews += hex
        boardContentPane.add(hex)
    }

    private fun currentPlayer(game: CascadiaGame): Player? =
        game.players.getOrNull(viewedPlayer) ?: game.players.getOrNull(game.currentPlayer)

    private fun isViewingActivePlayer(game: CascadiaGame) = viewedPlayer == game.currentPlayer

    private fun isHumanTurn(game: CascadiaGame) =
        game.players.getOrNull(game.currentPlayer)?.playMode == PlayerType.HUMAN &&
                isLocalPlayersTurn(game)

    private fun isLocalPlayersTurn(game: CascadiaGame): Boolean {
        if (game.isHotseatMode) return true
        val seat = localSeatIndex ?: return false
        return game.currentPlayer == seat
    }

    private fun resolveLocalSeat() {
        val game = rootService.currentGame?.currentState
        val name = rootService.networkService.client?.playerName
        localSeatIndex = if (game != null && name != null) {
            game.players.indexOfFirst { it.name == name }.takeIf { it >= 0 }
        } else {
            null
        }
    }

    private fun isFreePairPreview(game: CascadiaGame) =
        game.gameState == GameState.USENATURETOKEN && natureAction == NatureAction.FREEPAIR

    private fun validPlacements(placed: Set<Pair<Int, Int>>): Set<Pair<Int, Int>> {
        if (placed.isEmpty()) return setOf(0 to 0)
        val neighbours = listOf(1 to 0, 1 to -1, 0 to -1, -1 to 0, -1 to 1, 0 to 1)
        return placed.flatMap { (q, r) -> neighbours.map { (dq, dr) -> (q + dq) to (r + dr) } }.toSet() - placed
    }

    private fun validPlacementHex(coord: Pair<Int, Int>) = HexagonView(
        size = 55, orientation = HexOrientation.POINTY_TOP, visual = ColorVisual(Color(255, 255, 255, 60))
    ).apply { onMouseClicked = { lockedInCoord = coord; updateView() } }

    private fun habitatHex(tile: HabitatTile, highlight: Boolean = false, extraRotation: Int = 0): HexagonView {
        val (imageName, offset) = tileImageInfo(tile)
        return HexagonView(
            size = 55, orientation = HexOrientation.POINTY_TOP,
            visual = ImageVisual("tiles/$imageName.png")
        ).apply {
            rotation = (tile.rotation + extraRotation + offset + 0) * 60.0
            if (highlight) opacity = 1.0
        }
    }

    private fun tileImageInfo(tile: HabitatTile): Pair<String, Int> {
        val currentCodes = tile.habitats.map { habitatCode(it) }
        val r = tile.rotation % 6
        // Recover the original unrotated array by shifting left (reversing the right shift of tile.rotation)
        val originalCodes = currentCodes.drop(r) + currentCodes.take(r)
        for (k in originalCodes.indices) {
            val rotated = (originalCodes.drop(k) + originalCodes.take(k)).joinToString("")
            if (resourceExists("/tiles/$rotated.png")) return Pair(rotated, k)
        }
        return Pair(originalCodes.joinToString(""), 0)
    }

    private fun resourceExists(path: String) = javaClass.getResource(path) != null

    private fun habitatCode(habitat: Habitat) = when (habitat) {
        Habitat.MOUNTAINS -> "M"
        Habitat.RIVERS -> "R"
        Habitat.FORESTS -> "F"
        Habitat.WETLANDS -> "W"
        Habitat.PRAIRIES -> "P"
    }

    private fun tileTokens(tile: HabitatTile, centerX: Double, centerY: Double): List<ComponentView> {
        val visLeft = centerX - HEX_WIDTH / 2
        val visTop = centerY - HEX_SIZE
        val visW = HEX_WIDTH
        val visH = 2 * HEX_SIZE

        val placed = tile.placedWildlifeToken
        if (placed != null) {
            val size = PLACED_FRAC * visW
            return listOf(
                tokenImage("${animalCode(placed.animal)}Active", visLeft + 0.22 * visW, visTop + 0.226 * visH, size)
            )
        }

        val animals = tile.possibleAnimals
        if (animals.isEmpty()) return emptyList()
        val slots = slotLayout(animals.size)
        val size = HINT_FRAC * visW
        return animals.mapIndexed { i, animal ->
            val (leftFrac, topFrac) = slots.getOrElse(i) { slots.last() }
            tokenImage(animalCode(animal), visLeft + leftFrac * visW, visTop + topFrac * visH, size)
        }
    }

    private fun slotLayout(count: Int): List<Pair<Double, Double>> = when (count) {
        1 -> listOf(0.33 to 0.365)
        2 -> listOf(0.23 to 0.305, 0.45 to 0.425)
        3 -> listOf(0.23 to 0.295, 0.36 to 0.485, 0.50 to 0.305)
        else -> listOf(0.33 to 0.365)
    }

    private fun tokenImage(name: String, posX: Double, posY: Double, size: Double) = Label(
        posX = posX, posY = posY, width = size, height = size, visual = ImageVisual("tokens/$name.png")
    )

    private fun animalCode(animal: Wildlife) = when (animal) {
        Wildlife.BEAR -> "B"
        Wildlife.ELK -> "E"
        Wildlife.SALMON -> "S"
        Wildlife.HAWK -> "H"
        Wildlife.FOX -> "F"
    }

    private fun rawHexPos(coord: Pair<Int, Int>): Pair<Double, Double> {
        val (cx, cy) = coord
        val height = 2 * HEX_SIZE
        val q = cx + (cy - (cy and 1)) / 2
        val r = cy
        val x = HEX_WIDTH * q + if (r % 2 == 0) 0.0 else HEX_WIDTH / 2
        val y = height * r - r * height / 4
        return x to y
    }

    private fun log(msg: String) = println("[GameScene] $msg")

    private fun overlay(content: ComponentView): Pane<ComponentView> {
        val pane = Pane<ComponentView>(
            posX = 0, posY = 0, width = 1920, height = 1080, visual = ColorVisual(Color(0, 0, 0, 160))
        )
        content.onMouseClicked = {}
        pane.add(content)
        pane.isVisible = false
        pane.onMouseClicked = { pane.isVisible = false }
        return pane
    }

    private companion object {
        const val HEX_SIZE = 55.0
        const val HINT_FRAC = 0.33
        const val PLACED_FRAC = 0.60
        const val CONTENT_SIZE = 2000.0
        const val BASE_X = 600.0
        const val BASE_Y = 600.0
        const val ART_EDGE_OFFSET = 1
        val HEX_WIDTH = HEX_SIZE * sqrt(3.0)
    }
}
