package gui

import entity.Player
import service.Refreshable
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ImageVisual

/**
 * Shows the final scores after the game ends.
 * The table is built dynamically in [refreshAfterEndGame]: one column per player,
 * one row per score category (as reported by GameActionService.calculateScore()),
 * with the total score in the last row.
 */
class ResultMenuScene : MenuScene(1920, 1080), Refreshable {

    var menuContainer: Pane<StaticComponentView<*>>? = null

    private val crestLabel = ThemeLayout.crestLabel(Theme.Icons.TROPHY)

    val titleLabel = Label(
        width = 460,
        height = 80,
        text = "FINAL SCORES",
        font = Theme.titleFont,
        alignment = Alignment.CENTER
    )
    private val titleDivider = ThemeLayout.divider(320)

    private val rowLabelFont = Font(size = 16, fontWeight = Font.FontWeight.BOLD, color = Theme.textWarm)
    private val totalRowFont = Font(size = 18, fontWeight = Font.FontWeight.BOLD, color = Theme.cream)
    private val winnerTotalFont = Font(size = 18, fontWeight = Font.FontWeight.BOLD, color = Theme.highlight)
    private val playerHeaderFont = Font(size = 20, fontWeight = Font.FontWeight.BOLD, color = Theme.cream)
    private val winnerHeaderFont = Font(size = 20, fontWeight = Font.FontWeight.BOLD, color = Theme.highlight)

    // Player score columns are added here dynamically
    private val dataPane = Pane<StaticComponentView<*>>(width = 760, height = 420)
    private val dynamicLabels = mutableListOf<Label>()

    val viewBoardBtn = Theme.backButton(
        width = 200, height = 60, text = "View Board", icon = Theme.Icons.GRID_CREAM, scene = this
    )
    val mainMenuBtn = Theme.iconButton(
        text = "Main Menu", width = 200, height = 60, scene = this,
        style = Theme.IconStyle(fill = Theme.cream, textColor = Color(20, 30, 24), icon = Theme.Icons.HOME_DARK)
    )

    val buttonsRow = Pane<StaticComponentView<*>>(width = 420, height = 60).apply {
        viewBoardBtn.posX = 0.0
        viewBoardBtn.posY = 0.0
        mainMenuBtn.posX = 220.0
        mainMenuBtn.posY = 0.0
        add(viewBoardBtn)
        add(mainMenuBtn)
    }

    // Score categories shown as table rows, in the order produced by GameActionService.calculateScore()
    // ("Total" is appended separately as the final summary row)
    private val scoreRows = listOf(
        "Bears" to "BEARS",
        "Elks" to "ELKS",
        "Salmons" to "SALMONS",
        "Hawks" to "HAWKS",
        "Foxes" to "FOXES",
        "Habitat Base" to "HABITAT BASE",
        "Habitat Bonus" to "HABITAT BONUS",
        "Nature Tokens" to "NATURE TOKENS"
    )

    init {
        background = ImageVisual("background.png")
        backgroundOpacity = 1.0

        rebuild()
    }

    private fun rebuild() {
        menuContainer?.let {
            ThemeLayout.clearCard(it)
            removeComponents(it)
        }

        val newContainer = ThemeLayout.cardContainer(
            posX = 0, posY = 0, spacing = 22.0,
            fill = Theme.greenPanel,
            components = listOf(crestLabel, titleLabel, titleDivider, dataPane, buttonsRow),
            style = ThemeLayout.CardStyle(
                topGap = 32.0,
                centered = setOf(crestLabel, titleLabel, titleDivider, buttonsRow, dataPane)
            )
        )
        newContainer.posX = (1920.0 - newContainer.width) / 2.0
        newContainer.posY = (1080.0 - newContainer.height) / 2.0

        menuContainer = newContainer
        addComponents(newContainer)
    }

    // Adds a single label to dataPane and tracks it for later removal
    private fun addScoreLabel(
        text: String, x: Double, y: Double, font: Font,
        width: Number = 180, alignment: Alignment = Alignment.CENTER
    ) {
        val label = Label(width = width, height = 32, text = text, font = font, alignment = alignment)
            .apply {
                posX = x
                posY = y
            }
        dataPane.add(label)
        dynamicLabels.add(label)
    }

    /**
     * Clears and rebuilds the score table with actual player data.
     * Score map keys as produced by GameActionService.calculateScore():
     * "Bears", "Elks", "Salmons", "Hawks", "Foxes", "Habitat Base", "Habitat Bonus", "Nature Tokens", "Total"
     *
     * Layout: player names as column headers, one score category per row, total in the last row.
     */
    override fun refreshAfterEndGame(scores: List<Pair<Player, Map<String, Int>>>) {
        dynamicLabels.forEach { dataPane.remove(it) }
        dynamicLabels.clear()

        val sorted = scores.sortedByDescending { (_, map) -> map["Total"] ?: 0 }
        val topScore = sorted.firstOrNull()?.second?.get("Total") ?: 0

        val labelColWidth = 220.0
        val playerColWidth = 200.0
        val colSpacing = 20.0
        val rowHeight = 40.0

        val totalWidth = labelColWidth + colSpacing + sorted.size * (playerColWidth + colSpacing)
        dataPane.width = totalWidth
        dataPane.height = (scoreRows.size + 2) * rowHeight
        val xOffset = 0.0

        // Row labels (category names + TOTAL), left column
        scoreRows.forEachIndexed { rowIndex, (_, displayName) ->
            val y = (rowIndex + 1) * rowHeight
            addScoreLabel(
                displayName, xOffset, y, rowLabelFont,
                width = labelColWidth, alignment = Alignment.CENTER_LEFT
            )
        }
        val totalRowY = (scoreRows.size + 1) * rowHeight
        addScoreLabel(
            "★ TOTAL", xOffset, totalRowY, totalRowFont,
            width = labelColWidth, alignment = Alignment.CENTER_LEFT
        )

        // One column per player
        sorted.forEachIndexed { colIndex, (player, map) ->
            val isWinner = (map["Total"] ?: 0) == topScore
            val headerFont = if (isWinner) winnerHeaderFont else playerHeaderFont
            val bodyFont = if (isWinner) Theme.bodyFont.copy(color = Theme.highlight) else Theme.bodyFont
            val totalFont = if (isWinner) winnerTotalFont else totalRowFont

            val x = xOffset + labelColWidth + colSpacing + colIndex * (playerColWidth + colSpacing)

            val headerText = if (isWinner) "★ ${player.name}" else player.name
            addScoreLabel(headerText, x, 0.0, headerFont, width = playerColWidth)
            scoreRows.forEachIndexed { rowIndex, (key, _) ->
                val y = (rowIndex + 1) * rowHeight
                addScoreLabel("${map[key] ?: 0}", x, y, bodyFont, width = playerColWidth)
            }
            addScoreLabel("${map["Total"] ?: 0}", x, totalRowY, totalFont, width = playerColWidth)
        }

        rebuild()
    }
}
