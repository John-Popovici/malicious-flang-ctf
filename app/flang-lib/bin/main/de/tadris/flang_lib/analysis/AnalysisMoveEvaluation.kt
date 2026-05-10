package de.tadris.flang_lib.analysis

import kotlin.math.roundToInt

class AnalysisMoveEvaluation(
    val notation: String,
    val bestMove: String,
    val isWhite: Boolean,
    val score: Double,
    val currentBoardEval: Double,
    val bestBoardEval: Double,
    val weight: Double,
    val type: AnalysisMoveType,
) {

    override fun toString(): String {
        val diffStr = (if(currentBoardEval > 0) "+" else "") + currentBoardEval.toFloat()
        return "$notation -> ${(score * 100).roundToInt()}% ($diffStr) w=${(weight * 100).roundToInt()}% ==> $type"
    }
}