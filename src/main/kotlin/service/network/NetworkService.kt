
package service.network

import entity.Wildlife
import service.AbstractRefreshingService
import service.RootService
import edu.udo.cs.sopra.ntf.GameInitMessage
import edu.udo.cs.sopra.ntf.NetPlayer
import edu.udo.cs.sopra.ntf.NetWildlife
import entity.BotSpeed
import entity.PlayerType




/**
 * Service layer class that realizes the necessary logic for sending and receiving messages
 * in multiplayer network games. Bridges between the [CascadiaNetworkClient] and the other services.
 *
 * @param rootService The [RootService] instance to access the other service methods and entity layer
 */
class NetworkService(internal val rootService: RootService) : AbstractRefreshingService() {

    internal var localPlayerName: String? = null
    internal var hostPlayerName: String? = null
    internal val joinedPlayerNames = mutableListOf<String>()
    internal var selectedBotPlayers: List<String> = emptyList()
    internal var selectedBotSpeed: BotSpeed = BotSpeed.SLOW
    internal var selectedScoringCards: List<Boolean>? = null
    internal var messageHabitatShopIndex: Int? = null

    // Test wenn game stirbt nimm das raus
    internal var waitingForGameInitConfirmation = false
    internal var waitingForWipeAfterDiscard = false

    /** Network client. Nullable for offline games. */
    var client: CascadiaNetworkClient? = null
        private set

    /**
     * current state of the connection in a network game.
     */
    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
        private set

    /**
     * Connects to server and creates a new game session.
     *
     * @param secret Server secret.
     * @param name Player name.
     * @param sessionID identifier of the hosted session (to be used by guest on join)
     *
     * @throws IllegalStateException if already connected to another game or connection attempt fails
     */
    fun hostGame(
        secret: String, name: String, sessionID: String?, scoringCards: List<Boolean>,
        botSpeed: BotSpeed, botPlayers: List<String> = emptyList()
    ) {

        if (scoringCards.size != 5){throw IllegalStateException("es müssen 5 Cards sein")}

        selectedScoringCards = scoringCards
        selectedBotSpeed = botSpeed
        selectedBotPlayers = botPlayers


        hostPlayerName = name
        localPlayerName = name
        joinedPlayerNames.clear()

        if (!connect(secret, name)) {
            error("Connection failed")
        }

        updateConnectionState(ConnectionState.CONNECTED)

        if (sessionID.isNullOrBlank()) {
            client?.createGame(GAME_ID, "Was geht!")
        } else {
            client?.createGame(GAME_ID, sessionID, "Was geht!")
        }
        updateConnectionState(ConnectionState.WAITING_FOR_HOST_CONFIRMATION)
    }

    /**
     * das bvrauchen wir damit wir player hinzufügen können ohne dass das game sofort startet
     */

    fun addJoinedPlayer(playerName: String) {
        check(connectionState == ConnectionState.WAITING_FOR_GUEST||
               /*weg machen wenn nicht klappt*/
                connectionState == ConnectionState.WAITING_FOR_INIT) {
            "Worauf warten wir? Kein Gast kommt mehr!"
        }

        if (playerName !in joinedPlayerNames) {
            joinedPlayerNames.add(playerName)
        }

        onAllRefreshables {
            refreshConnectionState(connectionState)
        }
    }

    /**
     * ist dafür da damit die gui bescheid weiss welche speiler da sind
     */
    fun getLobbyPlayers(): List<String> {
        return listOfNotNull(hostPlayerName) + joinedPlayerNames + selectedBotPlayers
    }

    /**
     * Disconnects the [client] from the server, nulls it and updates the
     * [connectionState] to [ConnectionState.DISCONNECTED]. Can safely be called
     * even if no connection is currently active.
     */
    fun disconnect() {
        client?.apply {
            if (sessionID != null) leaveGame("Ciao")
            if (isOpen) disconnect()
        }
        client = null
        updateConnectionState(ConnectionState.DISCONNECTED)
    }

    /**
     * Connects to server and joins a game session as guest player.
     *
     * @param secret Server secret.
     * @param name Player name.
     * @param sessionID identifier of the joined session (as defined by host on create)
     *
     * @throws IllegalStateException if already connected to another game or connection attempt fails
     */
    fun joinGame(secret: String, name: String, sessionID: String) {
        localPlayerName = name

        if (!connect(secret, name)) {
            error("Connection failed")
        }
        updateConnectionState(ConnectionState.CONNECTED)

        client?.joinGame(sessionID, "Was geht!")

        updateConnectionState(ConnectionState.WAITING_FOR_JOIN_CONFIRMATION)
    }

