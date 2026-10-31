package gui

import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ImageVisual
import tools.aqua.bgw.core.Color

/**
 * Shows the game rules as a sequence of images. The player pages through them with the
 * previous and next buttons and closes the scene with the close button.
 */
class RulesScene : MenuScene(1920, 1080) {
    
    private var currentRuleIndex = 1
    private val maxRules = 8

    val rulesImageLabel = Label(
        width = 1925, height = 964,
        visual = ImageVisual("CascadiaRules/CascadiaRules-1.png")
    ).apply {
        scale = 0.8
        posX = (1920 - width) / 2
        posY = (1080 - height) / 2
    }

    val prevButton = Theme.modernButton(
        width = 100, height = 100,
        text = "<", 
        scene = this,
        buttonStyle = Theme.ButtonStyle(
            font = Font(size = 50, fontWeight = Font.FontWeight.BOLD, color = Color.WHITE),
            baseColor = Color(0, 0, 0, 150)
        )
    ).apply {
        posX = 80.0
        posY = 490.0
        isDisabled = true
        onMouseClicked = {
            if (currentRuleIndex > 1) {
                currentRuleIndex--
                updateImage()
            }
        }
    }

    val nextButton = Theme.modernButton(
        width = 100, height = 100,
        text = ">", 
        scene = this,
        buttonStyle = Theme.ButtonStyle(
            font = Font(size = 50, fontWeight = Font.FontWeight.BOLD, color = Color.WHITE),
            baseColor = Color(0, 0, 0, 150)
        )
    ).apply {
        posX = 1740.0
        posY = 490.0
        onMouseClicked = {
            if (currentRuleIndex < maxRules) {
                currentRuleIndex++
                updateImage()
            }
        }
    }

    val closeButton = Theme.modernButton(
        width = 80, height = 80,
        text = "X", 
        scene = this,
        buttonStyle = Theme.ButtonStyle(
            font = Font(size = 40, fontWeight = Font.FontWeight.BOLD, color = Color.WHITE),
            baseColor = Color(244, 67, 54)
        )
    ).apply {
        posX = 1740.0
        posY = 108.0
    }

    init {
        background = ImageVisual("background.png")
        backgroundOpacity = 0.5
        
        addComponents(
            rulesImageLabel,
            prevButton, nextButton,
            closeButton
        )
    }

    private fun updateImage() {
        rulesImageLabel.visual = ImageVisual("CascadiaRules/CascadiaRules-$currentRuleIndex.png")
        prevButton.isDisabled = currentRuleIndex == 1
        nextButton.isDisabled = currentRuleIndex == maxRules
    }
}