package de.tadris.flang_lib.analysis

import de.tadris.flang_lib.bot.evaluation.FastNeoBoardEvaluation
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.tanh

data class PositionEvaluation(
    val centipawns: Double,
    val mateInMoves: Int? = null
) {
    companion object {
        const val MATE_THRESHOLD = 5000.0
        
        fun fromScore(score: Double): PositionEvaluation {
            return if (score.absoluteValue >= MATE_THRESHOLD) {
                val mateDistance = if (score > 0) {
                    ((FastNeoBoardEvaluation.MATE_EVAL - score) / FastNeoBoardEvaluation.MATE_STEP_LOSS).roundToInt()
                } else {
                    -(((FastNeoBoardEvaluation.MATE_EVAL + score) / FastNeoBoardEvaluation.MATE_STEP_LOSS).roundToInt())
                }
                PositionEvaluation(score, mateDistance)
            } else {
                PositionEvaluation(score)
            }
        }
    }
    
    val isMate: Boolean get() = mateInMoves != null
    
    val isMateForWhite: Boolean get() = mateInMoves != null && centipawns >= MATE_THRESHOLD
    
    val isMateForBlack: Boolean get() = mateInMoves != null && centipawns <= -MATE_THRESHOLD

    val isMateForMe get() = isMateForWhite

    val isMateForOpponent get() = isMateForBlack
    
    fun fromPlayerPerspective(isWhite: Boolean): PositionEvaluation {
        return if (isWhite) {
            this
        } else {
            // For black perspective, flip the evaluation
            if (isMate) {
                PositionEvaluation(-centipawns, mateInMoves?.let { -it })
            } else {
                PositionEvaluation(-centipawns, null)
            }
        }
    }
    
    override fun toString(): String {
        return if (isMate) {
            "M${mateInMoves}"
        } else {
            "${if (centipawns >= 0) "+" else ""}${centipawns.toInt()}"
        }
    }
}