    /**
     * Connects to server, sets the [NetworkService.client] if successful and returns `true` on success.
     *
     * @param secret Network secret. Must not be blank (i.e. empty or only whitespaces)
     * @param name Player name. Must not be blank
     *
     * @throws IllegalArgumentException if secret or name is blank
     * @throws IllegalStateException if already connected to another game
     */
    internal fun connect(secret: String, name: String): Boolean {
        require(connectionState == ConnectionState.DISCONNECTED && client == null)
        { "already connected to another game" }

        require(secret.isNotBlank()) { "server secret must be given" }
        require(name.isNotBlank()) { "player name must be given" }

        val newClient =
            CascadiaNetworkClient(
                playerName = name,
                host = SERVER_ADDRESS,
                secret = secret,
                networkService = this
            )

        return if (newClient.connect()) {
            this.client = newClient
            true
        } else {
            false
        }
    }

    /**
     * set up the game using ['GameService.startNewGame'] and send the game init message
     * to the guest player. ['connectionState'] needs to be ['ConnectionState.WAITING_FOR_GUEST'].
     * This method should be called from the ['WarNetworkClient'] when the guest joined notification
     * arrived. See ['WarNetworkClient.onPlayerJoined'].
     *
     * @param 'hostPlayerName' player name of the host player
     * @param 'guestPlayerName' player name of the guest player
     *
     * @throws IllegalStateException if [connectionState] != [ConnectionState.WAITING_FOR_GUEST]
     */
    fun startNewHostedGame(botPlayers: List<String> = selectedBotPlayers,botSpeed: BotSpeed = selectedBotSpeed) {

        check(connectionState == ConnectionState.WAITING_FOR_GUEST)
        { "currently not prepared to start a new hosted game." }

        val scoringCards = checkNotNull(selectedScoringCards) {
            "Es wurden keine ScoringCards ausgewählt"
        }


        selectedBotPlayers = botPlayers
        selectedBotSpeed = botSpeed


        val host = checkNotNull(hostPlayerName) {
            "Wer ist unser Host????"
        }

        val allPlayers = listOf(host) + joinedPlayerNames + selectedBotPlayers

        check(allPlayers.size in 2..4) {
            "Cascadia braucht 2 bis 4 Spieler."
        }

        check(allPlayers.distinct().size == allPlayers.size) {
            "Spielernamen müssen eindeutig sein: $allPlayers"
        }

        rootService.gameActionService.startNewGame(
            playerNames = allPlayers,
            isNetworkGame = true,
            scoringCardTypes = scoringCards,
            simulationSpeed = selectedBotSpeed
        )

        // rootService.gameActionService.startTurn()

        //lowk testet ob der bot ein mensch oder bot ist  paradox oder?
        val game = checkNotNull(rootService.currentGame?.currentState) {
            "game should not be null right after starting it."
        }
        /*

        !!!!!!!!!!!!!!!!!!!!!!! debug dev mode!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!


        println("PLAYERS: " + game.players.map { "${it.name}:${it.playMode}" })
        println("CURRENT PLAYER: ${game.players[game.currentPlayer].name}")

        checkNotNull(game) {"game should not be null right after starting it."}

        */

        /*
        old verison

        val tileStackForGuest =
    game.habitatShop.filterNotNull().map { it.id } +
        game.habitatDrawPile.peekAll().map { it.id }

val wildlifeBagForGuest =
    game.wildlifeShop.filterNotNull()
        .map { NetWildlife.valueOf(it.animal.name) } +
        game.wildlifeBag.peekAll()
            .map { NetWildlife.valueOf(it.animal.name) }

         */
        val tileStackForGuest =
            game.habitatDrawPile.peekAll().map { tile ->tile.id} + game.habitatShop
                        .map { tile ->checkNotNull(tile) {"Habitat shop muss vollständig gefüllt sein."}.id
                        }.reversed()

        val wildlifeBagForGuest =
            game.wildlifeBag.peekAll().map { token ->NetWildlife.valueOf(token.animal.name)} +
                    game.wildlifeShop.map { token -> NetWildlife.valueOf(checkNotNull(token) {
                                    "Wildlife shop muss vollständig gefüllt sein."}.animal.name)}.reversed()



        val netPlayers = game.players.map { player ->
            NetPlayer(
                name = player.name,
                startingTileID = checkNotNull(player.board[Pair(0, 0)]) {
                    "Player ${player.name} has no starting tile at (0, 0)."
                }.id
            )
        }

        waitingForGameInitConfirmation = true

        updateConnectionState(ConnectionState.PLAYING_MY_TURN)

        sendGameInitToOpponent(
            tileStack = tileStackForGuest,
            scoringCards = scoringCards,
            players = netPlayers,
            wildlifeBag = wildlifeBagForGuest
        )
        /*
        wenn es stirbt:
        val playersForGuest = game.players.mapIndexed { index, player ->
            NetPlayer(
                name = player.name,
                startingTileID = index
            )
        }

        val message = GameInitMessage(
            tileStack = tileStackForGuest,
            scoringCards = scoringCards,
            players = playersForGuest,
            wildlifeBag = wildlifeBagForGuest
        )


         */

        // teil des neuen fix 1.4



        /*
        val message = GameInitMessage(
            game.player1.leftDrawStack.peekAll().map { NetWarCardMessage(it) },
            game.player1.rightDrawStack.peekAll().map { NetWarCardMessage(it) },
            game.player2.leftDrawStack.peekAll().map { NetWarCardMessage(it) },
            game.player2.rightDrawStack.peekAll().map { NetWarCardMessage(it) }
        )
        updateConnectionState(ConnectionState.PLAYING_MY_TURN)
        client?.sendGameActionMessage(message)
        */


    }

