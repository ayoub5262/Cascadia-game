package service

import entity.*
import tools.aqua.bgw.util.Stack
import java.io.*
import java.time.LocalDateTime
import edu.udo.cs.sopra.ntf.NetPlayer
import service.network.sendWipeWildlifeToOpponent

/**
 * Class for handling all game intern logic.
 */
class GameActionService(internal val rootService: RootService) : AbstractRefreshingService() {

    /**
     * Restocks the shop, adds a new instance of [entity.CascadiaGame] with the same properties as the last to
     * [entity.CascadiaGameSave.history] and changes [entity.CascadiaGameSave.currentState] to the new game state.
     */
    fun startTurn() {
        // Assert that the game is active
        val game = checkNotNull(rootService.currentGame?.currentState) { "No active game found." }

        // Ensure the gameState is correct before proceeding
        check(game.gameState == GameState.TURNSTARTED) {
            "Cannot start a new turn. Expected state TURNSTARTED, but was ${game.gameState}."
        }

        // Reset turn-specific flags for the current player's new turn
        game.natureTokenUsed = false
        game.threeWildlifeIdentical = false
        game.drawnCombination = null
        game.threeRedrawnInTurn = false


        /*
        moin aus dem netzwerk wieder mal folgendes im network darf man nicht einfahc overpopulation checken
        muss das raus nehmen für mich und umändern sodass es nur local klappt
        ~kaan~
         */

        if (game.isHotseatMode) {
            checkOverpopulation()
        } else {
            val currentPlayer = game.players[game.currentPlayer]
            val localPlayerName =
                rootService.networkService.localPlayerName

            if (currentPlayer.name == localPlayerName) {
                checkOverpopulation()
            }
        }


        logTurn()
        val activeGame = checkNotNull(rootService.currentGame?.currentState)

        // Update the game state
        activeGame.gameState = GameState.CHOOSEPAIR
        onAllRefreshables { refreshAfterStartTurn() }

        //botTurn should be called properly -Finn
        val currentPlayerObject = activeGame.players[activeGame.currentPlayer]

        // Kurzes update network muss wissen wessen bot spielen darf also vom guest oder host
        // deswegen habe ich bot turn bissl verfeinert
        if (currentPlayerObject.playMode != PlayerType.HUMAN) {
            if (!activeGame.isHotseatMode) {
                rootService.networkService.runBotTurnIfResponsible()
            }
        }
    }

    internal fun endTurn() {
        val game = checkNotNull(rootService.currentGame?.currentState)

        game.natureTokenUsed = false

        game.currentPlayer = (game.currentPlayer + 1) % game.players.size
        if (game.currentPlayer == 0) {
            if (game.roundNumber == 20) {
                endGame()
                return
            }
            game.roundNumber++
        }

        restockShop()
        game.gameState = GameState.TURNSTARTED
        onAllRefreshables {
            refreshAfterEndTurn()
        }
        startTurn()
    }


