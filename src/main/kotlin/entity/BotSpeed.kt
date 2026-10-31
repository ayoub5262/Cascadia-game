package entity

/**
 * Enum class for listing different bot speed presets
 */
enum class BotSpeed {
    SLOW,
    NORMAL,
    FAST,
    INSTANT
    ;

    /**
     * Returns the given time the Bot should take between actions as [Long]
     *
     * [BotSpeed.SLOW] -> 1000 MS
     * [BotSpeed.NORMAL] -> 500 MS
     * [BotSpeed.FAST] -> 150 MS
     * [BotSpeed.INSTANT] -> 1 MS
     */
    fun toMillieSeconds() = when(this) {
        BotSpeed.SLOW -> 1000
        BotSpeed.NORMAL -> 500
        BotSpeed.FAST -> 150
        BotSpeed.INSTANT -> 1
    }.toLong()
}