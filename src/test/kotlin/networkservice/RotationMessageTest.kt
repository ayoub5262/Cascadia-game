package networkservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import edu.udo.cs.sopra.ntf.RotationMessage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for validating the JSON serialization of {@link RotationMessage}
 * against its corresponding JSON schema.
 */
class RotationMessageTest {

    /** Object mapper used to serialize Kotlin objects into JSON. */
    private val mapper = jacksonObjectMapper()

    /**
     * Verifies that a valid {@link RotationMessage} instance is correctly
     * serialized to JSON and conforms to the RotationMessage JSON schema.
     *
     * The test succeeds if the serialized JSON produces no schema validation
     * errors.
     */
    @Test
    fun testRotationMessageValidation() {
        val rotationMessage = RotationMessage(habitatRotation = 3)

        val schema = Util.loadSchema("json/RotationMessage.json")
        val json = mapper.writeValueAsString(rotationMessage)
        val errors = schema.validate(mapper.readTree(json))
        assertEquals(0, errors.size)
    }
}
