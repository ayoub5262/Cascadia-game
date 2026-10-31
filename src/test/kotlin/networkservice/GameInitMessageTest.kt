package networkservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import edu.udo.cs.sopra.ntf.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for validating the JSON serialization of {@link GameInitMessage}
 * against its corresponding JSON schema.
 */
class GameInitMessageTest {

    /** Object mapper used to serialize Kotlin objects into JSON. */
    private val mapper = jacksonObjectMapper()

    /**
     * Verifies that a valid {@link GameInitMessage} instance is correctly
     * serialized to JSON and conforms to the GameInitMessage JSON schema.
     *
     * The test creates a sample game initialization message containing a tile
     * stack, scoring cards, player information, and a wildlife bag. It then
     * serializes the message to JSON and validates it against the schema.
     *
     * The test succeeds if the validation produces no schema errors.
     */
    @Test
    fun testGameInitMessageValidation() {

        val gameInitMessage = GameInitMessage(
            tileStack = listOf(1, 2, 3),
            scoringCards = listOf(true, false, true, false, true),
            players = listOf(
                NetPlayer(name = "Kaan", startingTileID = 0),
                NetPlayer(name = "Metehan", startingTileID = 1)
            ),
            wildlifeBag = (0 until 100).map { NetWildlife.entries.random() }
        )

        println(mapper.writeValueAsString(gameInitMessage))

        val schema = Util.loadSchema("json/gameInitMessage.json")
        val json = mapper.writeValueAsString(gameInitMessage)
        val errors = schema.validate(mapper.readTree(json))
        println(errors.map { it.message })
        assertEquals(0, errors.size)
    }
}