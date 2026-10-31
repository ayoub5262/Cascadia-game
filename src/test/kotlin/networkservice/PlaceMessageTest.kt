package networkservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import edu.udo.cs.sopra.ntf.PlaceMessage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for validating the JSON serialization and message formatting
 * of {@link PlaceMessage}.
 */
class PlaceMessageTest {

    /** Object mapper used to serialize Kotlin objects into JSON. */
    private val mapper = jacksonObjectMapper()

    /**
     * Verifies that a valid {@link PlaceMessage} instance is correctly
     * serialized to JSON and conforms to the PlaceMessage JSON schema.
     *
     * The test succeeds if the generated JSON produces no schema validation
     * errors.
     */
    @Test
    fun testPlaceMessageValidation() {
        val placeMessage = PlaceMessage(
            habitatCoordinates = Pair(1, 2),
            wildlifeCoordinates = Pair(-1, -6),
            habitatRotation = 3
        )

        val schema = Util.loadSchema("json/placeMessage.json")
        val json = mapper.writeValueAsString(placeMessage)
        val errors = schema.validate(mapper.readTree(json))
        assertEquals(0, errors.size)
    }

    /**
     * Verifies that {@code formatMessage()} returns the expected formatted
     * string when both habitat and wildlife placements are provided.
     */
    @Test
    fun testFormatMessageWithWildlife() {
        val wildlifeMessage = PlaceMessage(
            Pair(1, 2),
            Pair(-1, -6),
            3
        )

        val wildlifeString = wildlifeMessage.formatMessage()

        assertEquals(
            wildlifeString,
            "Place habitat on 1 Q 2 R.\n" +
                    "Wildlife placed on -1 Q -6 R.\n" +
                    "Habitat rotation is 3"
        )
    }

    /**
     * Verifies that {@code formatMessage()} returns the expected formatted
     * string when no wildlife is placed and the wildlife token is discarded.
     */
    @Test
    fun testFormatMessageDiscardWildlife() {
        val wildlifeMessage = PlaceMessage(
            Pair(1, 2),
            null,
            3
        )

        val wildlifeString = wildlifeMessage.formatMessage()

        assertEquals(
            wildlifeString,
            "Place habitat on 1 Q 2 R.\n" +
                    "Wildlife discarded.\n" +
                    "Habitat rotation is 3"
        )
    }
}
