package de.tadris.flang_lib.bot

import de.tadris.flang_lib.Board
import de.tadris.flang_lib.bot.evaluation.MoveEvaluation

object RandomEngine : Engine {

    override fun findBestMove(
        board: Board,
        printTime: Boolean
    ): BotResult {
        val evals = board.getMoves().map { MoveEvaluation(it, Math.random() * 100, 0) }
        return BotResult(
            evals.random(),
            evals,
            0
        )
    }
}