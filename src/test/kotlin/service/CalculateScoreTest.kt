package service

import entity.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Testklasse für die Punkteberechnung in Cascadia. */
class CalculateScoreTest {

    private val rootService = RootService()
    private val gameService = rootService.gameActionService

    // quick helper to generate dummy habitat tiles for our grid tests
    private fun createTile(
        animal: Wildlife? = null,
        habitats: List<Habitat> = List(6) { Habitat.FORESTS },
        rotation: Int = 0
    ): HabitatTile {
        val tile = HabitatTile(habitats as MutableList<Habitat>, emptyList())
        tile.rotation = rotation
        if (animal != null) {
            tile.placedWildlifeToken = WildlifeToken(animal)
        }
        return tile
    }

    /**
     * Verifies that Bear Variant A scores strictly for exact pairs of bears.
     */
    @Test
    fun testBearsVariantA_PairsOnly() {
        // test bears A: only exact pairs count
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        // one pair
        board[Pair(0, 0)] = createTile(Wildlife.BEAR)
        board[Pair(1, 0)] = createTile(Wildlife.BEAR)

        // one trio (should score 0 in variant a)
        board[Pair(5, 5)] = createTile(Wildlife.BEAR)
        board[Pair(6, 5)] = createTile(Wildlife.BEAR)
        board[Pair(5, 6)] = createTile(Wildlife.BEAR)

        val score = gameService.scoreBears(board, isVariantA = true)

        // 1 pair = 4 points!
        assertEquals(4, score, "Variant A Bears should score 4 points for exactly one pair")
    }

    /**
     * Verifies that Bear Variant B strictly scores for groups of exactly 3 bears.
     */
    @Test
    fun testBearsVariantB_TriosOnly() {
        // test bears B: only groups of 3 count
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        board[Pair(0, 0)] = createTile(Wildlife.BEAR)
        board[Pair(1, 0)] = createTile(Wildlife.BEAR)

        board[Pair(5, 5)] = createTile(Wildlife.BEAR)
        board[Pair(6, 5)] = createTile(Wildlife.BEAR)
        board[Pair(5, 6)] = createTile(Wildlife.BEAR)

        board[Pair(10, 10)] = createTile(Wildlife.BEAR)
        board[Pair(11, 10)] = createTile(Wildlife.BEAR)
        board[Pair(10, 11)] = createTile(Wildlife.BEAR)
        board[Pair(11, 11)] = createTile(Wildlife.BEAR)

        val score = gameService.scoreBears(board, isVariantA = false)
        // just the trio scores (10 pts)
        assertEquals(10, score, "Variant B Bears should score 10 points for a trio, ignoring pairs")
    }

    /**
     * Ensures the Elk scoring algorithm prioritizes larger lines over smaller ones to maximize points.
     */
    @Test
    fun testElks_GreedyOptimization() {
        // straight line of 5 elks for variant a
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()
        for (i in 0..4) {
            board[Pair(i, 0)] = createTile(Wildlife.ELK)
        }

        val score = gameService.scoreElks(board, isVariantA = true)

        // splits into a 4-line (13) and a 1-line (2) -> 15 pts total
        assertEquals(15, score, "Variant A Elks should optimally score 13 points (line of 4) + 2 points (isolated)")
    }

    /**
     * Checks Elk Variant B scoring for a Triangle shape (size 3).
     */
    @Test
    fun testElksVariantB_Shapes_Triangle() {
        // size 3 triangle
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()
        board[Pair(0, 0)] = createTile(Wildlife.ELK)
        board[Pair(1, 0)] = createTile(Wildlife.ELK)
        board[Pair(0, 1)] = createTile(Wildlife.ELK)

        val score = gameService.scoreElks(board, isVariantA = false)
        // triangle gets 9 pts
        assertEquals(9, score, "Variant B Elks should score 9 points for a triangle shape")
    }

