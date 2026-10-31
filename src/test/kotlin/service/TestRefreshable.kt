package service

import entity.Player
import service.network.ConnectionState

/**
 * Test-Refreshable, das speichert, welche Refresh-Methoden aufgerufen wurden.
 */
class TestRefreshable(val rootService: RootService? = null) : Refreshable {

    var refreshAfterStartTurnCalled = false
        private set

    var refreshAfterEndTurnCalled = false
        private set

    var refreshAfterStartNewGameCalled = false
        private set

    var refreshAfterCreateWildlifeBagCalled = false
        private set

    var refreshAfterCreateHabitatTilesCalled = false
        private set

    var refreshAfterEndGameCalled = false
        private set

    var refreshAfterCalculateScoreCalled = false
        private set

    var refreshAfterSaveGameCalled = false
        private set

    var refreshAfterLoadGameCalled = false
        private set

    var refreshAfterCheckOverpopulationCalled = false
        private set

    var refreshAfterLogTurnCalled = false
        private set

    var refreshAfterRestockShopsCalled = false
        private set

    var refreshAfterChooseCombinationCalled = false
        private set

    var refreshAfterPlaceHabitatTileCalled = false
        private set

    var refreshAfterPlaceWildlifeTokenCalled = false
        private set

    var refreshAfterRedrawWildlifeTokensCalled = false
        private set

    var refreshAfterRedrawOn3WildlifeCalled = false
        private set

    var refreshAfterUndoCalled = false
        private set

    var refreshAfterRedoCalled = false
        private set

    var refreshAfterUseNatureTokenCalled = false
        private set

    var refreshAfterRotateTileCalled = false
        private set

    var refreshAfterDiscardWildlifeTokenCalled = false
        private set

    var refreshAfterChatMessageCalled = false
        private set

    var refreshConnectionStateCalled = false
        private set

    var lastChatSender: String? = null
        private set

    var lastChatMessage: String? = null
        private set

    var lastConnectionState: ConnectionState? = null
        private set

    /**
     * Setzt alle gespeicherten Aufrufe zurück, ebenso die zuletzt empfangene Chat-Nachricht
     * und den zuletzt gemeldeten Verbindungsstatus.
     */
    fun reset() {
        refreshAfterStartTurnCalled = false
        refreshAfterEndTurnCalled = false
        refreshAfterStartNewGameCalled = false
        refreshAfterCreateWildlifeBagCalled = false
        refreshAfterCreateHabitatTilesCalled = false
        refreshAfterEndGameCalled = false
        refreshAfterCalculateScoreCalled = false
        refreshAfterSaveGameCalled = false
        refreshAfterLoadGameCalled = false
        refreshAfterCheckOverpopulationCalled = false
        refreshAfterLogTurnCalled = false
        refreshAfterRestockShopsCalled = false
        refreshAfterChooseCombinationCalled = false
        refreshAfterPlaceHabitatTileCalled = false
        refreshAfterPlaceWildlifeTokenCalled = false
        refreshAfterRedrawWildlifeTokensCalled = false
        refreshAfterRedrawOn3WildlifeCalled = false
        refreshAfterUndoCalled = false
        refreshAfterRedoCalled = false
        refreshAfterUseNatureTokenCalled = false
        refreshAfterRotateTileCalled = false
        refreshAfterDiscardWildlifeTokenCalled = false
        refreshAfterChatMessageCalled = false
        refreshConnectionStateCalled = false

        lastChatSender = null
        lastChatMessage = null
        lastConnectionState = null
    }

    override fun refreshAfterStartTurn() {
        refreshAfterStartTurnCalled = true
    }

    override fun refreshAfterEndTurn() {
        refreshAfterEndTurnCalled = true
    }

    override fun refreshAfterStartNewGame() {
        refreshAfterStartNewGameCalled = true
    }

    override fun refreshAfterCreateWildlifeBag() {
        refreshAfterCreateWildlifeBagCalled = true
    }

    override fun refreshAfterCreateHabitatTiles() {
        refreshAfterCreateHabitatTilesCalled = true
    }

    override fun refreshAfterEndGame(scores: List<Pair<Player, Map<String, Int>>>) {
        refreshAfterEndGameCalled = true
    }

    override fun refreshAfterCalculateScore() {
        refreshAfterCalculateScoreCalled = true
    }

    override fun refreshAfterSaveGame() {
        refreshAfterSaveGameCalled = true
    }

    override fun refreshAfterLoadGame() {
        refreshAfterLoadGameCalled = true
    }

    override fun refreshAfterCheckOverpopulation() {
        refreshAfterCheckOverpopulationCalled = true
    }

    override fun refreshAfterLogTurn() {
        refreshAfterLogTurnCalled = true
    }

    override fun refreshAfterRestockShops() {
        refreshAfterRestockShopsCalled = true
    }

    override fun refreshAfterChooseCombination() {
        refreshAfterChooseCombinationCalled = true
    }

    override fun refreshAfterPlaceHabitatTile() {
        refreshAfterPlaceHabitatTileCalled = true
    }

    override fun refreshAfterPlaceWildlifeToken() {
        refreshAfterPlaceWildlifeTokenCalled = true
    }

    override fun refreshAfterRedrawWildlifeTokens() {
        refreshAfterRedrawWildlifeTokensCalled = true
    }

    override fun refreshAfterRedrawOn3Wildlife() {
        refreshAfterRedrawOn3WildlifeCalled = true
    }

    override fun refreshAfterUndo() {
        refreshAfterUndoCalled = true
    }

    override fun refreshAfterRedo() {
        refreshAfterRedoCalled = true
    }

    override fun refreshAfterUseNatureToken() {
        refreshAfterUseNatureTokenCalled = true
    }

    override fun refreshAfterRotateTile() {
        refreshAfterRotateTileCalled = true
    }

    override fun refreshAfterDiscardWildlifeToken() {
        refreshAfterDiscardWildlifeTokenCalled = true
    }

    override fun refreshAfterChatMessage(sender: String, message: String) {
        refreshAfterChatMessageCalled = true
        lastChatSender = sender
        lastChatMessage = message
    }

    override fun refreshConnectionState(state: ConnectionState) {
        refreshConnectionStateCalled = true
        lastConnectionState = state
    }
}