package service

import entity.GameState
import entity.PlayerType
import entity.Wildlife
import entity.WildlifeToken
import tools.aqua.bgw.util.Stack
import service.network.sendPlaceToOpponent
import service.network.sendRotationToOpponent
import service.network.sendSelectToOpponent
import service.network.sendWipeWildlifeToOpponent

/**
 * Class for handling all player specific logic.
 */
class PlayerActionService(private val rootService: RootService) : AbstractRefreshingService() {

    /** Zwischenspeichern der Cords für die network Messages */
    private var lastPlacedHabitatCoordinates: Pair<Int, Int>? = null

    /**
     * Allows the player to select a combination of a habitat tile and a wildlife token from the shop
     *
     * @param indices: Position of the habitat tile and wildlife token from the shop, saved in a pair of two ints
     * between 0..3
     *
     * @throws IllegalArgumentException if indices are not between 0..3
     * @throws IllegalStateException if indices are not the same when nature token is not active
     */
    fun chooseCombination(indices: Pair<Int, Int>, sendNetworkMessage: Boolean = true) {
        val game = rootService.currentGame?.currentState
        checkNotNull(game) { "No game is currently active" }

        check(
            game.gameState == GameState.CHOOSEPAIR ||
                    game.gameState == GameState.USENATURETOKEN ||
                    game.gameState == GameState.PLACEHABITAT
        ) { "Cannot choose a combination in game state ${game.gameState}" }

        val habitat = indices.first
        val wildlife = indices.second

        /** Damit Network weiß ob der NatureToken genutzt wurde */
        val usedNatureToken = game.natureTokenUsed

        if (habitat !in game.habitatShop.indices || wildlife !in game.wildlifeShop.indices) {
            return
        }

        if (!game.natureTokenUsed && habitat != wildlife) {
            throw IllegalStateException("indices must be the same")
        }

        val newHabitat = game.habitatShop[habitat]
        val newWildlife = game.wildlifeShop[wildlife]

        if (newHabitat != null && newWildlife != null) {
            game.drawnCombination = Pair(newHabitat, newWildlife)
            game.gameState = GameState.PLACEHABITAT

            if (game.natureTokenUsed) {
                game.natureTokenUsed = false
            }

            onAllRefreshables { refreshAfterChooseCombination() }

            /**
             * Network teil
             * sorgt dafür, dass die Nachrichten versendet werden also ruft networkService auf und
             * verschickt die Nachricht
             */
            if (!game.isHotseatMode && sendNetworkMessage) {
                rootService.networkService.sendSelectToOpponent(
                    habitatShopIndex = habitat,
                    wildlifeShopIndex = wildlife,
                    usedNatureToken = usedNatureToken
                )
            }
        }
    }

    /**
     * Allows the player to place the habitat tile from the drawn combination
     *
     * @param cords: Coordinates of the chosen position on the board
     *
     * @throws IllegalArgumentException if coordinates contain a habitat tile already
     * @throws IllegalArgumentException if the habitat tile is not adjacent to the existing tiles
     */
    fun placeHabitat(cords: Pair<Int, Int>) {
        val game = rootService.currentGame?.currentState
        checkNotNull(game) { "No game is currently active" }

        check(game.gameState == GameState.PLACEHABITAT) { "GameState should be PLACEHABITAT" }

        val player = game.players[game.currentPlayer]
        val habitatPlacement = player.board[cords]

        var isAdjacent = false
        val q = cords.first
        val r = cords.second

        val otherCords = listOf(
            Pair(q + 1, r - 1), Pair(q + 1, r), Pair(q, r + 1), Pair(q - 1, r + 1), Pair(q - 1, r), Pair(q, r - 1)
        )

        for (otherHabitats in otherCords) {
            if (player.board[otherHabitats] != null) {
                isAdjacent = true
            }
        }

        val chosenPair = game.drawnCombination ?: return
        if (!isAdjacent) {
            throw IllegalArgumentException("Tile is not adjacent to other tiles")
        }
        if (habitatPlacement != null) {
            throw IllegalArgumentException("Cords contain tile already")
        }
        val habitat = chosenPair.first
        player.board[cords] = habitat

        /** brauche das um später zu wissen ob wildlife discarded wird bevor man die message sendet */
        lastPlacedHabitatCoordinates = cords

        val shopIndex = game.habitatShop.indexOfFirst { it === habitat }
        if (shopIndex != -1) game.habitatShop[shopIndex] = null
        game.gameState = GameState.PLACEWILDLIFE
        onAllRefreshables { refreshAfterPlaceHabitatTile() }
    }