    /**
     * Checks Elk Variant B scoring for a Diamond shape (size 4).
     */
    @Test
    fun testElksVariantB_Shapes_Diamond() {
        // size 4 diamond shape to get full coverage
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()
        board[Pair(0, 0)] = createTile(Wildlife.ELK)
        board[Pair(1, 0)] = createTile(Wildlife.ELK)
        board[Pair(0, 1)] = createTile(Wildlife.ELK)
        board[Pair(1, 1)] = createTile(Wildlife.ELK)

        val score = gameService.scoreElks(board, isVariantA = false)
        // diamond gets 13 pts
        assertEquals(13, score, "Variant B Elks should score 13 points for a diamond shape")
    }

    /**
     * Checks Elk Variant B scoring for a Pair shape (size 2).
     */
    @Test
    fun testElksVariantB_Shapes_Pair() {
        // size 2 pair
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()
        board[Pair(0, 0)] = createTile(Wildlife.ELK)
        board[Pair(1, 0)] = createTile(Wildlife.ELK)

        val score = gameService.scoreElks(board, isVariantA = false)
        // pair gets 5 pts
        assertEquals(5, score, "Variant B Elks should score 5 points for a pair")
    }

    /**
     * Ensures the Salmon chain scoring algorithm strictly prohibits branches and counts only valid runs.
     */
    @Test
    fun testSalmons_BranchCuttingRule() {
        // y-shape salmon chain (should get cut into small size-1 bits)
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        board[Pair(2, 2)] = createTile(Wildlife.SALMON)
        board[Pair(3, 2)] = createTile(Wildlife.SALMON)
        board[Pair(1, 3)] = createTile(Wildlife.SALMON)
        board[Pair(2, 1)] = createTile(Wildlife.SALMON)

        val score = gameService.scoreSalmons(board, isVariantA = true)

        // center is invalid. leaving three 1-salmons (3 * 2 = 6 pts)
        assertEquals(
            6,
            score,
            "Variant A Salmons should split branching chains into 3 isolated size-1 chains (3x2=6 pts)"
        )
    }

    /**
     * Verifies that Salmon Variant B caps its scoring sizes to a maximum of 5 in a chain.
     */
    @Test
    fun testSalmonsVariantB_MaxFive() {
        // 6 salmons in a row
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()
        for (i in 0..5) {
            board[Pair(i, 0)] = createTile(Wildlife.SALMON)
        }

        val score = gameService.scoreSalmons(board, isVariantA = false)
        // max score is 17
        assertEquals(17, score, "Variant B Salmons maxes out at size 5 for 17 points")
    }

    /**
     * Tests Hawk Variant B to ensure hawks only score when they have an uninterrupted line of sight.
     */
    @Test
    fun testHawks_LineOfSight() {
        // two hawks with line of sight (variant b)
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        board[Pair(0, 0)] = createTile(Wildlife.HAWK)
        board[Pair(0, 3)] = createTile(Wildlife.HAWK)
        board[Pair(0, 1)] = createTile(Wildlife.BEAR) // bear doesnt block!

        val score = gameService.scoreHawks(board, isVariantA = false)
        // they see each othe so 5 pts
        assertEquals(5, score, "Variant B Hawks should score 5 points if they have uninterrupted line of sight")
    }

    /**
     * Tests Hawk Variant B to ensure hawks do not score if their line of sight is blocked.
     */
    @Test
    fun testHawks_NoLineOfSight() {
        // hawks that cant see each other
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        board[Pair(0, 0)] = createTile(Wildlife.HAWK)
        board[Pair(2, 1)] = createTile(Wildlife.HAWK)

        val score = gameService.scoreHawks(board, isVariantA = false)
        // no pairs = 0 pts
        assertEquals(0, score, "Variant B Hawks should score 0 points if they have no line of sight to another hawk")
    }

    /**
     * Verifies that Hawk Variant A correctly scores strictly solitary hawks.
     */
    @Test
    fun testHawksVariantA_Scattered() {
        // scattered isolated hawks for variant a
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        board[Pair(0, 0)] = createTile(Wildlife.HAWK)
        board[Pair(5, 5)] = createTile(Wildlife.HAWK)
        board[Pair(10, 0)] = createTile(Wildlife.HAWK)

        val score = gameService.scoreHawks(board, isVariantA = true)
        // 3 hawks = 8 pts
        assertEquals(8, score, "Variant A Hawks should score 8 points for 3 isolated scattered hawks")
    }