    /**
     * Initializes the entity structure with the data given by the [GameInitMessage] sent by the host.
     * [connectionState] needs to be [ConnectionState.WAITING_FOR_INIT].
     * This method should be called from the [CascadiaNetworkClient] when the host sends the init message.
     * See ['CascadiaNetworkClient.onInitReceived'].
     *
     * @throws IllegalStateException if not currently waiting for an init message
     */
    fun startNewJoinedGame(message: GameInitMessage, playerName: String) {
        check(connectionState == ConnectionState.WAITING_FOR_INIT) {
            "not waiting for game init message."
        }

        val wildlifeBagTypes = message.wildlifeBag.map {
            Wildlife.valueOf(it.name)
        }

        rootService.gameActionService.startNewNetworkGameFromInitMessage(
            playersFromMessage = message.players,
            scoringCardTypes = message.scoringCards,
            tileStackIds = message.tileStack,
            wildlifeBagTypes = wildlifeBagTypes,
            simulationSpeed = BotSpeed.SLOW
        )

        //rootService.gameActionService.startTurn()

        //lowk testet ob der bot ein mensch oder bot ist  paradox oder?
        val game = checkNotNull(rootService.currentGame?.currentState)
        println("PLAYERS: " + game.players.map { "${it.name}:${it.playMode}" })
        println("CURRENT PLAYER: ${game.players[game.currentPlayer].name}")

        updateConnectionStateForPlayer(playerName)
    }

    /**
     * Also diese funktion sorgt dafür dass das game/Network weis wer wessen bot ist also welcher player
     * seinen bot nutzen darf sodass sich der bot sich nciht überlappt und zu keiner aktion führt
     * das sorgt zu gleich dafür dass 1 player 2 züge schickt also einmal player und bot
     */
    fun runBotTurnIfResponsible() {
        val game = checkNotNull(rootService.currentGame?.currentState) {
            "kein game aktiv"
        }

        val currentPlayer = game.players[game.currentPlayer]
        val localName = checkNotNull(localPlayerName) {
            "Es gibt keinen player name"
        }

        if (currentPlayer.name != localName) {
            updateConnectionState(
                ConnectionState.PLAYING_WAITING_FOR_OPPONENT
            )
            return
        }

        updateConnectionState(ConnectionState.PLAYING_MY_TURN)

        if (currentPlayer.playMode != PlayerType.HUMAN) {
            rootService.botActionService.botTurn()
        }
    }

    /**
     * Updates the [connectionState] to [newState] and notifies all
     */
    fun updateConnectionState(newState: ConnectionState) {
        this.connectionState = newState
        onAllRefreshables {
            refreshConnectionState(newState)
        }
    }


    /**
     * Beinhaltet Server Address und Game ID
     */
    companion object {
        /** URL of the BGW net server hosted for SoPra participants */
        const val SERVER_ADDRESS = "sopra.cs.tu-dortmund.de:80/bgw-net/connect"

        /** Name of the game as registered with the server */
        const val GAME_ID = "Cascadia"
    }
}
