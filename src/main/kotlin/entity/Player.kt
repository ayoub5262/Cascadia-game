package entity

import java.io.Serializable

/**
 *Die Klasse Player beschreibt die Klasse, die den Namen des Players erhält und dessen Board.
 */

data class Player(val name: String, val playMode: PlayerType, var natureTokens: Int): Serializable {
    val board: MutableMap<Pair<Int, Int>, HabitatTile> = mutableMapOf()

    /**
     * Companion object containing constants for serialization.
     */
    companion object {
        private const val serialVersionUID = 1L
    }
}