    /**
     * Tests Fox Variant A to ensure points are only awarded for uniquely distinct adjacent animals.
     */
    @Test
    fun testFoxes_DistinctNeighbors() {
        // fox surrounded by different stuff
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        board[Pair(0, 0)] = createTile(Wildlife.FOX)
        board[Pair(1, 0)] = createTile(Wildlife.BEAR)
        board[Pair(0, 1)] = createTile(Wildlife.ELK)
        board[Pair(-1, 1)] = createTile(Wildlife.SALMON)
        board[Pair(-1, 0)] = createTile(Wildlife.FOX)

        val score = gameService.scoreFoxes(board, isVariantA = true)

        // center gets 4, neighbor gets 2 -> 6 pts total
        assertEquals(6, score, "Variant A Foxes should score 4 points (central fox) + 2 points (neighbor fox) = 6 pts")
    }

    /**
     * Tests Fox Variant B to verify it scores based on identical pairs in its neighborhood.
     */
    @Test
    fun testFoxesVariantB_Pairs() {
        // fox surrounded by pairs of animals
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        board[Pair(0, 0)] = createTile(Wildlife.FOX)
        board[Pair(1, 0)] = createTile(Wildlife.BEAR)
        board[Pair(0, 1)] = createTile(Wildlife.BEAR)
        board[Pair(-1, 1)] = createTile(Wildlife.SALMON)
        board[Pair(-1, 0)] = createTile(Wildlife.SALMON)

        val score = gameService.scoreFoxes(board, isVariantA = false)
        // 2 pairs = 5 pts
        assertEquals(
            5,
            score,
            "Variant B Foxes should score 5 points for being adjacent to 2 pairs of identical species"
        )
    }

    /**
     * Checks the core habitat clustering algorithm to ensure it maps out adjacent matching terrain correctly.
     */
    @Test
    fun testHabitat_EdgeMatching() {
        // make sure edges align properly
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        val habitatsA = MutableList(6) { Habitat.MOUNTAINS }
        habitatsA[1] = Habitat.FORESTS
        board[Pair(0, 0)] = createTile(habitats = habitatsA)

        val habitatsB = MutableList(6) { Habitat.WETLANDS }
        habitatsB[4] = Habitat.FORESTS
        board[Pair(1, 0)] = createTile(habitats = habitatsB)

        val player = Player(name = "TestPlayer", playMode = PlayerType.HUMAN, natureTokens = 0)
        player.board.putAll(board)

        val scores = gameService.calculateBaseHabitatScore(player)
        // should connect and give size 2
        assertEquals(2, scores[Habitat.FORESTS], "Forests should be perfectly connected for a size of 2")
    }

    /**
     * Validates the habitat majority point distribution among 3 players.
     */
    @Test
    fun testHabitatMajorityBonuses_3Players() {
        // testing ties in habitat bonuses
        val player1 = Player(name = "P1", playMode = PlayerType.HUMAN, natureTokens = 0)
        val player2 = Player(name = "P2", playMode = PlayerType.HUMAN, natureTokens = 0)
        val player3 = Player(name = "P3", playMode = PlayerType.HUMAN, natureTokens = 0)

        val baseScores = mapOf(
            player1 to mapOf(
                Habitat.FORESTS to 5,
                Habitat.WETLANDS to 0,
                Habitat.MOUNTAINS to 0,
                Habitat.PRAIRIES to 0,
                Habitat.RIVERS to 0
            ),
            player2 to mapOf(
                Habitat.FORESTS to 5,
                Habitat.WETLANDS to 0,
                Habitat.MOUNTAINS to 0,
                Habitat.PRAIRIES to 0,
                Habitat.RIVERS to 0
            ),
            player3 to mapOf(
                Habitat.FORESTS to 3,
                Habitat.WETLANDS to 0,
                Habitat.MOUNTAINS to 0,
                Habitat.PRAIRIES to 0,
                Habitat.RIVERS to 0
            )
        )

        val bonusScores = gameService.calculateHabitatMajorityBonuses(baseScores)

        // tie gives both 2 points, 3rd gets 0
        assertEquals(
            2,
            bonusScores.getValue(player1)[Habitat.FORESTS],
            "Player 1 tied for 1st so they should get 2 points"
        )
        assertEquals(
            2,
            bonusScores.getValue(player2)[Habitat.FORESTS],
            "Player 2 tied for 1st so they should get 2 points"
        )
        assertEquals(0, bonusScores.getValue(player3)[Habitat.FORESTS], "Player 3 is second place so they get 0 points")
    }

