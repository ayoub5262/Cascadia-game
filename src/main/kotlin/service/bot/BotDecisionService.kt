package service.bot

import entity.Habitat
import entity.Wildlife
import entity.HabitatTile
import entity.WildlifeToken
import service.RootService

/**
 * A class for all decisions the bot has to make
 */
class BotDecisionService(private val rootService: RootService) {

    //List of all directions offsets in hex grid
    val directions = listOf(
        Pair(1, -1), Pair(1, 0), Pair(0, 1), Pair(-1, 1), Pair(-1, 0), Pair(0, -1)
    )

    /**
     * Returns a sorted List of RatedCombination
     *
     * Deducts 1.1 points for natureToken use
     */
    internal fun rateCombinations(
        placeableHabitatCoords: List<Pair<Int, Int>>
    ): List<RatedCombination> {

        val currentState = checkNotNull(rootService.currentGame?.currentState)
        val board = currentState.players[currentState.currentPlayer].board

        val ratedCombinations = mutableListOf<RatedCombination>()


        // Try every shop combination
        for (wildlifeIndex in 0..3) {
            for (habitatIndex in 0..3) {

                val animal =
                    checkNotNull(currentState.wildlifeShop[wildlifeIndex])

                val habitat =
                    checkNotNull(currentState.habitatShop[habitatIndex])


                var bestTotalScore = Double.NEGATIVE_INFINITY

                var bestHabitatCoord: Pair<Int, Int>? = null
                var bestRotation = 0
                var bestWildlifeCoord: Pair<Int, Int>? = null



                // every pos
                for (coord in placeableHabitatCoords) {


                    // every Rot
                    for (rotation in 0..5) {


                        val rotatedEdges =
                            rotateEdges(
                                habitat.habitats,
                                rotation
                            )


                        val simulatedBoard =
                            HashMap<Pair<Int, Int>, HabitatTile>()

                        simulatedBoard.putAll(board)



                        val simulatedTile =
                            habitat.copy(
                                habitats =
                                    rotatedEdges.toMutableList()
                            )

                        simulatedTile.rotation = rotation


                        simulatedBoard[coord] = simulatedTile




                         //HABITAT SCORE


                        val edgeScore =
                            computeEdgeMatchScoreUniqueHabitats(
                                coord,
                                rotatedEdges,
                                board
                            )
                        var clusterScore = 0
                        for (habitat in simulatedTile.habitats){
                            clusterScore += connectedClusterSizeFrom(
                                coord,
                                habitat,
                                simulatedBoard
                            )
                        }




                        val futureAnimalScore =
                            calculateFutureAnimalPotential(
                                simulatedTile,
                                coord,
                                simulatedBoard
                            )


                        val habitatScore =
                            edgeScore * 2.0 +
                                    clusterScore +
                                    futureAnimalScore * 3.0




                            //WILDLIFE SCORE


                        var placeableAnimalCoords =
                            getAllPlaceableTokenCoords(
                                animal.animal
                            )


                        if (habitat.possibleAnimals.contains(animal.animal)) {

                            placeableAnimalCoords =
                                placeableAnimalCoords + coord
                        }


                        val wildlifeResult =
                            calcBestWildlifePositionWithScore(
                                animal,
                                placeableAnimalCoords,
                                simulatedBoard
                            )


                        val wildlifeScore =
                            wildlifeResult.second




                         // COMPLETE MOVE SCORE


                        val totalScore =
                            habitatScore + wildlifeScore //TODO zum wiederfinden



                        if (totalScore > bestTotalScore) {

                            bestTotalScore = totalScore

                            bestHabitatCoord = coord
                            bestRotation = rotation
                            bestWildlifeCoord = wildlifeResult.first
                        }
                    }
                }



                ratedCombinations.add(
                    RatedCombination(
                        shopWildlifeIndex = wildlifeIndex,
                        shopHabitatIndex = habitatIndex,
                        combinationScore = bestTotalScore,
                        coordsOfHabitat = checkNotNull(bestHabitatCoord),
                        habitatRotationCount = bestRotation,
                        coordsOfWildlife = bestWildlifeCoord
                    )
                )
            }
        }


        return ratedCombinations.sortedWith(
            compareByDescending {
                it.combinationScore
            }
        )
    }
    private fun calculateFutureAnimalPotential(
        habitat: HabitatTile,
        coord: Pair<Int, Int>,
        board: HashMap<Pair<Int, Int>, HabitatTile>
    ): Double {

        val resList = mutableListOf<Pair<Pair<Int, Int>?, Double>>()
        for (animalType in habitat.possibleAnimals) {


            val fakeAnimal =
                WildlifeToken(animalType)


            val possibleCoords = listOf(coord)



            val result =
                calcBestWildlifePositionWithScore(
                    fakeAnimal,
                    possibleCoords,
                    board
                )

            resList.add(result)

        }

        var score = resList.sortedByDescending { it.second }.first().second
        score += habitat.possibleAnimals.size
        return score
    }

