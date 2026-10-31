package networkservice

import service.RootService
import edu.udo.cs.sopra.ntf.ChatMessage
import edu.udo.cs.sopra.ntf.GameInitMessage
import edu.udo.cs.sopra.ntf.NetPlayer
import edu.udo.cs.sopra.ntf.NetWildlife
import edu.udo.cs.sopra.ntf.PlaceMessage
import edu.udo.cs.sopra.ntf.RotationMessage
import edu.udo.cs.sopra.ntf.SelectHabitatTileMessage
import edu.udo.cs.sopra.ntf.SelectMessage
import edu.udo.cs.sopra.ntf.SelectWildlifeMessage
import edu.udo.cs.sopra.ntf.WipeWildlifeMessage
import entity.BotSpeed
import entity.CascadiaGame
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import service.Refreshable
import entity.GameState
import service.network.ConnectionState
import service.network.receiveChatMessage
import service.network.receivePlace
import service.network.receiveRotation
import service.network.receiveSelect
import service.network.receiveSelectHabitatTile
import service.network.receiveSelectWildlife
import service.network.receiveUseNatureToken
import service.network.receiveWipeWildlife
import service.network.sendChatMessageToOpponent
import service.network.sendGameConfigToOpponent
import service.network.sendGameInitToOpponent
import service.network.sendPlaceToOpponent
import service.network.sendRotationToOpponent
import service.network.sendSelectHabitatTileToOpponent
import service.network.sendSelectToOpponent
import service.network.sendSelectWildlifeToOpponent
import service.network.sendUseNatureTokenToOpponent
import service.network.sendWipeWildlifeToOpponent
import kotlin.test.assertTrue
import kotlin.test.assertFalse


/**
 * Class that provides tests for the [service.network.NetworkService]. It will connect to the
 * SoPra BGW-Net server twice and actually send messages through that server.
 * This might fail if the server is offline or [NETWORK_SECRET] is outdated.
 */
class NetworkServiceTest {

    private lateinit var rootServiceHost: RootService
    private lateinit var rootServiceGuest: RootService

    //region the good path

    /**
     * Initialize both connections and start the game, so that host and guest
     * both have an active Cascadia game.
     */
    private fun initConnections() {
        rootServiceHost = RootService()
        rootServiceGuest = RootService()

        val scoringCards = listOf(true, true, true, true, true)

        rootServiceHost.networkService.hostGame(
            secret = NETWORK_SECRET,
            name = "kaan",
            sessionID = null,
            scoringCards = scoringCards,
            botSpeed = BotSpeed.SLOW
        )

        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_GUEST)

        val sessionID = rootServiceHost.networkService.client?.sessionID
        assertNotNull(sessionID)

        rootServiceGuest.networkService.joinGame(
            secret = NETWORK_SECRET,
            name = "metehan",
            sessionID = sessionID
        )

        rootServiceGuest.waitForState(ConnectionState.WAITING_FOR_INIT)

        rootServiceHost.networkService.startNewHostedGame()

        rootServiceHost.waitForState(ConnectionState.PLAYING_MY_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
    }

    /**
     * Test that host and guest can connect and start the same game.
     */
    @Test
    fun testHostAndJoinGame() {
        initConnections()

        assertEquals(ConnectionState.PLAYING_MY_TURN, rootServiceHost.networkService.connectionState)
        assertEquals(ConnectionState.PLAYING_WAITING_FOR_OPPONENT, rootServiceGuest.networkService.connectionState)

        val hostGame = rootServiceHost.currentGame?.currentState
        val guestGame = rootServiceGuest.currentGame?.currentState

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        assertEquals(
            hostGame.players.map { it.name },
            guestGame.players.map { it.name }
        )

        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()
    }