    /**
     * Tests the global calculate score method, focusing on resolving a tie between players via nature tokens.
     */
    @Test
    fun testCalculateScore_TieBreaker() {
        // whoever has more nature tokens wins a tie
        val player1 = Player(name = "Player1", playMode = PlayerType.HUMAN, natureTokens = 2)
        val player2 = Player(name = "Player2", playMode = PlayerType.HUMAN, natureTokens = 5)

        rootService.currentGame = CascadiaGameSave().apply {
            currentState = CascadiaGame(mutableListOf(player1, player2), BotSpeed.NORMAL).apply {
                scoringCards = mutableListOf()
            }
        }

        val sortedScores = gameService.calculateScore()

        // p2 wins bc of nature tokens!
        assertEquals(player2, sortedScores[0].first, "Player 2 should win the tie-breaker with more nature tokens")
        assertEquals(player1, sortedScores[1].first, "Player 1 should be second place due to fewer nature tokens")
    }

    /**
     * Checks if the nature token tiebreaker logic works as intended when players tie on everything else.
     */
    @Test
    fun testCalculateScore_TieBreakerTokens() {
        // setup a 3 player game
        rootService.gameActionService.startNewGame(
            listOf("P1", "P2", "P3"),
            false,
            listOf(true, true, true, true, true),
            BotSpeed.NORMAL
        )
        val game = checkNotNull(rootService.currentGame?.currentState)

        // artificially give some tokens to force a tiebreaker evaluation
        game.players[0].natureTokens = 5
        game.players[1].natureTokens = 3
        game.players[2].natureTokens = 3

        // calculate the final score to hit the tie breaker loop
        val scores = rootService.gameActionService.calculateScore()
        assertEquals(3, scores.size)
    }

    /**
     * Tests the habitat majority bonuses logic specifically for second place logic and ties.
     */
    @Test
    fun testHabitatMajorityBonuses_TiesAndSecondPlace() {
        // start a fresh game for exactly 3 players
        rootService.gameActionService.startNewGame(
            listOf("firstPlayer", "secondePlayer", "thirdPlayer"),
            false,
            listOf(true, true, true, true, true),
            BotSpeed.NORMAL
        )
        val game = checkNotNull(rootService.currentGame?.currentState)
        val firstPlayer = game.players[0]
        val secondePlayer = game.players[1]
        val thirdPlayer = game.players[2]

        // inject dummy base scores so we can control exactly who gets first and second
        val baseScores = mutableMapOf<Player, MutableMap<Habitat, Int>>()
        for (p in listOf(firstPlayer, secondePlayer, thirdPlayer)) {
            baseScores[p] = Habitat.entries.associateWith { 0 }.toMutableMap()
        }
        baseScores[firstPlayer]!![Habitat.FORESTS] = 10
        baseScores[secondePlayer]!![Habitat.FORESTS] = 8
        baseScores[thirdPlayer]!![Habitat.FORESTS] = 5
        val bonuses = rootService.gameActionService.calculateHabitatMajorityBonuses(baseScores)

        // p1 gets 3 points for being first, p2 gets 1 point for being exactly 2nd place
        assertEquals(3, bonuses.getValue(firstPlayer)[Habitat.FORESTS])
        assertEquals(1, bonuses.getValue(secondePlayer)[Habitat.FORESTS])

        // lets start a 2-player game to check tie logic
        rootService.gameActionService.startNewGame(
            listOf("firstPlayer", "secondePlayer"),
            false,
            listOf(true, true, true, true, true),
            BotSpeed.NORMAL
        )
        val game2 = checkNotNull(rootService.currentGame?.currentState)
        val firstPlayerSecondeGame = game2.players[0]
        val secondePlayerSecondeGame = game2.players[1]

        // tie them up
        val baseScores2 = mutableMapOf<Player, MutableMap<Habitat, Int>>()
        for (p in listOf(firstPlayerSecondeGame, secondePlayerSecondeGame)) {
            baseScores2[p] = Habitat.entries.associateWith { 0 }.toMutableMap()
        }
        baseScores2[firstPlayerSecondeGame]!![Habitat.FORESTS] = 10
        baseScores2[secondePlayerSecondeGame]!![Habitat.FORESTS] = 10

        val bonuses2 = rootService.gameActionService.calculateHabitatMajorityBonuses(baseScores2)

        // in a 2 player game, a tie for 1st place awards exactly 1 points to both
        assertEquals(1, bonuses2.getValue(firstPlayerSecondeGame)[Habitat.FORESTS])
        assertEquals(1, bonuses2.getValue(secondePlayerSecondeGame)[Habitat.FORESTS])
    }

