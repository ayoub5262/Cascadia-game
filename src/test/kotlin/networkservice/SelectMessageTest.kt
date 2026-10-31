package networkservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import edu.udo.cs.sopra.ntf.SelectMessage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for validating the JSON serialization of
 * {@link SelectMessage} against its corresponding JSON schema.
 */
class SelectMessageTest {

    /** Object mapper used to serialize Kotlin objects into JSON. */
    private val mapper = jacksonObjectMapper()

    /**
     * Verifies that a valid {@link SelectMessage} instance is correctly
     * serialized to JSON and validated against the SelectMessage JSON schema.
     *
     * The test creates a sample message, serializes it to JSON, and validates
     * the generated JSON using the corresponding schema.
     *
     * The test succeeds if the validation produces no schema errors.
     */
    @Test
    fun testSelectMessageValidation() {
        val selectMessage = SelectMessage(
            wildlifeShopIndex = 1,
            habitatShopIndex = 2,
            usedNatureToken = false
        )

        val schema = Util.loadSchema("json/SelectMessageSchema.json")
        val json = mapper.writeValueAsString(selectMessage)
        val errors = schema.validate(mapper.readTree(json))
        assertEquals(0, errors.size)
    }
}