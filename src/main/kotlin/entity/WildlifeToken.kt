package entity

import java.io.Serializable

/**
 * Class [WildlifeToken] represents the wildlife tokens
 */
data class WildlifeToken (val animal : Wildlife): Serializable{

    /**
     * Companion object containing constants for serialization.
     */
    companion object {
        private const val serialVersionUID = 1L
    }
}