package gui

import tools.aqua.bgw.animation.ScaleAnimation
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.Scene
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual
import tools.aqua.bgw.style.BorderRadius

/**
 * Holds the shared design resources for the GUI: the colors, fonts and icon paths, plus the
 * factory functions that build the styled buttons and inputs used across the scenes. The cards,
 * stacks and grids they are placed into are built by [ThemeLayout].
 */
object Theme {

    const val FONT_FAMILY = "'Poppins', 'Segoe UI', sans-serif"

    val gold = Color(0x5D, 0x47, 0x00, 225)
    val buttonBorder = Color(0x90, 0x72, 0x13)
    val navyPanel = Color(0x0A, 0x21, 0x32, 165)
    val greenPanel = Color(0x08, 0x29, 0x28, 165)
    val navyRowFill = Color(0x14, 0x36, 0x52, 225)
    val navyRowFillHover = Color(0x1C, 0x46, 0x66, 225)
    val redAccent = Color(176, 62, 46, 225)
    // val redAccentHover = Color(198, 80, 60, 225)
    val tealAccent = Color(42, 82, 72, 225)
    // val tealAccentHover = Color(58, 103, 90, 225)
    val cream = Color(238, 228, 201)
    val white = Color(240, 240, 236)
    val rowFill = Color(255, 255, 255, 22)
    // val rowFillHover = Color(255, 255, 255, 42)
    val textWarm = Color(226, 219, 199)
    val inputFill = Color(223, 213, 187)
    val inputText = Color(26, 36, 30)
    val warnColor = Color(216, 137, 74)
    val highlight = Color(255, 214, 122)

    val titleFont = Font(
        size = 48, fontWeight = Font.FontWeight.BOLD, color = Color(246, 238, 220), family = FONT_FAMILY
    )
    // val subtitleFont = Font(size = 36, fontWeight = Font.FontWeight.BOLD, color = textWarm, family = "Segoe UI")
    val headerFont = Font(size = 24, fontWeight = Font.FontWeight.BOLD, color = white, family = "Segoe UI")
    val bodyFont = Font(size = 18, color = textWarm, family = "Segoe UI")
    val btnFont = Font(size = 20, fontWeight = Font.FontWeight.BOLD, color = Color(20, 30, 24), family = "Segoe UI")
    // val smallBtnFont = Font(
    //     size = 16, fontWeight = Font.FontWeight.BOLD, color = Color(20, 30, 24), family = "Segoe UI"
    // )
    val inputFont = Font(size = 17, color = inputText, family = "Segoe UI")
    val warnFont = Font(size = 13, color = warnColor, family = "Segoe UI")

    /**
     * Relative file paths of the icon images used by the GUI scenes.
     */
    object Icons {
        const val MOUNTAIN = "icons/crest_mountain.png"
        const val TROPHY = "icons/crest_trophy.png"
        const val LOCAL = "icons/icon_local.png"
        const val ONLINE = "icons/icon_online.png"
        const val JOIN = "icons/icon_join.png"
        const val LOAD = "icons/icon_load.png"
        const val RULES = "icons/icon_rules.png"
        const val CHEVRON_GOLD = "icons/icon_chevron_gold.png"
        const val LOGOUT_WHITE = "icons/icon_logout_white.png"
        const val BACK_CREAM = "icons/icon_back_cream.png"
        const val SHUFFLE_CREAM = "icons/icon_shuffle_cream.png"
        const val USERPLUS_CREAM = "icons/icon_userplus_cream.png"
        const val X_WHITE = "icons/icon_x_white.png"
        const val PLAY_CREAM = "icons/icon_play_cream.png"
        const val GRID_CREAM = "icons/icon_grid_cream.png"
        //const val HOME_CREAM = "icons/icon_home_cream.png"
        const val HOME_DARK = "icons/icon_home_dark.png"
        const val SAVE_CREAM = "icons/icon_save_cream.png"
    }

    /**
     * A pane that behaves like a button and carries an optional text label.
     * The [text] property reads and updates that label; it stays empty when no label was set.
     */
    class IconButton(width: Number, height: Number, visual: ColorVisual) :
        Pane<StaticComponentView<*>>(width = width, height = height, visual = visual) {
        internal var textLabel: Label? = null
        var text: String
            get() = textLabel?.text ?: ""
            set(value) {
                textLabel?.text = value
            }
    }

    /**
     * Visual styling for [modernButton]: the label [font] (falls back to [btnFont] when null),
     * the [baseColor] fill and the corner [radius].
     */
    data class ButtonStyle(
        val font: Font? = null,
        val baseColor: Color = Color(240, 240, 240),
        val radius: BorderRadius = BorderRadius(15.0)
    )