    /**
     * Starts a new Cascadia game with the given players and game settings.
     *
     * Creates a new Cascadia instance using the provided player names,
     * initializes the selected scoring card types (A or B), and configures whether the
     * game is played locally or as a network game (isNetworkGame). The game state is prepared
     * for the first round and all participating players are initialized.
     *
     * After the game has been created, the game refreshes with 'refreshAfterGameStart'.
     *
     * @param playerNames The names of all players in the game.
     * @param isNetworkGame Indicates if the game is played as a network game or locally.
     * @param scoringCardTypes A list of booleans specifying which scoring card (A or B)
     */
    fun startNewGame(
        playerNames: List<String>,
        isNetworkGame: Boolean,
        scoringCardTypes: List<Boolean>,
        simulationSpeed: BotSpeed
    ) {
        //I added simulationSpeed param -Finn
        requireValidSetup(playerNames, scoringCardTypes)

        //Map players and deal start tiles
        val startTileSets = loadStartTileSets().shuffled()
        val players = playerNames.mapIndexed { index, name ->
            createPlayer(name, startTileSets[index])
        }.toMutableList()

        //Prepare the Habitat Deck
        val habitatDrawPile = createHabitatTiles()
        trimHabitatDeck(habitatDrawPile, players.size)

        // Prepare the Wildlife Bag
        val wildlifeBag = createWildlifeBag()

        // Populate the Shops (Display)
        val (habitatShop, wildlifeShop) = dealShops(habitatDrawPile, wildlifeBag)

        println("HOST habitatShop = " + habitatShop.map { it?.id })
        println("HOST wildlifeShop = " + wildlifeShop.map { it?.animal })

        // creating the Scoring Cards
        val scoringCards = createScoringCards(scoringCardTypes)

        //Instantiate the new CascadiaGame
        //I added simulationSpeed as input -Finn
        val newGame = CascadiaGame(players, simulationSpeed).apply {
            this.isHotseatMode = !isNetworkGame
            this.currentPlayer = 0
            this.roundNumber = 1
            this.natureTokenCount = 25
            this.habitatShop = habitatShop
            this.wildlifeShop = wildlifeShop
            this.wildlifeBag = wildlifeBag
            this.habitatDrawPile = habitatDrawPile
            this.scoringCards = scoringCards
            this.gameState = GameState.TURNSTARTED
        }

        //Assign to rootService and validate starting display state
        rootService.currentGame = CascadiaGameSave().apply {
            this.currentState = newGame
        }

        // Check if the initial 4 tokens in the shop trigger an immediate Overpopulation check
        checkOverpopulation()
        onAllRefreshables { refreshAfterStartNewGame() }
    }

    /**
     *
     * Kurze info das ist 1zu1 start new game von oben mit einem kleinen unterschied network muss das game
     * initialisieren dardruch kann ich das normale start new game nciht nutzen da es ansonsten dazu führt dass
     * jeder player einen anderen drawpile hat bag hat usw deswegen müssen wir das vom network verwenden
     * aber zu 99% ist das der alte code nur init wird vom network emssage übernommen
     *
     *
     * Starts a new Cascadia game with the given players and game settings.
     *
     * Creates a new Cascadia instance using the provided player names,
     * initializes the selected scoring card types (A or B), and configures whether the
     * game is played locally or as a network game (isNetworkGame). The game state is prepared
     * for the first round and all participating players are initialized.
     *
     * After the game has been created, the game refreshes with 'refreshAfterGameStart'.
     *
     * @param scoringCardTypes A list of booleans specifying which scoring card (A or B)
     */

