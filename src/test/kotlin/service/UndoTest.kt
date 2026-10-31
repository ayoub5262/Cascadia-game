package service

import entity.BotSpeed
import org.junit.jupiter.api.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Class for testing [PlayerActionService.undo]
 */
class UndoTest {

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
            listOf(true,true,true,true,true),
            BotSpeed.FAST
        )
    }

    /**
     * tests undoing with no game active
     */
    @Test
    fun testGameNull() {

        //set game to null
        rootService.currentGame = null

        //check if undo throws an exception
        assertThrows<IllegalStateException> {
            rootService.playerActionService.undo()
        }
    }

    /**
     * tests undoing with one state
     */
    @Test
    fun testUndoWithOneState() {

        //Add test refreshable
        val testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)

        //Check if current game is null
        val game = checkNotNull(rootService.currentGame)

        //Execute undo()
        rootService.playerActionService.undo()

        //Check if undo stack has an element -> fail if undo executed
        assert(game.undoes.isEmpty())

        //Check if refresh was called -> fail if so
        assert(!testRefreshable.refreshAfterUndoCalled)
    }

    /**
     * tests undoing with two states
     */
    @Test
    fun testUndoWithTwoStates() {

        //Add test refreshable
        val testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)

        //Check if current game is null
        val game = checkNotNull(rootService.currentGame)

        //Play one turn
        rootService.gameActionService.startTurn()
        rootService.playerActionService.chooseCombination(Pair(0,0))
        rootService.playerActionService.placeHabitat(Pair(0,-1))
        rootService.playerActionService.discardWildlifeToken()

        //Execute undo()
        rootService.playerActionService.undo()

        //Check if undo stack has an element -> fail if undo didn't execute
        assert(!game.undoes.isEmpty())

        //Check if refresh was called -> fail if not
        assert(testRefreshable.refreshAfterUndoCalled)
    }
}