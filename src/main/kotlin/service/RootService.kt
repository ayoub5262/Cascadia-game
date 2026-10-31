package service

import entity.CascadiaGame
import entity.CascadiaGameSave
import service.bot.BotActionService
import service.network.NetworkService


/**
 * The root service class is responsible for managing services and the entity layer reference.
 * This class acts as a central hub for every other service within the application.
 *
 */
class RootService{
    /** The connected [GameActionService] for this rootService */
    val gameActionService = GameActionService(this)
    /** The connected [PlayerActionService] for this rootService */
    val playerActionService = PlayerActionService(this)
    /** The connected [BotActionService] for this rootService */
    val botActionService = BotActionService(this)
    /** The connected [NetworkService] for this rootService */
    val networkService = NetworkService(this)

    /**
     * The currently active [entity.CascadiaGameSave]. Can be `null`, if no game has started yet.
     */
    var currentGame: CascadiaGameSave? = null

    /**
     * Adds the provided [newRefreshable] to all services connected to this root service
     *
     * @param newRefreshable The [Refreshable] to be added
     */
    fun addRefreshable(newRefreshable: Refreshable) {
        gameActionService.addRefreshable(newRefreshable)
        playerActionService.addRefreshable(newRefreshable)
    }

    /**
     * Adds each of the provided [newRefreshables] to all services
     * connected to this root service
     *
     * @param newRefreshables The [Refreshable]s to be added
     */
    fun addRefreshables(vararg newRefreshables: Refreshable) {
        newRefreshables.forEach { addRefreshable(it) }
    }
}