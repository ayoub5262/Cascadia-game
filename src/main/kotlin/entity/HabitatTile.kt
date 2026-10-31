package entity

import java.io.Serializable

/**
 * Class [HabitatTile] represents a habitat tile
 */
data class HabitatTile(val habitats: MutableList<Habitat>, val possibleAnimals: List<Wildlife>): Serializable{
    var id: Int = 0
    var isKeyStone: Boolean = false
    var rotation: Int = 0
    var placedWildlifeToken : WildlifeToken? = null

    /**
     * Companion object containing constants for serialization.
     */
    companion object {
        private const val serialVersionUID = 1L
    }
}