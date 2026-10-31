package networkservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import edu.udo.cs.sopra.ntf.SelectWildlifeMessage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for validating the JSON serialization of
 * {@link SelectWildlifeMessage} against its corresponding JSON schema.
 */
class SelectWildlifeMessageTest {

    /** Object mapper used to serialize Kotlin objects into JSON. */
    private val mapper = jacksonObjectMapper()

    /**
     * Verifies that a valid {@link SelectWildlifeMessage} instance is correctly
     * serialized to JSON and validated against the
     * SelectWildlifeMessage JSON schema.
     *
     * The test creates a sample message, serializes it to JSON, and validates
     * the generated JSON using the corresponding schema.
     *
     * The test succeeds if the validation produces no schema errors.
     */
    @Test
    fun testSelectWildlifeMessageValidation() {
        val selectWildlifeMessage = SelectWildlifeMessage(wildlifeShopIndex = 1)

        val schema = Util.loadSchema("json/SelectWildlifeMessage.json")
        val json = mapper.writeValueAsString(selectWildlifeMessage)
        val errors = schema.validate(mapper.readTree(json))
        assertEquals(0, errors.size)
    }
}