    /**
     * Allows the player to place the wildlife token from the drawn combination
     *
     * @param cords: Coordinates of the chosen habitat on the board
     *
     * @throws IllegalStateException if no habitat is placed at the coordinates or has a token already
     */
    fun placeWildlifeTokens(cords: Pair<Int, Int>, sendNetworkMessage: Boolean = true) {
        val game = rootService.currentGame?.currentState
        checkNotNull(game) { "No game is currently active" }

        check(game.gameState == GameState.PLACEWILDLIFE) { "GameState should be PLACEWILDLIFE" }

        val player = game.players[game.currentPlayer]
        val habitatPlacement = player.board[cords]

        val chosenPair = game.drawnCombination
        val wildlife = chosenPair?.second

        if (chosenPair != null && habitatPlacement != null && wildlife != null) {

            if (habitatPlacement.placedWildlifeToken == null &&
                habitatPlacement.possibleAnimals.contains(wildlife.animal)
            ) {

                habitatPlacement.placedWildlifeToken = wildlife
                val shopIndex = game.wildlifeShop.indexOfFirst { it === wildlife }
                if (shopIndex != -1) game.wildlifeShop[shopIndex] = null
                if (habitatPlacement.isKeyStone && game.natureTokenCount > 0) {
                    player.natureTokens += 1
                    game.natureTokenCount -= 1
                }

                onAllRefreshables { refreshAfterPlaceWildlifeToken() }

                /**
                 * Hier wird die placedmessageversendet und danach lastPlacedHabitatCoordinates resettet
                 */
                if (!game.isHotseatMode && sendNetworkMessage) {
                    val habitatCoordinates = lastPlacedHabitatCoordinates
                    checkNotNull(habitatCoordinates) { "Es wurde kein habitat ausgewählt" }

                val habitatRotation = chosenPair.first.rotation

                rootService.networkService.sendPlaceToOpponent(
                    habitatCoordinates = habitatCoordinates,
                    wildlifeCoordinates = cords,
                    habitatRotation = habitatRotation
                    )

                lastPlacedHabitatCoordinates = null
                }

                rootService.gameActionService.endTurn()


            } else {
                throw IllegalStateException(
                    "No habitat placed at the coordinates/ has a token placed already/" +
                            "wildlife isn#t depicted on the tile"
                )
            }
        }

    }

    /**
     * @params list of  a number of indices between 0..3
     * these indices are the Wildlife tokens in shop that will be redrawn
     *
     * Sets the given list of wildlife tokens to side draws new ones and puts the set aside tokens back into tha bag.
     * then shuffles
     */
    fun redrawWildlifeTokens(indices: List<Int>, sendNetworkMessage: Boolean = true) {
        val game = checkNotNull(rootService.currentGame?.currentState)
        val drawn: Stack<WildlifeToken> = Stack()

        repeat(indices.size) {
            drawn.push(
                rootService.gameActionService.removeBottomEntry(
                    game.wildlifeBag
                )
            )
        }

        for (i in indices) {
            game.wildlifeBag.push(checkNotNull(game.wildlifeShop[i]))
            game.wildlifeShop[i] = drawn.pop()
        }

        game.wildlifeBag.shuffle()
        game.natureTokenUsed = false

        onAllRefreshables {
            refreshAfterRedrawWildlifeTokens()
        }

        /** Ruft networkService auf wegen der Message */
        if (!game.isHotseatMode && sendNetworkMessage) {
            rootService.networkService.sendWipeWildlifeToOpponent(
                usedNatureToken = true,
                wipedWildlifeIndices = indices
            )
        }

        rootService.gameActionService.checkOverpopulation()
    }

