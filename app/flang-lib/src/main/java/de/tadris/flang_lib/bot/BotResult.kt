package de.tadris.flang_lib.bot

import de.tadris.flang_lib.bot.evaluation.MoveEvaluation
import kotlin.math.pow

class BotResult(val bestMove: MoveEvaluation, val evaluations: List<MoveEvaluation>, val count: Long) {

    fun getVariance(): Double {
        val sum = evaluations.sumOf { it.evaluation }
        val avg = sum / evaluations.size
        val summedDiff = evaluations.sumOf { (it.evaluation - avg).pow(2) }
        return summedDiff / evaluations.size
    }

    override fun toString() = "Evals: $evaluations"

}