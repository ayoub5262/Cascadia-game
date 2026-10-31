package service

import entity.BotSpeed
import org.junit.jupiter.api.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Class for testing [PlayerActionService.redo]
 */
class RedoTest {

    /**
     * This service is initialized in the [setUp] function hence it is a late-initialized property.
     */
    private lateinit var rootService: RootService

    /**
     * Initialize a new non network game with 4 players (Test1, Test2, Test3, Test4) and all scoring cards set to A
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        rootService.gameActionService.startNewGame(
            listOf("Test1", "Test2", "Test3", "Test4"),
            false,
            listOf(true, true, true, true, true),
            BotSpeed.FAST
        )
    }

    /**
     * tests redoing with no game active
     */
    @Test
    fun testGameNull() {

        //set game to null
        rootService.currentGame = null

        //check if undo throws an exception
        assertThrows<IllegalStateException> {
            rootService.playerActionService.redo()
        }
    }

    /**
     * tests redoing with no states saved
     */
    @Test
    fun testRedoWithZeroStates() {

        //Add test refreshable
        val testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)

        //Check if current game is null
        val game = checkNotNull(rootService.currentGame)

        //Save game state before redo() is called
        val saveGame = checkNotNull(game.currentState).copy()

        //Execute redo
        rootService.playerActionService.redo()

        //Check if currentState is still the same
        assert(game.currentState == saveGame)

        //Check if refreshable has been called -> if so fail
        assert(!testRefreshable.refreshAfterRedoCalled)
    }

    /**
     * tests redoing with one taste saved
     */
    @Test
    fun testRedoWithOneState() {

        //Add test refreshable
        val testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)
        //Check if current game is null
        val game = checkNotNull(rootService.currentGame)


        //Play one turn
        rootService.gameActionService.startTurn()
        rootService.playerActionService.chooseCombination(Pair(0, 0))
        rootService.playerActionService.placeHabitat(Pair(0, -1))
        rootService.playerActionService.discardWildlifeToken()

        //Save game state before redo() is called
        val saveGame = checkNotNull(game.currentState).copy()

        //Execute undo
        rootService.playerActionService.undo()

        //Check if undo worked -> currentState gameState is different from saveGame gameState

        assert(game.currentState?.gameState != saveGame.gameState)

        //Execute redo
        rootService.playerActionService.redo()

        //Check if redo worked -> currentState is same as saveGame
        assert(game.currentState == saveGame)

        //Check if refreshable has been called
        assert(testRefreshable.refreshAfterRedoCalled)
    }
}