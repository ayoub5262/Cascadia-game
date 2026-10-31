package entity

import java.io.Serializable
import java.util.Stack

/**
 * Class [CascadiaGameSave] represents the save data
 */
class CascadiaGameSave: Serializable {
    var currentState: CascadiaGame? = null
    val history: Stack<CascadiaGame?> = Stack()
    val undoes: Stack<CascadiaGame?> = Stack()

    /**
     * Companion object containing constants for serialization.
     */
    companion object {
        private const val serialVersionUID = 1L
    }
}