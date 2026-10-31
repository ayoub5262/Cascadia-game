package gui

import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.StaticComponentView
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual
import tools.aqua.bgw.style.BorderRadius

/**
 * Builds the layout structures used across the GUI scenes: the bordered cards, the vertical
 * stacks and the grids, plus the dividers and crests placed inside them. The colors and icons
 * come from [Theme].
 */
object ThemeLayout {

    /**
     * Visual and layout options for [cardContainer]: an optional fixed [width], the set of
     * [centered] components, the [contentPadding] of left-aligned components, the [topGap] and
     * [bottomGap] insets and the [borderColor], [borderWidth] and corner [radius] of the frame.
     */
    data class CardStyle(
        val width: Number? = null,
        val centered: Set<StaticComponentView<*>> = emptySet(),
        val contentPadding: Number = 32,
        val topGap: Number? = null,
        val bottomGap: Number? = null,
        val borderColor: Color = Theme.gold,
        val borderWidth: Number = 3.0,
        val radius: Number = 26.0
    )

    /**
     * Builds a bordered card panel around the given [components], stacked vertically with [spacing]
     * between them and a frame around a [fill]-colored inner area. The frame, padding and centering
     * are configured through [style].
     */
    fun cardContainer(
        posX: Number,
        posY: Number,
        spacing: Number,
        fill: Color,
        components: List<StaticComponentView<*>>,
        style: CardStyle = CardStyle()
    ): Pane<StaticComponentView<*>> {
        val bw = style.borderWidth.toDouble()
        val pad = style.contentPadding.toDouble()
        val cardRadius = style.radius.toDouble()
        val innerWidth = style.width?.let { it.toDouble() - 2 * bw }
            ?: ((components.maxOfOrNull { it.width } ?: 0.0) + 2 * pad)

        val topGapValue = style.topGap?.toDouble() ?: spacing.toDouble()
        val bottomGapValue = style.bottomGap?.toDouble() ?: spacing.toDouble()

        var currentY = topGapValue
        for ((idx, comp) in components.withIndex()) {
            if (idx == components.size - 1 && components.size > 1) {
                currentY = currentY - spacing.toDouble() + bottomGapValue
            }
            comp.posX = if (comp in style.centered) (innerWidth - comp.width) / 2.0 else pad
            comp.posY = currentY
            currentY += comp.height + spacing.toDouble()
        }

        val inner = Pane<StaticComponentView<*>>(
            posX = bw, posY = bw, width = innerWidth, height = currentY,
            visual = ColorVisual(fill).apply { this.style.borderRadius = BorderRadius(cardRadius - bw) }
        )
        for (comp in components) inner.add(comp)

        val outer = Pane<StaticComponentView<*>>(
            posX = posX, posY = posY,
            width = innerWidth + 2 * bw, height = inner.height + 2 * bw,
            visual = ColorVisual(style.borderColor).apply { this.style.borderRadius = BorderRadius(cardRadius) }
        )
        outer.add(inner)
        return outer
    }

    /**
     * Empties a card built by [cardContainer], removing all components from its inner pane while
     * keeping the card frame.
     */
    fun clearCard(card: Pane<StaticComponentView<*>>) {
        (card.components.firstOrNull() as? Pane<*>)?.clear()
    }

    /**
     * Stacks the [components] vertically with [spacing] between them and returns a pane sized to fit.
     */
    fun stackVertical(spacing: Number, components: List<StaticComponentView<*>>): Pane<StaticComponentView<*>> {
        var y = 0.0
        var maxWidth = 0.0
        for (comp in components) {
            comp.posX = 0.0
            comp.posY = y
            y += comp.height + spacing.toDouble()
            if (comp.width > maxWidth) maxWidth = comp.width
        }
        val height = (y - spacing.toDouble()).coerceAtLeast(0.0)
        val pane = Pane<StaticComponentView<*>>(width = maxWidth, height = height)
        for (comp in components) pane.add(comp)
        return pane
    }

    /**
     * A thin horizontal line used to separate sections, [width] wide and [height] tall in [color].
     */
    fun divider(width: Number, height: Number = 3, color: Color = Theme.gold): Label {
        return Label(width = width, height = height, text = "", visual = ColorVisual(color))
    }

    /**
     * A square label showing the [icon] image at the given [size], used as a scene crest.
     */
    fun crestLabel(icon: String = Theme.Icons.MOUNTAIN, size: Number = 54): Label {
        return Label(width = size, height = size, text = "", visual = ImageVisual(icon))
    }

    /**
     * Creates a grid layout for components.
     */
    fun createGrid(
        columns: Int,
        rows: Int,
        spacing: Number,
        vararg components: Pair<Pair<Int, Int>, StaticComponentView<*>>
    ): Pane<StaticComponentView<*>> {
        val colWidths = DoubleArray(columns) { 0.0 }
        val rowHeights = DoubleArray(rows) { 0.0 }

        for ((pos, comp) in components) {
            val (col, row) = pos
            if (comp.width > colWidths[col]) colWidths[col] = comp.width
            if (comp.height > rowHeights[row]) rowHeights[row] = comp.height
        }

        val paneWidth = colWidths.sum() + (columns - 1) * spacing.toDouble()
        val paneHeight = rowHeights.sum() + (rows - 1) * spacing.toDouble()
        val pane = Pane<StaticComponentView<*>>(width = paneWidth, height = paneHeight)

        for ((pos, comp) in components) {
            val (col, row) = pos
            var posX = 0.0
            for (i in 0 until col) posX += colWidths[i] + spacing.toDouble()
            var posY = 0.0
            for (i in 0 until row) posY += rowHeights[i] + spacing.toDouble()

            comp.posX = posX
            comp.posY = posY + (rowHeights[row] - comp.height) / 2.0
            pane.add(comp)
        }

        return pane
    }
}
