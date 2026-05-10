package de.tadris.flang_lib.bot.evaluation

import de.tadris.flang_lib.Move
import de.tadris.flang_lib.getNotationV1

class MoveEvaluation(val move: Move, val evaluation: Double, val depth: Int){

    companion object {
        val DUMMY = MoveEvaluation(0, 0.0, 0)
    }

    override fun toString(): String {
        return "[${move.getNotationV1()} -> $evaluation#$depth]"
    }

    fun getNotation(): String {
        return "${move.getNotationV1()}->${evaluation.toFloat()}"
    }

}