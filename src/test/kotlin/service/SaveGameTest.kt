package service

import entity.BotSpeed
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Class for testing the saveGame method.
 */
class SaveGameTest {

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
            listOf(true, true, true, true, true),
            BotSpeed.SLOW
        )
    }

    /**
     * Tests that no file is created if there is no active game.
     */
    @Test
    fun `Should not save if no game is active`() {

        rootService.currentGame = null

        val filesBefore = File(".").listFiles()?.size ?: 0

        rootService.gameActionService.saveGame()

        val filesAfter = File(".").listFiles()?.size ?: 0

        assertEquals(filesBefore, filesAfter)
    }

    /**
     * Tests that no file is created in network mode.
     */
    @Test
    fun `Should not save network game`() {

        val game = rootService.currentGame?.currentState
        checkNotNull(game)

        game.isHotseatMode = false

        val filesBefore = File(".").listFiles()?.size ?: 0

        rootService.gameActionService.saveGame()

        val filesAfter = File(".").listFiles()?.size ?: 0

        assertEquals(filesBefore, filesAfter)
    }

    /**
     * Tests that a save file is created.
     */
    @Test
    fun `Should save game`() {

        val filesBefore = File(".").listFiles { _, name ->
            name.endsWith(".dat")
        }?.size ?: 0

        rootService.gameActionService.saveGame()

        val filesAfter = File(".").listFiles { _, name ->
            name.endsWith(".dat")
        }?.size ?: 0

        assertTrue(filesAfter > filesBefore)

        File(".").listFiles { _, name ->
            name.endsWith(".dat")
        }?.forEach {
            it.delete()
        }
    }
}