    /**
     * Creates a modern-styled button with hover animations.
     */
    fun modernButton(
        width: Number = 300,
        height: Number = 60,
        text: String,
        scene: Scene<*>,
        buttonStyle: ButtonStyle = ButtonStyle()
    ): Button {
        val btn = Button(
            width = width, height = height,
            text = text,
            font = buttonStyle.font ?: btnFont,
            visual = ColorVisual(buttonStyle.baseColor).apply {
                style.borderRadius = buttonStyle.radius
            }
        )

        attachHoverScale(btn, scene)

        return btn
    }

    private fun attachHoverScale(component: ComponentView, scene: Scene<*>) {
        var animating = false
        var hovered = false

        fun settle() {
            val target = if (hovered) 1.05 else 1.0
            animating = true
            val anim = ScaleAnimation(
                componentView = component, toScaleX = target, toScaleY = target, duration = 150
            ).apply {
                onFinished = {
                    animating = false
                    val desired = if (hovered) 1.05 else 1.0
                    if (desired != target) settle()
                }
            }
            scene.playAnimation(anim)
        }

        component.onMouseEntered = { hovered = true; if (!animating) settle() }
        component.onMouseExited = { hovered = false; if (!animating) settle() }
    }

    private fun addButtonContent(
        inner: Pane<StaticComponentView<*>>,
        text: String,
        icon: String?,
        fontSize: Int,
        textColor: Color
    ): Label? {
        val iw = inner.width
        val ih = inner.height
        return when {
        text.isEmpty() && icon != null -> {
            val iconSize = minOf(iw, ih) * 0.45
            inner.add(Label(width = iconSize, height = iconSize, visual = ImageVisual(icon)).apply {
                posX = (iw - iconSize) / 2.0; posY = (ih - iconSize) / 2.0
            })
            null
        }
        icon == null -> {
            val textLbl = Label(
                width = iw, height = ih, text = text,
                font = btnFont.copy(size = fontSize, color = textColor), alignment = Alignment.CENTER
            )
            inner.add(textLbl)
            textLbl
        }
        else -> {
            val iconSize = (ih * 0.4).coerceAtMost(24.0)
            val gap = 10.0
            val approxTextWidth = text.length * fontSize * 0.56
            val startX = ((iw - iconSize - gap - approxTextWidth) / 2.0).coerceAtLeast(8.0)
            inner.add(Label(width = iconSize, height = iconSize, visual = ImageVisual(icon)).apply {
                posX = startX; posY = (ih - iconSize) / 2.0
            })
            val textX = startX + iconSize + gap
            val textLbl = Label(
                width = (iw - textX).coerceAtLeast(10.0), height = ih, text = text,
                font = btnFont.copy(size = fontSize, color = textColor), alignment = Alignment.CENTER_LEFT
            ).apply { posX = textX; posY = 0.0 }
            inner.add(textLbl)
            textLbl
        }
        }
    }

    /**
     * Visual styling for [iconButton]: the [fill] and [borderColor]/[borderWidth] of the box, the
     * [textColor] and [fontSize] of the caption, an optional [icon] and the corner [radius].
     */
    data class IconStyle(
        val fill: Color,
        val textColor: Color,
        val icon: String? = null,
        val fontSize: Int = 20,
        val radius: Number = 14.0,
        val borderColor: Color = buttonBorder,
        val borderWidth: Number = 2.0
    )

    /**
     * Builds the styled button used throughout the GUI: a rounded box (see [style]) that shows
     * [text], an icon, or both, and grows slightly on hover. The other button helpers
     * ([primaryButton], [backButton], [dangerButton], [chipButton], [removeIconButton]) delegate
     * here with preset colors.
     */
    fun iconButton(
        text: String,
        width: Number,
        height: Number,
        scene: Scene<*>,
        style: IconStyle
    ): IconButton {
        val w = width.toDouble()
        val h = height.toDouble()
        val bw = style.borderWidth.toDouble()
        val r = style.radius.toDouble()

        val btn = IconButton(
            width = w, height = h,
            visual = ColorVisual(style.borderColor).apply { this.style.borderRadius = BorderRadius(r) }
        )

        val iw = w - 2 * bw
        val ih = h - 2 * bw
        val inner = Pane<StaticComponentView<*>>(
            posX = bw, posY = bw, width = iw, height = ih,
            visual = ColorVisual(style.fill).apply {
                this.style.borderRadius = BorderRadius((r - bw).coerceAtLeast(0.0))
            }
        )
        btn.add(inner)

        btn.textLabel = addButtonContent(inner, text, style.icon, style.fontSize, style.textColor)

        attachHoverScale(btn, scene)
        return btn
    }

    /**
     * A gold action button for the main confirm action of a scene, such as Start, Connect or Load.
     */
    fun primaryButton(
        width: Number = 220,
        height: Number = 60,
        text: String,
        icon: String? = null,
        fontSize: Int = 20,
        scene: Scene<*>
    ): IconButton = iconButton(
        text = text, width = width, height = height, scene = scene,
        style = IconStyle(fill = gold, textColor = cream, icon = icon, fontSize = fontSize)
    )

