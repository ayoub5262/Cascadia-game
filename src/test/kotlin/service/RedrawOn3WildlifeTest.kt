package service

import entity.BotSpeed
import entity.Wildlife
import entity.WildlifeToken
import org.junit.jupiter.api.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 *Class for testing the [PlayerActionService.redrawOn3Wildlife] method of the [PlayerActionService]
 */
class RedrawOn3WildlifeTest {
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
     * Test redraw 3 wildlife with no game active
     */
    @Test
    fun `Should throw IllegalStateException if no game is active`() {
        rootService.currentGame = null
        assertThrows<IllegalStateException> { rootService.playerActionService.redrawOn3Wildlife() }
    }

    /**
     * tests if wildlifeTokens are not redrawn if 3 are not identical
     */
    @Test
    fun `Shouldn't redraw 3 tokens if 3 tokens aren't the same`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.threeRedrawnInTurn=false
        val shop: Array<WildlifeToken?> = arrayOf(
            WildlifeToken(Wildlife.HAWK),
            WildlifeToken(Wildlife.HAWK),
            WildlifeToken(Wildlife.SALMON),
            WildlifeToken(Wildlife.SALMON)
        )
        game.wildlifeShop = shop

        rootService.playerActionService.redrawOn3Wildlife()
        assertEquals(shop.toList(), game.wildlifeShop.toList())
    }

    /**
     * Test redraw 3 wildlife
     */
    @Test
    fun `Should redraw tokens`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)

        game.threeRedrawnInTurn=false

        val shop: Array<WildlifeToken?> = arrayOf(
            WildlifeToken(Wildlife.HAWK),
            WildlifeToken(Wildlife.HAWK),
            WildlifeToken(Wildlife.HAWK),
            WildlifeToken(Wildlife.SALMON)
        )
        game.wildlifeShop = shop

        rootService.playerActionService.redrawOn3Wildlife()
        assertEquals(shop.toList(), game.wildlifeShop.toList())

    }

    /**
     * tests if wildlifeTokens are not redrawn if player has redrawn in the turn already
     */
    @Test
    fun `Shouldn't redraw 3 tokens if threeRedrawnInTurn is true`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.threeRedrawnInTurn=true
        val shop: Array<WildlifeToken?> = arrayOf(
            WildlifeToken(Wildlife.HAWK),
            WildlifeToken(Wildlife.HAWK),
            WildlifeToken(Wildlife.SALMON),
            WildlifeToken(Wildlife.SALMON)
        )
        game.wildlifeShop = shop

        rootService.playerActionService.redrawOn3Wildlife()
        assertEquals(shop.toList(), game.wildlifeShop.toList())
    }
}