    fun startNewNetworkGameFromInitMessage(
        playersFromMessage: List<NetPlayer>,
        scoringCardTypes: List<Boolean>,
        tileStackIds: List<Int>,
        wildlifeBagTypes: List<Wildlife>,
        simulationSpeed: BotSpeed
    ) {
        requireValidSetup(playersFromMessage.map { it.name }, scoringCardTypes)

        // Map players and deal start tiles
        val startTileSets = loadStartTileSets()
        val players = playersFromMessage.map { netPlayer ->
            createPlayer(netPlayer.name, startSetForTileId(startTileSets, netPlayer.startingTileID))
        }.toMutableList()

        // Prepare the Habitat Deck from network message.
        // NICHT createHabitatTiles() NUTZEN
        val allTilesById = parseTilesCsv("/tiles.csv").associateBy { it.id }
        val habitatDrawPile = Stack<HabitatTile>()

        for (tileId in tileStackIds.reversed()) {
            habitatDrawPile.push(
                checkNotNull(allTilesById[tileId])
            )
        }

        // Prepare the Wildlife Bag from network message.
        // NICHT  createWildlifeBag() NUTZEN
        val wildlifeBag = Stack<WildlifeToken>()

        for (wildlife in wildlifeBagTypes.reversed()) {
            wildlifeBag.push(WildlifeToken(wildlife))
        }

        // Populate the Shops (Display)
        val (habitatShop, wildlifeShop) = dealShops(habitatDrawPile, wildlifeBag)

        println("GUEST habitatDrawPile (bottom->top): " + habitatDrawPile.peekAll().reversed().map { it.id })
        println("GUEST wildlifeBag (bottom->top): " + wildlifeBag.peekAll().reversed().map { it.animal })

        // creating the Scoring Cards
        val scoringCards = createScoringCards(scoringCardTypes)

        // Instantiate the new CascadiaGame
        val newGame = CascadiaGame(players, simulationSpeed).apply {
            this.isHotseatMode = false
            this.currentPlayer = 0
            this.roundNumber = 1
            this.natureTokenCount = 25
            this.habitatShop = habitatShop
            this.wildlifeShop = wildlifeShop
            this.wildlifeBag = wildlifeBag
            this.habitatDrawPile = habitatDrawPile
            this.scoringCards = scoringCards
            this.gameState = GameState.CHOOSEPAIR
        }

        // Assign to rootService and validate starting display state
        rootService.currentGame = CascadiaGameSave().apply {
            this.currentState = newGame
        }

        // Check if the initial 4 tokens in the shop trigger an immediate Overpopulation check
        //checkOverpopulation()
        onAllRefreshables { refreshAfterStartNewGame() }
    }

// Hilfsfunktionen direkt darunter in derselben Datei:

    private fun requireValidSetup(playerNames: List<String>, scoringCardTypes: List<Boolean>) {
        require(playerNames.size in 2..4) { "A Cascadia game requires between 2 and 4 players." }
        require(playerNames.distinct().size == playerNames.size) { "All player names must be unique." }
        require(scoringCardTypes.size == 5) {
            "Exactly 5 scoring card types must be provided (one for each animal variant)."
        }
    }

    private fun loadStartTileSets(): List<List<HabitatTile>> {
        val startTileSets = parseTilesCsv("/start_tiles.csv").chunked(3)
        check(startTileSets.size == 5) { "Expected exactly 5 start tile sets, but found ${startTileSets.size}." }
        return startTileSets
    }

    private fun startSetForTileId(startTileSets: List<List<HabitatTile>>, startingTileId: Int): List<HabitatTile> {
        val startSet = startTileSets.find { tileSet ->
            tileSet.firstOrNull()?.id == startingTileId
        }

        checkNotNull(startSet) {
            "Starting tile id ist falsch lo: $startingTileId. " +
                    "starting tile ids: ${startTileSets.map { it.first().id }}"
        }

        return startSet
    }

    private fun createPlayer(name: String, startSet: List<HabitatTile>): Player {
        check(startSet.size == 3) { "Each start tile set must contain exactly 3 tiles." }

        //allow unique names like "RANDOMBOT1", "STRONGBOT 2"
        val type = when {
            name.startsWith("RANDOMBOT") -> PlayerType.RANDOMBOT
            name.startsWith("STRONGBOT") -> PlayerType.STRONGBOT
            else -> PlayerType.HUMAN
        }

        val playerObj = Player(
            name = name,
            playMode = type,
            natureTokens = 0
        )

        // Place the 3 starting tiles next to each other using axial coordinates (q, r).
        playerObj.board[Pair(0, 0)] = startSet[0]
        playerObj.board[Pair(0, 1)] = startSet[1]
        playerObj.board[Pair(-1, 1)] = startSet[2]

        return playerObj
    }

    internal fun <T> removeBottomEntry(stack: Stack<T>): T {

        // der jetzige stack wird neu gemacht
        val temp = Stack<T>()

        // solagen poppen bis alle einträge im temp sind
        while (stack.size > 1) {
            temp.push(stack.pop())
        }
        // der letzte eintrag also last wird dann als result gespeichert
        val result = stack.pop()

        //dannach wird alles wieder in den stack geladen
        while (temp.isNotEmpty()) {
            stack.push(temp.pop())
        }
        // hier wird last ausgegeben
        return result
    }


