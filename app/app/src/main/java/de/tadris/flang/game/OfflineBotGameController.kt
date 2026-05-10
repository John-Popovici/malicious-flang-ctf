package de.tadris.flang.game

import android.app.Activity
import de.tadris.flang.network_api.model.GameConfiguration
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang.network_api.model.GamePlayerInfo
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.bot.Engine
import de.tadris.flang_lib.bot.fast.FastFlangBot
import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.Variant
import de.tadris.flang_lib.packResignMove
import kotlin.concurrent.thread

open class OfflineBotGameController(activity: Activity) : AbstractGameController(activity) {

    companion object {
        const val NAME = "Flangbot Classic"
        const val EASY_STRENGTH = 2
        const val DEFAULT_STRENGTH = 6
        const val MIN_OFFLINE_THINK_TIME = 1000
    }

    protected var color = COLOR_WHITE // Color of user
    protected var game = Game()

    protected var strength = EASY_STRENGTH
        set(value) {
            field = value
            updateBot()
        }
    protected var engineCreator: () -> Engine = { FastFlangBot(strength, strength) }
        set(value) {
            field = value
            updateBot()
        }
    protected var engine: Engine = engineCreator()
    protected var name = "Flangbot Classic#$strength"

    private fun updateBot(){
        engine.destroy()
        engine = engineCreator()
    }

    override fun stop() {
        engine.destroy()
    }

    override fun resume() {
        updateBot()
    }

    fun updateConfiguration(name: String, engineCreator: () -> Engine, strength: Int){
        this.name = name
        this.engineCreator = engineCreator
        this.strength = strength
        updateGameState()
    }

    override fun onMoveRequested(move: Move, newBoardRequest: Game?, onCancel: (() -> Unit)?) {
        if(newBoardRequest != null){
            // Accept new board
            game = newBoardRequest
        }
        game.execute(move)
        callback.onUpdate(move)
        botTurn()
    }

    override fun requestGame() {
        color = if(Math.random() > 0.5) COLOR_WHITE else COLOR_BLACK
        updateGameState()
        if(!color){
            botTurn()
        }
    }

    private fun updateGameState(){
        val player1Info = GamePlayerInfo("Player1", 0f, -1, 0f, false, "")
        val player2Info = GamePlayerInfo(this.name, 0f, -1, 0f, true, "")
        val whiteInfo = if(color) player1Info else player2Info
        val blackInfo = if(color) player2Info else player1Info
        callback.onGameRequestSuccess(GameInfo(-1, whiteInfo, blackInfo, "", 0, running = true, GameConfiguration(
            isRated = false,
            infiniteTime = true,
            time = 0,
            isBotRequest = false
        ), lastAction = 0, won = 0, spectatorCount = 0), true, color, game.copy())
    }

    private fun botTurn(){
        if(game.currentState.gameIsComplete()){
            return
        }
        thread {
            val start = System.currentTimeMillis()
            val botMoves = if(strength > 0){
                engine.findBestMove(game.currentState)
            }else{
                engine.findBestMoveIterative(game.currentState, true, -(strength * 1000L))
            }
            println(botMoves.evaluations)
            println("Evaluations: " + botMoves.count)
            val diff = System.currentTimeMillis() - start
            Thread.sleep((MIN_OFFLINE_THINK_TIME - diff).coerceAtLeast(0))
            val botMove = botMoves.bestMove.move
            game.execute(botMove)
            activity.runOnUiThread {
                callback.onUpdate(botMove)
            }
        }
    }

    override fun resignGame() {
        callback.onUpdate(packResignMove(color))
    }

    override fun isCreativeGame() = true

    fun updateVariant(variant: Variant){
        game.initialBoard.variant = variant
        game.currentState.variant = variant
        updateGameState()
    }
}