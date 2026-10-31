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
 *Class for testing the [PlayerActionService.placeHabitat] method of the [PlayerActionService]
 */
class PlaceHabitatTileTest {
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
     * Test place Habitat with no game active
     */
    @Test
    fun `Should throw IllegalStateException if no game is active`() {
        rootService.currentGame = null
        assertThrows<IllegalStateException> { rootService.playerActionService.placeHabitat(Pair(2, 2)) }
    }

    /**
     * tests if gameState was correctly set to PLACEHABITAT before
     */
    @Test
    fun `Should throw exception if gameState is not correct`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEHABITAT
        game.gameState = GameState.CHOOSEPAIR
        assertThrows<IllegalStateException> { rootService.playerActionService.placeHabitat(Pair(0, 0)) }
    }

    /**
     * tests if exception is thrown when no habitat was chosen before
     */
    @Test
    fun `Should not place habitat if none was chosen`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEHABITAT
        game.drawnCombination = null
        val players = game.players[game.currentPlayer]
        rootService.playerActionService.placeHabitat(Pair(2, 2))
        assertEquals(null, players.board[Pair(2, 2)])
    }

    /**
     * tests if exception is thrown when coordinates contain a tile already
     */
    @Test
    fun `Should throw IllegalArgumentException if coords contain tile`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEHABITAT

        val players = game.players[game.currentPlayer]

        val containedTile = HabitatTile(
            mutableListOf(Habitat.MOUNTAINS),
            listOf(Wildlife.BEAR)
        )
        players.board[Pair(2, 2)] = containedTile

        players.board[Pair(2,3)]=HabitatTile(
            mutableListOf(Habitat.RIVERS),
            listOf(Wildlife.SALMON)
        )

        val wildlife = WildlifeToken(Wildlife.BEAR)
        val newHabitat = HabitatTile(
            mutableListOf(Habitat.RIVERS),
            listOf(Wildlife.SALMON)
        )

        game.drawnCombination = Pair(newHabitat, wildlife)

        assertThrows<IllegalArgumentException> {
            rootService.playerActionService.placeHabitat(Pair(2, 2))
        }
    }

    /**
     * tests if exception is thrown if no adjacent habitats exist
     */
    @Test
    fun `Should throw IllegalStateException if habitat isn't adjacent to existing tiles`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEHABITAT

        val players = game.players[game.currentPlayer]
        players.board[Pair(2, 2)] = HabitatTile(
            mutableListOf(Habitat.MOUNTAINS),
            listOf(Wildlife.BEAR)
        )
        val wildlife = WildlifeToken(Wildlife.BEAR)
        val newHabitat = HabitatTile(
            mutableListOf(Habitat.RIVERS),
            listOf(Wildlife.SALMON)
        )
        game.drawnCombination = Pair(newHabitat, wildlife)

        assertThrows<IllegalArgumentException> {
            rootService.playerActionService.placeHabitat(Pair(-4, 4))
        }
    }

    /**
     * tests if habitat is placed
     */
    @Test
    fun `Should successfully place habitat`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEHABITAT
        val players = game.players[game.currentPlayer]
        val habitatToBePlaced = HabitatTile(mutableListOf(Habitat.RIVERS), listOf(Wildlife.HAWK))
        game.drawnCombination = Pair(habitatToBePlaced, WildlifeToken(Wildlife.HAWK))
        players.board[Pair(1, 2)] = HabitatTile(
            mutableListOf(Habitat.MOUNTAINS),
            listOf(Wildlife.BEAR)
        )
        rootService.playerActionService.placeHabitat(Pair(2, 2))
        assertEquals(habitatToBePlaced, players.board[Pair(2, 2)])
        assertTrue(testRefreshable.refreshAfterPlaceHabitatTileCalled)
    }

}