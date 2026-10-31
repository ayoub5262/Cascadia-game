package networkservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import edu.udo.cs.sopra.ntf.UseNatureTokenMessage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for validating the JSON serialization of
 * {@link UseNatureTokenMessage} against its corresponding JSON schema.
 */
class UseNatureTokenMessageTest {

    /** Object mapper used to serialize Kotlin objects into JSON. */
    private val mapper = jacksonObjectMapper()

    /**
     * Verifies that a valid {@link UseNatureTokenMessage} instance is correctly
     * serialized to JSON and validated against the
     * UseNatureTokenMessage JSON schema.
     *
     * The test creates a sample message, serializes it to JSON, and validates
     * the generated JSON using the corresponding schema.
     *
     * The test succeeds if the validation produces no schema errors.
     */
    @Test
    fun testUseNatureTokenMessageValidation() {
        val useNatureTokenMessage = UseNatureTokenMessage()

        val schema = Util.loadSchema("json/useNatureTokenMessage.json")
        val json = mapper.writeValueAsString(useNatureTokenMessage)
        val errors = schema.validate(mapper.readTree(json))
        assertEquals(0, errors.size)
    }
}