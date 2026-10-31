package service.bot

import entity.BotSpeed
import service.AbstractRefreshingService
import service.RootService
import entity.PlayerType
import kotlin.random.Random

/**
 * A class for all bot related actions
 */
class BotActionService(private val rootService: RootService) : AbstractRefreshingService() {

    val pActService = rootService.playerActionService

    /**
     * Calls the bot.
     *
     * The bot executes all functions in the correct order.
     */
    fun botTurn() {

        val game = checkNotNull(rootService.currentGame?.currentState)
        val player = game.players[game.currentPlayer]
        val botType = player.playMode

        if (botType == PlayerType.RANDOMBOT) {
            randomBotTurn()
        }

        else if (botType == PlayerType.STRONGBOT) {
            strongBotTurn()
        }
    }

    private fun randomBotTurn() {

        val game = checkNotNull(rootService.currentGame?.currentState)
        val decisionService = BotDecisionService(rootService)
        val botSpeed = game.simulationSpeed
        val botPlayer = game.players[game.currentPlayer]

        sleep(botSpeed)

        //Redraws 3 %70 of the time (if it's not possible nothing happens cause of check in the func)
        if (returnTrueChance(70)&& game.threeWildlifeIdentical) {
            pActService.redrawOn3Wildlife()
        }

        //choose if we should use Nature Token to redraw
        if (returnTrueChance(10) && botPlayer.natureTokens > 0) {
            pActService.useNatureToken()
            //muss gucken welche values bei random included sind
            val indices = retNRandomInInterval(
                n = Random.nextInt(0, 4), // 1 bis 4
                interval = 3
            )
            pActService.redrawWildlifeTokens(indices)
        }

        sleep(botSpeed)

        //choose if we should use Nature Token to pick any combo
        if (returnTrueChance(10) && botPlayer.natureTokens > 0) {
            pActService.useNatureToken()
            val indices = retNRandomInInterval(
                n = 2,
                interval = 3
            )
            pActService.chooseCombination(Pair(indices[0], indices[1]))
        } else {
            //choose Tile and Token  Combo when no Nature Token used
            val indices = Random.nextInt(0, 4)
            pActService.chooseCombination(Pair(indices, indices))
        }

        //choose random rotation
        var randomNumber = Random.nextInt(6)
        while (randomNumber < 5) {
            pActService.rotateHabitat(false)
            randomNumber += 1
        }

        //Place Tile
        val placeableCoordsHabitat = decisionService.getAllPlaceableHabitatCoords()
        val chosenCordH = placeableCoordsHabitat.shuffled().first()
        pActService.placeHabitat(chosenCordH)

        //place Token
        val placeableCoordsToken =
            decisionService.getAllPlaceableTokenCoords(checkNotNull(game.drawnCombination).second.animal)
        if (!placeableCoordsToken.isEmpty()) {
            val chosenCordT = placeableCoordsToken.shuffled().first()
            pActService.placeWildlifeTokens(chosenCordT)
        }
        else {
            pActService.discardWildlifeToken()
        }

        sleep(botSpeed)
    }

    private fun strongBotTurn(){

        val desiredScoreC = 60.0

        val game = checkNotNull(rootService.currentGame?.currentState)
        val botSpeed = game.simulationSpeed
        val botPlayer = game.players[game.currentPlayer]
        val decisionService = BotDecisionService(rootService)

        val placeableHabitatCoords = decisionService.getAllPlaceableHabitatCoords()

        //List of rated combinations
        var ratedCombinations = decisionService.rateCombinations(placeableHabitatCoords)


        sleep(botSpeed)

        // TODO weight anpassen
        // Redraw WildlifeTokens if certain score thresholds are not met
        if (game.threeWildlifeIdentical && checkNotNull(ratedCombinations[0].combinationScore) < desiredScoreC) {
            pActService.redrawOn3Wildlife()
            ratedCombinations = decisionService.rateCombinations(placeableHabitatCoords)
        }

        // If all combinations are bad, reroll shop
        else if (ratedCombinations[0].combinationScore == null && botPlayer.natureTokens >= 1) {
            pActService.useNatureToken()
            pActService.redrawWildlifeTokens(listOf(0,1,2,3))
            ratedCombinations = decisionService.rateCombinations(placeableHabitatCoords)
        }

        sleep(botSpeed)

        // Iterate over all Wildlife, Habitat Pairs and choose best playable combination
        val chosenCombination = decideCombination(ratedCombinations)

        println("Player: ${game.currentPlayer}")
        println("Turn: ${game.roundNumber}")
        for (combination in ratedCombinations) {
            println("Animal: ${game.wildlifeShop[combination.shopWildlifeIndex]}")
            println("Animal Coords: ${combination.coordsOfWildlife}")
            println("Habitat Coords: ${combination.coordsOfHabitat}")
            println("Score: ${combination.combinationScore}")
        }


        if (chosenCombination.shopHabitatIndex != chosenCombination.shopWildlifeIndex) {
            pActService.useNatureToken()
        }

        println("--------------------------------------------")
        println("Actually chosen: ${game.wildlifeShop[chosenCombination.shopWildlifeIndex]}")
        println("--------------------------------------------")

        playOutTurn(chosenCombination)

        sleep(botSpeed)
    }

    //plays out the turn after combination was chosen
    private fun playOutTurn(combination: RatedCombination)
    {
        pActService.chooseCombination(
            Pair(combination.shopHabitatIndex, combination.shopWildlifeIndex)
        )

        val rotationCount = combination.habitatRotationCount
        repeat(rotationCount){
            pActService.rotateHabitat(false)
        }

        pActService.placeHabitat(combination.coordsOfHabitat)

        if (combination.coordsOfWildlife == null) {
            pActService.discardWildlifeToken()
            println("Bot discarded!!")
        }
        else {
            pActService.placeWildlifeTokens(checkNotNull(combination.coordsOfWildlife))
        }
    }

    private fun decideCombination(ratedCombinations: List<RatedCombination>) : RatedCombination {

        val game = checkNotNull(rootService.currentGame?.currentState)
        val player = game.players[game.currentPlayer]

        val defaultCombinations = mutableListOf<RatedCombination>()

        for (combination in ratedCombinations) {

            if (combination.shopHabitatIndex == combination.shopWildlifeIndex) {

                defaultCombinations.add(combination)
            }
        }

        if (player.natureTokens >= 1 && defaultCombinations[0].coordsOfWildlife == null) return ratedCombinations[0]

        for (combination in defaultCombinations) {

            if (combination.coordsOfWildlife != null) {
                return combination
            }
        }

        return defaultCombinations[0]
    }

    //returns True %odds of the time
    private fun returnTrueChance(odds: Int): Boolean {
        val roll = Random.nextInt(1, 100)
        return (roll <= odds)
    }

    //returns n Random Numbers between 0 and interval no duplicates
    private fun retNRandomInInterval(n: Int, interval: Int): List<Int> {
        val indices = (0..interval).shuffled().take(n)
        return indices
    }

    //Sets the thread to sleep mode for specified time
    private fun sleep(botSpeed: BotSpeed) {
        Thread.sleep(botSpeed.toMillieSeconds())
    }


}