    /**
     * Das soll testen ob das game richtig lädt bei den leuten also gleicher bag name shop usw also
     * unsere config muss equal sein niemand zeiht sein einges ding durch alles wird zsm gelöst
     *
     * Nie ohne mein team
     */
    @Test
    fun testInitialGameStateIsSynchronized() {
        initConnections()

        val hostGame = rootServiceHost.currentGame?.currentState
        val guestGame = rootServiceGuest.currentGame?.currentState

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        assertEquals(
            hostGame.players.map { it.name },
            guestGame.players.map { it.name },
            "player sind nicht gesync"
        )

        assertEquals(
            hostGame.habitatShop.map { it?.id },
            guestGame.habitatShop.map { it?.id },
            "Habitat shop ist nicht gesync"
        )

        assertEquals(
            hostGame.wildlifeShop.map { it?.animal },
            guestGame.wildlifeShop.map { it?.animal },
            "Wildlife shop ist nicht gesync"
        )

        assertEquals(
            hostGame.habitatDrawPile.peekAll().map { it.id },
            guestGame.habitatDrawPile.peekAll().map { it.id },
            "Habitat draw pile ist nicht gesync"
        )

        val hostBag = hostGame.wildlifeBag.peekAll().map { it.animal }
        val gastBag = guestGame.wildlifeBag.peekAll().map { it.animal }

        println("HOST BAG:        " + hostBag.take(10))
        println("GUEST BAG:       " + gastBag.take(10))
        println("HOST BAG REV:    " + hostBag.reversed().take(10))
        println("HOST SIZE:       " + hostBag.size)
        println("GUEST SIZE:      " + gastBag.size)
        println("HOST == GUEST:   " + (hostBag == gastBag))
        println("HOSTREV == GUEST:" + (hostBag.reversed() == gastBag))


        assertEquals(
            hostGame.wildlifeBag.peekAll().map { it.animal },
            guestGame.wildlifeBag.peekAll().map { it.animal },
            "Wildlife bag ist nicht gesync"
        )


        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()
    }

