package service

import entity.BotSpeed
import entity.GameState
import entity.Habitat
import entity.HabitatTile
import entity.Wildlife
import entity.WildlifeToken
import org.junit.jupiter.api.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 *Class for testing the [PlayerActionService.discardWildlifeToken] method of the [PlayerActionService]
 */
class DiscardWildlifeTest {
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
     * Test discard wildlifeToken with no game active
     */
    @Test
    fun `Should throw IllegalStateException if no game is active`() {
        rootService.currentGame = null
        assertThrows<IllegalStateException> { rootService.playerActionService.discardWildlifeToken() }
    }

    /**
     * tests if exception is thrown if no wildlife was chosen
     */
    @Test
    fun `Should throw IllegalStateException if no combination was chosen`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEWILDLIFE
        game.drawnCombination = null
        assertThrows<IllegalStateException> { rootService.playerActionService.discardWildlifeToken() }
    }

    /**
     * tests if wildlifeToken is added to the bag
     */
    @Test
    fun `Should successfully add discarded wildlifeToken back into the bag`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEWILDLIFE
        val bagSize = game.wildlifeBag.size
        val wildlifeToDiscard = WildlifeToken(animal = Wildlife.BEAR)
        val habitatTile = HabitatTile(mutableListOf(Habitat.RIVERS), listOf(Wildlife.HAWK))
        game.drawnCombination = Pair (habitatTile, wildlifeToDiscard)
        rootService.playerActionService.discardWildlifeToken()
        assertEquals(bagSize + 1, game.wildlifeBag.size)
        assertTrue(testRefreshable.refreshAfterDiscardWildlifeTokenCalled)
    }

}