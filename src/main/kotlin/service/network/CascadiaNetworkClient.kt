
package service.network

import tools.aqua.bgw.core.BoardGameApplication
import edu.udo.cs.sopra.ntf.ChatMessage
import edu.udo.cs.sopra.ntf.GameConfigMessage
import edu.udo.cs.sopra.ntf.GameInitMessage
import edu.udo.cs.sopra.ntf.PlaceMessage
import edu.udo.cs.sopra.ntf.RotationMessage
import edu.udo.cs.sopra.ntf.SelectHabitatTileMessage
import edu.udo.cs.sopra.ntf.SelectMessage
import edu.udo.cs.sopra.ntf.SelectWildlifeMessage
import edu.udo.cs.sopra.ntf.UseNatureTokenMessage
import edu.udo.cs.sopra.ntf.WipeWildlifeMessage
import tools.aqua.bgw.net.client.BoardGameClient
import tools.aqua.bgw.net.client.NetworkLogging
import tools.aqua.bgw.net.common.annotations.GameActionReceiver
import tools.aqua.bgw.net.common.notification.PlayerJoinedNotification
import tools.aqua.bgw.net.common.response.*

/**
 * [BoardGameClient] implementation for network communication.
 *
 * @param playerName the name of the player using this client.
 * @param host the host to connect to.
 * @param secret the secret to use for the connection.
 * @property networkService the [NetworkService] to potentially forward received messages to.
 */