    /**
     * Testet ob der ganze zug des hosts auch wiklich richtig gesendet wird und auch richtig eingearbeitet wird
     * also ob alle züge ankommen ungesetzt werden und auch richtig umgesetzt werden
     */
    @Test
    fun testHostTurnIsCorrectOnGuest() {
        initConnections()

        rootServiceHost.playerActionService.chooseCombination(Pair(0, 0))

        println(rootServiceHost.currentGame?.currentState?.gameState)

        rootServiceHost.playerActionService.placeHabitat(Pair(1, 0))
        rootServiceHost.playerActionService.discardWildlifeToken()

        rootServiceHost.waitForState(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        val hostGame = rootServiceHost.currentGame?.currentState
        val guestGame = rootServiceGuest.currentGame?.currentState

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        assertEquals(hostGame.currentPlayer, guestGame.currentPlayer)
        assertEquals(hostGame.roundNumber, guestGame.roundNumber)

        assertBoardsMatch(hostGame, guestGame, 0)

        assertEquals(
            hostGame.habitatShop.map { it?.id },
            guestGame.habitatShop.map { it?.id }
        )

        assertEquals(
            hostGame.wildlifeShop.map { it?.animal },
            guestGame.wildlifeShop.map { it?.animal }
        )

        assertEquals(
            hostGame.habitatDrawPile.peekAll().map { it.id },
            guestGame.habitatDrawPile.peekAll().map { it.id }
        )

        assertEquals(
            ConnectionState.PLAYING_WAITING_FOR_OPPONENT,
            rootServiceHost.networkService.connectionState
        )

        assertEquals(
            ConnectionState.PLAYING_MY_TURN,
            rootServiceGuest.networkService.connectionState
        )

        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()
    }

    /**
     * Ist eigentlich 1zu1 das gleiche von oben aber nur mit dem unterschied dass der gast gecheckt wird
     *
     * Testet ob der ganze zug des hosts auch wiklich richtig gesendet wird und auch richtig eingearbeitet wird
     * also ob alle züge ankommen ungesetzt werden und auch richtig umgesetzt werden
     */
    @Test
    fun testHostTurnIsCorrectOnHost() {
        initConnections()

        // host turn
        rootServiceHost.playerActionService.chooseCombination(Pair(0, 0))
        rootServiceHost.playerActionService.placeHabitat(Pair(1, 0))
        rootServiceHost.playerActionService.discardWildlifeToken()

        rootServiceHost.waitForState(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        // gast turn
        rootServiceGuest.playerActionService.chooseCombination(Pair(0, 0))
        rootServiceGuest.playerActionService.placeHabitat(Pair(1, 0))
        rootServiceGuest.playerActionService.discardWildlifeToken()

        rootServiceGuest.waitForState(ConnectionState.PLAYING_WAITING_FOR_OPPONENT)
        rootServiceHost.waitForState(ConnectionState.PLAYING_MY_TURN)

        val hostGame = rootServiceHost.currentGame?.currentState
        val guestGame = rootServiceGuest.currentGame?.currentState

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        assertEquals(hostGame.currentPlayer, guestGame.currentPlayer)
        assertEquals(hostGame.roundNumber, guestGame.roundNumber)

        assertEquals(
            hostGame.players.map { it.name },
            guestGame.players.map { it.name }
        )

        assertBoardsMatch(hostGame, guestGame, 0)
        assertBoardsMatch(hostGame, guestGame, 1)

        assertEquals(
            hostGame.habitatShop.map { it?.id },
            guestGame.habitatShop.map { it?.id }
        )

        assertEquals(
            ConnectionState.PLAYING_MY_TURN,
            rootServiceHost.networkService.connectionState
        )

        assertEquals(
            ConnectionState.PLAYING_WAITING_FOR_OPPONENT,
            rootServiceGuest.networkService.connectionState
        )

        assertEquals(
            hostGame.habitatDrawPile.peekAll().map { it.id },
            guestGame.habitatDrawPile.peekAll().map { it.id }
        )


        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()
    }

    /**
     * Tests if the connection fails if the wrong server secret is provided.
     */
    @Test
    fun testWrongSecret() {
        val scoringCards = listOf(true, true, true, true, true)
        rootServiceHost = RootService()
        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.hostGame(
                secret = "Fake Secret (Es ist alles eine lüge)",
                name = "kaan",
                sessionID = null,
                scoringCards = scoringCards,
                botSpeed = BotSpeed.SLOW
            )
        }
    }

    /**
     * Test disconnecting after starting the game. State expected to be [ConnectionState.DISCONNECTED]
     * afterwards and the [service.network.NetworkService.client]s must be null.
     */
    @Test
    fun testDisconnect() {
        initConnections()

        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()

        rootServiceHost.waitForState(ConnectionState.DISCONNECTED)
        rootServiceGuest.waitForState(ConnectionState.DISCONNECTED)

        assertNull(rootServiceHost.networkService.client)
        assertNull(rootServiceGuest.networkService.client)
    }

    /**
     * tested ob nachreichten gesendet werden während der status auf dc ist
     */
    @Test
    fun testSendMessagesFailWhenDisconnected() {
        rootServiceHost = RootService()

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.sendGameConfigToOpponent(
                players = listOf("kaan", "metehan"),
                scoringCards = listOf(true, true, true, true, true)
            )
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.sendGameInitToOpponent(
                tileStack = listOf(1, 2, 3, 4),
                scoringCards = listOf(true, true, true, true, true),
                players = listOf(
                    NetPlayer(name = "kaan", startingTileID = 0),
                    NetPlayer(name = "metehan", startingTileID = 1)
                ),
                wildlifeBag = sampleWildlifeBag()
            )
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.sendSelectToOpponent(
                habitatShopIndex = 0, wildlifeShopIndex = 0, usedNatureToken = false
            )
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.sendSelectHabitatTileToOpponent(0)
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.sendSelectWildlifeToOpponent(0)
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.sendRotationToOpponent(1)
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.sendUseNatureTokenToOpponent()
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.sendWipeWildlifeToOpponent(
                usedNatureToken = false, wipedWildlifeIndices = listOf(0, 1, 2)
            )
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.sendPlaceToOpponent(
                habitatCoordinates = Pair(1, 0), wildlifeCoordinates = null, habitatRotation = 0
            )
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.sendChatMessageToOpponent("Was geht?")
        }
    }

