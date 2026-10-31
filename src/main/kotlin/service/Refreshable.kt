package service

import entity.Player
import service.network.ConnectionState
//import service.network.message.ChatMessage

/**
 * This interface provides a mechanism for the service layer classes to communicate
 * (usually to the GUI classes) that certain changes have been made to the entity
 * layer, so that the user interface can be updated accordingly.
 *
 * Default (empty) implementations are provided for all methods, so that implementing
 * GUI classes only need to react to events relevant to them.
 *
 * @see AbstractRefreshingService
 */
interface Refreshable {
    /** Refresh for turn start. */
    fun refreshAfterStartTurn() {}
    /** Refresh after turn end. */
    fun refreshAfterEndTurn() {}
    /** Refresh for game start. */
    fun refreshAfterStartNewGame() {}
    /** Refresh after create a wildlife bag. */
    fun refreshAfterCreateWildlifeBag(){}
    /** Refresh after create habitat tiles. */
    fun refreshAfterCreateHabitatTiles(){}
    /** Refresh after end game. */
    fun refreshAfterEndGame(scores: List<Pair<Player, Map<String, Int>>>){}
    /** Refresh after calculate score. */
    fun refreshAfterCalculateScore(){}
    /** Refresh after save the game. */
    fun refreshAfterSaveGame(){}
    /** Refresh after load a game. */
    fun refreshAfterLoadGame(){}
    /** Refresh after checking overpopulation. */
    fun refreshAfterCheckOverpopulation(){}
    /** Refresh for log entry. */
    fun refreshAfterLogTurn() {}
    /** Refresh after restock the shop. */
    fun refreshAfterRestockShops(){}
    /** Refresh after choose a combination. */
    fun refreshAfterChooseCombination(){}
    /** Refresh after place habitat tile. */
    fun refreshAfterPlaceHabitatTile(){}
    /** Refresh after place a wildlife token. */
    fun refreshAfterPlaceWildlifeToken(){}
    /** Refresh after redraw wildlife tokens. */
    fun refreshAfterRedrawWildlifeTokens(){}
    /** Refresh after redraw on three wildlife */
    fun refreshAfterRedrawOn3Wildlife(){}
    /** Refresh after undo. */
    fun refreshAfterUndo(){}
    /** Refresh after redo. */
    fun refreshAfterRedo(){}
    /** Refresh after use a nature token. */
    fun refreshAfterUseNatureToken(){}
    /** Refresh after rotating a habitat tile */
    fun refreshAfterRotateTile(){}
    /** Refresh after discarding wildlife token */
    fun refreshAfterDiscardWildlifeToken(){}
    /** Refresh after chat Message */
    fun refreshAfterChatMessage(sender: String, message: String) {}
    /** Refresh after connection state */
    fun refreshConnectionState(state: ConnectionState) {}
}
