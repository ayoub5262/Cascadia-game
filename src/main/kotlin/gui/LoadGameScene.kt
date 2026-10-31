package gui

import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.event.WheelDirection
import service.RootService
import tools.aqua.bgw.visual.ImageVisual
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The load game scene of the game.
 *
 * @constructor Creates a new Load Game Scene with the specified rootService.
 *
 * @param rootService The [RootService] that manages the game state.
 */
class LoadGameScene(private val rootService: RootService) : MenuScene(1920, 1080) {

    val menuContainer: Pane<StaticComponentView<*>>

    private val crestLabel = ThemeLayout.crestLabel(Theme.Icons.LOAD)
    val titleLabel = Label(width = 380, height = 80, text = "LOAD GAME", font = Theme.titleFont,
        alignment = Alignment.CENTER)
    private val titleDivider = ThemeLayout.divider(280)

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val rowHeight = 54.0
    private val viewportHeight = 260.0
    private val maxVisibleRows = (viewportHeight / rowHeight).toInt()

    private val entriesPane = Pane<StaticComponentView<*>>(width = 480, height = viewportHeight).apply {
        onMouseWheel = { event ->
            val step = if (event.direction == WheelDirection.UP) -1 else 1
            val maxOffset = maxOf(0, allSaveFiles.size - maxVisibleRows)
            scrollOffset = (scrollOffset + step).coerceIn(0, maxOffset)
            renderVisibleEntries()
        }
    }
    private val entryComponents = mutableListOf<StaticComponentView<*>>()

    private var allSaveFiles: List<File> = emptyList()
    private var scrollOffset = 0

    // Hints that indicate more entries can be reached by scrolling; only shown when applicable
    private val scrollUpHint = Label(width = 480, height = 18, text = "▲ scroll for more",
        font = Theme.bodyFont.copy(size = 12), alignment = Alignment.CENTER).apply { isVisible = false }
    private val scrollDownHint = Label(width = 480, height = 18, text = "▼ scroll for more",
        font = Theme.bodyFont.copy(size = 12), alignment = Alignment.CENTER).apply { isVisible = false }

    val backButton = Theme.backButton(width = 230, height = 64, scene = this)

    init {
        background = ImageVisual("background.png")
        backgroundOpacity = 1.0

        menuContainer = ThemeLayout.cardContainer(
            posX = 0, posY = 0, spacing = 16.0,
            fill = Theme.greenPanel,
            components = listOf(
                crestLabel, titleLabel, titleDivider, scrollUpHint, entriesPane, scrollDownHint, backButton
            ),
            style = ThemeLayout.CardStyle(
                centered = setOf(crestLabel, titleLabel, titleDivider, scrollUpHint, scrollDownHint),
                topGap = 26.0, bottomGap = 32.0
            )
        )
        menuContainer.posX = (1920.0 - menuContainer.width) / 2.0
        menuContainer.posY = (1080.0 - menuContainer.height) / 2.0

        addComponents(menuContainer)

        refreshEntries()
    }

    /**
     * Rescans the working directory for save files and resets the scrollable entry list.
     * Call this whenever the scene is shown, since save files may have been created
     * after this scene was constructed.
     */
    fun refreshEntries() {
        allSaveFiles = (File(".").listFiles { file ->
            file.isFile && file.name.startsWith("savegame_") && file.name.endsWith(".dat")
        } ?: emptyArray()).sortedByDescending { it.lastModified() }

        scrollOffset = 0
        renderVisibleEntries()
    }

    // Renders only the entries in [scrollOffset, scrollOffset + maxVisibleRows) at fixed row
    // slots 0..maxVisibleRows-1, so entriesPane's content never exceeds its own bounds.
    private fun renderVisibleEntries() {
        entryComponents.forEach { entriesPane.remove(it) }
        entryComponents.clear()

        scrollUpHint.isVisible = scrollOffset > 0
        scrollDownHint.isVisible = scrollOffset + maxVisibleRows < allSaveFiles.size

        if (allSaveFiles.isEmpty()) {
            val noneLabel = Label(width = 480, height = 30, text = "No saved games found",
                font = Theme.bodyFont, alignment = Alignment.CENTER)
                .apply { posX = 0.0; posY = (viewportHeight - 30) / 2.0 }
            entriesPane.add(noneLabel)
            entryComponents.add(noneLabel)
            return
        }

        val visible = allSaveFiles.drop(scrollOffset).take(maxVisibleRows)
        visible.forEachIndexed { row, file ->
            val y = row * rowHeight
            val modified = Instant.ofEpochMilli(file.lastModified()).atZone(ZoneId.systemDefault())

            val titleLbl = Label(width = 300, height = 44, text = dateFormatter.format(modified),
                font = Theme.headerFont, alignment = Alignment.CENTER_LEFT).apply { posX = 0.0; posY = y }
            val loadBtn = Theme.primaryButton(width = 150, height = 44, text = "Load", fontSize = 17, scene = this)
                .apply { posX = 330.0; posY = y }
            loadBtn.onMouseClicked = { rootService.gameActionService.loadGame(file.name) }

            entriesPane.add(titleLbl); entryComponents.add(titleLbl)
            entriesPane.add(loadBtn); entryComponents.add(loadBtn)
        }
    }
}