    /**
     * Sets the three identical wildlife tokens to side and draws 3 new tokens from the wildlife bag and puts them to
     * shop. After that the set aside tokens are put into the bag.
     */
    fun redrawOn3Wildlife(sendNetworkMessage: Boolean = true) {
        val game = checkNotNull(rootService.currentGame?.currentState)
        val drawn: Stack<WildlifeToken> = Stack()
        val majority = IntArray(5)

        //Zähle jedes Tier auf
        for (wToken in game.wildlifeShop) {
            majority[checkNotNull(wToken).animal.ordinal]++
        }
        //finde im welchen index das tier mit der höchsten Anzahl liegt
        val maxIndex = majority.indices.maxBy { majority[it] }

        /** Network braucht das damit ich weiß was redrawed wurde */
        val wipedIndices = mutableListOf<Int>()

        if (majority[maxIndex] == 3 && !game.threeRedrawnInTurn) {

            //ziehe 3 Wildlife Tokens von unten mit buttomentry fix ~kaan~
            repeat(3) {
                drawn.push(
                    rootService.gameActionService.removeBottomEntry(
                        game.wildlifeBag
                    )
                )
            }
            //Die Animals die redrawn werden sollen werden redrawn
            for (wTokenI in game.wildlifeShop.indices) {
                if (Wildlife.entries[maxIndex] == game.wildlifeShop[wTokenI]?.animal) {
                    /** added das wildlifetoken in die list für die message später */
                    wipedIndices.add(wTokenI)
                    game.wildlifeBag.push(checkNotNull(game.wildlifeShop[wTokenI]))
                    game.wildlifeShop[wTokenI] = drawn.pop()
                }
            }
            game.threeRedrawnInTurn = true

            game.wildlifeBag.shuffle()
            onAllRefreshables { refreshAfterRedrawOn3Wildlife() }

            /** Verschickt die nachricht samt den Wildlifes */
            if (!game.isHotseatMode && sendNetworkMessage) {
                rootService.networkService.sendWipeWildlifeToOpponent(
                    usedNatureToken = false,
                    wipedWildlifeIndices = wipedIndices
                )
            }

            rootService.gameActionService.checkOverpopulation()
        }


    }

    /**
     * Resets the game to the beginning of the last turn played by a human.
     * Top entry of [entity.CascadiaGameSave.history] is popped into [entity.CascadiaGameSave.undoes].
     * [entity.CascadiaGameSave.currentState] is updated to new top entry of [entity.CascadiaGameSave.history].
     * Returns without undoing if [entity.CascadiaGameSave.history] has only one element.
     */
    fun undo() {

        val cascadiaGameSave = checkNotNull(rootService.currentGame)

        //check if there are enough states to undo
        if (cascadiaGameSave.history.size <= 1) return

        replayToHumanTurn(redo = false)

        onAllRefreshables { refreshAfterUndo() }
    }

    private fun replayToHumanTurn(redo: Boolean) {

        val cascadiaGameSave = checkNotNull(rootService.currentGame)

        do {
            if (redo) {
                cascadiaGameSave.history.push(cascadiaGameSave.undoes.pop())
            } else {
                cascadiaGameSave.undoes.push(cascadiaGameSave.history.pop())
            }
            cascadiaGameSave.currentState = cascadiaGameSave.history.peek()

            val currentState = checkNotNull(cascadiaGameSave.currentState)
            val currentPlayer = currentState.players[currentState.currentPlayer]
        } while (currentPlayer.playMode != PlayerType.HUMAN)

        // Set game state
        checkNotNull(cascadiaGameSave.currentState).gameState = GameState.CHOOSEPAIR
    }

    /**
     * Advances the game one turn if a saved turn is available.
     * The top entry of [entity.CascadiaGameSave.undoes] is popped into [entity.CascadiaGameSave.history].
     * [entity.CascadiaGameSave.currentState] is updated to new top entry of [entity.CascadiaGameSave.history].
     * Returns without redoing if [entity.CascadiaGameSave.undoes] is empty.
     */
    fun redo() {

        val cascadiaGameSave = checkNotNull(rootService.currentGame)

        //check if there are enough states to redo
        if (cascadiaGameSave.history.isEmpty()) return

        replayToHumanTurn(redo = true)

        onAllRefreshables { refreshAfterRedo() }
    }

