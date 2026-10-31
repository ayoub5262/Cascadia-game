package service

import entity.BotSpeed
import entity.Wildlife
import entity.WildlifeToken
import org.junit.jupiter.api.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 *Class for testing the [PlayerActionService.redrawWildlifeTokens] method of the [PlayerActionService]
 */
class RedrawWildlifeTokenTest {
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
     * Test redraw wildlife with no game active
     */
    @Test
    fun `Should throw IllegalStateException if no game is active`() {
        rootService.currentGame = null
        assertThrows<IllegalStateException> { rootService.playerActionService.redrawWildlifeTokens(listOf()) }
    }

    /**
     * Test redraw wildlife
     */
    @Test
    fun `Should redraw tokens`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)

        val shop: Array<WildlifeToken?> = arrayOf(
            WildlifeToken(Wildlife.HAWK),
            WildlifeToken(Wildlife.FOX),
            WildlifeToken(Wildlife.HAWK),
            WildlifeToken(Wildlife.SALMON)
        )
        game.wildlifeShop = shop

        game.wildlifeBag.clear()
        game.wildlifeBag.push(WildlifeToken(Wildlife.ELK))

        rootService.playerActionService.redrawWildlifeTokens(listOf(0))
        assertEquals(Wildlife.ELK, game.wildlifeShop[0]?.animal)

    }
}