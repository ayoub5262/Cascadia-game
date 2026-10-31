package networkservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import edu.udo.cs.sopra.ntf.GameConfigMessage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for validating the JSON serialization of {@link GameConfigMessage}
 * against its corresponding JSON schema.
 */
class GameConfigMessageTest {
    private val mapper = jacksonObjectMapper()

    /**
     * Verifies that a valid {@link GameConfigMessage} instance is correctly
     * serialized to JSON and conforms to the GameConfigMessage JSON schema.
     *
     * The test succeeds if the serialized JSON produces no schema validation
     * errors.
     */
    @Test
    fun testGameConfigMessageValidation() {
        val gameConfigMessage = GameConfigMessage(
            players = listOf("Kaan", "Metehan"),
            scoringCards = listOf(true, false, null, true, false)
        )

        val schema = Util.loadSchema("json/GameConfigMessage.json")
        val json = mapper.writeValueAsString(gameConfigMessage)
        val errors = schema.validate(mapper.readTree(json))
        assertEquals(0, errors.size)
    }
}