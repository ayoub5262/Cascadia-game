package service

import entity.GameState
import entity.BotSpeed
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

/**
 * Test class for the startTurn method of GameActionService.
 */
class StartTurnTest {
    private lateinit var rootService: RootService
    /** The [TestRefreshable] used for testing. Will be initialized before each test. */
    private lateinit var testRefreshable: TestRefreshable

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
     * Given: An active game in the TURNSTARTED state.
     * When: startTurn is called.
     * Then: Turn flags are reset, gamestate changes to CHOOSEPAIR, and history size increases by 1.
     */
    @Test
    fun testStartTurn_Success() {
        // Setup game
        rootService.gameActionService.startNewGame(
            listOf("Alice", "Bob"),
            false,
            listOf(true, true, true, true, true),
            BotSpeed.NORMAL
        )
        val gameSave = checkNotNull(rootService.currentGame)
        val game = checkNotNull(gameSave.currentState)

        // Initial history size
        val initialHistorySize = gameSave.history.size

        // Call startTurn
        rootService.gameActionService.startTurn()

        // Assert flags are reset
        assertFalse(game.natureTokenUsed, "Nature token used flag should be false.")
        assertFalse(game.threeWildlifeIdentical, "Three wildlife identical flag should be false.")
        assertNull(game.drawnCombination, "Drawn combination should be null.")

        // Assert state changed
        val activeGame = checkNotNull(gameSave.currentState)
        assertEquals(GameState.CHOOSEPAIR, activeGame.gameState, "Game state should be CHOOSEPAIR.")

        // Assert history size increased
        assertEquals(initialHistorySize + 1, gameSave.history.size, "History size should increase by 1.")

        // Assert the state the game continues on is playable (exposes the logTurn copy() bug)
        assertEquals(
            GameState.CHOOSEPAIR,
            checkNotNull(gameSave.currentState).gameState,
            "currentState after startTurn must be in CHOOSEPAIR - logTurn must not replace it with a default copy."
        )
    }

    /**
     * Given: No active game is running.
     * When: startTurn is called.
     * Then: An IllegalStateException is thrown.
     */
    @Test
    fun testStartTurn_NoActiveGame() {
        assertThrows<IllegalStateException>("Should crash if no active game exists") {
            rootService.gameActionService.startTurn()
        }
    }

    /**
     * Given: An active game in the CHOOSEPAIR state.
     * When: startTurn is called.
     * Then: An IllegalStateException is thrown.
     */
    @Test
    fun testStartTurn_WrongGameState() {
        // setup game and force a bad state to hit the crash line
        rootService.gameActionService.startNewGame(
            listOf("Alice", "Bob"),
            false,
            listOf(true, true, true, true, true),
            BotSpeed.NORMAL
        )
        val gameSave = checkNotNull(rootService.currentGame)
        val game = checkNotNull(gameSave.currentState)
        game.gameState = GameState.CHOOSEPAIR

        // it should crash telling us we expected TURNSTARTED
        assertThrows<IllegalStateException>("Should crash if game state is not TURNSTARTED") {
            rootService.gameActionService.startTurn()
        }
    }

    /**
     * Given: An active game where the current player is a Bot.
     * When: startTurn is called.
     * Then: The botTurn method is triggered.
     */
    @Test
    fun testStartTurn_BotTurn() {
        // setup game where Player 1 is bot
        rootService.gameActionService.startNewGame(
            listOf("RANDOMBOT 1", "Bob"),
            false,
            listOf(true, true, true, true, true),
            BotSpeed.NORMAL
        )

        // calling startTurn will automatically trigger botActionService.botTurn()
        rootService.gameActionService.startTurn()
    }

    /**
     * tests if end game is called if roundNumber is 20 and the player index is 0
     */
    @Test
    fun `Should end game if turnNumber = 20`() {
        testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)
        rootService.gameActionService.startNewGame(
            listOf("Alice", "Bob"),
            false,
            listOf(true, true, true, true, true),
            BotSpeed.NORMAL
        )
        val game = rootService.currentGame?.currentState
        checkNotNull(game)

        game.roundNumber=20
        game.currentPlayer=1

        rootService.gameActionService.startTurn()
        rootService.playerActionService.chooseCombination(Pair(0, 0))
        rootService.playerActionService.placeHabitat(Pair(0, -1))
        rootService.playerActionService.discardWildlifeToken()

        assertTrue(testRefreshable.refreshAfterEndGameCalled)
    }
}

