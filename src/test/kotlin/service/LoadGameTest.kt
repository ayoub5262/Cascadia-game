package service

import entity.BotSpeed
import org.junit.jupiter.api.AfterEach
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Class for testing the loadGame method.
 */
class LoadGameTest {
    /** The [RootService] used for testing. Will be initialized before each test. */
    private lateinit var rootService: RootService
    /** The [TestRefreshable] used for testing. Will be initialized before each test. */
    private lateinit var testRefreshable: TestRefreshable

    private val fileName = "testSave.dat"

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
     * Löscht die erstellte Testdatei nach jedem Testlauf.
     */
    @AfterEach
    fun deleteFile() {
        File(fileName).delete()
    }

    /**
     * Tests if a saved game is loaded correctly.
     */
    @Test
    fun `Should load saved game`() {

        val game = rootService.currentGame?.currentState
        checkNotNull(game)

        game.roundNumber = 5
        game.currentPlayer = 2

        val output = ObjectOutputStream(FileOutputStream(fileName))
        output.writeObject(rootService.currentGame)
        output.close()

        rootService.currentGame = null

        rootService.gameActionService.loadGame(fileName)

        val loadedGame = rootService.currentGame?.currentState
        checkNotNull(loadedGame)

        assertEquals(5, loadedGame.roundNumber)
        assertEquals(2, loadedGame.currentPlayer)
    }

    /**
     * Tests if refreshAfterLoadGame is called.
     */
    @Test
    fun `Should call refresh after load`() {

        val output = ObjectOutputStream(FileOutputStream(fileName))
        output.writeObject(rootService.currentGame)
        output.close()

        rootService.currentGame = null

        rootService.gameActionService.loadGame(fileName)

        assertTrue(testRefreshable.refreshAfterLoadGameCalled)
    }

    /**
     * Tests Habitat Drawable order.
     */
    @Test
    fun `Should keep stack order after load`() {

        val game = rootService.currentGame?.currentState
        checkNotNull(game)

        val expectedTiles = game.habitatDrawPile.peekAll().map { it.id }
        val expectedTopTile = game.habitatDrawPile.peek().id
        val expectedTokens = game.wildlifeBag.peekAll().map { it.animal }

        val output = ObjectOutputStream(FileOutputStream(fileName))
        output.writeObject(rootService.currentGame)
        output.close()

        rootService.currentGame = null

        rootService.gameActionService.loadGame(fileName)

        val loadedGame = rootService.currentGame?.currentState
        checkNotNull(loadedGame)

        assertEquals(expectedTiles, loadedGame.habitatDrawPile.peekAll().map { it.id })
        assertEquals(expectedTokens, loadedGame.wildlifeBag.peekAll().map { it.animal })
        assertEquals(expectedTopTile, loadedGame.habitatDrawPile.pop().id)
    }

    /**
     * Tests loading a file that does not exist.
     */
    @Test
    fun `Should do nothing if file does not exist`() {

        rootService.currentGame = null

        rootService.gameActionService.loadGame("notExisting.dat")

        assertEquals(null, rootService.currentGame)
    }
}