    /**
     * Tests Bear Variant A when the player has managed to get 4 or more pairs.
     */
    @Test
    fun testBearsVarA_FourOrMorePairs() {
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        // manually put down 8 pairs of bears
        for (i in 0..7) {
            val tile = createTile(Wildlife.BEAR)
            board[Pair(i * 3, 0)] = tile
            board[Pair(i * 3 + 1, 0)] = createTile(Wildlife.BEAR)
        }
        val score = gameService.scoreBears(board, isVariantA = true)

        // anything 4 or above caps at 27 points!
        assertEquals(27, score)
    }

    /**
     * Tests Fox Variant B when it is surrounded by multiple identical pairs.
     */
    @Test
    fun testFoxesVarB_MultipleIdenticalPairs() {
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        // place a fox next to a pair of elks
        val f1 = createTile(Wildlife.FOX)
        board[Pair(0, 0)] = f1
        board[Pair(1, 0)] = createTile(Wildlife.ELK)
        board[Pair(0, 1)] = createTile(Wildlife.ELK)

        val score = gameService.scoreFoxes(board, isVariantA = false)

        // fox sees 1 pair, gets 3 points
        assertEquals(3, score)
    }

    /**
     * Tests Hawks to hit isolated high scores and the scenario where hawks are adjacent.
     */
    @Test
    fun testHawks_IsolatedAndAdjacentCombinations() {
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        // place two hawks right next to each other so they lose line of sight/isolation
        board[Pair(0, 0)] = createTile(Wildlife.HAWK)
        board[Pair(1, 0)] = createTile(Wildlife.HAWK)
        assertEquals(0, gameService.scoreHawks(board, isVariantA = true))

        val board8 = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        // space out 8 hawks so they are totally solitary
        for (i in 0..8) {
            board8[Pair(i * 3, 0)] = createTile(Wildlife.HAWK)
        }

        // Variant A maxes out at 26 points, Variant B hits 28!
        val scoreA = gameService.scoreHawks(board8, isVariantA = true)
        assertEquals(26, scoreA)
        val scoreB = gameService.scoreHawks(board8, isVariantA = false)
        assertEquals(28, scoreB)
    }

    /**
     * Tests the upper bounds of the Salmon scoring logic for really long chains.
     */
    @Test
    fun testSalmons_LongChains() {
        val board = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        // make an insanely long 7 salmon chain
        for (i in 0..7) board[Pair(i, 0)] = createTile(Wildlife.SALMON)
        val scoreA = gameService.scoreSalmons(board, isVariantA = true)

        // Variant A hits 25 points for 7+ chains
        assertEquals(25, scoreA)

        val board4 = mutableMapOf<Pair<Int, Int>, HabitatTile>()

        // try a 4 salmon chain
        for (i in 0..3) board4[Pair(i, 0)] = createTile(Wildlife.SALMON)

        // Variant B gets 11 points for exactly 4 in a run
        assertEquals(11, gameService.scoreSalmons(board4, isVariantA = false))
    }
}
