package de.tadris.flang.network_api.model

import de.tadris.flang_lib.bot.evaluation.MoveEvaluation
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.parseMove

data class ComputerResult(val result: String, val name: String, val depth: Int){

    fun getMoveEvals(board: Board): List<MoveEvaluation> {
        return result.split(";").mapNotNull {
            if(it.isEmpty()) return@mapNotNull null
            val evalData = it.split("->")
            val move = parseMove(board, evalData[0])
            val eval = evalData[1].toFloat()
            return@mapNotNull MoveEvaluation(move, eval.toDouble(), depth)
        }
    }

}
