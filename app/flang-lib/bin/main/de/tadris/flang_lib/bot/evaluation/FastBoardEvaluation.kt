package de.tadris.flang_lib.bot.evaluation

import de.tadris.flang_lib.Board

/**
 * Can evaluate instances of [Board]. Similar to [BoardEvaluation] but uses [Board].
 */
interface FastBoardEvaluation {

    /**
     * Evaluates the board and gives an estimation of how good the position is for white or black.
     * Note: this method is not Thread-safe and may only be called after another call has been completed.
     * @return evaluation in centipawns
     */
    fun evaluate(board: Board): Double

}