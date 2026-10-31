package service

import entity.*

/*
 * Scoring rules for a finished game. Split out of GameActionService the same way
 * NetworkServiceSend/Receive are split out of NetworkService.
 */

    /**
     * Calcs the final score based on the board state.
     * We go through every wildlife scoring variant (Bears, Foxes, Hawks, Salmons, Elks)
     * and calculate the base habitat scores plus the majority bonuses.
     * Finaly we break ties using any nature tokens left over!
     *
     * @return sorted list of players and their scores, winner is first element
     */
    fun GameActionService.calculateScore(): List<Pair<Player, Map<String, Int>>> {
        val game = checkNotNull(rootService.currentGame?.currentState) { "No active game found" }

        // Determine Variants
        // Create a quick lookup map to determine if Variant A is active for each animal
        val variantAMap = mutableMapOf<Wildlife, Boolean>()
        for (card in game.scoringCards) {
            variantAMap[card.wildlife] = card.isCardTypeA
        }

        // Pre-calculate Habitats
        val playerBaseScores = mutableMapOf<Player, Map<Habitat, Int>>()
        for (player in game.players) {
            playerBaseScores[player] = calculateBaseHabitatScore(player)
        }
        val habitatBonusScores = calculateHabitatMajorityBonuses(playerBaseScores)

        val playerScores = mutableListOf<Pair<Player, Map<String, Int>>>()

        // Compile Player Scores
        for (player in game.players) {
            val scoreMap = mutableMapOf<String, Int>()
            val board = player.board

            // Wildlife Scores (Fall back to true/Variant A if a card is missing)
            scoreMap["Bears"] = scoreBears(board, variantAMap[Wildlife.BEAR] ?: true)
            scoreMap["Elks"] = scoreElks(board, variantAMap[Wildlife.ELK] ?: true)
            scoreMap["Salmons"] = scoreSalmons(board, variantAMap[Wildlife.SALMON] ?: true)
            scoreMap["Hawks"] = scoreHawks(board, variantAMap[Wildlife.HAWK] ?: true)
            scoreMap["Foxes"] = scoreFoxes(board, variantAMap[Wildlife.FOX] ?: true)

            // Habitat Scores

            val baseHabitatTotal = playerBaseScores.getValue(player).values.sum()
            val bonusHabitatTotal = habitatBonusScores.getValue(player).values.sum()

            scoreMap["Habitat Base"] = baseHabitatTotal
            scoreMap["Habitat Bonus"] = bonusHabitatTotal

            // Nature Tokens
            val natureTokenScore = player.natureTokens
            scoreMap["Nature Tokens"] = natureTokenScore

            // Total Score
            val totalScore = scoreMap.values.sum()
            scoreMap["Total"] = totalScore

            playerScores.add(Pair(player, scoreMap))
        }

        // Sort and Return
        playerScores.sortWith { player1, player2 ->
            val totalScore1 = player1.second.getValue("Total")
            val totalScore2 = player2.second.getValue("Total")

            if (totalScore1 != totalScore2) {
                //If totals are different, the higher score comes first
                totalScore2 - totalScore1
            } else {
                // If total scores are tied, the player with more Nature Tokens wins
                val tokens1 = player1.second.getValue("Nature Tokens")
                val tokens2 = player2.second.getValue("Nature Tokens")
                tokens2 - tokens1
            }
        }

        return playerScores
    }

    // runs through all 5 habitats and returns the max size for each
    internal fun GameActionService.calculateBaseHabitatScore(player: Player): Map<Habitat, Int> {
        val scores = mutableMapOf<Habitat, Int>()

        for (habitat in Habitat.entries) {
            val largestSize = getLargestHabitatSize(player.board, habitat)
            scores[habitat] = largestSize
        }

        return scores
    }

    // figures out who has the biggest habitat of each type and hands out the 3/1 or 2/0 bonuses
    internal fun GameActionService.calculateHabitatMajorityBonuses(
        playerBaseScores: Map<Player, Map<Habitat, Int>>
    ): Map<Player, Map<Habitat, Int>> {
        val playerCount = playerBaseScores.size
        require(playerCount in 2..4) { "Player count must be between 2 and 4." }

        // Initialize output map with 0 bonus points for all players and habitats
        val bonusScores = playerBaseScores.keys.associateWith {
            Habitat.entries.associateWith { 0 }.toMutableMap()
        }.toMutableMap()

        for (habitat in Habitat.entries) {
            val scoresForHabitat = playerBaseScores.mapValues { it.value.getValue(habitat) }
            distributeHabitatBonus(bonusScores, habitat, scoresForHabitat, playerCount)
        }

        return bonusScores
    }

    private fun distributeHabitatBonus(
        bonusScores: MutableMap<Player, MutableMap<Habitat, Int>>,
        habitat: Habitat,
        scores: Map<Player, Int>,
        playerCount: Int
    ) {
        val firstPlaceScore = scores.values.maxOrNull() ?: -1
        val secondPlaceScore = scores.values.filter { it < firstPlaceScore }.maxOrNull() ?: -1

        val firstPlacePlayers = scores.filterValues { it == firstPlaceScore }.keys
        val secondPlacePlayers = scores.filterValues { it == secondPlaceScore }.keys

        if (playerCount in 3..4) {
            when {
                firstPlacePlayers.size == 1 -> {
                    bonusScores.getValue(firstPlacePlayers.first())[habitat] = 3
                    if (secondPlacePlayers.size == 1) {
                        bonusScores.getValue(secondPlacePlayers.first())[habitat] = 1
                    }
                }

                firstPlacePlayers.size == 2 -> {
                    for (player in firstPlacePlayers) bonusScores.getValue(player)[habitat] = 2
                }

                firstPlacePlayers.size >= 3 -> {
                    for (player in firstPlacePlayers) bonusScores.getValue(player)[habitat] = 1
                }
            }
        } else { // 2 Players
            when (firstPlacePlayers.size) {
                1 -> {
                    bonusScores.getValue(firstPlacePlayers.first())[habitat] = 2
                }

                2 -> {
                    for (player in firstPlacePlayers) bonusScores.getValue(player)[habitat] = 1
                }
            }
        }
    }

    //find the biggest cluuster of a specific habitat
    private fun getLargestHabitatSize(board: Map<Pair<Int, Int>, HabitatTile>, targetHabitat: Habitat): Int {
        var maxClusterSize = 0
        val globalVisited = mutableSetOf<Pair<Int, Int>>()

        for ((coord, tile) in board) {
            check(tile.habitats.size == 6) { "Tile at $coord must have exactly 6 habitats." }

            // Only start a BFS if this tile hasn't been visited AND contains the target habitat
            if (coord !in globalVisited && tile.habitats.contains(targetHabitat)) {
                val clusterSize = exploreCluster(coord, board, targetHabitat, globalVisited)
                if (clusterSize > maxClusterSize) {
                    maxClusterSize = clusterSize
                }
            }
        }

        return maxClusterSize
    }

    // recursive dfs to count connected habitats
    private fun exploreCluster(
        startCoord: Pair<Int, Int>,
        board: Map<Pair<Int, Int>, HabitatTile>,
        targetHabitat: Habitat,
        globalVisited: MutableSet<Pair<Int, Int>>
    ): Int {
        var currentClusterSize = 0
        val queue = ArrayDeque<Pair<Int, Int>>()

        queue.add(startCoord)
        globalVisited.add(startCoord)

        while (queue.isNotEmpty()) {
            val currentCoord = queue.removeFirst()
            val currentTile = checkNotNull(board[currentCoord]) { "Tile at $currentCoord is missing from board." }

            currentClusterSize++

            for ((neighborCoord, direction) in getNeighbors(currentCoord.first, currentCoord.second)) {
                val neighborTile = board[neighborCoord]
                if (neighborTile == null || neighborCoord in globalVisited) continue

                check(neighborTile.habitats.size == 6) {
                    "Neighbor tile at $neighborCoord must have exactly 6 habitats."
                }

                // Use the helper function to check if the touching edges match each other,
                // and also verify they are the specific target habitat we are looking for.
                val isConnected = doEdgesMatch(currentTile, neighborTile, direction) &&
                        currentTile.habitats[direction] == targetHabitat

                if (isConnected) {
                    globalVisited.add(neighborCoord)
                    queue.add(neighborCoord)
                }
            }
        }

        return currentClusterSize
    }

    // simple helper to get adjacent hexes on our grid
    private fun getNeighbors(column: Int, row: Int): List<Pair<Pair<Int, Int>, Int>> {
        return listOf(
            // Direction 0 (Top-Right): Move 1 column right, move 1 row up
            Pair(Pair(column + 1, row - 1), 0),
            // Direction 1 (Right): Move 1 column right, stay on same row
            Pair(Pair(column + 1, row), 1),
            // Direction 2 (Bottom-Right): Stay on same column, move 1 row down
            Pair(Pair(column, row + 1), 2),
            // Direction 3 (Bottom-Left): Move 1 column left, move 1 row down
            Pair(Pair(column - 1, row + 1), 3),
            // Direction 4 (Left): Move 1 column left, stay on same row
            Pair(Pair(column - 1, row), 4),
            // Direction 5 (Top-Left): Stay on same column, move 1 row up
            Pair(Pair(column, row - 1), 5)
        )
    }

    // checks if the habitat types align perfectly between two tiles
    private fun doEdgesMatch(tileA: HabitatTile, tileB: HabitatTile, directionFromAToB: Int): Boolean {
        require(directionFromAToB in 0..5) { "Direction must be between 0 and 5, but was $directionFromAToB." }
        check(tileA.habitats.size == 6) { "Tile A must have exactly 6 habitats, but had ${tileA.habitats.size}." }
        check(tileB.habitats.size == 6) { "Tile B must have exactly 6 habitats, but had ${tileB.habitats.size}." }


        // We find the opposite edge by adding 3 and taking modulo 6.
        val oppositeDirection = (directionFromAToB + 3) % 6

        return tileA.habitats[directionFromAToB] == tileB.habitats[oppositeDirection]
    }

    // Score bears logic:
    // Variant A: scores for pairs of bears exactly size 2. 1 pair = 4, 2 pairs = 11, 3 pairs = 19, 4 pairs = 27.
    // Variant B: scores groups of exactly 3 bears. 1 group = 10, 2 groups = 20, 3 groups = 30.
    internal fun GameActionService.scoreBears(board: Map<Pair<Int, Int>, HabitatTile>, isVariantA: Boolean): Int {
        val visited = mutableSetOf<Pair<Int, Int>>()
        var pairCount = 0
        var trioCount = 0

        for ((coord, tile) in board) {
            // Only explore if this tile has a bear and hasn't been searched yet
            if (coord !in visited && tile.placedWildlifeToken?.animal == Wildlife.BEAR) {
                val currentGroupSize = exploreBearGroup(coord, board, visited)

                // Evaluate the group size based on the active variant
                if (isVariantA && currentGroupSize == 2) {
                    pairCount++
                } else if (!isVariantA && currentGroupSize == 3) {
                    trioCount++
                }
            }
        }

        // Calculate the final score based on the variant rules
        return if (isVariantA) {
            // Variant A Point Scaling: 1 pair = 4, 2 pairs = 11, 3 pairs = 19, 4+ pairs = 27
            when (pairCount) {
                0 -> 0
                1 -> 4
                2 -> 11
                3 -> 19
                else -> 27
            }
        } else {
            // Variant B: Each trio is worth 10 points
            trioCount * 10
        }
    }

    // dfs just for bears to see how big their group is
    private fun exploreBearGroup(
        startCoord: Pair<Int, Int>,
        board: Map<Pair<Int, Int>, HabitatTile>,
        visited: MutableSet<Pair<Int, Int>>
    ): Int {
        var groupSize = 0
        val queue = ArrayDeque<Pair<Int, Int>>()

        queue.add(startCoord)
        visited.add(startCoord)

        while (queue.isNotEmpty()) {
            val currentCoord = queue.removeFirst()
            groupSize++

            for (neighborData in getNeighbors(currentCoord.first, currentCoord.second)) {
                val neighborCoord = neighborData.first
                val neighborTile = board[neighborCoord]
                if (neighborTile == null || neighborCoord in visited) continue

                if (neighborTile.placedWildlifeToken?.animal == Wildlife.BEAR) {
                    visited.add(neighborCoord)
                    queue.add(neighborCoord)
                }
            }
        }

        return groupSize
    }

    // Fox scoring
    // Variant A: 1 point for each unqiue animal touching the fox
    // Variant B: 1 pair =3, 2 pairs =5, 3 pairs = 7, *pair* of the same animal touching the fox
    internal fun GameActionService.scoreFoxes(board: Map<Pair<Int, Int>, HabitatTile>, isVariantA: Boolean): Int {
        var totalScore = 0

        for ((coord, tile) in board) {
            // Find every Fox on the board
            if (tile.placedWildlifeToken?.animal == Wildlife.FOX) {

                // Collect all valid animals from adjacent tiles
                val neighborAnimals = getNeighborAnimals(coord, board)

                totalScore += if (isVariantA) {
                    scoreFoxVariantA(neighborAnimals)
                } else {
                    scoreFoxVariantB(neighborAnimals)
                }
            }
        }

        return totalScore
    }

    private fun getNeighborAnimals(coord: Pair<Int, Int>, board: Map<Pair<Int, Int>, HabitatTile>): List<Wildlife> {
        val neighborAnimals = mutableListOf<Wildlife>()
        for (neighborData in getNeighbors(coord.first, coord.second)) {
            val neighborCoord = neighborData.first
            val neighborTile = board[neighborCoord]
            val animal = neighborTile?.placedWildlifeToken?.animal
            if (animal != null) {
                neighborAnimals.add(animal)
            }
        }
        return neighborAnimals
    }

    private fun scoreFoxVariantA(neighborAnimals: List<Wildlife>): Int {
        // Variant A: Count distinct adjacent species using a Set
        val distinctAnimals = mutableSetOf<Wildlife>()
        for (animal in neighborAnimals) {
            distinctAnimals.add(animal)
        }
        return distinctAnimals.size
    }

    private fun scoreFoxVariantB(neighborAnimals: List<Wildlife>): Int {
        // Variant B: Points for pairs of identical species

        //count how many of each animal we have
        val animalCounts = mutableMapOf<Wildlife, Int>()
        for (animal in neighborAnimals) {
            if (animal != Wildlife.FOX) {
                val currentCount = animalCounts[animal] ?: 0
                animalCounts[animal] = currentCount + 1
            }
        }

        //count how many pairs we found
        var pairsCount = 0
        for (count in animalCounts.values) {
            if (count >= 2) {
                pairsCount++
            }
        }

        //add points based on how many pairs we found
        return when (pairsCount) {
            1 -> 3
            2 -> 5
            3 -> 7
            else -> 0
        }
    }

    // Hawks score based on how they see each other
    // Variant A: 1=2, 2=5, 3=8, 4=11, 5=14, 6=18, 7=22, 8+=26
    // Variant B: score based on-line of sight. 2=5, 3=9, 4=12, 5=16, 6=20, 7=24, 8+=28
    internal fun GameActionService.scoreHawks(board: Map<Pair<Int, Int>, HabitatTile>, isVariantA: Boolean): Int {
        val solitaryHawks = mutableListOf<Pair<Int, Int>>()

        // Find all Solitary Hawks
        for ((coord, tile) in board) {
            if (tile.placedWildlifeToken?.animal == Wildlife.HAWK) {
                if (isHawkSolitary(coord, board)) {
                    solitaryHawks.add(coord)
                }
            }
        }

        // Evaluate Variant A
        if (isVariantA) {
            // Variant A Point Scaling based on solitary hawk count
            return getHawkPointsVariantA(solitaryHawks.size)
        } else {
            //Evaluate Variant B (Line of Sight)
            var hawksWithLineOfSight = 0
            for (hawkCoord in solitaryHawks) {
                if (hasLineOfSightToAnotherHawk(hawkCoord, board)) {
                    hawksWithLineOfSight++
                }
            }

            // Variant B Point Scling based on solitary hawks WITH line of sight
            return getHawkPointsVariantB(hawksWithLineOfSight)
        }
    }

    // shoots a ray in a straight line to see if another hawk is visible
    private fun hasLineOfSightToAnotherHawk(
        startCoord: Pair<Int, Int>,
        board: Map<Pair<Int, Int>, HabitatTile>
    ): Boolean {
        // The 6 axial directions (deltaColumn, deltaRow) for raycating
        val directions = listOf(
            Pair(1, 0), Pair(0, 1), Pair(-1, 1),
            Pair(-1, 0), Pair(0, -1), Pair(1, -1)
        )

        for ((deltaColumn, deltaRow) in directions) {
            // Check along this ray up to 25 steps, this means the max possible board size
            for (step in 1..25) {
                val checkColumn = startCoord.first + (step * deltaColumn)
                val checkRow = startCoord.second + (step * deltaRow)
                val checkCoord = Pair(checkColumn, checkRow)

                val checkTile = board[checkCoord]

                // If there's a tile here, does it have a Hawk?
                if (checkTile?.placedWildlifeToken?.animal == Wildlife.HAWK) {
                    return true // Line of Sight established!
                }
            }
        }

        return false
    }

    private fun isHawkSolitary(coord: Pair<Int, Int>, board: Map<Pair<Int, Int>, HabitatTile>): Boolean {
        var isSolitary = true
        for (neighborData in getNeighbors(coord.first, coord.second)) {
            val neighborCoord = neighborData.first
            val neighborTile = board[neighborCoord]
            if (neighborTile?.placedWildlifeToken?.animal == Wildlife.HAWK) {
                isSolitary = false
                break
            }
        }
        return isSolitary
    }

    private fun getHawkPointsVariantA(count: Int): Int {
        return when (count) {
            0 -> 0
            1 -> 2
            2 -> 5
            3 -> 8
            4 -> 11
            5 -> 14
            6 -> 18
            7 -> 22
            else -> 26
        }
    }

    private fun getHawkPointsVariantB(count: Int): Int {
        return when (count) {
            0 -> 0
            1 -> 0
            2 -> 5
            3 -> 9
            4 -> 12
            5 -> 16
            6 -> 20
            7 -> 24
            else -> 28
        }
    }

    // Salmon scoring:
    // Variant A: scores a chain of salmons. 1=2, 2=5, 3=8, 4=12, 5=16, 6=20, 7+=25
    // Variant B: similar, but chains have different point thresholds. 1=2, 2=4, 3=9, 4=11, 5+=17
    internal fun GameActionService.scoreSalmons(board: Map<Pair<Int, Int>, HabitatTile>, isVariantA: Boolean): Int {
        //Removing nodes with >= 3 neighbors automatically splits branched chains into smaller chains.
        val validSalmons = mutableSetOf<Pair<Int, Int>>()

        for ((coord, tile) in board) {
            if (tile.placedWildlifeToken?.animal == Wildlife.SALMON) {
                //count adjacent salmons
                val adjacentSalmons = countAdjacentSalmons(coord, board)

                //keep only salmons that have 2 or fewer adjacent salmons
                if (adjacentSalmons <= 2) {
                    validSalmons.add(coord)
                }
            }
        }

        //Find Chains (BFS)
        val visited = mutableSetOf<Pair<Int, Int>>()
        var totalScore = 0

        for (coord in validSalmons) {
            if (coord !in visited) {
                // Explore this valid chain
                val chainSize = exploreSalmonChain(coord, validSalmons, visited)

                // Evaluate and Score the chain
                totalScore += getSalmonChainPoints(chainSize, isVariantA)
            }
        }

        return totalScore
    }

    private fun countAdjacentSalmons(coord: Pair<Int, Int>, board: Map<Pair<Int, Int>, HabitatTile>): Int {
        var adjacentSalmons = 0
        for (neighborData in getNeighbors(coord.first, coord.second)) {
            val neighborCoord = neighborData.first
            val neighborTile = board[neighborCoord]
            if (neighborTile?.placedWildlifeToken?.animal == Wildlife.SALMON) {
                adjacentSalmons++
            }
        }
        return adjacentSalmons
    }

    private fun getSalmonChainPoints(chainSize: Int, isVariantA: Boolean): Int {
        // Array-Index entspricht der Kettengröße (Index 0 wird nicht genutzt, daher 0 Punkte)
        val pointsVariantA = intArrayOf(0, 2, 5, 8, 12, 16, 20, 25)
        val pointsVariantB = intArrayOf(0, 2, 4, 9, 11, 17)

        return if (isVariantA) {
            // Variant A Point Scaling
            val cappedSize = minOf(chainSize, 7)
            pointsVariantA[cappedSize]
        } else {
            // Variant B Point Scaling
            val cappedSize = minOf(chainSize, 5)
            pointsVariantB[cappedSize]
        }
    }

    private fun exploreSalmonChain(
        startCoord: Pair<Int, Int>,
        validSalmons: Set<Pair<Int, Int>>,
        visited: MutableSet<Pair<Int, Int>>
    ): Int {
        var size = 0
        val queue = ArrayDeque<Pair<Int, Int>>()

        queue.add(startCoord)
        visited.add(startCoord)

        while (queue.isNotEmpty()) {
            val currentCoord = queue.removeFirst()
            size++

            for (neighborData in getNeighbors(currentCoord.first, currentCoord.second)) {
                val neighborCoord = neighborData.first
                // Only traverse to neighbors that are ALSO in the validSalmons set
                if (neighborCoord in validSalmons && neighborCoord !in visited) {
                    visited.add(neighborCoord)
                    queue.add(neighborCoord)
                }
            }
        }

        return size
    }

    // Elk scring
    // Variant A: straight lines of elks (1=2, 2=5, 3=9, 4=13)
    // Variant B: specific shapes (diamond, line, etc) rotated in any direction.
    internal fun GameActionService.scoreElks(board: Map<Pair<Int, Int>, HabitatTile>, isVariantA: Boolean): Int {
        val unscoredElks = mutableSetOf<Pair<Int, Int>>()
        for ((coord, tile) in board) {
            if (tile.placedWildlifeToken?.animal == Wildlife.ELK) {
                unscoredElks.add(coord)
            }
        }

        var totalScore = 0

        // Greedy selection guarantees maximum points by prioritizing the largest valid shapes first
        for (targetSize in 4 downTo 2) {
            while (true) {
                val foundShape = if (isVariantA) {
                    findElkShapeVariantA(targetSize, unscoredElks)
                } else {
                    findElkShapeVariantB(targetSize, unscoredElks)
                }

                if (foundShape == null) {
                    // No shapes of targetSize found, break the while loop to move down to targetSize - 1
                    break
                }

                // Shape found! Remove used Elks so they can't be scored again, and ad points
                for (coord in foundShape) {
                    unscoredElks.remove(coord)
                }

                totalScore += when (targetSize) {
                    4 -> 13
                    3 -> 9
                    2 -> 5
                    else -> 0
                }
            }
        }

        // Any remaining Elks in unscoredElks are isolated size 1s these worth 2 points each
        totalScore += unscoredElks.size * 2

        return totalScore
    }


    // checks for straight elk lines
    private fun findElkShapeVariantA(
        targetSize: Int,
        unscoredElks: Set<Pair<Int, Int>>
    ): List<Pair<Int, Int>>? {
        // Variant A: Straight lines. Check 3 positive axial directions: (1,0), (0,1), (-1,1)
        val directions = listOf(Pair(1, 0), Pair(0, 1), Pair(-1, 1))

        for (origin in unscoredElks) {
            for (direction in directions) {
                val candidateShape = buildElkLine(origin, direction, targetSize, unscoredElks)
                if (candidateShape.size == targetSize) {
                    return candidateShape
                }
            }
        }
        return null
    }

    private fun buildElkLine(
        origin: Pair<Int, Int>,
        direction: Pair<Int, Int>,
        targetSize: Int,
        unscoredElks: Set<Pair<Int, Int>>
    ): List<Pair<Int, Int>> {
        val candidateShape = mutableListOf<Pair<Int, Int>>()
        for (step in 0 until targetSize) {
            val currentCoord = Pair(
                origin.first + step * direction.first,
                origin.second + step * direction.second
            )
            if (currentCoord in unscoredElks) {
                candidateShape.add(currentCoord)
            } else {
                break
            }
        }
        return candidateShape
    }

    // checks for elk cluster shapes
    private fun findElkShapeVariantB(
        targetSize: Int,
        unscoredElks: Set<Pair<Int, Int>>
    ): List<Pair<Int, Int>>? {
        // Variant B: Exact Shapes (Diamond, Triangle, Pair)
        val baseOffsets = when (targetSize) {
            4 -> listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1), Pair(1, 1)) // Diamond
            3 -> listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1)) // Triangle
            2 -> listOf(Pair(0, 0), Pair(1, 0)) // Pair
            else -> emptyList()
        }

        val allRotatedOffsets = rotateOffsets(baseOffsets)

        for (origin in unscoredElks) {
            for (offsets in allRotatedOffsets) {
                val candidateShape = offsets.map { (columnOffset, rowOffset) ->
                    Pair(origin.first + columnOffset, origin.second + rowOffset)
                }

                // Check if ALL coordinates in the candidate shape exist in unscoredElks
                val isValid = candidateShape.all { it in unscoredElks }

                if (isValid) {
                    return candidateShape
                }
            }
        }
        return null
    }

    // spins the coordinate offsets 60 degrees around the hex grid
    private fun rotateOffsets(offsets: List<Pair<Int, Int>>): List<List<Pair<Int, Int>>> {
        val allRotations = mutableListOf<List<Pair<Int, Int>>>()
        var current = offsets
        repeat(6)
        {
            allRotations.add(current)
            // On an axial grid, rotating by 60 degrees clockwise maps (column, row) to (-row, column + row)
            current = current.map { (column, row) -> Pair(-row, column + row) }
        }
        return allRotations
    }