    private fun dealShops(
        habitatDrawPile: Stack<HabitatTile>,
        wildlifeBag: Stack<WildlifeToken>
    ): Pair<Array<HabitatTile?>, Array<WildlifeToken?>> {
        val habitatShop = arrayOfNulls<HabitatTile>(4)
        val wildlifeShop = arrayOfNulls<WildlifeToken>(4)
        check(habitatDrawPile.size >= 4) { "Not enough habitat tiles left to deal to the shop." }
        check(wildlifeBag.size >= 4) { "Not enough wildlife tokens left to deal to the shop." }

        for (index in 0..3) {
            habitatShop[index] = removeBottomEntry(habitatDrawPile)
            wildlifeShop[index] = removeBottomEntry(wildlifeBag)
        }

        return Pair(habitatShop, wildlifeShop)
    }

    private fun trimHabitatDeck(habitatDrawPile: Stack<HabitatTile>, playerSize: Int) {
        val requiredDeckSize = (playerSize * 20) + 3
        check(habitatDrawPile.size >= requiredDeckSize) {
            "Base deck doesn't have enough tiles. Needed: $requiredDeckSize."
        }
        while (habitatDrawPile.size > requiredDeckSize) {
            habitatDrawPile.pop()
        }
    }

    private fun createScoringCards(scoringCardTypes: List<Boolean>): MutableList<ScoringCard> {
        val scoringCards = mutableListOf<ScoringCard>()
        for (i in 0..4) {
            val wildlife = Wildlife.entries[i]
            scoringCards.add(ScoringCard(scoringCardTypes[i], wildlife))
        }
        return scoringCards
    }

    private fun createWildlifeBag(): Stack<WildlifeToken> {
        val wildlifeBag = Stack<WildlifeToken>()
        for (animal in Wildlife.entries) {
            repeat(20) { wildlifeBag.push(WildlifeToken(animal)) }
        }
        wildlifeBag.shuffle()
        return wildlifeBag
    }

    private fun endGame() {
        val game = checkNotNull(rootService.currentGame?.currentState)
        game.currentPlayer = 0


        val scores = calculateScore()

        onAllRefreshables {
            refreshAfterEndGame(scores)
        }
    }


    private fun createHabitatTiles(): Stack<HabitatTile> {
        val allTiles = parseTilesCsv("/tiles.csv")

        // Shuffle and push to Stack
        val shuffledTiles = allTiles.shuffled()
        val tileStack = Stack<HabitatTile>()

        for (tile in shuffledTiles) {
            tileStack.push(tile)
        }

        onAllRefreshables { refreshAfterCreateHabitatTiles() }
        return tileStack
    }


    private fun parseTilesCsv(fileName: String): List<HabitatTile> {
        val inputStream = this::class.java.getResourceAsStream(fileName)
        checkNotNull(inputStream) { "Could not find resource '$fileName' on the classpath." }

        val lines = inputStream.bufferedReader().readLines()
        check(lines.isNotEmpty()) { "The CSV file $fileName is empty." }

        return lines.drop(1)
            .filter { it.isNotBlank() }
            .mapNotNull { parseLineToTile(it) }
    }

    private fun parseLineToTile(line: String): HabitatTile? {
        val parts = line.split(";")
        if (parts.size != 4) return null

        val id = parts[0].toIntOrNull()
        if (id == null) return null

        val habitatsString = parts[1]
        val wildlifeString = parts[2]
        val keystoneString = parts[3]

        val habitats = parseHabitats(habitatsString)
        val possibleAnimals = parseWildlife(wildlifeString)
        val isKeyStone = parseKeystone(keystoneString)

        return HabitatTile(
            habitats = habitats.toMutableList(),
            possibleAnimals = possibleAnimals
        ).apply {
            this.id = id
            this.isKeyStone = isKeyStone
            this.rotation = 0
        }
    }

