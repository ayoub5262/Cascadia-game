package gui

import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.visual.ImageVisual

/**
 * The main menu scene of the game. Lets the player start a local or online game, join a game,
 * load a save, open the rules or exit.
 */
class MainMenuScene : MenuScene(1920, 1080) {

    val menuContainer: Pane<StaticComponentView<*>>

    private val crestLabel = ThemeLayout.crestLabel()

    val titleLabel = Label(
        width = 320, height = 70,
        text = "CASCADIA",
        font = Theme.titleFont,
        alignment = Alignment.CENTER
    )

    private val titleDivider = ThemeLayout.divider(260)

    val localGameButton = Theme.menuRow(Theme.Icons.LOCAL, "LOCAL GAME", scene = this)
    val onlineGameButton = Theme.menuRow(Theme.Icons.ONLINE, "ONLINE GAME", scene = this)
    val joinGameButton = Theme.menuRow(Theme.Icons.JOIN, "JOIN GAME", scene = this)
    val loadGameButton = Theme.menuRow(Theme.Icons.LOAD, "LOAD GAME", scene = this)
    val rulesButton = Theme.menuRow(Theme.Icons.RULES, "GAME RULES", scene = this)
    val exitButton = Theme.dangerButton(width = 440, text = "EXIT", scene = this)

    init {
        background = ImageVisual("background.png")
        backgroundOpacity = 1.0

        menuContainer = ThemeLayout.cardContainer(
            posX = 0, posY = 0, spacing = 22.0,
            fill = Theme.navyPanel,
            components = listOf(
                crestLabel, titleLabel, titleDivider,
                localGameButton, onlineGameButton, joinGameButton, loadGameButton, rulesButton, exitButton
            ),
            style = ThemeLayout.CardStyle(
                centered = setOf(
                    crestLabel, titleLabel, titleDivider,
                    localGameButton, onlineGameButton, joinGameButton, loadGameButton, rulesButton, exitButton
                ),
                topGap = 32.0
            )
        )
        menuContainer.posX = (1920.0 - menuContainer.width) / 2.0
        menuContainer.posY = (1080.0 - menuContainer.height) / 2.0

        addComponents(menuContainer)
    }
}
