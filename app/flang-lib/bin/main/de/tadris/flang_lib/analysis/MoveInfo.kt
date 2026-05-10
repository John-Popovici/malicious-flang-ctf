package de.tadris.flang_lib.analysis

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.parseMove

data class MoveInfo(
    val ply: Int,
    val moveNotation: String,
    val evaluation: PositionEvaluation,
    val bestMoveNotation: String? = null,
    val bestEvaluation: PositionEvaluation? = null,
    val bestIsForced: Boolean? = null,
    val judgment: MoveJudgment? = null,
    val depth: Int = 0,
    val nodesSearched: Long = 0
) {

    val isWhiteMove: Boolean get() = ply % 2 == 1
    
    val hasJudgment: Boolean get() = judgment != null
    
    val isError: Boolean get() = judgment?.isError == true
    
    val isGoodMove: Boolean get() = judgment?.isGoodMove == true

    fun getBoard(fmn: String) = Game.fromFMNAtIndex(fmn, ply - 1)

    fun getAction(fmn: String) = Game.fromFMN(fmn).moveList[ply - 1]

    fun getBestMove(fmn: String) = bestMoveNotation?.let { notation ->
        parseMove(getBoard(fmn).currentState, notation)
    }
    
    fun withJudgment(judgment: MoveJudgment): MoveInfo {
        return copy(judgment = judgment)
    }
    
    fun getEvaluationString(): String {
        return evaluation.toString()
    }
}