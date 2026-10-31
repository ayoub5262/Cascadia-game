package networkservice

import edu.udo.cs.sopra.ntf.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for verifying the string formatting of {@link NetPlayer}.
 */
class NetPlayerTest {

    /**
     * Verifies that the {@code formatMessage()} method of {@link NetPlayer}
     * returns the expected formatted string representation.
     *
     * The test creates a sample player, formats it using
     * {@code formatMessage()}, and compares the result with the expected
     * output.
     */
    @Test
    fun testFormatMessage() {
        val netPlayerJohn = NetPlayer("Kaan", 20)
        val netPlayerJohnString = netPlayerJohn.formatMessage()

        assertEquals(
            netPlayerJohnString,
            "NetPlayer:\n" +
                    "\t\"name\" : \"Kaan\",\n" +
                    "\t\"startingTileID\" : 20"
        )

        println(netPlayerJohn.formatMessage())
    }
}