    /**
     * Returns a List of indices that should be redrawn
     * Saves best combination if it's over a certain threshold
     */
    internal fun decideTokensToRedraw(ratedCombinations: List<RatedCombination>) : List<Int> {

        val listOfIndices = mutableListOf(0, 1, 2, 3)

        for (combination in ratedCombinations) {
            if (combination.shopHabitatIndex == combination.shopWildlifeIndex) {
                listOfIndices.remove(combination.shopWildlifeIndex)
                break
            }
        }

        return listOfIndices
    }

    /**
     * returns all available coordinates for HabitatTile placement
     */
    internal fun getAllPlaceableHabitatCoords(): List<Pair<Int, Int>> {
        val game = checkNotNull(rootService.currentGame?.currentState)
        val player = game.players[game.currentPlayer]
        val placeableCoords = mutableSetOf<Pair<Int, Int>>()

        //iterates over all already placed tiles
        for (placedTile in player.board.keys) {

//            println("Tile at: $placedTile")

            //adds an offset to check all 6 edges of placed tile for another tile
            for ((x, y) in directions) {

                val modifiedCoords = Pair(placedTile.first + x, placedTile.second + y)

                //if there is no tile at given coordinates add it to set
                if (modifiedCoords !in player.board.keys) {
                    placeableCoords.add(modifiedCoords)
                }
            }
        }

//        println("Placeable Coordinates: $placeableCoords")

        return placeableCoords.toList()
    }

//    /**
//     * Get all Habitat Cords That have an open Token slot
//     */
//    internal fun getAllPlaceableTokenCoords(): List<Pair<Int, Int>> {
//        val game = checkNotNull(rootService.currentGame?.currentState)
//        val player = game.players[game.currentPlayer]
//        val placeableCoords = mutableSetOf<Pair<Int, Int>>()
//
//        //iterates over all already placed tiles
//        for (placedTileMap in player.board) {
//
//            //if no WildlifeToke is placed on tile, add tile coordinates to set
//            if (placedTileMap.value.placedWildlifeToken == null) {
//                placeableCoords.add(placedTileMap.key)
//            }
//        }
//
//        return placeableCoords.toList()
//    }

    /**
     * Get all Habitat Cords That have an open Token slot with given animal type
     */
    internal fun getAllPlaceableTokenCoords(animal: Wildlife): List<Pair<Int, Int>> {
        val game = checkNotNull(rootService.currentGame?.currentState)
        val player = game.players[game.currentPlayer]

        return player.board.filter {
            it.value.possibleAnimals.contains(animal) && it.value.placedWildlifeToken == null
        }.keys.toList()
    }

    private fun getOppositeCoordinateOffset(offset: Pair<Int, Int>) : Pair<Int, Int>? {

        return when(offset) {
            Pair(1, -1) -> Pair(0, 1)
            Pair(1, 0) -> Pair(-1, 0)
            Pair(0, 1) -> Pair(0, -1)
            Pair(0, 1) -> Pair(1, -1)
            Pair(-1, 0) -> Pair(1, 0)
            Pair(0, -1) -> Pair(0, 1)
            else -> null
        }
    }

    /**
     * Returns the points a keystone gives relative to current round as double
     */
    private fun calcKeystoneBonus() : Double {

        val game = checkNotNull(rootService.currentGame?.currentState)

        val bonus = when(game.roundNumber) {
            in 1..5 -> 5.0 //TODO
            in 6..15 -> 1.5
            else -> 1
        }.toDouble()

        return bonus
    }

//    /**
//     * Result of scoring one habitat tile at a single board coordinate: the [coord] itself,
//     * the [bestRotation] found there, its [edgeMatchScore] and the largest cluster size per
//     * habitat type in [largestClusterByHabitat].
//     */
//    data class BestHabitatAtCoord(
//        val coord: Pair<Int, Int>,
//        val bestRotation: Int,
//        val edgeMatchScore: Int, // unique habitat matches, not per-edge
//        val largestClusterByHabitat: Map<Habitat, Int>,
//        val potentialAnimalScore: Int
//    )

