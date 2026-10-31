package service.bot

/**
 * A class for easier access to elk counting data
 */
class ElkCounting {
    var potentialElkCounter = 0
    var actualElkCounter = 0
    var chainBroken = false
    var chainBrokenAfter: Int? = 0
}