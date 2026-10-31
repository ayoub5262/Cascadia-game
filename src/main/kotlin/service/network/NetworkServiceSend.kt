package service.network

import edu.udo.cs.sopra.ntf.ChatMessage
import edu.udo.cs.sopra.ntf.GameConfigMessage
import edu.udo.cs.sopra.ntf.GameInitMessage
import edu.udo.cs.sopra.ntf.NetPlayer
import edu.udo.cs.sopra.ntf.NetWildlife
import edu.udo.cs.sopra.ntf.PlaceMessage
import edu.udo.cs.sopra.ntf.RotationMessage
import edu.udo.cs.sopra.ntf.SelectHabitatTileMessage
import edu.udo.cs.sopra.ntf.SelectMessage
import edu.udo.cs.sopra.ntf.SelectWildlifeMessage
import edu.udo.cs.sopra.ntf.UseNatureTokenMessage
import edu.udo.cs.sopra.ntf.WipeWildlifeMessage

    /**
     * sendet die GameConfig sachen an den gegner
     */
    fun NetworkService.sendGameConfigToOpponent(players: List<String>, scoringCards: List<Boolean?>) {

        if (connectionState != ConnectionState.PLAYING_MY_TURN &&
            connectionState != ConnectionState.WAITING_FOR_GUEST
        ) {throw IllegalStateException("du darfst keine Config senden")}

        val message = GameConfigMessage(
            players = players,
            scoringCards = scoringCards
        )

        client?.sendGameActionMessage(message)
    }
    /**
     * sendet GameInitMessage an den gegner
     */
    fun NetworkService.sendGameInitToOpponent(
        tileStack: List<Int>,
        scoringCards: List<Boolean>,
        players: List<NetPlayer>,
        wildlifeBag: List<NetWildlife>
    ){
        if (connectionState != ConnectionState.PLAYING_MY_TURN) {
            throw IllegalStateException("du bist nicht dran")
        }

        val message = GameInitMessage(
            tileStack = tileStack,
            scoringCards = scoringCards,
            players = players,
            wildlifeBag = wildlifeBag
        )

        client?.sendGameActionMessage(message)
    }

    /**
     * sendet unsere Choosecombination (SelectMessage) an den gegner
     */
    fun NetworkService.sendSelectToOpponent(
        habitatShopIndex: Int,
        wildlifeShopIndex: Int,
        usedNatureToken: Boolean
    ) {
        if (connectionState != ConnectionState.PLAYING_MY_TURN) {
            throw IllegalStateException("du bist nicht dran")
        }

        val message = SelectMessage(
            wildlifeShopIndex = wildlifeShopIndex,
            habitatShopIndex = habitatShopIndex,
            usedNatureToken = usedNatureToken
        )

        client?.sendGameActionMessage(message)
    }

    /**
     * sendet den ausgewählten Habitat an den gegner
     */
    fun NetworkService.sendSelectHabitatTileToOpponent(habitatShopIndex: Int) {
        if (connectionState != ConnectionState.PLAYING_MY_TURN) {
            throw IllegalStateException("du bist nicht dran")
        }

        val message = SelectHabitatTileMessage(
            habitatShopIndex = habitatShopIndex
        )

        client?.sendGameActionMessage(message)
    }

    /**
     * sendet den ausgewählten Wildlife an den gegner
     */
    fun NetworkService.sendSelectWildlifeToOpponent(wildlifeShopIndex: Int) {
        if (connectionState != ConnectionState.PLAYING_MY_TURN) {
            throw IllegalStateException("du bist nicht dran")
        }

        val message = SelectWildlifeMessage(
            wildlifeShopIndex = wildlifeShopIndex
        )

        client?.sendGameActionMessage(message)
    }

    /**
     * sendet die gewünschte Rotation an den gegner
     */
    fun NetworkService.sendRotationToOpponent(habitatRotation: Int) {

        if (connectionState != ConnectionState.PLAYING_MY_TURN) {
            throw IllegalStateException("du bist nicht dran")
        }
        val message = RotationMessage(
            habitatRotation = habitatRotation
        )

        client?.sendGameActionMessage(message)
    }

    /**
     * Benachrichtigt den gegner wenn der Naturetoken benutzt wurde
     */
    fun NetworkService.sendUseNatureTokenToOpponent() {

        if (connectionState != ConnectionState.PLAYING_MY_TURN) {
            throw IllegalStateException("du bist nicht dran")
        }

        val message = UseNatureTokenMessage()

        client?.sendGameActionMessage(message)
    }

    /**
     * Schickt unser Rerollen (WipeWildlifeMessage)
     */
    fun NetworkService.sendWipeWildlifeToOpponent(
        usedNatureToken: Boolean,
        wipedWildlifeIndices: List<Int>
    ) {

        if (connectionState != ConnectionState.PLAYING_MY_TURN) {
            throw IllegalStateException("du bist nicht dran")
        }

        val game = rootService.currentGame?.currentState

        checkNotNull(game){"Es ist kein game aktiv"}

        val player = game.players[game.currentPlayer]

        val wildlifeBag = game.wildlifeBag.peekAll().map {
            NetWildlife.valueOf(it.animal.name)
        }

        val message = WipeWildlifeMessage(
            usedNatureToken = usedNatureToken,
            natureTokenAmount = player.natureTokens,
            wipedWildlifeIndices = wipedWildlifeIndices,
            wildlifeBag = wildlifeBag
        )

        client?.sendGameActionMessage(message)

        if (wipedWildlifeIndices.isEmpty() && !usedNatureToken) {
            updateConnectionState( ConnectionState.PLAYING_WAITING_FOR_OPPONENT )
        }
    }

    /**
     * Sendet den gegner alles wichtige was Placen umfasst
     */
    fun NetworkService.sendPlaceToOpponent(
        habitatCoordinates: Pair<Int, Int>,
        wildlifeCoordinates: Pair<Int, Int>?,
        habitatRotation: Int
    ) {

        if (connectionState != ConnectionState.PLAYING_MY_TURN) {
            throw IllegalStateException("du bist nicht dran")
        }
        val message = PlaceMessage(
            habitatCoordinates = habitatCoordinates,
            wildlifeCoordinates = wildlifeCoordinates,
            habitatRotation = habitatRotation
        )

        client?.sendGameActionMessage(message)

        if (wildlifeCoordinates != null) {
            updateConnectionState(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
        }
    }

    /**
     * sendet unseren ungefilterten und genialen chat XD
     */
    fun NetworkService.sendChatMessageToOpponent(text: String) {

        if (connectionState == ConnectionState.DISCONNECTED) {
            throw IllegalStateException("du bist nicht connected")
        }

        val message = ChatMessage(text)

        client?.sendGameActionMessage(message)
    }
