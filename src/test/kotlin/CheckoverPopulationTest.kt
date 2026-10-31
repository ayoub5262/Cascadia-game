package service

import entity.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.aqua.bgw.util.Stack
import kotlin.test.BeforeTest

/**
 * This class tests the functionality of the [CheckoverPopulationTest] class.
 */
    class CheckoverPopulationTest {


        /**
         * This refreshable is initialized in the [setUp] function hence it is a late-initialized property.
         */
        private lateinit var rootService: RootService

        /**
         * This abstractRefreshingService is initialized in the [setUp] function hence it is a late-initialized property.
         */
        private lateinit var game: CascadiaGame

        /**
         * Initialize service to set up the test environment. This function is executed before every test.
         */
        @BeforeTest
        fun setUp() {
            rootService = RootService()

            val player = Player(
                name = "KameCifer",
                playMode = PlayerType.HUMAN,
                natureTokens = 0
            )

            game = CascadiaGame(
                players = mutableListOf(player),
                simulationSpeed = BotSpeed.SLOW
            ).apply {
                isHotseatMode = true
                currentPlayer = 0
                roundNumber = 1
                gameState = GameState.TURNSTARTED

                habitatShop = arrayOf(
                    createHabitatTile(1),
                    createHabitatTile(2),
                    createHabitatTile(3),
                    createHabitatTile(4)
                )
            }

            rootService.currentGame = CascadiaGameSave().apply {
                currentState = game
            }
        }

        /**
         * wildlife wird nicht automatisch entfernt bei drei gleichen
         */
        @Test
        fun threeIdenticalWildlife() {
            game.wildlifeShop = arrayOf(
                WildlifeToken(Wildlife.BEAR),
                WildlifeToken(Wildlife.BEAR),
                WildlifeToken(Wildlife.BEAR),
                WildlifeToken(Wildlife.FOX)
            )

            game.wildlifeBag = wildlifeStack(
                Wildlife.ELK,
                Wildlife.HAWK,
                Wildlife.SALMON
            )

            val bagBefore = game.wildlifeBag.peekAll().map { it.animal }

            rootService.gameActionService.checkOverpopulation()

            assertTrue(game.threeWildlifeIdentical)

            assertEquals(
                listOf(
                    Wildlife.BEAR,
                    Wildlife.BEAR,
                    Wildlife.BEAR,
                    Wildlife.FOX
                ),
                game.wildlifeShop.map { it?.animal }
            )

            assertEquals(
                bagBefore,
                game.wildlifeBag.peekAll().map { it.animal }
            )

            assertTrue(game.discardedWildlife.isEmpty())
        }

        /**
         * Vier gleiche wildlife werden entfernt
         */
        @Test
        fun fourIdenticalWildlife() {
            game.wildlifeShop = arrayOf(
                WildlifeToken(Wildlife.BEAR),
                WildlifeToken(Wildlife.BEAR),
                WildlifeToken(Wildlife.BEAR),
                WildlifeToken(Wildlife.BEAR)
            )

            game.wildlifeBag = wildlifeStack(
                Wildlife.SALMON,
                Wildlife.HAWK,
                Wildlife.ELK,
                Wildlife.FOX
            )
            rootService.gameActionService.checkOverpopulation()

            assertTrue(game.wildlifeShop.all { it != null })

            assertEquals(
                listOf(
                    Wildlife.SALMON,
                    Wildlife.HAWK,
                    Wildlife.ELK,
                    Wildlife.FOX
                ),
                game.wildlifeShop.map { it?.animal }
            )

            assertEquals(4, game.wildlifeBag.size)

            assertTrue(
                game.wildlifeBag.peekAll().all {
                    it.animal == Wildlife.BEAR
                }
            )

            assertTrue(game.discardedWildlife.isEmpty())
            assertFalse(game.threeWildlifeIdentical)
        }

        /**
         * ohne drei gleiche oder vier darf ncihts geändert werden
         */
        @Test
        fun shopWithoutOverpopEqual() {
            game.wildlifeShop = arrayOf(
                WildlifeToken(Wildlife.BEAR),
                WildlifeToken(Wildlife.FOX),
                WildlifeToken(Wildlife.HAWK),
                WildlifeToken(Wildlife.SALMON)
            )

            game.wildlifeBag = wildlifeStack(
                Wildlife.ELK,
                Wildlife.BEAR,
                Wildlife.FOX
            )

            val shopBefore = game.wildlifeShop.map { it?.animal }
            val bagBefore = game.wildlifeBag.peekAll().map { it.animal }

            rootService.gameActionService.checkOverpopulation()

            assertEquals(
                shopBefore,
                game.wildlifeShop.map { it?.animal }
            )

            assertEquals(
                bagBefore,
                game.wildlifeBag.peekAll().map { it.animal }
            )

            assertFalse(game.threeWildlifeIdentical)
            assertTrue(game.discardedWildlife.isEmpty())
        }

        /**
         * Erstellt wildlifeStack
         */
        private fun wildlifeStack(
            vararg wildlife: Wildlife
        ): Stack<WildlifeToken> {
            val stack = Stack<WildlifeToken>()

            for (index in wildlife.indices.reversed()) {
                stack.push(WildlifeToken(wildlife[index]))
            }

            return stack
        }

        /**
         * erstellt habitat tile
         */
        private fun createHabitatTile(id: Int): HabitatTile {
            return HabitatTile(
                habitats = mutableListOf(
                    Habitat.FORESTS,
                    Habitat.FORESTS,
                    Habitat.FORESTS,
                    Habitat.FORESTS,
                    Habitat.FORESTS,
                    Habitat.FORESTS
                ),
                possibleAnimals = listOf(Wildlife.BEAR)
            ).apply {
                this.id = id
            }
        }
    }