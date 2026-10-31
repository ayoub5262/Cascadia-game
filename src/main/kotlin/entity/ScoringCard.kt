package entity

import java.io.Serializable

/**
 * Class [ScoringCard] represents the scoring cards
 */
data class ScoringCard(val isCardTypeA: Boolean,val wildlife: Wildlife): Serializable{

    /**
     * Companion object containing constants for serialization.
     */
    companion object {
        private const val serialVersionUID = 1L
    }
}