    /**
     * A teal secondary button, defaulting to a "Back" caption with a back-arrow icon.
     */
    fun backButton(
        width: Number = 200,
        height: Number = 60,
        text: String = "Back",
        icon: String? = Icons.BACK_CREAM,
        scene: Scene<*>
    ): IconButton = iconButton(
        text = text, width = width, height = height, scene = scene,
        style = IconStyle(fill = tealAccent, textColor = cream, icon = icon)
    )

    /**
     * A red button for destructive or exit actions, such as Quit or Exit.
     */
    fun dangerButton(
        width: Number = 300,
        height: Number = 60,
        text: String,
        icon: String? = Icons.LOGOUT_WHITE,
        fontSize: Int = 20,
        scene: Scene<*>
    ): IconButton = iconButton(
        text = text, width = width, height = height, scene = scene,
        style = IconStyle(
            fill = redAccent, textColor = Color.WHITE, icon = icon, fontSize = fontSize,
            borderColor = redAccent, borderWidth = 0.0
        )
    )

    /**
     * A small, low-contrast chip button for inline actions like Add, Randomize or a type toggle.
     */
    fun chipButton(
        width: Number = 170,
        height: Number = 42,
        text: String,
        icon: String? = null,
        scene: Scene<*>
    ): IconButton = iconButton(
        text = text, width = width, height = height, scene = scene,
        style = IconStyle(fill = rowFill, textColor = cream, icon = icon, fontSize = 17, radius = 10.0)
    )

    /**
     * A small round red button with a white X, used to remove a row.
     */
    fun removeIconButton(size: Number = 40, scene: Scene<*>): IconButton = iconButton(
        text = "", width = size, height = size, scene = scene,
        style = IconStyle(
            fill = redAccent, textColor = Color.WHITE, icon = Icons.X_WHITE,
            radius = size.toDouble() / 2.0, borderColor = redAccent, borderWidth = 0.0
        )
    )

    /**
     * A styled text input with a cream fill and rounded corners.
     */
    fun textField(
        width: Number = 220,
        height: Number = 42,
        text: String = "",
        prompt: String = "",
        font: Font = inputFont
    ): TextField = TextField(
        width = width, height = height, text = text, prompt = prompt, font = font,
        visual = ColorVisual(inputFill).apply { style.borderRadius = BorderRadius(10.0) }
    )

    /**
     * A wide main-menu row button showing an [icon] on the left and [text] next to it.
     */
    fun menuRow(
        icon: String,
        text: String,
        width: Number = 440,
        height: Number = 64,
        scene: Scene<*>
    ): Pane<StaticComponentView<*>> {
        val w = width.toDouble()
        val h = height.toDouble()
        val bw = 2.0
        val iw = w - 2 * bw
        val ih = h - 2 * bw

        fun bg(color: Color, radius: Double) = ColorVisual(color).apply { style.borderRadius = BorderRadius(radius) }

        val row = Pane<StaticComponentView<*>>(width = w, height = h, visual = bg(buttonBorder, 14.0))
        val inner = Pane<StaticComponentView<*>>(
            posX = bw, posY = bw, width = iw, height = ih, visual = bg(navyRowFill, 12.0)
        )
        row.add(inner)

        val iconSize = 30.0
        val iconLabel = Label(
            width = iconSize, height = iconSize, visual = ImageVisual(icon)
        ).apply { posX = 20.0; posY = (ih - iconSize) / 2.0 }
        val textLabel = Label(
            width = iw - 138, height = ih, text = text, font = Font(
                size = 20, fontWeight = Font.FontWeight.BOLD, color = cream, family = "Segoe UI"
            ), alignment = Alignment.CENTER_LEFT
        ).apply { posX = 64.0; posY = 0.0 }
        val chevronSize = 18.0
        val chevron = Label(
            width = chevronSize, height = chevronSize, visual = ImageVisual(Icons.CHEVRON_GOLD)
        ).apply { posX = iw - 38.0; posY = (ih - chevronSize) / 2.0 }

        inner.add(iconLabel)
        inner.add(textLabel)
        inner.add(chevron)

        var animating = false
        var hovered = false

        fun settle() {
            inner.visual = bg(if (hovered) navyRowFillHover else navyRowFill, 12.0)
            val target = if (hovered) 1.02 else 1.0
            animating = true
            val anim = ScaleAnimation(
                componentView = row, toScaleX = target, toScaleY = target, duration = 150
            ).apply {
                onFinished = {
                    animating = false
                    val desired = if (hovered) 1.02 else 1.0
                    if (desired != target) settle()
                }
            }
            scene.playAnimation(anim)
        }

        row.onMouseEntered = { hovered = true; if (!animating) settle() }
        row.onMouseExited = { hovered = false; if (!animating) settle() }

        return row
    }
}
