package service

import entity.GameState
import entity.BotSpeed
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

/**
 * Test class for the startNewGame method of GameActionService.
 */
class StartNewGameTest {
    private lateinit var rootService: RootService

    /**
     * Given: A clean test environment.
     * When: The test starts.
     * Then: A new RootService is initialized.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
    }

    /**
     * Given: A valid game setup with 2 unique players and 5 scoring cards.
     * When: startNewGame is called.
     * Then: Deck size is 43, shops have exactly 4 items, players get 3 start tiles, and gamestate is TURNSTARTED.
     */
    @Test
    fun testStartNewGame_Valid2Players() {
        val playerNames = listOf("Alice", "Bob")
        val scoringCards = listOf(true, true, true, true, true)

        rootService.gameActionService.startNewGame(playerNames, false, scoringCards, BotSpeed.NORMAL)

        val game = checkNotNull(rootService.currentGame?.currentState)

        // Assert deck size is exactly 39 ( ((2 * 20) + 3) - 4 for the shop )
        assertEquals(39, game.habitatDrawPile.size, "Deck size should be 39 for 2 players after filling the shop.")

        // Assert habitatShop and wildlifeShop have exactly 4 items
        assertEquals(4, game.habitatShop.filterNotNull().size, "Habitat shop should have exactly 4 items.")
        assertEquals(4, game.wildlifeShop.filterNotNull().size, "Wildlife shop should have exactly 4 items.")

        // Assert players are dealt 3 start tiles each
        for (player in game.players) {
            assertEquals(3, player.board.size, "Player ${player.name} should have 3 start tiles.")
        }

        // Assert gamestate is TURNSTARTED
        assertEquals(GameState.TURNSTARTED, game.gameState, "Game state should be TURNSTARTED.")
    }

    /**
     * Given: A valid game setup with 3 unique players.
     * When: startNewGame is called.
     * Then: Deck size is exactly 63.
     */
    @Test
    fun testStartNewGame_Valid3Players() {
        val playerNames = listOf("Alice", "Bob", "Charlie")
        val scoringCards = listOf(true, true, true, true, true)

        rootService.gameActionService.startNewGame(playerNames, false, scoringCards, BotSpeed.NORMAL)

        val game = checkNotNull(rootService.currentGame?.currentState)

        // Assert deck size is exactly 59 ( ((3 * 20) + 3) - 4 for the shop )
        assertEquals(59, game.habitatDrawPile.size, "Deck size should be 59 for 3 players after filling the shop.")
    }

    /**
     * Given: A valid game setup with 4 unique players.
     * When: startNewGame is called.
     * Then: Deck size is exactly 83.
     */
    @Test
    fun testStartNewGame_Valid4Players() {
        val playerNames = listOf("Alice", "Bob", "Charlie", "Diana")
        val scoringCards = listOf(true, true, true, true, true)

        rootService.gameActionService.startNewGame(playerNames, false, scoringCards, BotSpeed.NORMAL)

        val game = checkNotNull(rootService.currentGame?.currentState)

        // Assert deck size is exactly 79 ( ((4 * 20) + 3) - 4 for the shop )
        assertEquals(79, game.habitatDrawPile.size, "Deck size should be 79 for 4 players after filling the shop.")
    }

    /**
     * Given: Invalid player counts (1 and 5 players).
     * When: startNewGame is called.
     * Then: An IllegalArgumentException is thrown.
     */
    @Test
    fun testStartNewGame_InvalidPlayerCounts() {
        val scoringCards = listOf(true, true, true, true, true)

        assertThrows<IllegalArgumentException>("Should crash for 1 player") {
            rootService.gameActionService.startNewGame(listOf("Alice"), false, scoringCards, BotSpeed.NORMAL)
        }

        assertThrows<IllegalArgumentException>("Should crash for 5 players") {
            rootService.gameActionService.startNewGame(
                listOf("Alice", "Bob", "Charlie", "Diana", "Eve"),
                false,
                scoringCards,
                BotSpeed.NORMAL
            )
        }
    }

    /**
     * Given: A list of player names containing duplicates.
     * When: startNewGame is called.
     * Then: An IllegalArgumentException is thrown.
     */
    @Test
    fun testStartNewGame_DuplicateNames() {
        val scoringCards = listOf(true, true, true, true, true)

        assertThrows<IllegalArgumentException>("Should crash for duplicate player names") {
            rootService.gameActionService.startNewGame(
                listOf("Player1", "Player1"),
                false,
                scoringCards,
                BotSpeed.NORMAL
            )
        }
    }

    /**
     * Given: A scoring card list that does not have exactly 5 elements.
     * When: startNewGame is called.
     * Then: An IllegalArgumentException is thrown.
     */
    @Test
    fun testStartNewGame_InvalidScoringCards() {
        val playerNames = listOf("Alice", "Bob")

        assertThrows<IllegalArgumentException>("Should crash for invalid scoring cards list size") {
            rootService.gameActionService.startNewGame(
                playerNames,
                false,
                listOf(true, true),
                BotSpeed.NORMAL
            ) // Only 2 elements
        }
    }
}