    private fun parseHabitats(habitatsString: String): List<Habitat> {
        require(habitatsString.length == 6) { "Habitats string must be exactly 6 characters long: '$habitatsString'" }
        val parsed = habitatsString.map { char ->
            when (char) {
                'M' -> Habitat.MOUNTAINS
                'R' -> Habitat.RIVERS
                'F' -> Habitat.FORESTS
                'W' -> Habitat.WETLANDS
                'P' -> Habitat.PRAIRIES
                else -> error("Unknown habitat char: $char")
            }
        }
        return parsed.drop(0) + parsed.take(0)
    }

    private fun parseWildlife(wildlifeString: String): List<Wildlife> {
        require(wildlifeString.length in 1..3) {
            "Wildlife string must be between 1 and 3 characters long: '$wildlifeString'"
        }
        return wildlifeString.map { char ->
            when (char) {
                'B' -> Wildlife.BEAR
                'F' -> Wildlife.FOX
                'H' -> Wildlife.HAWK
                'S' -> Wildlife.SALMON
                'E' -> Wildlife.ELK
                else -> error("Unknown wildlife character '$char' found in string: '$wildlifeString'")
            }
        }
    }

    private fun parseKeystone(keystoneString: String): Boolean {
        return when (keystoneString.trim().lowercase()) {
            "yes" -> true
            "no" -> false
            else -> error("Keystone column must be 'yes' or 'no', but was: '$keystoneString'")
        }
    }


    /**
     * Saves the current Cascadia game state.
     *
     * This includes all information required to continue the game later,
     * such as the placed habitat tiles, wildlife tokens, nature tokens,
     * player score, remaining tiles and tokens, and any other relevant
     * game data.
     */
    fun saveGame() {

        if (rootService.currentGame == null) {
            return
        }

        val game = checkNotNull(rootService.currentGame?.currentState)

        if (!game.isHotseatMode) {
            return
        }

        val now = LocalDateTime.now()

        val fileName = "savegame_" +
                now.year + "-" +
                now.monthValue + "-" +
                now.dayOfMonth + "_" +
                now.hour + "-" +
                now.minute + "-" +
                now.second +
                ".dat"

        val output = ObjectOutputStream(FileOutputStream(fileName))

        output.writeObject(rootService.currentGame)

        output.close()

        onAllRefreshables { refreshAfterSaveGame() }
    }

    /**
     * Quits the currently active Cascadia game, discarding all of its state.
     */
    fun quitGame() {
        rootService.currentGame = null
    }

    /**
     * Loads a previously saved Cascadia game state.
     *
     * Restores all game data required to continue the game, including
     * placed habitat tiles, wildlife tokens, nature tokens, player score,
     * remaining tiles and tokens, and any other relevant game information.
     *
     * @throws IllegalStateException if no valid saved game is available or the game state cannot be restored.
     */
    fun loadGame(fileName: String) {

        val file = File(fileName)

        if (!file.exists()) {
            return
        }

        val input = ObjectInputStream(FileInputStream(file))

        rootService.currentGame = input.readObject() as CascadiaGameSave

        input.close()

        onAllRefreshables {
            refreshAfterLoadGame()
        }
    }

