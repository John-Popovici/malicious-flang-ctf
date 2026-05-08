package de.tadris.flang.game

import android.app.Activity
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.Move

class PersistentAnalysisGameController(activity: Activity, active: Boolean) : AnalysisGameController(activity, Game(), active) {

    private val gameStateManager = GameStateManager(activity)

    init {
        // Load saved board state
        game = gameStateManager.loadGameState()
    }

    override fun onMoveRequested(move: Move, newBoardRequest: Game?, onCancel: (() -> Unit)?) {
        super.onMoveRequested(move, newBoardRequest, onCancel)
        // Save board state after each move
        gameStateManager.saveGameState(game)
    }

    override fun resignGame() {
        super.resignGame()
        // Save board state after resignation
        gameStateManager.saveGameState(game)
    }

    fun clearSavedState() {
        gameStateManager.clearGameState()
    }
}