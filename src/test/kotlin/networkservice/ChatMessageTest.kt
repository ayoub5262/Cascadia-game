package networkservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import edu.udo.cs.sopra.ntf.ChatMessage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for validating the JSON serialization of {@link ChatMessage}
 * against its corresponding JSON schema.
 */
class ChatMessageTest {
    private val mapper = jacksonObjectMapper()

    /**
     * Verifies that a valid {@link ChatMessage} object can be serialized to JSON
     * and successfully validated against the ChatMessage JSON schema.
     *
     * The test passes if the generated JSON produces no schema validation errors.
     */
    @Test
    fun testChatMessageValidation() {
        val chatMessage = ChatMessage(message = "Hello, world!")

        val schema = Util.loadSchema("json/ChatMessage.json")
        val json = mapper.writeValueAsString(chatMessage)
        val errors = schema.validate(mapper.readTree(json))
        assertEquals(0, errors.size)
    }
}