    internal fun checkOverpopulation() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game) { "No game is currently active" }
        var bearCount = 0
        var foxCount = 0
        var hawkCount = 0
        var salmonCount = 0
        var elkCount = 0

        for (token in game.wildlifeShop) {
            if (token != null) {
                when (token.animal) {
                    Wildlife.BEAR -> bearCount++
                    Wildlife.FOX -> foxCount++
                    Wildlife.HAWK -> hawkCount++
                    Wildlife.SALMON -> salmonCount++
                    Wildlife.ELK -> elkCount++
                }
            }
        }
        val count = maxOf(bearCount, foxCount, hawkCount, salmonCount, elkCount)

        /*
        moin kaan noch mal folgendes wenn overpopulation so ist passiert folgendes:
        das network weiss was overpopulated ist aber nicht wo und wenn es später ersetzt wird wird es desynct
        bspw der vorherige code wusste count == 4 aber nicht welches animal und wo
         */
        var overpopulatedAnimal: Wildlife? = null

        if (bearCount == 4) {
            overpopulatedAnimal = Wildlife.BEAR
        } else if (foxCount == 4) {
            overpopulatedAnimal = Wildlife.FOX
        } else if (hawkCount == 4) {
            overpopulatedAnimal = Wildlife.HAWK
        } else if (salmonCount == 4) {
            overpopulatedAnimal = Wildlife.SALMON
        } else if (elkCount == 4) {
            overpopulatedAnimal = Wildlife.ELK
        }

        val wipedIndices = resolveOverpopulation(game, count, overpopulatedAnimal)

        /*
        das ist die alte version wenn game stirb das nehmen untere raus nehmen
        ~kaan~
            if (game.wildlifeShop.all { it != null }) {
            game.wildlifeBag.pushAll(game.discardedWildlife)
            game.discardedWildlife.clear()
            game.wildlifeBag.shuffle()
        }
         */
        if (game.wildlifeShop.all { it != null } && game.discardedWildlife.isNotEmpty()) {
            game.wildlifeBag.pushAll(game.discardedWildlife)
            game.discardedWildlife.clear()
            game.wildlifeBag.shuffle()
        }

        // muss nach neuen ntf regeln eine nachricht senden
        if (!game.isHotseatMode && wipedIndices.isNotEmpty()) {
            rootService.networkService.sendWipeWildlifeToOpponent(
                usedNatureToken = false,
                wipedWildlifeIndices = wipedIndices
            )
        }
    }

    // ist eine hilfsfunktion damit die warnings weg sind also die warnings vom checkoverpopulation

    private fun resolveOverpopulation(game: CascadiaGame, count: Int, overpopulatedAnimal: Wildlife?): List<Int> {
        val wipedIndices = mutableListOf<Int>()

        if (count == 3) {
            game.threeWildlifeIdentical = true

            onAllRefreshables {
                refreshAfterCheckOverpopulation()
            }
        } else if (count == 4 && overpopulatedAnimal != null) {
            for (index in game.wildlifeShop.indices) {
                val wildlifeToken = game.wildlifeShop[index]

                if (wildlifeToken?.animal == overpopulatedAnimal) {
                    wipedIndices.add(index)
                    game.discardedWildlife.add(wildlifeToken)
                    game.wildlifeShop[index] = null
                }
            }

            restockShop()
        }

        return wipedIndices
    }

    internal fun logTurn() {

        val game = checkNotNull(rootService.currentGame)

        val aktuellerStand = game.currentState
        if (aktuellerStand != null) {
            val kopieFuerHistory = aktuellerStand.deepCopyState()
            game.history.push(kopieFuerHistory)
        }

        val obersteInHistory = game.history.peek()
        if (obersteInHistory != null) {
            game.currentState = obersteInHistory.deepCopyState()
        }

        game.undoes.clear()

        onAllRefreshables {
            refreshAfterLogTurn()
        }
    }

    private fun restockShop() {
        val game = rootService.currentGame?.currentState
        checkNotNull(game) { "No game is currently active" }

        println("RESTOCK habitatDrawPile (bottom->top): " + game.habitatDrawPile.peekAll().reversed().map { it.id })
        println("RESTOCK wildlifeBag (bottom->top): " + game.wildlifeBag.peekAll().reversed().map { it.animal })

        for (index in game.wildlifeShop.indices.reversed()) {
            if (game.wildlifeShop[index] == null) {
                if (game.wildlifeBag.isEmpty()) {
                    endGame()
                    return
                }

                game.wildlifeShop[index] = removeBottomEntry(game.wildlifeBag)
            }
        }

        for (index in game.habitatShop.indices.reversed()) {
            if (game.habitatShop[index] == null) {
                if (game.habitatDrawPile.isEmpty()) {
                    endGame()
                    return
                }

                game.habitatShop[index] = removeBottomEntry(game.habitatDrawPile)
            }
        }

        if (game.isHotseatMode) {
            checkOverpopulation()
        }

        onAllRefreshables {
            refreshAfterRestockShops()
        }
    }


    private fun CascadiaGame.deepCopyState(): CascadiaGame {

        val kopierteSpieler = mutableListOf<Player>()
        for (spieler in this.players) {
            kopierteSpieler.add(spieler.deepCopy())
        }

        val kopie = CascadiaGame(
            players = kopierteSpieler,
            simulationSpeed = this.simulationSpeed
        )

        kopie.isHotseatMode = this.isHotseatMode
        kopie.currentPlayer = this.currentPlayer
        kopie.roundNumber = this.roundNumber
        kopie.natureTokenUsed = this.natureTokenUsed
        kopie.natureTokenCount = this.natureTokenCount
        kopie.threeWildlifeIdentical = this.threeWildlifeIdentical
        kopie.gameState = this.gameState

        val combination = this.drawnCombination

        if (combination != null) {
            val tile = combination.first
            val token = combination.second
            kopie.drawnCombination = Pair(tile.deepCopy(), token.copy())
        } else {
            kopie.drawnCombination = null
        }

        val kopierteAbgelegteTiere = mutableListOf<WildlifeToken>()
        for (token in this.discardedWildlife) {
            kopierteAbgelegteTiere.add(token.copy())
        }
        kopie.discardedWildlife = kopierteAbgelegteTiere

        val kopierterWildlifeShop = arrayOfNulls<WildlifeToken?>(this.wildlifeShop.size)
        for (i in this.wildlifeShop.indices) {
            val token = this.wildlifeShop[i]
            if (token != null) {
                kopierterWildlifeShop[i] = token.copy()
            }
        }
        kopie.wildlifeShop = kopierterWildlifeShop

        val kopierterHabitatShop = arrayOfNulls<HabitatTile?>(this.habitatShop.size)
        for (i in this.habitatShop.indices) {
            val tile = this.habitatShop[i]
            if (tile != null) {
                kopierterHabitatShop[i] = tile.deepCopy()
            }
        }
        kopie.habitatShop = kopierterHabitatShop

        kopie.wildlifeBag = this.wildlifeBag.deepCopy()
        kopie.habitatDrawPile = this.habitatDrawPile.deepCopy()

        val kopierteScoringCards = mutableListOf<ScoringCard>()
        for (card in this.scoringCards) {
            kopierteScoringCards.add(card.copy())
        }
        kopie.scoringCards = kopierteScoringCards

        return kopie
    }

    private fun Player.deepCopy(): Player {

        val kopie = Player(
            name = this.name,
            playMode = this.playMode,
            natureTokens = this.natureTokens
        )

        for ((position, tile) in this.board) {
            kopie.board[position] = tile.deepCopy()
        }

        return kopie
    }

    private fun HabitatTile.deepCopy(): HabitatTile {

        val kopie = HabitatTile(
            habitats = this.habitats.toMutableList(),
            possibleAnimals = this.possibleAnimals.toList()
        )

        kopie.id = this.id
        kopie.isKeyStone = this.isKeyStone
        kopie.rotation = this.rotation

        val placedToken = this.placedWildlifeToken

        if (placedToken != null) {
            kopie.placedWildlifeToken = placedToken.copy()
        } else {
            kopie.placedWildlifeToken = null
        }

        return kopie
    }

    private fun <T> Stack<T>.deepCopy(): Stack<T> {

        val elemente = mutableListOf<T>()
        while (!this.isEmpty()) {
            elemente.add(this.pop())
        }

        val kopie = Stack<T>()
        for (i in elemente.indices.reversed()) {
            this.push(elemente[i])
            kopie.push(elemente[i])
        }

        return kopie
    }
}