class CascadiaNetworkClient(
    playerName: String,
    host: String,
    secret: String,
    var networkService: NetworkService,
): BoardGameClient(playerName, host, secret, NetworkLogging.VERBOSE) {

    /** the identifier of this game session; can be null if no session started yet. */
    var sessionID: String? = null

    /** the name of the opponent player; can be null if no message from the opponent received yet */
    var otherPlayerName: String? = null

    /**
     * Handle a [CreateGameResponse] sent by the server. Will await the guest player when its
     * status is [CreateGameResponseStatus.SUCCESS]. As recovery from network problems is not
     * implemented in NetWar, the method disconnects from the server and throws an
     * [IllegalStateException] otherwise.
     *
     * @throws IllegalStateException if status != success or currently not waiting for a game creation response.
     */
    override fun onCreateGameResponse(response: CreateGameResponse) {
        BoardGameApplication.runOnGUIThread {
            check(networkService.connectionState == ConnectionState.WAITING_FOR_HOST_CONFIRMATION)
            { "unexpected CreateGameResponse" }

            when (response.status) {
                CreateGameResponseStatus.SUCCESS -> {
                    sessionID = response.sessionID
                    networkService.updateConnectionState(ConnectionState.WAITING_FOR_GUEST)
                }
                else -> disconnectAndError(response.status)
            }
        }
    }



    /**
     * Handle a [JoinGameResponse] sent by the server. Will await the init message when its
     * status is [JoinGameResponseStatus.SUCCESS]. As recovery from network problems is not
     * implemented in NetWar, the method disconnects from the server and throws an
     * [IllegalStateException] otherwise.
     *
     * @throws IllegalStateException if status != success or currently not waiting for a join game response.
     */

    override fun onJoinGameResponse(response: JoinGameResponse) {
        BoardGameApplication.runOnGUIThread {
            check(networkService.connectionState == ConnectionState.WAITING_FOR_JOIN_CONFIRMATION)
            { "unexpected JoinGameResponse" }

            when (response.status) {
                JoinGameResponseStatus.SUCCESS -> {
                    sessionID = response.sessionID
                    networkService.updateConnectionState(ConnectionState.WAITING_FOR_INIT)
                    response.opponents.forEach { opponentName ->networkService.addJoinedPlayer(opponentName)
                    }
                }
                else -> disconnectAndError(response.status)
            }
        }
    }


    /**
     * Handle a [PlayerJoinedNotification] sent by the server. As War only supports two players,
     * this will immediately start the hosted game (and send the init message to the opponent).
     *
     * @throws IllegalStateException if not currently expecting any guests to join.
     */

    override fun onPlayerJoined(notification: PlayerJoinedNotification) {
        BoardGameApplication.runOnGUIThread {
            check(
                networkService.connectionState == ConnectionState.WAITING_FOR_GUEST ||
                     /*weg machen wenn ncit klappt*/
                        networkService.connectionState == ConnectionState.WAITING_FOR_INIT
            ) {
                "PlayerJoinedNotification in unerwartetem Zustand: " +
                        networkService.connectionState
            }

            otherPlayerName = notification.sender
            networkService.addJoinedPlayer(notification.sender)
        }
    }
    /**
     * Handle a [GameActionResponse] sent by the server. Does nothing when its
     * status is [GameActionResponseStatus.SUCCESS]. As recovery from network problems is not
     * implemented in NetWar, the method disconnects from the server and throws an
     * [IllegalStateException] otherwise.
     */
    override fun onGameActionResponse(response: GameActionResponse) {
        BoardGameApplication.runOnGUIThread {
            check(networkService.connectionState == ConnectionState.PLAYING_MY_TURN ||
                    networkService.connectionState == ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
            { "not currently playing in a network game."}

            /*
            wenn game stirbt lade das wieder und untere löschen patch 1.4

            when (response.status) {
                GameActionResponseStatus.SUCCESS -> {}
                else -> disconnectAndError(response.status)
            }
             */

            when (response.status) {
                GameActionResponseStatus.SUCCESS -> {
                    if (networkService.waitingForGameInitConfirmation) {
                        networkService.waitingForGameInitConfirmation = false
                        networkService.rootService.gameActionService.startTurn()
                    }
                }

                else -> disconnectAndError(response.status)
            }
        }
    }

    /**
     * ist eine GameInitMessage die vom server geschickt wurde
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onGameInitReceived(message: GameInitMessage, sender: String) {
        println("GAME INIT RECEIVED from $sender")

        BoardGameApplication.runOnGUIThread {
            otherPlayerName = sender
            networkService.startNewJoinedGame(
                message = message,
                playerName = playerName
            )
        }
    }

    /**
     * ist eine GameConfigMessage die vom server geschickt wurde
     */

    @GameActionReceiver
    fun onGameConfigReceived(message: GameConfigMessage, sender: String) {
        println("GAME CONFIG RECEIVED from $sender: $message")

        BoardGameApplication.runOnGUIThread {
            otherPlayerName = sender
        }
    }

    /**
     * ist eine SelectMessage die vom server geschickt wurde
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onSelectReceived(message: SelectMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.receiveSelect(message)
        }
    }

    internal fun disconnectAndError(message: Any) {
        networkService.disconnect()
        error(message)
    }

    /**
     * ist eine ChatMessage die vom server geschickt wurde
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    internal fun onChatMessageReceived(message: ChatMessage, sender: String) {
        /*
        chat wird irgnoriert
        BoardGameApplication.runOnGUIThread {
            networkService.receiveChatMessage(message, sender)
        }

         */
    }

    /**
     * ist eine SelectHabitatTileMessage die vom server geschickt wurde
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    internal fun onSelectHabitatTileReceived(message: SelectHabitatTileMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.receiveSelectHabitatTile(message)
        }
    }

    /**
     * ist eine SelectWildlifeMessage die vom server geschickt wurde
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    internal fun onSelectWildlifeReceived(message: SelectWildlifeMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.receiveSelectWildlife(message)
        }
    }

    /**
     * ist eine PlaceMessage die vom server geschickt wurde
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    internal fun onPlaceReceived(message: PlaceMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.receivePlace(message)
        }
    }

    /**
     * ist eine RotationMessage die vom server geschickt wurde
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    internal fun onRotationReceived(message: RotationMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.receiveRotation(message)
        }
    }

    /**
     * ist eine UseNatureTokenMessage die vom server geschickt wurde
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    internal fun onUseNatureTokenReceived(message: UseNatureTokenMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.receiveUseNatureToken()
        }
    }

    /**
     * ist eine WipeWildlifeMessage die vom server geschickt wurde
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    internal fun onWipeWildlifeReceived(message: WipeWildlifeMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.receiveWipeWildlife(message)
        }
    }
}
