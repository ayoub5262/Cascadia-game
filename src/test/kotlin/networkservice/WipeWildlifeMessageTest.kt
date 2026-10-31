package networkservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import edu.udo.cs.sopra.ntf.NetWildlife
import edu.udo.cs.sopra.ntf.WipeWildlifeMessage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for validating the JSON serialization of
 * {@link WipeWildlifeMessage} against its corresponding JSON schema.
 */
class WipeWildlifeMessageTest {

    /** Object mapper used to serialize Kotlin objects into JSON. */
    private val mapper = jacksonObjectMapper()

    /**
     * Verifies that a valid {@link WipeWildlifeMessage} instance is correctly
     * serialized to JSON and validated against the
     * WipeWildlifeMessage JSON schema.
     *
     * The test creates a sample message containing information about the
     * usage of a nature token, the remaining number of nature tokens, the
     * indices of the wildlife tokens that were wiped, and the updated
     * wildlife bag. It then serializes the message to JSON and validates the
     * generated JSON using the corresponding schema.
     *
     * The test succeeds if the validation produces no schema errors.
     */
    @Test
    fun testWipeWildlifeMessageValidation() {
        val wipeWildlifeMessage = WipeWildlifeMessage(
            usedNatureToken = false,
            natureTokenAmount = 2,
            wipedWildlifeIndices = listOf(0, 1),
            wildlifeBag = listOf(NetWildlife.BEAR, NetWildlife.ELK, NetWildlife.FOX)
        )

        val schema = Util.loadSchema("json/wipeWildlifeMessage.json")
        val json = mapper.writeValueAsString(wipeWildlifeMessage)
        val errors = schema.validate(mapper.readTree(json))
        assertEquals(0, errors.size)
    }
}