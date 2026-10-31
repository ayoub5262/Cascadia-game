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
 *Class for testing the [PlayerActionService.rotateHabitat] method of the [PlayerActionService]
 */
class RotateHabitatTest {
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
            listOf(true, true, true, true, true),BotSpeed.SLOW
        )
    }

    /**
     * Test rotate Habitat with no game active
     */
    @Test
    fun `Should throw IllegalStateException if no game is active`() {
        rootService.currentGame = null
        assertThrows<IllegalStateException> { rootService.playerActionService.rotateHabitat(false) }
    }

    /**
     * tests exception if no combination was chosen before
     */
    @Test
    fun `Should throw IllegalStateException if no combination was chosen`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEHABITAT
        game.drawnCombination = null
        assertThrows<IllegalStateException> { rootService.playerActionService.rotateHabitat(false) }
    }

    /**
     * tests left rotation
     */
    @Test
    fun `Should rotate left`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEHABITAT

        game.drawnCombination = Pair(
            HabitatTile(
                mutableListOf(
                    Habitat.MOUNTAINS, Habitat.RIVERS, Habitat.WETLANDS,
                    Habitat.FORESTS, Habitat.PRAIRIES, Habitat.RIVERS
                ),
                listOf(Wildlife.SALMON)
            ),
            WildlifeToken(Wildlife.BEAR)
        )
        val habitat=game.drawnCombination?.first
        habitat?.rotation=0

        rootService.playerActionService.rotateHabitat(true)
        assertEquals(5, habitat?.rotation)
        assertEquals<List<Habitat>?>(
            listOf(
                Habitat.RIVERS, Habitat.WETLANDS, Habitat.FORESTS,
                Habitat.PRAIRIES, Habitat.RIVERS, Habitat.MOUNTAINS
            ),
            habitat?.habitats
        )
        assertTrue(testRefreshable.refreshAfterRotateTileCalled)
    }

    /**
     * tests right rotation
     */
    @Test
    fun `Should rotate right`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEHABITAT

        game.drawnCombination = Pair(
            HabitatTile(
                mutableListOf(
                    Habitat.MOUNTAINS, Habitat.RIVERS, Habitat.WETLANDS,
                    Habitat.FORESTS, Habitat.PRAIRIES, Habitat.RIVERS
                ),
                listOf(Wildlife.SALMON)
            ),
            WildlifeToken(Wildlife.BEAR)
        )
        val habitat=game.drawnCombination?.first
        habitat?.rotation=0

        rootService.playerActionService.rotateHabitat(false)
        assertEquals(1, habitat?.rotation)
        assertEquals<List<Habitat>?>(
            listOf(
                Habitat.RIVERS, Habitat.MOUNTAINS, Habitat.RIVERS,
                Habitat.WETLANDS, Habitat.FORESTS, Habitat.PRAIRIES
            ),
            habitat?.habitats
        )
        assertTrue(testRefreshable.refreshAfterRotateTileCalled)
    }

}