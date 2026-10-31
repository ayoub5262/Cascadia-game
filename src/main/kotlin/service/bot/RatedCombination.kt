package service.bot

/**
 * Class for saving bot decision related information
 */
data class RatedCombination(
    val shopWildlifeIndex: Int,
    val shopHabitatIndex: Int,
    val combinationScore: Double?,
    val coordsOfHabitat: Pair<Int, Int>,
    val habitatRotationCount: Int,
    val coordsOfWildlife: Pair<Int, Int>?
    )