package service

import service.network.ConnectionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * testet refreshables 
 */
class RefreshableTest {

    /**
     * prüft ob die methoden von refreshables aufrufbar sind
     */
    @Test
    fun testDefaultRefreshMethodsDoNotThrowException() {
        val refreshable = object : Refreshable {}

        refreshable.refreshAfterStartTurn()
        refreshable.refreshAfterEndTurn()
        refreshable.refreshAfterStartNewGame()
        refreshable.refreshAfterCreateWildlifeBag()
        refreshable.refreshAfterCreateHabitatTiles()
        refreshable.refreshAfterEndGame(emptyList())
        refreshable.refreshAfterCalculateScore()
        refreshable.refreshAfterSaveGame()
        refreshable.refreshAfterLoadGame()
        refreshable.refreshAfterCheckOverpopulation()
        refreshable.refreshAfterLogTurn()
        refreshable.refreshAfterRestockShops()
        refreshable.refreshAfterChooseCombination()
        refreshable.refreshAfterPlaceHabitatTile()
        refreshable.refreshAfterPlaceWildlifeToken()
        refreshable.refreshAfterRedrawWildlifeTokens()
        refreshable.refreshAfterRedrawOn3Wildlife()
        refreshable.refreshAfterUndo()
        refreshable.refreshAfterRedo()
        refreshable.refreshAfterUseNatureToken()
        refreshable.refreshAfterRotateTile()
        refreshable.refreshAfterDiscardWildlifeToken()
        refreshable.refreshAfterChatMessage("kaan", "metehan")
        refreshable.refreshConnectionState(ConnectionState.PLAYING_MY_TURN)

        assertIs<Refreshable>(refreshable)
    }

    /**
     * prüft dass testRefreshable alle aufrufe korrekt speichert.
     */
    @Test
    fun testTestRefreshableStoresRefreshCalls() {
        val refreshable = TestRefreshable()

        refreshable.refreshAfterStartTurn()
        refreshable.refreshAfterEndTurn()
        refreshable.refreshAfterStartNewGame()
        refreshable.refreshAfterCreateWildlifeBag()
        refreshable.refreshAfterCreateHabitatTiles()
        refreshable.refreshAfterEndGame(emptyList())
        refreshable.refreshAfterCalculateScore()
        refreshable.refreshAfterSaveGame()
        refreshable.refreshAfterLoadGame()
        refreshable.refreshAfterCheckOverpopulation()
        refreshable.refreshAfterLogTurn()
        refreshable.refreshAfterRestockShops()
        refreshable.refreshAfterChooseCombination()
        refreshable.refreshAfterPlaceHabitatTile()
        refreshable.refreshAfterPlaceWildlifeToken()
        refreshable.refreshAfterRedrawWildlifeTokens()
        refreshable.refreshAfterRedrawOn3Wildlife()
        refreshable.refreshAfterUndo()
        refreshable.refreshAfterRedo()
        refreshable.refreshAfterUseNatureToken()
        refreshable.refreshAfterRotateTile()
        refreshable.refreshAfterDiscardWildlifeToken()
        refreshable.refreshAfterChatMessage("kaan", "metehan")
        refreshable.refreshConnectionState(ConnectionState.PLAYING_MY_TURN)

        assertTrue(refreshable.refreshAfterStartTurnCalled)
        assertTrue(refreshable.refreshAfterEndTurnCalled)
        assertTrue(refreshable.refreshAfterStartNewGameCalled)
        assertTrue(refreshable.refreshAfterCreateWildlifeBagCalled)
        assertTrue(refreshable.refreshAfterCreateHabitatTilesCalled)
        assertTrue(refreshable.refreshAfterEndGameCalled)
        assertTrue(refreshable.refreshAfterCalculateScoreCalled)
        assertTrue(refreshable.refreshAfterSaveGameCalled)
        assertTrue(refreshable.refreshAfterLoadGameCalled)
        assertTrue(refreshable.refreshAfterCheckOverpopulationCalled)
        assertTrue(refreshable.refreshAfterLogTurnCalled)
        assertTrue(refreshable.refreshAfterRestockShopsCalled)
        assertTrue(refreshable.refreshAfterChooseCombinationCalled)
        assertTrue(refreshable.refreshAfterPlaceHabitatTileCalled)
        assertTrue(refreshable.refreshAfterPlaceWildlifeTokenCalled)
        assertTrue(refreshable.refreshAfterRedrawWildlifeTokensCalled)
        assertTrue(refreshable.refreshAfterRedrawOn3WildlifeCalled)
        assertTrue(refreshable.refreshAfterUndoCalled)
        assertTrue(refreshable.refreshAfterRedoCalled)
        assertTrue(refreshable.refreshAfterUseNatureTokenCalled)
        assertTrue(refreshable.refreshAfterRotateTileCalled)
        assertTrue(refreshable.refreshAfterDiscardWildlifeTokenCalled)
        assertTrue(refreshable.refreshAfterChatMessageCalled)
        assertTrue(refreshable.refreshConnectionStateCalled)

        assertEquals("kaan", refreshable.lastChatSender)
        assertEquals("metehan", refreshable.lastChatMessage)
        assertEquals(ConnectionState.PLAYING_MY_TURN, refreshable.lastConnectionState)
    }

    /**
     * prüft dass reset alle flags wieder zurücksetzt.
     */
    @Test
    fun testTestRefreshableReset() {
        val refreshable = TestRefreshable()

        refreshable.refreshAfterStartTurn()
        refreshable.refreshAfterChatMessage("kaan", "metehan")
        refreshable.refreshConnectionState(ConnectionState.PLAYING_MY_TURN)

        refreshable.reset()

        assertFalse(refreshable.refreshAfterStartTurnCalled)
        assertFalse(refreshable.refreshAfterChatMessageCalled)
        assertFalse(refreshable.refreshConnectionStateCalled)

        assertEquals(null, refreshable.lastChatSender)
        assertEquals(null, refreshable.lastChatMessage)
        assertEquals(null, refreshable.lastConnectionState)
    }
}