    /**
     * Uses the current player's Nature Token.
     *
     * Sets natureTokenUsed to true and decreases the player's
     * natureTokenCount by one.
     * @throws IllegalStateException if the player has no Nature Tokens available.
     */
    fun useNatureToken() {
        val game = checkNotNull(rootService.currentGame?.currentState) { "No active game found." }

        val player = game.players[game.currentPlayer]

        require(player.natureTokens > 0) {
            "Player ${player.name} does not have any nature tokens to use."
        }

        // Ensure the game is in a valid state to spend a token.
        check(game.gameState == GameState.CHOOSEPAIR || game.gameState == GameState.USENATURETOKEN) {
            "Cannot use a nature token in the current gamestate: ${game.gameState}."
        }

        player.natureTokens -= 1
        game.natureTokenCount += 1
        game.natureTokenUsed = true

        // Toggle the gamestate to unlock special interactions.
        game.gameState = GameState.USENATURETOKEN
        onAllRefreshables { refreshAfterUseNatureToken() }
    }

    /**
     * Allows the player to rotate the chosen habitat from the combination before placing the habitat
     *
     * @param isLeftRotation if rotate left button is clicked, spin will be set to -1
     *
     * @throws IllegalStateException if no combination was chosen
     */
    fun rotateHabitat(isLeftRotation: Boolean, sendNetworkMessage: Boolean = true) {
        val game = rootService.currentGame?.currentState
        checkNotNull(game) { "No game is currently active" }

        check(game.gameState == GameState.PLACEHABITAT) { "GameState should be PLACEHABITAT" }

        val habitat = game.drawnCombination?.first
        val direction = if (isLeftRotation) -1 else 1
        if (habitat != null) {
            if (isLeftRotation) {
                val first = habitat.habitats.removeFirst()
                habitat.habitats.add(5, first)
            } else {
                val last = habitat.habitats.removeLast()
                habitat.habitats.add(0, last)
            }

            habitat.rotation = (habitat.rotation + direction + 6) % 6
            onAllRefreshables { refreshAfterRotateTile() }

            /** Ruft Networkservice für die message auf */
            if (!game.isHotseatMode && sendNetworkMessage) {
                rootService.networkService.sendRotationToOpponent(habitat.rotation)
            }
        } else {
            throw IllegalStateException("no combination chosen")
        }
    }


    /**
     * Allows the player to discard the wildlife token
     *
     * @throws IllegalStateException if no combination was chosen
     */
    fun discardWildlifeToken(sendNetworkMessage: Boolean = true) {
        val game = rootService.currentGame?.currentState
        checkNotNull(game) { "No game is currently active" }

        check(game.gameState == GameState.PLACEWILDLIFE) { "GameState should be PLACEWILDLIFE" }

        val chosenPair = game.drawnCombination
        if (chosenPair != null) {
            val discardableWildlife = chosenPair.second

            /** das ist der teil -> placemessage mit discarded wildlife für´das network um den zug
             * beenden zu können wenn ein wildlife discarded wird und wir den bag schicken müssen
             */
            if (!game.isHotseatMode && sendNetworkMessage) {
                val habitatCoordinates = checkNotNull(lastPlacedHabitatCoordinates) {
                    "No habitat coordinates saved for network place message."
                }

                val habitatRotation = chosenPair.first.rotation

                rootService.networkService.sendPlaceToOpponent(
                    habitatCoordinates = habitatCoordinates,
                    wildlifeCoordinates = null,
                    habitatRotation = habitatRotation
                )
            }

            val shopIndex = game.wildlifeShop.indexOfFirst { it === discardableWildlife }
            if (shopIndex != -1) game.wildlifeShop[shopIndex] = null
            game.wildlifeBag.push(discardableWildlife)
            game.wildlifeBag.shuffle()
            onAllRefreshables { refreshAfterDiscardWildlifeToken() }

            /**
             * das ist die nachricht die einfach das placen hat ohne wildlife und ohne bag da der später
             * mitgegeben wird
             */
            if (!game.isHotseatMode && sendNetworkMessage) {
                rootService.networkService.sendWipeWildlifeToOpponent(
                    usedNatureToken = false,
                    wipedWildlifeIndices = emptyList()
                )

                lastPlacedHabitatCoordinates = null
            }

            rootService.gameActionService.endTurn()
        } else {
            throw IllegalStateException("no animal token chosen")
        }

    }
}