package entity

import tools.aqua.bgw.util.Stack
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * Klasse für CascadiaGame, sind alle wichtigen daten für das Game mit deren Start werten.
 */

data class CascadiaGame(val players: MutableList<Player>, val simulationSpeed: BotSpeed): Serializable {

    var isHotseatMode: Boolean = false
    var currentPlayer: Int = 0
    var roundNumber: Int = 0
    var natureTokenUsed: Boolean = false
    var natureTokenCount: Int = 0
    var drawnCombination: Pair<HabitatTile, WildlifeToken>? = null
    var threeWildlifeIdentical: Boolean = false
    var discardedWildlife: MutableList<WildlifeToken> = mutableListOf()
    var wildlifeShop: Array<WildlifeToken?> = arrayOfNulls(4)
    var habitatShop: Array<HabitatTile?> = arrayOfNulls(4)

    @Transient
    var wildlifeBag: Stack<WildlifeToken> = Stack()
    @Transient
    var habitatDrawPile: Stack<HabitatTile> = Stack()

    var scoringCards: MutableList<ScoringCard> = mutableListOf()
    var gameState: GameState = GameState.TURNSTARTED
    var threeRedrawnInTurn: Boolean = false

    private fun writeObject(out: ObjectOutputStream) {
        out.defaultWriteObject()                             // speichert alle normalen Felder
        out.writeObject(ArrayList(wildlifeBag.peekAll()))     // Stack -> Liste -> speichern
        out.writeObject(ArrayList(habitatDrawPile.peekAll()))
    }

    @Suppress("UNCHECKED_CAST")
    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
        wildlifeBag = Stack((input.readObject() as ArrayList<WildlifeToken>).asReversed())
        habitatDrawPile = Stack((input.readObject() as ArrayList<HabitatTile>).asReversed())
    }

    /**
     * Companion object containing constants for serialization.
     */
    companion object {
        private const val serialVersionUID = 1L
    }
}
