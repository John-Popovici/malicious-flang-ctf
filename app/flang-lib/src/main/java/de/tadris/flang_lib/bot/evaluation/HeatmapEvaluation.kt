package de.tadris.flang_lib.bot.evaluation

import de.tadris.flang_lib.TYPE_FLANGER
import de.tadris.flang_lib.TYPE_HORSE
import de.tadris.flang_lib.TYPE_KING
import de.tadris.flang_lib.TYPE_PAWN
import de.tadris.flang_lib.TYPE_ROOK
import de.tadris.flang_lib.TYPE_UNI
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.FastType
import de.tadris.flang_lib.TYPE_RIDER
import de.tadris.flang_lib.evaluationNumber
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getType
import de.tadris.flang_lib.pieceValue
import de.tadris.flang_lib.x
import de.tadris.flang_lib.y
import kotlin.math.roundToLong

class HeatmapEvaluation : FastBoardEvaluation {

    companion object {

        const val PAWN_MULTIPLIER = 0.5
        const val ROOK_MULTIPLIER = 2.0
        const val HORSE_MULTIPLIER = 1.3
        const val FLANGER_MULTIPLIER = 1.5
        const val UNI_MULTIPLIER = 2.0
        const val KING_MULTIPLIER = 10.0

        val PAWN_HEATMAP = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(6, 6, 6, 6, 6, 7, 7, 7),
            intArrayOf(3, 3, 4, 5, 5, 6, 6, 6),
            intArrayOf(2, 2, 2, 2, 3, 4, 5, 6),
            intArrayOf(3, 3, 2, 1, 2, 3, 4, 5),
            intArrayOf(4, 4, 3, 2, 1, 2, 3, 3),
            intArrayOf(0, 4, 1, 0, 0, 1, 3, 3),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        )

        val KING_HEATMAP = arrayOf(
            intArrayOf(9, 9, 9, 9, 9, 9, 9, 9),
            intArrayOf(9, 9, 9, 9, 9, 9, 9, 9),
            intArrayOf(5, 5, 5, 5, 6, 6, 7, 7),
            intArrayOf(3, 3, 3, 3, 4, 4, 5, 5),
            intArrayOf(1, 1, 2, 2, 2, 2, 3, 3),
            intArrayOf(0, 0, 0, 0, 1, 2, 2, 2),
            intArrayOf(0, 0, 0, 0, 0, 1, 1, 1),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        )

        val HORSE_HEATMAP = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 2, 2, 2, 2, 2, 2, 0),
            intArrayOf(0, 2, 2, 2, 2, 2, 2, 0),
            intArrayOf(0, 2, 2, 2, 2, 2, 2, 0),
            intArrayOf(1, 2, 2, 2, 2, 2, 2, 0),
            intArrayOf(1, 2, 2, 3, 2, 3, 2, 0),
            intArrayOf(1, 2, 3, 2, 2, 2, 2, 0),
            intArrayOf(1, 1, 1, 0, 0, 0, 0, 0),
        )

        val ROOK_HEATMAP_MID = arrayOf(
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(3, 3, 3, 3, 3, 3, 3, 3),
            intArrayOf(2, 2, 2, 2, 2, 2, 2, 2),
        )

        val ROOK_HEATMAP_END = arrayOf(
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(3, 3, 3, 3, 3, 3, 3, 3),
            intArrayOf(2, 2, 2, 2, 2, 2, 2, 2),
           intArrayOf(-1,-1,-1,-1,-1,-1,-1,-1),
        )

        val UNI_HEATMAP_EARLY = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 1, 2, 2, 1, 0, 0),
            intArrayOf(0, 0, 1, 3, 3, 3, 1, 1),
            intArrayOf(0, 0, 1, 3, 3, 3, 2, 2),
            intArrayOf(0, 0, 3, 5, 5, 4, 4, 3),
            intArrayOf(0, 0, 1, 1, 1, 1, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        )

        val UNI_HEATMAP_MID = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 1, 1, 1, 1, 0, 0),
            intArrayOf(0, 1, 2, 3, 3, 2, 1, 0),
            intArrayOf(1, 1, 3, 5, 5, 3, 1, 1),
            intArrayOf(1, 1, 3, 5, 5, 3, 1, 1),
            intArrayOf(1, 2, 3, 4, 4, 4, 2, 1),
            intArrayOf(0, 0, 1, 1, 1, 1, 0, 0),
            intArrayOf(5, 0, 0, 0, 0, 0, 0, 0),
        )

        val FLANGER_HEATMAP = arrayOf(
            intArrayOf(2, 2, 2, 2, 2, 2, 2, 2),
            intArrayOf(2, 1, 1, 1, 1, 1, 1, 2),
            intArrayOf(2, 1, 0, 0, 0, 0, 1, 2),
            intArrayOf(2, 1, 0, 0, 0, 0, 1, 2),
            intArrayOf(2, 1, 1, 1, 1, 1, 1, 2),
            intArrayOf(3, 3, 3, 3, 3, 3, 3, 3),
            intArrayOf(5, 5, 5, 5, 5, 5, 5, 5),
            intArrayOf(2, 2, 2, 1, 1, 1, 1, 1),
        )

        fun getBonus(stage: GameStage, type: FastType, normalizedX: Int, normalizedY: Int): Double {
            return when(type){
                TYPE_PAWN -> {
                    val factor = if(stage == GameStage.EARLY) 1.5 else 1.0
                    PAWN_HEATMAP[normalizedY][normalizedX] * PAWN_MULTIPLIER * factor
                }
                TYPE_KING -> {
                    val factor = when(stage){
                        GameStage.EARLY -> 0.2
                        GameStage.MID -> 1.0
                        GameStage.END -> 3.0
                    }
                    KING_HEATMAP[normalizedY][normalizedX] * KING_MULTIPLIER * factor
                }
                TYPE_HORSE, TYPE_RIDER -> HORSE_HEATMAP[normalizedY][normalizedX] * HORSE_MULTIPLIER
                TYPE_ROOK -> {
                    val map = if(stage == GameStage.END) ROOK_HEATMAP_END else ROOK_HEATMAP_MID
                    map[normalizedY][normalizedX] * ROOK_MULTIPLIER
                }
                TYPE_UNI -> {
                    val map = if(stage == GameStage.EARLY) UNI_HEATMAP_EARLY else UNI_HEATMAP_MID
                    map[normalizedY][normalizedX] * UNI_MULTIPLIER
                }
                TYPE_FLANGER -> FLANGER_HEATMAP[normalizedY][normalizedX] * FLANGER_MULTIPLIER
                else -> throw Exception("Unknown piece type $type")
            }
        }

    }

    override fun evaluate(board: Board): Double {
        val adder = (board.getWinningColor()?.evaluationNumber ?: 0) * 10000.0
        var bonus = 0.0
        var material = 0

        val stage = getStage(board)

        board.eachPiece(null){ index, state ->
            val color = state.getColor()
            val type = state.getType()
            val normalizedX = if(color) index.x else Board.BOARD_SIZE - 1 - index.x
            val normalizedY = if(color) Board.BOARD_SIZE - 1 - index.y else index.y

            material += type.pieceValue / 100 * color.evaluationNumber
            bonus += getBonus(stage, type, normalizedX, normalizedY) * color.evaluationNumber
        }

        return ((material * 1 + bonus + adder) * 100).roundToLong() / 100.0
    }

    private fun getStage(board: Board): GameStage {
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