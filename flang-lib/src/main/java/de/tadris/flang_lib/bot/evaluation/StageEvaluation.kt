package de.tadris.flang_lib.bot.evaluation

import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.TYPE_PAWN
import de.tadris.flang_lib.TYPE_UNI
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getType
import de.tadris.flang_lib.x
import de.tadris.flang_lib.y
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.sqrt

class StageEvaluation : FastNeoBoardEvaluation() {

    override fun evaluate(): Double {
        if(board.hasWon(COLOR_WHITE)) return MATE_EVAL
        if(board.hasWon(COLOR_BLACK)) return -MATE_EVAL
        return when(getStage()){
            GameStage.EARLY -> evaluateEarlyGame()
            GameStage.MID -> super.evaluate()
            GameStage.END -> evaluateEndgame()
        }
    }

    private fun evaluateEarlyGame(): Double {
        var pawnsBonus = 0.0

        board.eachPiece(null) { index, piece ->
            if(piece.getType() != TYPE_PAWN) return@eachPiece

            val isWhite = piece.getColor()
            val colorFactor = if(isWhite) 1 else -1

            val borderX = if(isWhite) 7 else 0
            val borderY = if(isWhite) 0 else 7

            val xDistance = (index.x - borderX).absoluteValue.toDouble()
            val yDistance = (index.y - borderY).absoluteValue.toDouble()

            pawnsBonus += colorFactor * exp(-xDistance) * (sqrt(yDistance) - 1) // It's good to push pawns on the king's side
        }

        return super.evaluate() + pawnsBonus + 4 * getPieceValueEval() / FACTOR_PIECE_VALUE
    }

    private fun evaluateEndgame(): Double {
        return super.evaluate() + getKingsEval()
    }

    private fun getStage(): GameStage {
        if(board.moveNumber < 20){
            return GameStage.EARLY
        }

        var pawns = 0
        var unis = 0
        var total = 0

        board.eachPiece(null){ _, piece ->
            val type = piece.getType()
            when(type){
                TYPE_PAWN -> pawns++
                TYPE_UNI -> unis++
            }
            total++
        }

        return if(pawns <= 9 || total + unis - pawns <= 7) GameStage.END
        else GameStage.MID
    }


    enum class GameStage {
        EARLY,
        MID,
        END
    }

}