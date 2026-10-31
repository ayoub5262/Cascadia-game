package service

import entity.GameState
import entity.BotSpeed
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

/**
 * Test class for the useNatureToken method of PlayerActionService.
 */
class UseNatureTokenTest {
    private lateinit var rootService: RootService

    private lateinit var testRefreshable: TestRefreshable

    /**
     * Given: A clean test environment.
     * When: The test starts.
     * Then: A new RootService is initialized.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)
    }

    /**
     * Given: An active game in CHOOSEPAIR state with the current player having 1 nature token.
     * When: useNatureToken is called.
     * Then: Player token is spent, global token count increases, natureTokenUsed becomes true,
     * and state becomes USENATURETOKEN.
     */
    @Test
    fun testUseNatureToken_Success() {
        // Setup game and start turn to reach CHOOSEPAIR state
        rootService.gameActionService.startNewGame(
            listOf("Alice", "Bob"),
            false,
            listOf(true, true, true, true, true),
            BotSpeed.NORMAL
        )
        rootService.gameActionService.startTurn()

        val game = checkNotNull(rootService.currentGame?.currentState)
        val player = game.players[game.currentPlayer]

        // Manually give token
        player.natureTokens = 1
        val initialGlobalCount = game.natureTokenCount

        // Use token
        rootService.playerActionService.useNatureToken()

        // Assert player token is spent
        assertEquals(0, player.natureTokens, "Player should have 0 nature tokens left.")

        // Assert global count increased
        assertEquals(initialGlobalCount + 1, game.natureTokenCount, "Global nature token count should increase by 1.")

        // Assert flags and state
        assertTrue(game.natureTokenUsed, "natureTokenUsed flag should be true.")
        assertEquals(GameState.USENATURETOKEN, game.gameState, "Game state should be USENATURETOKEN.")

        // Assert refreshable called
        assertTrue(testRefreshable.refreshAfterUseNatureTokenCalled, "refreshAfterUseNatureToken should be called.")
    }

    /**
     * Given: A player has 2 nature tokens and is already in the USENATURETOKEN state.
     * When: useNatureToken is called a second time.
     * Then: Player token is spent and gamestate remains USENATURETOKEN.
     */
    @Test
    fun testUseNatureToken_ConsecutiveUsesAllowed() {
        // Setup game and start turn to reach CHOOSEPAIR state
        rootService.gameActionService.startNewGame(
            listOf("Alice", "Bob"),
            false,
            listOf(true, true, true, true, true),
            BotSpeed.NORMAL
        )
        rootService.gameActionService.startTurn()

        val game = checkNotNull(rootService.currentGame?.currentState)
        val player = game.players[game.currentPlayer]

        // Give 2 tokens
        player.natureTokens = 2

        // Use first token
        rootService.playerActionService.useNatureToken()
        assertEquals(GameState.USENATURETOKEN, game.gameState, "Game state should be USENATURETOKEN after first use.")
        assertEquals(1, player.natureTokens, "Player should have 1 nature token left.")

        // Use second token consecutively
        rootService.playerActionService.useNatureToken()

        // Assert state remains the same and tokens spent
        assertEquals(GameState.USENATURETOKEN, game.gameState, "Game state should remain USENATURETOKEN.")
        assertEquals(0, player.natureTokens, "Player should have 0 nature tokens left.")
    }

    /**
     * Given: A player has 0 nature tokens.
     * When: useNatureToken is called.
     * Then: An IllegalArgumentException is thrown.
     */
    @Test
    fun testUseNatureToken_NoTokens() {
        // Setup game and start turn to reach CHOOSEPAIR state
        rootService.gameActionService.startNewGame(
            listOf("Alice", "Bob"),
            false,
            listOf(true, true, true, true, true),
            BotSpeed.NORMAL
        )
        rootService.gameActionService.startTurn()

        val game = checkNotNull(rootService.currentGame?.currentState)
        val player = game.players[game.currentPlayer]

        // Ensure 0 tokens
        player.natureTokens = 0

        assertThrows<IllegalArgumentException>("Should crash if player has no tokens") {
            rootService.playerActionService.useNatureToken()
        }
    }

    /**
     * Given: A game in an invalid state (e.g., TURNENDED).
     * When: useNatureToken is called.
     * Then: An IllegalStateException is thrown.
     */
    @Test
    fun testUseNatureToken_InvalidState() {
        // Setup game
        rootService.gameActionService.startNewGame(
            listOf("Alice", "Bob"),
            false,
            listOf(true, true, true, true, true),
            BotSpeed.NORMAL
        )

        val game = checkNotNull(rootService.currentGame?.currentState)
        val player = game.players[game.currentPlayer]

        // Give token
        player.natureTokens = 1

        // Set invalid state manually
        game.gameState = GameState.TURNENDED

        assertThrows<IllegalStateException>("Should crash if game state is invalid") {
            rootService.playerActionService.useNatureToken()
        }
    }
}