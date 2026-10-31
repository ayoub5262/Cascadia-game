package service

import entity.BotSpeed
import entity.GameState
import entity.Habitat
import entity.HabitatTile
import entity.Wildlife
import entity.WildlifeToken
import org.junit.jupiter.api.assertThrows
import kotlin.collections.set
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 *Class for testing the [PlayerActionService.placeWildlifeTokens] method of the [PlayerActionService]
 */
class PlaceWildlifeTokenTest {
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
     * Test place WildlifeToken with no game active
     */
    @Test
    fun `Should throw IllegalStateException if no game is active`() {
        rootService.currentGame = null
        assertThrows<IllegalStateException> { rootService.playerActionService.placeWildlifeTokens(Pair(2, 2)) }
    }

    /**
     * tests if gameState was correctly set to PLACEWILDLIFE before
     */
    @Test
    fun `Should throw exception if gameState is not correct`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.CHOOSEPAIR
        assertThrows<IllegalStateException> { rootService.playerActionService.placeWildlifeTokens(Pair(0, 0)) }
    }

    /**
     * habitat tile should stay empty if no combination was chosen
     */
    @Test
    fun `Shouldn't place animal token if drawnCombination is empty`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.drawnCombination = null
        game.gameState = GameState.PLACEWILDLIFE
        val tile = HabitatTile(
            mutableListOf(Habitat.MOUNTAINS),
            listOf(Wildlife.BEAR)
        )
        tile.placedWildlifeToken = null
        rootService.playerActionService.placeWildlifeTokens(Pair(2, 2))
        assertEquals(null, tile.placedWildlifeToken)
    }

    /**
     * tests exception if chosen habitat tile is full already
     */
    @Test
    fun `Should throw IllegalStateException if tile contains token already`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEWILDLIFE
        val players = game.players[game.currentPlayer]
        val tile = HabitatTile(
            mutableListOf(Habitat.MOUNTAINS),
            listOf(Wildlife.BEAR)
        )
        val wildlife = WildlifeToken(Wildlife.BEAR)

        tile.placedWildlifeToken = wildlife
        players.board[Pair(2, 2)] = tile
        game.drawnCombination = Pair(tile, wildlife)
        assertThrows< IllegalStateException> { rootService.playerActionService.placeWildlifeTokens(Pair(2, 2)) }

    }

    /**
     * tests exception if wildlifeToken isn't the same animal as the animal of the habitat tile
     */
    @Test
    fun `Should throw IllegalStateException if tokens wildlife isn't depicted on the tile`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEWILDLIFE
        val players = game.players[game.currentPlayer]

        val tile = HabitatTile(
            mutableListOf(Habitat.MOUNTAINS),
            listOf(Wildlife.SALMON)
        )

        players.board[Pair(2, 2)] = tile
        val wildlife = WildlifeToken(Wildlife.BEAR)

        game.drawnCombination = Pair(tile, wildlife)

        tile.placedWildlifeToken = null
        assertThrows<IllegalStateException> { rootService.playerActionService.placeWildlifeTokens(Pair(2, 2)) }
    }

    /**
     * tests if natureTokenCount of the player doesn't go up if no nature tokens are left in the game
     */
    @Test
    fun `Shouldn't add nature token if natureTokenCount = 0`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEWILDLIFE
        game.natureTokenCount = 0
        val players = game.players[game.currentPlayer]
        players.natureTokens = 1
        val tile = HabitatTile(
            mutableListOf(Habitat.MOUNTAINS),
            listOf(Wildlife.SALMON)
        )
        players.board[Pair(2, 2)] = tile
        val wildlife = WildlifeToken(Wildlife.SALMON)

        game.drawnCombination = Pair(tile, wildlife)
        tile.isKeyStone = true
        rootService.playerActionService.placeWildlifeTokens(Pair(2, 2))
        assertEquals(1, players.natureTokens)
        assertEquals(wildlife, tile.placedWildlifeToken)
        assertTrue(testRefreshable.refreshAfterPlaceWildlifeTokenCalled)
    }

    /**
     * tests if natureTokenCount of the player goes up by one after placing token on keystone
     */
    @Test
    fun `Should add nature token if keyStone`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEWILDLIFE
        game.natureTokenCount = 10
        val players = game.players[game.currentPlayer]
        players.natureTokens = 1
        val tile = HabitatTile(
            mutableListOf(Habitat.MOUNTAINS),
            listOf(Wildlife.SALMON)
        )
        players.board[Pair(2, 2)] = tile
        val wildlife = WildlifeToken(Wildlife.SALMON)

        game.drawnCombination = Pair(tile, wildlife)
        tile.isKeyStone = true
        rootService.playerActionService.placeWildlifeTokens(Pair(2, 2))
        assertEquals(2, players.natureTokens)
        assertEquals(wildlife, tile.placedWildlifeToken)
        assertTrue(testRefreshable.refreshAfterPlaceWildlifeTokenCalled)
    }

    /**
     * tests if wildlifeToken is really placed at the chosen coords
     */
    @Test
    fun `Should successfully place wildlifeToken`() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game)
        game.gameState = GameState.PLACEWILDLIFE

        val players = game.players[game.currentPlayer]

        val tile = HabitatTile(
            mutableListOf(Habitat.MOUNTAINS),
            listOf(Wildlife.SALMON)
        )

        players.board[Pair(2, 2)] = tile
        val wildlife = WildlifeToken(Wildlife.SALMON)

        game.drawnCombination = Pair(tile, wildlife)

        tile.placedWildlifeToken = null
        rootService.playerActionService.placeWildlifeTokens(Pair(2, 2))
        assertEquals(wildlife, tile.placedWildlifeToken)
        assertTrue(testRefreshable.refreshAfterPlaceWildlifeTokenCalled)
    }

}