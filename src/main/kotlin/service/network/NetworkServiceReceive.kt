package service.network

import edu.udo.cs.sopra.ntf.ChatMessage
import edu.udo.cs.sopra.ntf.NetWildlife
import edu.udo.cs.sopra.ntf.PlaceMessage
import edu.udo.cs.sopra.ntf.RotationMessage
import edu.udo.cs.sopra.ntf.SelectHabitatTileMessage
import edu.udo.cs.sopra.ntf.SelectMessage
import edu.udo.cs.sopra.ntf.SelectWildlifeMessage
import edu.udo.cs.sopra.ntf.WipeWildlifeMessage
import entity.Wildlife
import entity.WildlifeToken
import tools.aqua.bgw.util.Stack

    /**
     * funktion um spieler spezifisch die conncetionstate zu ändern
     */
    internal fun NetworkService.updateConnectionStateForPlayer(playerName: String) {

        val game = rootService.currentGame?.currentState
        checkNotNull(game) {"Es ist kein game Aktiv"}

        val currentPlayerName = game.players[game.currentPlayer].name

        if (currentPlayerName == playerName) {
            updateConnectionState(ConnectionState.PLAYING_MY_TURN)
        } else {
            updateConnectionState(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
        }
    }

    /**
     * die funktion ist wichtig nach den neuen NTF da es den bag aus der wipewildlifemessage nimmt
     */
    private fun wildlifeStackFromMessage(wildlifeBag: List<NetWildlife>): Stack<WildlifeToken> {
        val newBag = Stack<WildlifeToken>()

        for (netWildlife in wildlifeBag.reversed()) {
            newBag.push(
                WildlifeToken(
                    Wildlife.valueOf(netWildlife.name)
                )
            )
        }

        return newBag
    }

    /**
     * diese funktion sorgt einfachd dafür dass beim discarden der gegebene bag nachrdem übernehmen
     * genommen wird und der zug richtig beendet wird
     * --> der zug endet hier bei discard (choosepiar wildilge = null)
     */
    internal fun NetworkService.finishDiscardAfterWipe(message: WipeWildlifeMessage) {
        check(message.wipedWildlifeIndices.isEmpty()) {
            "Beim Discard nach PlaceMessage(null) muss die WipeWildlifeMessage leere Indizes haben."
        }

        val game = checkNotNull(rootService.currentGame?.currentState) {
            "Es ist kein game aktiv"
        }

        val player = game.players[game.currentPlayer]
        val chosenPair = checkNotNull(game.drawnCombination) {
            "es wurde keine combination gezoigen"
        }

        // Die Message enthält die Bag, aus der der Shop anschließend neu befüllt wird.
        game.wildlifeBag = wildlifeStackFromMessage(message.wildlifeBag)

        val discardableWildlife = chosenPair.second

        val shopIndex = game.wildlifeShop.indexOfFirst {
            it === discardableWildlife
        }

        if (shopIndex != -1) {
            game.wildlifeShop[shopIndex] = null
        }


        player.natureTokens = message.natureTokenAmount

        onAllRefreshables {
            refreshAfterDiscardWildlifeToken()
        }

        waitingForWipeAfterDiscard = false

        rootService.gameActionService.endTurn()

        val updatedGame = checkNotNull(
            rootService.currentGame?.currentState
        ) {
            "Es ist nach endTurn kein Game aktiv."
        }

        val currentPlayerName =
            updatedGame.players[updatedGame.currentPlayer].name

        if (currentPlayerName == localPlayerName) {
            updateConnectionState(ConnectionState.PLAYING_MY_TURN)
        } else {
            updateConnectionState(
                ConnectionState.PLAYING_WAITING_FOR_OPPONENT
            )
        }

    }

    /**
     * handelt die choosepairMessage
     */
    fun NetworkService.receiveSelect(message: SelectMessage) {
        check(connectionState in listOf(
            ConnectionState.PLAYING_MY_TURN,
            ConnectionState.PLAYING_WAITING_FOR_OPPONENT
        )
        )
        { "Es wird kein Spielzug des gegners erwartet" }

        if (message.usedNatureToken) {
            rootService.playerActionService.useNatureToken()
        }

        rootService.playerActionService.chooseCombination(
            Pair(message.habitatShopIndex, message.wildlifeShopIndex),
            sendNetworkMessage = false
        )
    }

    /**
     * handelt die selectHabitatMessage
     */
    fun NetworkService.receiveSelectHabitatTile(message: SelectHabitatTileMessage) {
        check(connectionState in listOf(
            ConnectionState.PLAYING_MY_TURN,
            ConnectionState.PLAYING_WAITING_FOR_OPPONENT
        )
        )
        { "Es wird kein Spielzug des gegners erwartet" }

        messageHabitatShopIndex = message.habitatShopIndex
    }

    /**
     * handelt die SelectWildlifeMessage und setzt diese ein
     */
    fun NetworkService.receiveSelectWildlife(message: SelectWildlifeMessage) {

        check(connectionState in listOf(
            ConnectionState.PLAYING_MY_TURN,
            ConnectionState.PLAYING_WAITING_FOR_OPPONENT
        )
        )
        { "Es wird kein Spielzug des gegners erwartet" }

        val habitatShopIndex = messageHabitatShopIndex
        checkNotNull(habitatShopIndex){"Es wurde kein Habitat ausgewählt"}

        rootService.playerActionService.chooseCombination(
            Pair(habitatShopIndex, message.wildlifeShopIndex),
            sendNetworkMessage = false
        )
        messageHabitatShopIndex = null
    }

    /**
     * empfangen der place
     */
    fun NetworkService.receivePlace(message: PlaceMessage) {
        check(connectionState in listOf(
            ConnectionState.PLAYING_MY_TURN,
            ConnectionState.PLAYING_WAITING_FOR_OPPONENT
        )
        )
        { "Es wird kein Spielzug des gegners erwartet" }

        rootService.playerActionService.placeHabitat(message.habitatCoordinates)


        val wildlifeCoordinates = message.wildlifeCoordinates

        if (wildlifeCoordinates != null) {

            val game = checkNotNull(rootService.currentGame?.currentState) {
                "Es ist kein game aktiv."
            }

            val nextPlayerIndex =
                (game.currentPlayer + 1) % game.players.size

            val nextPlayerName =
                game.players[nextPlayerIndex].name

            if (nextPlayerName == localPlayerName) {
                updateConnectionState(ConnectionState.PLAYING_MY_TURN)
            } else {
                updateConnectionState(
                    ConnectionState.PLAYING_WAITING_FOR_OPPONENT
                )
            }

            rootService.playerActionService.placeWildlifeTokens(
                wildlifeCoordinates,
                sendNetworkMessage = false
            )
        } else {
            /**
             * hier ist der teil der entscheidet pb der neue bag genommen wird
             */
            waitingForWipeAfterDiscard = true
        }

    }

    /**
     * empfangen der Rotationmessage
     */
    fun NetworkService.receiveRotation(message: RotationMessage){

        check(connectionState in listOf(
            ConnectionState.PLAYING_MY_TURN,
            ConnectionState.PLAYING_WAITING_FOR_OPPONENT
        )
        )
        { "Es wird kein Spielzug des gegners erwartet" }

        val game = rootService.currentGame?.currentState
        checkNotNull(game) { "es ist kein game am laufen" }
        val habitat = game.drawnCombination?.first
        checkNotNull(habitat) { "Es wurde kein Habitat gezogen" }


        val actualRotation = ((message.habitatRotation % 6) + 6) % 6

        val currentRotation = ((habitat.rotation % 6) + 6) % 6

        val rotationDiff = (actualRotation - currentRotation + 6) % 6

        repeat(rotationDiff) {
            val last = habitat.habitats.removeLast()
            habitat.habitats.add(0, last)
        }

        habitat.rotation = actualRotation

        onAllRefreshables { refreshAfterRotateTile() }

    }

    /**
     * Empfangen NatureToken Ack
     */
    fun NetworkService.receiveUseNatureToken(){
        check(connectionState in listOf(
            ConnectionState.PLAYING_MY_TURN,
            ConnectionState.PLAYING_WAITING_FOR_OPPONENT
        )
        )
        { "Es wird kein Spielzug des gegners erwartet" }


        rootService.playerActionService.useNatureToken()

    }

    /**
     * empfangen von dem rerollen (WipeWildlife)
     */
    fun NetworkService.receiveWipeWildlife(message: WipeWildlifeMessage) {

        check(
            connectionState in listOf(
                ConnectionState.PLAYING_MY_TURN,
                ConnectionState.PLAYING_WAITING_FOR_OPPONENT
            )
        )
        { "Es wird kein Spielzug des gegners erwartet" }

        if (waitingForWipeAfterDiscard) {
            finishDiscardAfterWipe(message)
            return
        }


        val game = checkNotNull(rootService.currentGame?.currentState) {
            "No active game found."
        }
        val player = game.players[game.currentPlayer]

        val drawn = Stack<WildlifeToken>()

        repeat(message.wipedWildlifeIndices.size) {
            check(game.wildlifeBag.isNotEmpty()) {
                "Wildlife bag is empty while rebuilding the shop."
            }

            drawn.push(rootService.gameActionService.removeBottomEntry(
                game.wildlifeBag
            ))
        }

        for (index in message.wipedWildlifeIndices) {
            require(index in game.wildlifeShop.indices) {
                "Invalid wildlife shop index: $index"
            }

            checkNotNull(game.wildlifeShop[index]) {
                "Wildlife shop at index $index is empty."
            }

            game.wildlifeShop[index] = drawn.pop()
        }

        player.natureTokens = message.natureTokenAmount

        game.wildlifeBag = wildlifeStackFromMessage(message.wildlifeBag)

        onAllRefreshables {
            refreshAfterRedrawWildlifeTokens()
        }


    }
    /**
     * empfangen der Chat nachrichten
     */
    fun NetworkService.receiveChatMessage(message: ChatMessage, sender: String) {

        onAllRefreshables { refreshAfterChatMessage(sender, message.message) }
    }

