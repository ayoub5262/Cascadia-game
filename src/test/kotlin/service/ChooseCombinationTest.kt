package service

import entity.BotSpeed
import entity.GameState
import org.junit.jupiter.api.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 *Class for testing the [PlayerActionService.chooseCombination] method of the [PlayerActionService]
 */
class ChooseCombinationTest {
    /** The [RootService] used for testing. Will be initialized before each test. */
    private lateinit var rootService: RootService

    /** The [TestRefreshable] used for testing. Will be initialized before each test. */
    private lateinit var testRefreshable: TestRefreshable

    /**
     * Sets up a new game before each test with [TestRefreshable]s attached to a newly created [RootService].
     * Starts a new game before each test with three players (Alice, Bob, Charlie) and 5 cards each.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)
        rootService.gameActionService.startNewGame(
            listOf("Player1", "Player2", "Player3"),
            false,
            listOf(true, true, true, true, true), BotSpeed.SLOW
        )
    }

    /**
     * Test choosing combination with no game active
     */
    @Test
    fun `Should throw IllegalStateException if no game is active`() {
        rootService.currentGame = null
        assertThrows<IllegalStateException> { rootService.playerActionService.chooseCombination(Pair(0, 0)) }
    }

    /**
     * tests if exception is thrown if no nature token is used and indices are not the same
     */
    @Test
    fun `Should throw IllegalStateException if indices aren't the same with no nature token active`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEHABITAT
        game.natureTokenUsed = false
        assertThrows<IllegalStateException> { rootService.playerActionService.chooseCombination(Pair(0, 1)) }
    }

    /**
     * tests if gameState was correctly set to CHOOSEPAIR before
     */
    @Test
    fun `Should throw exception if gameState is not correct`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEWILDLIFE
        assertThrows<IllegalStateException> { rootService.playerActionService.chooseCombination(Pair(0, 0)) }
    }

    /**
     *tests if drawnCombination is successfully set to the chosen pair while nature token is not active
     */
    @Test
    fun `Should assert drawnCombination to chosen habitat and WildlifeToken`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.natureTokenUsed = false
        game.gameState = GameState.CHOOSEPAIR
        val habitatTile = game.habitatShop[0]
        val wildlifeToken = game.wildlifeShop[0]
        rootService.playerActionService.chooseCombination(Pair(0, 0))
        assertEquals(habitatTile, game.drawnCombination?.first)
        assertEquals(wildlifeToken, game.drawnCombination?.second)
        assertTrue(testRefreshable.refreshAfterChooseCombinationCalled)
    }

    /**
     * tests if drawnCombination is successfully set to the chosen pair while nature token is active
     */
    @Test
    fun `Should assert combination if nature token is active`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.natureTokenUsed = true
        game.gameState = GameState.CHOOSEPAIR
        val habitatTile = game.habitatShop[0]
        val wildlifeToken = game.wildlifeShop[1]
        rootService.playerActionService.chooseCombination(Pair(0, 1))
        assertEquals(habitatTile, game.drawnCombination?.first)
        assertEquals(wildlifeToken, game.drawnCombination?.second)
        assertTrue(testRefreshable.refreshAfterChooseCombinationCalled)

    }

    /**
     * test if exception is thrown if indices are out of bounds
     */
    @Test
    fun `drawn combination should stay null if indices are out of bounds`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.drawnCombination = null
        game.gameState = GameState.CHOOSEPAIR
        game.natureTokenUsed = true
        rootService.playerActionService.chooseCombination(Pair(-1, -1))
        assertEquals(null, game.drawnCombination)

    }


}