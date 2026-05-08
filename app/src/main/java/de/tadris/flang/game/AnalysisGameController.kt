package de.tadris.flang.game

import android.app.Activity
import android.util.Log
import de.tadris.flang.R
import de.tadris.flang.network_api.model.GameConfiguration
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang.network_api.model.GamePlayerInfo
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.packResignMove

open class AnalysisGameController(activity: Activity, var game: Game, val active: Boolean) : AbstractGameController(activity) {

    override fun onMoveRequested(move: Move, newBoardRequest: Game?, onCancel: (() -> Unit)?) {
        if(newBoardRequest != null){
            game = newBoardRequest
        }
        game.execute(move)
        callback.onUpdate(move)
        Log.d("Analysis", game.currentState.getFBN())
        Log.d("Analysis", game.getFMNv1())
    }

    override fun requestGame() {
        val player1Info = GamePlayerInfo(activity.getString(R.string.white), 0f, -1, 0f, false, "")
        val player2Info = GamePlayerInfo(activity.getString(R.string.black), 0f, -1, 0f, false, "")
        callback.onGameRequestSuccess(GameInfo(-1, player1Info, player2Info, game.getFMNv2(), game.moveList.size, running = active, GameConfiguration(
            isRated = false,
            infiniteTime = true,
            time = 0,
            isBotRequest = false
        ), lastAction = 0, won = 0, spectatorCount = 0), true, null, game.copy())
    }

    override fun resignGame() {
        callback.onUpdate(packResignMove(game.currentState.atMove))
    }

    override fun isCreativeGame() = true
}