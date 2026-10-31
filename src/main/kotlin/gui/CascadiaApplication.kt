package gui

import entity.Player
import service.Refreshable
import service.RootService
import tools.aqua.bgw.core.BoardGameApplication

/**
 * Represents the main application for the Cascadia board game.
 * The application initializes the [RootService] and displays the scenes.
 */
class CascadiaApplication : BoardGameApplication("Cascadia"), Refreshable {

    private val rootService = RootService()

    val mainMenuScene = MainMenuScene()
    val localLobbyScene = LocalLobbyScene(rootService)
    val onlineLobbyScene = OnlineLobbyScene(rootService)
    val loadGameScene = LoadGameScene(rootService)
    val joinGameScene = JoinGameScene(rootService)
    val resultScene = ResultMenuScene()
    val rulesScene = RulesScene()
    val gameScene = GameScene(rootService)

    init {
        rootService.addRefreshables(
            gameScene.lifecycleRefresher, gameScene.actionRefresher, this, resultScene
        )

        // Main Menu routing
        mainMenuScene.localGameButton.apply {
            onMouseClicked = {
                showMenuScene(localLobbyScene)
            }
        }

        mainMenuScene.onlineGameButton.apply {
            onMouseClicked = {
                showMenuScene(onlineLobbyScene)
            }
        }

        mainMenuScene.joinGameButton.apply {
            onMouseClicked = {
                showMenuScene(joinGameScene)
            }
        }

        mainMenuScene.loadGameButton.apply {
            onMouseClicked = {
                loadGameScene.refreshEntries()
                showMenuScene(loadGameScene)
            }
        }

        mainMenuScene.exitButton.apply {
            onMouseClicked = {
                exit()
            }
        }

        mainMenuScene.rulesButton.apply {
            onMouseClicked = {
                rulesScene.closeButton.onMouseClicked = { showMenuScene(mainMenuScene) }
                showMenuScene(rulesScene)
            }
        }

        gameScene.helpButton.apply {
            onMouseClicked = {
                rulesScene.closeButton.onMouseClicked = { hideMenuScene() }
                showMenuScene(rulesScene)
            }
        }

        gameScene.quitButton.apply {
            onMouseClicked = {
                rootService.gameActionService.quitGame()
                showMenuScene(mainMenuScene)
            }
        }

        // Join Game routing
        joinGameScene.backButton.apply {
            onMouseClicked = {
                showMenuScene(mainMenuScene)
            }
        }

        // Local Lobby routing
        localLobbyScene.backButton.apply {
            onMouseClicked = {
                showMenuScene(mainMenuScene)
            }
        }

        // Online Lobby routing
        onlineLobbyScene.backButton.apply {
            onMouseClicked = {
                showMenuScene(mainMenuScene)
            }
        }

        // Load Game routing
        loadGameScene.backButton.apply {
            onMouseClicked = {
                showMenuScene(mainMenuScene)
            }
        }

        // Results routing

        resultScene.viewBoardBtn.apply {
            onMouseClicked = {
                hideMenuScene()
            }
        }

        gameScene.showResultButton.apply {
            onMouseClicked = {
                showMenuScene(resultScene)
            }
        }

        resultScene.mainMenuBtn.apply {
            onMouseClicked = {
                showMenuScene(mainMenuScene)
            }
        }

        // Initial setup
        showMenuScene(mainMenuScene)
        show()
    }

    override fun refreshAfterStartNewGame() {
        hideMenuScene()
        showGameScene(gameScene)

        // brauche das weil network selber den zug startet und deswegen das game nicht doppelt starten darf
        if (rootService.currentGame?.currentState?.isHotseatMode == true) {
            rootService.gameActionService.startTurn()
        }
    }

    override fun refreshAfterLoadGame() {
        hideMenuScene()
        showGameScene(gameScene)
    }

    override fun refreshAfterEndGame(scores: List<Pair<Player, Map<String, Int>>>) {
        showMenuScene(resultScene)
    }
}
