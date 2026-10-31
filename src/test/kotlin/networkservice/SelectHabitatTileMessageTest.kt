package networkservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import edu.udo.cs.sopra.ntf.SelectHabitatTileMessage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for validating the JSON serialization of
 * {@link SelectHabitatTileMessage} against its corresponding JSON schema.
 */
class SelectHabitatTileMessageTest {

    /** Object mapper used to serialize Kotlin objects into JSON. */
    private val mapper = jacksonObjectMapper()

    /**
     * Verifies that a valid {@link SelectHabitatTileMessage} instance is
     * correctly serialized to JSON and conforms to the
     * SelectHabitatTileMessage JSON schema.
     *
     * The test succeeds if the serialized JSON produces no schema
     * validation errors.
     */
    @Test
    fun testSelectHabitatTileMessageValidation() {
        val selectHabitatTileMessage = SelectHabitatTileMessage(habitatShopIndex = 2)

        val schema = Util.loadSchema("json/SelectHabitatTileMessage.json")
        val json = mapper.writeValueAsString(selectHabitatTileMessage)
        val errors = schema.validate(mapper.readTree(json))
        assertEquals(0, errors.size)
    }
}
