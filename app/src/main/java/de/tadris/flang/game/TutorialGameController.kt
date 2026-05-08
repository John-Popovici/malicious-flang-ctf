package de.tadris.flang.game

import android.app.Activity
import de.tadris.flang.network_api.model.GameConfiguration
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang.network_api.model.GamePlayerInfo
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.Move

class TutorialGameController(private val tutorial: TutorialInfo, activity: Activity) : OfflineBotGameController(activity) {

    init {
        strength = EASY_STRENGTH
    }

    override fun onMoveRequested(move: Move, newBoardRequest: Game?, onCancel: (() -> Unit)?) {
        if(tutorial.botTurns){
            super.onMoveRequested(move, newBoardRequest, onCancel)
        }else{
            game.execute(move)
            callback.onUpdate(move)
            game.currentState.atMove = COLOR_WHITE
        }
        if(!tutorial.freezeEnabled){
            game.currentState.unfreezeOnBoard(COLOR_WHITE)
            game.currentState.unfreezeOnBoard(COLOR_BLACK)
        }
    }

    override fun requestGame() {
        game = Game(initialBoard = tutorial.toBoard())
        game.currentState.isInfiniteGame = !tutorial.botTurns
        callback.onGameRequestSuccess(GameInfo(-1,
                GamePlayerInfo("Player1", 0f, -1, 0f, false, ""),
                GamePlayerInfo("Flangbot Classic#$strength", 0f, -1, 0f, false, ""),
                "", 0, running = true, GameConfiguration(
                isRated = false,
                infiniteTime = true,
                time = 0,
                isBotRequest = false
        ), lastAction = 0, won = 0, spectatorCount = 0), true, color, game.copy())
    }

    override fun isCreativeGame() = true
}