    private fun rotateEdges(edges: List<Habitat>, rotation: Int): List<Habitat> {
        val result = MutableList(6) { edges[0] }
        val r = ((rotation % 6) + 6) % 6

        for (i in 0..5) {
            val oldIndex = (i - r + 6) % 6
            result[i] = edges[oldIndex]
        }

        return result
    }

    private fun computeEdgeMatchScoreUniqueHabitats(
        coord: Pair<Int, Int>,
        rotatedEdges: List<Habitat>,
        board: Map<Pair<Int, Int>, HabitatTile>
    ): Int {
        val matchedHabitats = mutableSetOf<Habitat>()

        for (dir in 0..5) {
            val nCoord = neighbor(coord, dir)
            val nTile = board[nCoord] ?: continue
            val opp = opposite(dir)

            val a = rotatedEdges[dir]
            val b = nTile.habitats[opp]

            if (a == b) {
                matchedHabitats.add(a)
//                println("--------------------------------")
//                println("MATCH coord=$coord dir=$dir opp=$opp habitat=$a neighbor=$nCoord")
//                println("placed tile: $rotatedEdges")
//                println("neighbor tile: ${nTile.habitats}")
            }
        }

        return matchedHabitats.size
    }

    private fun connectedClusterSizeFrom(
        startCoord: Pair<Int, Int>,
        habitat: Habitat,
        board: Map<Pair<Int, Int>, HabitatTile>
    ): Int {
        val startTile = board[startCoord] ?: return 0
        if (!startTile.habitats.contains(habitat)) return 0

        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        visited.add(startCoord)
        queue.add(startCoord)

        var size = 0

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            size++

            val currentTile = board[current] ?: continue
            val currentEdges = currentTile.habitats

            for (dir in 0..5) {
                if (currentEdges[dir] != habitat) continue

                val nCoord = neighbor(current, dir)
                val nTile = board[nCoord]
                val connectsToHabitat = nTile != null && nTile.habitats[opposite(dir)] == habitat

                if (connectsToHabitat && !visited.contains(nCoord)) {
                    visited.add(nCoord)
                    queue.add(nCoord)
                }
            }
        }

        return size
    }

    private fun opposite(dir: Int): Int {
        return (dir + 3) % 6
    }

    private fun neighbor(c: Pair<Int, Int>, dir: Int): Pair<Int, Int> {
        val q = c.first
        val r = c.second

        return when (dir) {
            0 -> Pair(q + 1, r - 1) // topRight
            1 -> Pair(q + 1, r)     // right
            2 -> Pair(q, r + 1)     // bottomRight
            3 -> Pair(q - 1, r + 1) // bottomLeft
            4 -> Pair(q - 1, r)     // left
            5 -> Pair(q, r - 1)     // topLeft
            else -> throw IllegalArgumentException("Invalid direction: $dir")
        }
    }