    /**
     * Tested ob die nachrichten abgelehnt werden wenn man dc ist
     */
    @Test
    fun testReceiveMessagesFailWhenDisconnected() {
        rootServiceHost = RootService()

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.receiveSelect(
                SelectMessage(wildlifeShopIndex = 0, habitatShopIndex = 0, usedNatureToken = false)
            )
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.receiveSelectHabitatTile(SelectHabitatTileMessage(habitatShopIndex = 0))
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.receiveSelectWildlife(SelectWildlifeMessage(wildlifeShopIndex = 0))
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.receivePlace(
                PlaceMessage(habitatCoordinates = Pair(4, 0), wildlifeCoordinates = null, habitatRotation = 4)
            )
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.receiveRotation(RotationMessage(habitatRotation = 4))
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.receiveUseNatureToken()
        }

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.receiveWipeWildlife(
                WipeWildlifeMessage(
                    usedNatureToken = false,
                    natureTokenAmount = 4,
                    wipedWildlifeIndices = listOf(0, 1, 2, 3),
                    wildlifeBag = sampleWildlifeBag()
                )
            )
        }
    }

    /**
     * Testet ob die nachrichten erstelt werden können wenn der spieler am zug ist
     */
    @Test
    fun testSendMessagesCanBeCreatedWhenPlayersTurn() {
        rootServiceHost = RootService()
        rootServiceHost.networkService.updateConnectionState(ConnectionState.PLAYING_MY_TURN)

        rootServiceHost.networkService.sendGameConfigToOpponent(
            players = listOf("kaan", "metehan"),
            scoringCards = listOf(true, true, true, true, true)
        )

        rootServiceHost.networkService.sendGameInitToOpponent(
            tileStack = listOf(4),
            scoringCards = listOf(true, true, true, true, true),
            players = listOf(
                NetPlayer(name = "kaan", startingTileID = 0),
                NetPlayer(name = "metehan", startingTileID = 1)
            ),
            wildlifeBag = listOf(NetWildlife.BEAR)
        )

        rootServiceHost.networkService.sendSelectToOpponent(
            habitatShopIndex = 0,
            wildlifeShopIndex = 0,
            usedNatureToken = false
        )

        rootServiceHost.networkService.sendSelectHabitatTileToOpponent(0)

        rootServiceHost.networkService.sendSelectWildlifeToOpponent(0)

        rootServiceHost.networkService.sendRotationToOpponent(4)

        rootServiceHost.networkService.sendUseNatureTokenToOpponent()

        rootServiceHost.networkService.sendChatMessageToOpponent("kaan")

        rootServiceHost.networkService.updateConnectionState(ConnectionState.PLAYING_MY_TURN)

        rootServiceHost.networkService.sendPlaceToOpponent(
            habitatCoordinates = Pair(1, 0),
            wildlifeCoordinates = Pair(1, 0),
            habitatRotation = 4
        )

        assertEquals(
            ConnectionState.PLAYING_WAITING_FOR_OPPONENT,
            rootServiceHost.networkService.connectionState
        )
    }

    /**
     * testet ob man die nachrichten erhält
     */
    @Test
    fun testReceiveSelectHabitatTileAndWildlifeSelectsCombination() {
        initConnections()

        rootServiceGuest.networkService.receiveSelectHabitatTile(
            SelectHabitatTileMessage(
                habitatShopIndex = 0
            )
        )

        rootServiceGuest.networkService.receiveSelectWildlife(
            SelectWildlifeMessage(
                wildlifeShopIndex = 0
            )
        )

        val guestGame = rootServiceGuest.currentGame?.currentState
        assertNotNull(guestGame)

        assertEquals(
            GameState.PLACEHABITAT,
            guestGame.gameState
        )

        assertNotNull(guestGame.drawnCombination)

        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()
    }

    /**
     * testet die rotation recieve message funktion
     */
    @Test
    fun testReceiveRotationRotatesSelectedHabitat() {
        initConnections()

        rootServiceGuest.networkService.receiveSelect(
            SelectMessage(
                wildlifeShopIndex = 0,
                habitatShopIndex = 0,
                usedNatureToken = false
            )
        )

        val guestGameBeforeRotation = rootServiceGuest.currentGame?.currentState
        assertNotNull(guestGameBeforeRotation)

        assertEquals(
            GameState.PLACEHABITAT,
            guestGameBeforeRotation.gameState
        )

        rootServiceGuest.networkService.receiveRotation(
            RotationMessage(
                habitatRotation = 4
            )
        )

        val guestGameAfterRotation = rootServiceGuest.currentGame?.currentState
        assertNotNull(guestGameAfterRotation)

        val drawnCombination = guestGameAfterRotation.drawnCombination
        assertNotNull(drawnCombination)

        assertEquals(
            4,
            drawnCombination.first.rotation
        )

        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()
    }

    /**
     * testet ob man die nachrichten erhält und richtig
     */
    @Test
    fun testReceiveChatMessageRefreshesMessage() {
        rootServiceHost = RootService()

        var receivedSender: String? = null
        var receivedMessage: String? = null

        rootServiceHost.networkService.addRefreshable(object : Refreshable {
            override fun refreshAfterChatMessage(sender: String, message: String) {
                receivedSender = sender
                receivedMessage = message
            }
        })

        rootServiceHost.networkService.receiveChatMessage(
            ChatMessage("kaan"),
            "metehan"
        )

        assertEquals("metehan", receivedSender)
        assertEquals("kaan", receivedMessage)
    }
    /**
     * testet ob man die nachrichten erhält und richtig
     */
    @Test
    fun testReceiveSelectChoosesCombination() {
        initConnections()

        rootServiceGuest.networkService.receiveSelect(
            SelectMessage(
                wildlifeShopIndex = 0,
                habitatShopIndex = 0,
                usedNatureToken = false
            )
        )

        val guestGame = rootServiceGuest.currentGame?.currentState
        assertNotNull(guestGame)

        assertEquals(GameState.PLACEHABITAT, guestGame.gameState)
        assertNotNull(guestGame.drawnCombination)

        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()
    }
    /**
     * testet ob man die nachrichten erhält und richtig
     */
    @Test
    fun testReceiveSelectHabitatTileAndWildlifeChoosesCombination() {
        initConnections()

        rootServiceGuest.networkService.receiveSelectHabitatTile(
            SelectHabitatTileMessage(
                habitatShopIndex = 0
            )
        )

        rootServiceGuest.networkService.receiveSelectWildlife(
            SelectWildlifeMessage(
                wildlifeShopIndex = 0
            )
        )

        val guestGame = rootServiceGuest.currentGame?.currentState
        assertNotNull(guestGame)

        assertEquals(GameState.PLACEHABITAT, guestGame.gameState)
        assertNotNull(guestGame.drawnCombination)

        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()
    }

    /**
     * testet ob man die nachrichten erhält und richtig
     */
    @Test
    fun testReceivePlaceWithNullWildlifeWaitsForWipeMessage() {
        initConnections()

        rootServiceGuest.networkService.receiveSelect(
            SelectMessage(
                wildlifeShopIndex = 0,
                habitatShopIndex = 0,
                usedNatureToken = false
            )
        )

        rootServiceGuest.networkService.receivePlace(
            PlaceMessage(
                habitatCoordinates = Pair(1, 0),
                wildlifeCoordinates = null,
                habitatRotation = 0
            )
        )

        val guestGame = rootServiceGuest.currentGame?.currentState
        assertNotNull(guestGame)

        assertTrue(
            guestGame.players[0].board.containsKey(Pair(1, 0))
        )

        assertTrue(rootServiceGuest.networkService.waitingForWipeAfterDiscard)

        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()
    }
    /**
     * testet ob man die nachrichten erhält und richtig
     */
    @Test
    fun testSendPlaceWithNullWildlifeDoesNotChangeToWaitingState() {
        rootServiceHost = RootService()

        rootServiceHost.networkService.updateConnectionState(ConnectionState.PLAYING_MY_TURN)

        rootServiceHost.networkService.sendPlaceToOpponent(
            habitatCoordinates = Pair(1, 0),
            wildlifeCoordinates = null,
            habitatRotation = 4
        )

        assertEquals(
            ConnectionState.PLAYING_MY_TURN,
            rootServiceHost.networkService.connectionState
        )
    }
    /**
     * testet ob man die nachrichten erhält und richtig und ob es dann zu watiting state wechselt
     */
    @Test
    fun testSendWipeWildlifeWithEmptyIndicesChangesToWaitingState() {
        rootServiceHost = RootService()

        rootServiceHost.gameActionService.startNewGame(
            playerNames = listOf("kaan", "metehan"),
            isNetworkGame = true,
            scoringCardTypes = listOf(true, true, true, true, true),
            simulationSpeed = BotSpeed.SLOW
        )

        rootServiceHost.gameActionService.startTurn()
        rootServiceHost.networkService.updateConnectionState(ConnectionState.PLAYING_MY_TURN)

        rootServiceHost.networkService.sendWipeWildlifeToOpponent(
            usedNatureToken = false,
            wipedWildlifeIndices = emptyList()
        )

        assertEquals(
            ConnectionState.PLAYING_WAITING_FOR_OPPONENT,
            rootServiceHost.networkService.connectionState
        )
    }
    /**
     * testet ob man die nachrichten erhält und richtig und ob der turn endet
     */
    @Test
    fun testReceivePlaceWithNullWildlifeAndWipeMessageEndsTurn() {
        initConnections()

        rootServiceGuest.networkService.receiveSelect(
            SelectMessage(
                wildlifeShopIndex = 0,
                habitatShopIndex = 0,
                usedNatureToken = false
            )
        )

        rootServiceGuest.networkService.receivePlace(
            PlaceMessage(
                habitatCoordinates = Pair(1, 0),
                wildlifeCoordinates = null,
                habitatRotation = 0
            )
        )

        assertTrue(rootServiceGuest.networkService.waitingForWipeAfterDiscard)

        val guestGameBeforeWipe = rootServiceGuest.currentGame?.currentState
        assertNotNull(guestGameBeforeWipe)

        val wildlifeBag = guestGameBeforeWipe.wildlifeBag.peekAll().map {
            NetWildlife.valueOf(it.animal.name)
        }

        rootServiceGuest.networkService.receiveWipeWildlife(
            WipeWildlifeMessage(
                usedNatureToken = false,
                natureTokenAmount = 0,
                wipedWildlifeIndices = emptyList(),
                wildlifeBag = wildlifeBag
            )
        )

        val guestGameAfterWipe = rootServiceGuest.currentGame?.currentState
        assertNotNull(guestGameAfterWipe)

        assertFalse(rootServiceGuest.networkService.waitingForWipeAfterDiscard)

        assertEquals(
            "metehan",
            guestGameAfterWipe.players[guestGameAfterWipe.currentPlayer].name
        )

        assertEquals(
            ConnectionState.PLAYING_MY_TURN,
            rootServiceGuest.networkService.connectionState
        )

        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()
    }

    /**
     * tested was passiert wenn der host die falsche scoringcard anzahl hat
     */
    @Test
    fun testHostGameFailWithWrongScoringCardAmount() {
        rootServiceHost = RootService()

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.hostGame(
                secret = NETWORK_SECRET,
                name = "kaan",
                sessionID = null,
                scoringCards = listOf(true, true, true, true),
                botSpeed = BotSpeed.SLOW
            )
        }
    }

    /**
     * testet was passiert wenn man den player im falschen state einsetzt
     */
    @Test
    fun testAddJoinedPlayerFailsWhenNotWaitingForGuest() {
        rootServiceHost = RootService()

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.addJoinedPlayer("metehan")
        }
    }

    /**
     * Testet ob alle rollen auch wirkklich zu den player passen
     */
    @Test
    fun testGetLobbyPlayersReturnsHostGuestAndBots() {
        rootServiceHost = RootService()

        rootServiceHost.networkService.hostPlayerName = "kaan"
        rootServiceHost.networkService.selectedBotPlayers = listOf("kame", "cifer")
        rootServiceHost.networkService.updateConnectionState(ConnectionState.WAITING_FOR_GUEST)

        rootServiceHost.networkService.addJoinedPlayer("metehan")

        assertEquals(
            listOf("kaan", "metehan", "kame", "cifer"),
            rootServiceHost.networkService.getLobbyPlayers()
        )
    }

    /**
     * testet was passiert wenn man ein game ohne lobby macht
     */
    @Test
    fun testStartNewHostedGameFailsWithoutLobby() {
        rootServiceHost = RootService()

        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.startNewHostedGame()
        }
    }

    /**
     * testet was in dern init passiert ohne init
     */
    @Test
    fun testStartNewJoinedGameFailsWhenNotWaitingForInit() {
        rootServiceGuest = RootService()

        val message = GameInitMessage(
            tileStack = listOf(4, 5, 6, 7),
            scoringCards = listOf(true, true, true, true, true),
            players = listOf(
                NetPlayer(name = "kaan", startingTileID = 0),
                NetPlayer(name = "metehan", startingTileID = 1)
            ),
            wildlifeBag = listOf(
                NetWildlife.BEAR,
                NetWildlife.ELK,
                NetWildlife.FOX,
                NetWildlife.HAWK
            )
        )

        assertFailsWith<IllegalStateException> {
            rootServiceGuest.networkService.startNewJoinedGame(
                message = message,
                playerName = "metehan"
            )
        }
    }


    /**
     * testet ob man die nachrichten erhält und richtig
     *
     * aber durch CascadiaNetworkClient
     */
    @Test
    fun testPlaceMessageWithWildlifeIsTransferredThroughCascadiaNetworkClient() {
        initConnections()

        rootServiceHost.networkService.sendSelectToOpponent(
            habitatShopIndex = 0,
            wildlifeShopIndex = 0,
            usedNatureToken = false
        )

        Thread.sleep(500)

        rootServiceHost.networkService.sendPlaceToOpponent(
            habitatCoordinates = Pair(1, 0),
            wildlifeCoordinates = Pair(1, 0),
            habitatRotation = 0
        )

        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        val guestGame = rootServiceGuest.currentGame?.currentState
        assertNotNull(guestGame)

        val placedTile = guestGame.players[0].board[Pair(1, 0)]
        assertNotNull(placedTile)
        assertNotNull(placedTile.placedWildlifeToken)

        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()
    }

    /**
     * testet ob man die nachrichten erhält und richtig
     *
     * aber durch CascadiaNetworkClient
     */

    @Test
    fun testSelectMessageIsTransferredThroughCascadiaNetworkClient() {
        initConnections()

        rootServiceHost.networkService.sendSelectToOpponent(
            habitatShopIndex = 0,
            wildlifeShopIndex = 0,
            usedNatureToken = false
        )

        Thread.sleep(500)

        val guestGame = rootServiceGuest.currentGame?.currentState
        assertNotNull(guestGame)

        assertEquals(GameState.PLACEHABITAT, guestGame.gameState)
        assertNotNull(guestGame.drawnCombination)

        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()
    }

       //endregion

    private fun sampleWildlifeBag() =
        listOf(NetWildlife.BEAR, NetWildlife.ELK, NetWildlife.FOX, NetWildlife.HAWK)

    private fun assertBoardsMatch(hostGame: CascadiaGame, guestGame: CascadiaGame, playerIndex: Int) {
        val hostBoard = hostGame.players[playerIndex].board
        val guestBoard = guestGame.players[playerIndex].board

        assertEquals(hostBoard.mapValues { it.value.id }, guestBoard.mapValues { it.value.id })
        assertEquals(hostBoard.mapValues { it.value.rotation }, guestBoard.mapValues { it.value.rotation })
        assertEquals(
            hostBoard.mapValues { it.value.placedWildlifeToken?.animal },
            guestBoard.mapValues { it.value.placedWildlifeToken?.animal }
        )
    }

    /**
     * Busy waiting for the game represented by this [RootService] to reach the desired network.
     * Polls the desired state every 100 ms until the  is reached.
     *
     * @throws IllegalStateException if desired state is not reached within the
     */
    private fun RootService.waitForState(state: ConnectionState, timeout: Int = 20000) {
        var timePassed = 0
        while (timePassed < timeout) {
            if (networkService.connectionState == state)
                return
            else {
                Thread.sleep(100)
                timePassed += 100
            }
        }
        error("Did not arrive at state $state after waiting $timeout ms")
    }

    /**
     * Holds the secret used to connect to the SoPra BGW-Net server.
     */
    companion object {
        const val NETWORK_SECRET = "wildlife"
    }
}