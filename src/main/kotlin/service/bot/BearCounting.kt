package service.bot

/**
 * A class for easier access to bear counting data
 */
class BearCounting {
    var potentialBearPositions: MutableSet<Pair<Int, Int>> = mutableSetOf()
    var actualBearPositions: MutableSet<Pair<Int, Int>> = mutableSetOf()
}