//    internal fun pickBestOverallPlacement(
//        results: List<BestHabitatAtCoord>
//    ): BestHabitatAtCoord? {
//        return results.maxWithOrNull(
//            compareBy<BestHabitatAtCoord>(
//                { it.edgeMatchScore },
//                { it.largestClusterByHabitat.values.sum() },
//                { -it.bestRotation },
//                { -it.coord.first },
//                { -it.coord.second }
//            )
//        )
//    }

    /**
     * Calculates best position for HabitatTile with score
     *
     * @return Pair<CoordsOfWildlifeToken, ScoreOfWildlifeToken>
     */
    private fun calcBestWildlifePositionWithScore(
        animal: WildlifeToken,
        placeableAnimalCoords: List<Pair<Int, Int>>,
        board : HashMap<Pair<Int, Int>, HabitatTile>
    ) : Pair<Pair<Int, Int>?, Double> {

        val game = checkNotNull(rootService.currentGame?.currentState)

        val scoringCardSalmon = game.scoringCards.filter { it.wildlife == Wildlife.SALMON }
        val scoringCardHawk = game.scoringCards.filter { it.wildlife == Wildlife.HAWK }
        val scoringCardFox = game.scoringCards.filter { it.wildlife == Wildlife.FOX }
        val scoringCardElk = game.scoringCards.filter { it.wildlife == Wildlife.ELK }
        val scoringCardBear = game.scoringCards.filter { it.wildlife == Wildlife.BEAR }

        if (placeableAnimalCoords.isNotEmpty()) {
            // Call score function for given animal
            var coordsWithScore = when (animal.animal) {
                Wildlife.SALMON ->
                    calculateSalmonScore(scoringCardSalmon[0].isCardTypeA, placeableAnimalCoords, board)
                Wildlife.HAWK ->
                    calculateHawkScore(scoringCardHawk[0].isCardTypeA, placeableAnimalCoords, board)
                Wildlife.FOX ->
                    calculateFoxScore(scoringCardFox[0].isCardTypeA, placeableAnimalCoords, board)
                Wildlife.ELK ->
                    calculateElkScore(scoringCardElk[0].isCardTypeA, placeableAnimalCoords, board)
                Wildlife.BEAR ->
                    calculateBearScore(scoringCardBear[0].isCardTypeA, placeableAnimalCoords, board)
            }

            // sort coords by score
            coordsWithScore = coordsWithScore.sortedByDescending { it.second }

            // If all combinations break a formation, best choice would be to discard token
            if (coordsWithScore[0].second >= 0) {

                // return coords with highest score
                return coordsWithScore[0]
            }
        }

        // If no good choice was found, return negative score
        return Pair(null, -5.0)
    }

    private fun calculateSalmonScore(
        isTypeA: Boolean, placeableAnimalCoords: List<Pair<Int, Int>>, board: Map<Pair<Int, Int>, HabitatTile>):
            List<Pair<Pair<Int, Int>, Double>> {

        // Maps coords to score
        val scores = mutableListOf<Pair<Pair<Int, Int>, Double>>()

        // how many fish are adjacent without two or more fish adjacent to them
        for (coords in placeableAnimalCoords) {

            // Rates the tile as very negative if set
            var trashFlag = false

            // dfs
            val queue = ArrayDeque<Pair<Int, Int>>()
            val visited = mutableSetOf<Pair<Int, Int>>()

            // Saves the coords of salmon chain
            val chainCoords = mutableSetOf<Pair<Int, Int>>()

            queue.add(coords)

            // Stops dfs if Salmon chain has a triangle at any point
            while (queue.isNotEmpty() && !trashFlag) {

                val searchCoords = queue.removeFirst()

                if (!visited.add(searchCoords)) {
                    continue
                }

                val (scanFoundTriangle, wasLastFish) = scanSalmonNeighbors(board, searchCoords, queue, chainCoords)

                if (scanFoundTriangle) {
                    trashFlag = true
                }

                // Sonderfall: Letzter und erste offset Fisch
                val specialOffset = Pair(1, -1)
                val specialCoords =
                    Pair(searchCoords.first + specialOffset.first, searchCoords.second + specialOffset.second)
                if (board[specialCoords]?.placedWildlifeToken?.animal == Wildlife.SALMON && wasLastFish) {
                    trashFlag = true
                }

            }

            val keyStonePoints = if(board[coords]?.isKeyStone == true) calcKeystoneBonus() else 0.0

            val chainPositionPoints = if(trashFlag) -10.0 else salmonPoints(isTypeA, chainCoords.size)

            var score = chainPositionPoints + keyStonePoints

            // fix for single salmons
            val totalSalmons = countTotalSalmons(board)
            val salmonNeighbours = getAnimalsAroundCoords(coords, board).second[Wildlife.SALMON]
            if (totalSalmons > 0 && salmonNeighbours == 0) score = 0.0

            scores.add(Pair(coords, score))
        }

        return scores
    }

    private fun countTotalSalmons(board: Map<Pair<Int, Int>, HabitatTile>) : Int {

        return board.values.filter { it.placedWildlifeToken?.animal == Wildlife.SALMON }.size
    }

    private fun scanSalmonNeighbors(
        board: Map<Pair<Int, Int>, HabitatTile>,
        searchCoords: Pair<Int, Int>,
        queue: ArrayDeque<Pair<Int, Int>>,
        chainCoords: MutableSet<Pair<Int, Int>>
    ): Pair<Boolean, Boolean> {

        var adjacentPlacedFish = 0
        var wasLastFish = false
        var foundTriangle = false

        for (offset in directions) {

            val offsetCoords = Pair(searchCoords.first + offset.first, searchCoords.second + offset.second)
            val tile = board[offsetCoords]
            val holdsSalmon = tile?.placedWildlifeToken?.animal == Wildlife.SALMON

            if (tile?.possibleAnimals?.contains(Wildlife.SALMON) == true) {

                if (holdsSalmon) adjacentPlacedFish += 1

                // Two fish next to each other?
                if (holdsSalmon && wasLastFish) {
                    foundTriangle = true
                    break
                }

                wasLastFish = holdsSalmon
                queue.add(offsetCoords)
            }

            if (adjacentPlacedFish > 2) foundTriangle = true

            if (adjacentPlacedFish > 0) chainCoords.add(offsetCoords)
        }

        return Pair(foundTriangle, wasLastFish)
    }

    private fun salmonPoints(isTypeA: Boolean, chainSize: Int) : Double =
        if (isTypeA) salmonAPoints(chainSize) else salmonBPoints(chainSize)

    /**
     * Returns points for next spot in chain. Returns -2 if chain is full
     */
    private fun salmonAPoints(chainSize: Int) : Double {
        return when(chainSize + 1) {
                1 -> 2.0
                2, 3 -> 3.0
                4, 5, 6 -> 4.0
                7 -> 5.0
                else -> 0.0
        }
    }

    /**
     * Returns points for next spot in chain. Returns -2 if chain is full
     */
    private fun salmonBPoints(chainSize: Int) : Double {
        return when (chainSize + 1) {
            1, 2 -> 2.0
            3 -> 5.0
            4 -> 3.0
            5 -> 6.0
            else -> 0.0
        }
    }

    private fun calculateHawkScore(
        isTypeA: Boolean, placeableAnimalCoords: List<Pair<Int, Int>>, board: Map<Pair<Int, Int>, HabitatTile>):
            List<Pair<Pair<Int, Int>, Double>> {

        val scores = mutableListOf<Pair<Pair<Int, Int>, Double>>()

        val placedHawkPositions = getPlacedHawkPositions(board)

        val validHawkCounter = countFreeHawks(board, placedHawkPositions)

        // Check if open tiles have a placed hawk adjacent
        for (coords in placeableAnimalCoords) {

            var trashFlag = false

            if (anyHawksAround(board, coords)) trashFlag = true

            if (!hawkInSight(coords, placedHawkPositions) && !isTypeA) trashFlag = true

            var tileScore = if (trashFlag) -10.0 else hawkPoints(isTypeA, validHawkCounter)

            tileScore += if (placedHawkPositions.isEmpty() && !isTypeA) 14 else 0 //trashFlag will be set on first hawk

            tileScore += if (board[coords]?.isKeyStone == true) calcKeystoneBonus() else 0.0


            scores.add(Pair(coords, tileScore))
        }

        return scores
    }

    private fun hawkPoints(isTypeA: Boolean, validHawkCounter: Int) : Double {

        val score = if (isTypeA) {
            hawkAPoints(validHawkCounter)
        }
        else {
            hawkBPoints(validHawkCounter)
        }

        return score
    }

    private fun hawkAPoints(numberOfHawks: Int) : Double {

        return when(numberOfHawks + 1) {
            1 -> 2.0 + 4.0
            2, 3, 4, 5 -> 3.0
            6, 7, 8 -> 4.0
            else -> 0.0
        }
    }

    private fun hawkBPoints(numberOfHawks: Int) : Double {

        return when(numberOfHawks + 1) {
            2 -> 5.0
            4 -> 3.0
            3, 5, 6, 7, 8 -> 4.0
            else -> 0.0
        }
    }

    private fun countFreeHawks(board: Map<Pair<Int, Int>, HabitatTile>, placedHawkPositions: List<Pair<Int, Int>>) :
            Int {
        var validHawkCounter = 0

        // Count valid placed hawks (all hawks are valid, just not in this context)
        for (tile in placedHawkPositions) {

            if (!anyHawksAround(board, tile)) validHawkCounter += 1
        }

        return validHawkCounter
    }

    /**
     * Returns true if there is at least one hawk adjacent
     */
    private fun anyHawksAround(board: Map<Pair<Int, Int>, HabitatTile>, coords: Pair<Int, Int>) : Boolean {

        for (offset in directions) {

            val offsetCoords = Pair(coords.first + offset.first, coords.second + offset.second)
            val neighbourPlacedWildlife = board[offsetCoords]?.placedWildlifeToken?.animal

            if (neighbourPlacedWildlife == Wildlife.HAWK) {
                return true
            }
        }
        return false
    }

    /**
     * Returns true if animal at coords has line of sight to at least one other hawk
     */
    private fun hawkInSight(coords: Pair<Int, Int>, placedHawkPositions: List<Pair<Int, Int>>) : Boolean {

        for (hawkPosition in placedHawkPositions) {

            if (hawkPosition.first == coords.first || hawkPosition.second == coords.second) return true

            for (factor in -22..22) {

                if (hawkPosition == Pair(coords.first + factor, coords.second - factor)) return true
            }
        }

        return false
    }

    private fun getPlacedHawkPositions(board: Map<Pair<Int, Int>, HabitatTile>) : List<Pair<Int, Int>> {

        // List of all coords with a placed hawk
        val placedHawkPositions = board.filter {
            it.value.placedWildlifeToken?.animal == Wildlife.HAWK
        }.keys.toList()

        return placedHawkPositions
    }

    private fun calculateFoxScore(
        isTypeA: Boolean, placeableAnimalCoords: List<Pair<Int, Int>>, board: Map<Pair<Int, Int>, HabitatTile>):
            List<Pair<Pair<Int, Int>, Double>> {

        val scores = mutableListOf<Pair<Pair<Int, Int>, Double>>()

        for (coords in placeableAnimalCoords) {

            val animalsAround = getAnimalsAroundCoords(coords, board)
            val placedAnimals = animalsAround.second
            val openAnimals = animalsAround.first.filter {it1->it1.key !in placedAnimals.filter{ it.value > 0 }.keys}

            val potentialScore = if(isTypeA) {
                foxAPoints(openAnimals)
            }
            else {
                foxBPoints(openAnimals)
            }

            val actualScore = if(isTypeA) {
                foxAPoints(placedAnimals)
            }
            else {
                foxBPoints(placedAnimals)
            }

            var score = actualScore + potentialScore/4

            score += if (board[coords]?.isKeyStone == true) calcKeystoneBonus() else 0.0

            scores.add(Pair(coords, score/1.5))
        }

        return scores
    }

    private fun foxAPoints(animalsAround: Map<Wildlife, Int>) : Double {
        return animalsAround.values.filter { it > 0 }.size.toDouble()
    }

    private fun foxBPoints(animalsAround: Map<Wildlife, Int>) : Double {

        // count all pairs except foxes
        val pairCount = animalsAround.filter { it.key != Wildlife.FOX }.values.filter { it >= 2 }.size

        return when(pairCount) {
            1 -> 3.0
            2, 3 -> 2.0
            else -> 0.0
        }
    }

    /**
     * Returns a pair of open animals around and placed animals around
     */
    private fun getAnimalsAroundCoords(coords: Pair<Int, Int>, board: Map<Pair<Int, Int>, HabitatTile>) :
            Pair<Map<Wildlife, Int>, Map<Wildlife, Int>> {

        //Salmon, Hawk, Fox, Elk, Bear
        val openAnimalsAround = mutableMapOf(
            Wildlife.SALMON to 0, Wildlife.HAWK to 0, Wildlife.FOX to 0, Wildlife.ELK to 0, Wildlife.BEAR to 0)
        val placedAnimalsAround = mutableMapOf(
            Wildlife.SALMON to 0, Wildlife.HAWK to 0, Wildlife.FOX to 0, Wildlife.ELK to 0, Wildlife.BEAR to 0)

        for (offset in directions) {

            val offsetCoords = Pair(coords.first + offset.first, coords.second + offset.second)
            val offsetTile = board[offsetCoords]

            if (offsetTile != null) {

                if (offsetTile.placedWildlifeToken != null) {

                    val placedAnimal = checkNotNull(offsetTile.placedWildlifeToken?.animal)
                    placedAnimalsAround[placedAnimal] = (placedAnimalsAround[placedAnimal] ?: 0) + 1
                }

                for (animal in offsetTile.possibleAnimals) {

                    openAnimalsAround[animal] = (openAnimalsAround[animal] ?: 0) + 1
                }
            }
        }

        return Pair(openAnimalsAround, placedAnimalsAround)
    }

    private fun calculateElkScore(
        isTypeA: Boolean,
        placeableAnimalCoords: List<Pair<Int, Int>>,
        board: Map<Pair<Int, Int>, HabitatTile>):
            List<Pair<Pair<Int, Int>, Double>> {

        val scores = mutableListOf<Pair<Pair<Int, Int>, Double>>()

        for (coords in placeableAnimalCoords) {

            val elkCount = countElks(coords, board)
            val score = if (isTypeA) elkScoreTypeA(elkCount) else elkScoreTypeB(elkCount)

            scores.add(Pair(coords, score))
        }

        return scores
    }

    private fun elkScoreTypeA(elkCount: Map<Pair<Int, Int>, ElkCounting>): Double {

        //A list of just the first three directions, since going over all 6 would be unnecessary
        val limitedDirections = listOf(Pair(1, -1), Pair(1, 0), Pair(0, 1))

        var actualElkChainLength = 0
        var potentialElkChainLength = 0

        for (offset in limitedDirections) {

            val elkCounting = checkNotNull(elkCount[offset])
            val elkCountingOpposite = checkNotNull(elkCount[getOppositeCoordinateOffset(offset)])
            actualElkChainLength = elkCounting.actualElkCounter + elkCountingOpposite.actualElkCounter
            potentialElkChainLength = elkCounting.potentialElkCounter + elkCountingOpposite.potentialElkCounter
        }

        val points = elkAPoints(actualElkChainLength) +
                if (actualElkChainLength < 4)
                    elkAPoints(if (potentialElkChainLength < 4) potentialElkChainLength else 4) / 2
                else 0

        return points.toDouble()
    }

    private fun elkScoreTypeB(elkCount: Map<Pair<Int, Int>, ElkCounting>):
            Double {

        var twoElksAdjacent = false
        var lastWasElk = false
        // Band-Aid fix for elk b rating different quantities of elks differently
        var countNeighbours = 0

        for(offset in directions) {

            if (elkCount[offset] != null) countNeighbours += 1

            val elkCounting = checkNotNull(elkCount[offset])
            val elkHere = elkCounting.potentialElkCounter > 0

            if (lastWasElk && elkHere) twoElksAdjacent = true

            lastWasElk = elkHere
        }

        if (countNeighbours > 2) return 5.0

        return if (twoElksAdjacent) 4.0 else 3.0
    }

    /**
     * Returns how many elks are actually in one offset direction and how many potentially.
     * Stops counting actual elks after chain is broken.
     */
    private fun countElks(coords: Pair<Int, Int>, board: Map<Pair<Int, Int>, HabitatTile>) :
            Map<Pair<Int, Int>, ElkCounting> {

        // <Offset direction, actualElkCounter, potentialElkCounter
        val directionElkCount = mutableMapOf<Pair<Int, Int>, ElkCounting>()

        for (offset in directions) {

            val originalCoordsToCheck = Pair(coords.first + offset.first, coords.second + offset.second)
            var coordsToCheck = originalCoordsToCheck
            val elkCounting = ElkCounting()

            while (Wildlife.ELK in (board[coordsToCheck]?.possibleAnimals ?: listOf<Pair<Int, Int>>())) {

                if (elkCounting.actualElkCounter > 3) break

                elkCounting.potentialElkCounter += 1

                if (!elkCounting.chainBroken && board[coordsToCheck]?.placedWildlifeToken?.animal == Wildlife.ELK) {

                    elkCounting.actualElkCounter += 1
                } else {

                    elkCounting.chainBroken = true
                    elkCounting.chainBrokenAfter = elkCounting.actualElkCounter
                }

                coordsToCheck = Pair(
                    coords.first + offset.first * elkCounting.potentialElkCounter,
                    coords.second + offset.second * elkCounting.potentialElkCounter
                )
            }

            directionElkCount[offset] = elkCounting
        }

        return directionElkCount
    }

    private fun elkAPoints(count: Int) : Int {
        return when(count) {
            1 -> 2
            2 -> 3
            3, 4 -> 4
            else -> 0
        }
    }

    private fun calculateBearScore(
         isTypeA: Boolean,
         placeableAnimalCoords: List<Pair<Int, Int>>,
         board: Map<Pair<Int, Int>, HabitatTile>
    ): List<Pair<Pair<Int, Int>, Double>> {

        val scores = mutableListOf<Pair<Pair<Int, Int>, Double>>()

        val desiredBearCount = if (isTypeA) 2 else 3

        for (coords in placeableAnimalCoords) {

            val visitedBears = mutableSetOf<Pair<Int, Int>>()
            val visitedPotentialBears = mutableSetOf(coords)

            val queue = ArrayDeque<Pair<Int, Int>>()

            queue.add(coords)

            while (queue.isNotEmpty() && visitedBears.size < desiredBearCount) {

                val searchCoords = queue.removeFirst()

                val bearCounting = getNeighbourBears(searchCoords, board)

                for (actualBear in bearCounting.actualBearPositions) {

                    if (visitedBears.add(actualBear)) queue.add(actualBear)
                }

                for (potentialBear in bearCounting.potentialBearPositions) {

                    visitedPotentialBears.add(potentialBear)
                }
            }

            var score = if (visitedBears.size >= desiredBearCount) -10.0 else 0.0

            score += if (board[coords]?.isKeyStone == true) calcKeystoneBonus() else 0.0

            score += bearPoints(
                isTypeA,
                visitedBears.size,
                visitedPotentialBears.size,
                countTotalPlacedBears(board))

            scores.add(Pair(coords, score))
        }

        return scores
    }

    /**
     * Gets all placed and unplaced adjacent bears with coordinates
     */
    private fun getNeighbourBears(coords: Pair<Int, Int>, board: Map<Pair<Int, Int>, HabitatTile>) : BearCounting {

        val bearCounting = BearCounting()

        for (offset in directions) {

            val offsetCoords = Pair(coords. first + offset.first, coords.second + offset.second)

            val tileToCheck = board[offsetCoords]

            if (tileToCheck?.placedWildlifeToken?.animal == Wildlife.BEAR) {

                bearCounting.actualBearPositions.add(offsetCoords)
                continue
            }

            val possibleAnimals = if (tileToCheck?.possibleAnimals.isNullOrEmpty()) {
                emptyList()
            }
            else {
                tileToCheck.possibleAnimals
            }

            if (Wildlife.BEAR in possibleAnimals && tileToCheck?.placedWildlifeToken == null) {

                bearCounting.potentialBearPositions.add(offsetCoords)
            }
        }

        return bearCounting
    }

    /**
     * Returns total number of bears on board
     */
    private fun countTotalPlacedBears(board: Map<Pair<Int, Int>, HabitatTile>) : Int {

        return board.values.filter { it.placedWildlifeToken?.animal == Wildlife.BEAR }.size
    }

    private fun bearPoints(
        isTypeA: Boolean,
        bearCount: Int,
        potentialBearCount: Int,
        totalPlacedBearCount: Int) : Double {

        return if (isTypeA) {

            bearAPoints(bearCount, potentialBearCount, totalPlacedBearCount)
        } else {

            bearBPoints(bearCount, potentialBearCount, totalPlacedBearCount)
        }
    }

    private fun bearAPoints(bearCount: Int, potentialBearCount: Int, totalPlacedBearCount: Int) : Double {

        var score = 0.0

        if (potentialBearCount > 0) score = 2.5

        if (bearCount == 1) score = 5.0

        if (totalPlacedBearCount in listOf(4, 6, 8, 10, 12, 14, 16, 18, 20)) score = 0.0

        if (totalPlacedBearCount > 0 && bearCount == 0) score = 0.0

        return score
    }

    private fun bearBPoints(bearCount: Int, potentialBearCount: Int, totalPlacedBearCount: Int) : Double {

        var score = 0.0

        if (potentialBearCount > 0) score = 2.5

        if (potentialBearCount > 1) score = 5.5

        if (bearCount == 2) score = 6.0

        if (totalPlacedBearCount > 0 && bearCount == 0) score = -100.0

        if (totalPlacedBearCount >= 3) score = -100.0

        return score
    }
}