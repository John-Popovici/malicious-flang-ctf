package de.tadris.flang_lib.opening

import de.tadris.flang_lib.bot.BotResult
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.bot.evaluation.MoveEvaluation

class OpeningDatabase(source: String) {

    private val map = source.lineSequence()
        .filter { it.isNotEmpty() }
        .map { OpeningDatabaseEntry.fromFileString(it) }
        .associateBy { it.game.currentState.getFBN2() }

    fun query(board: Board, minDepth: Int): BotResult? {
        val result = query(board) ?: return null
        return if(result.bestMove.depth >= minDepth) result else null
    }

    fun query(board: Board): BotResult? {
        val entry = map[board.getFBN2()] ?: return null
        val evaluations = entry.evaluations.map { MoveEvaluation(it.first, it.second, entry.depth) }
        return BotResult(evaluations.first(), evaluations, 0)
    }

}