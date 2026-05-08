package de.tadris.flang_lib.bot.evaluation

import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.TYPE_FLANGER
import de.tadris.flang_lib.TYPE_HORSE
import de.tadris.flang_lib.TYPE_KING
import de.tadris.flang_lib.TYPE_NONE
import de.tadris.flang_lib.TYPE_PAWN
import de.tadris.flang_lib.TYPE_ROOK
import de.tadris.flang_lib.TYPE_UNI
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.FastType
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getFrozen
import de.tadris.flang_lib.getType
import de.tadris.flang_lib.x
import de.tadris.flang_lib.y
import kotlin.math.abs

/**
 * Ultra-fast, simple evaluation function that prioritizes speed over complexity.
 *
 * Evaluation components (in order of importance):
 * 1. Material balance (60% weight)
 * 2. King advancement (30% weight)
 * 3. Piece activity/mobility (10% weight)
 *
 * This is designed to be 3-5x faster than FastNeoBoardEvaluation while maintaining
 * reasonable playing strength through good material evaluation and king progression.
 */
class SimpleFastEvaluation : FastBoardEvaluation {

    // Pre-computed piece values for fast lookup
    private val pieceValues = intArrayOf(
        0,    // FAST_NONE
        100,  // FAST_PAWN
        300,  // FAST_HORSE
        500,  // FAST_ROOK
        400,  // FAST_FLANGER
        900,  // FAST_UNI
        10000 // FAST_KING
    )

    // Pre-computed king advancement bonuses by Y position
    private val whiteKingAdvancement = intArrayOf(0, 10, 25, 50, 100, 200, 400, 800) // Y=0 to Y=7
    private val blackKingAdvancement = intArrayOf(800, 400, 200, 100, 50, 25, 10, 0) // Y=7 to Y=0

    // Center control bonus by distance from center
    private val centerBonus = intArrayOf(20, 15, 10, 5, 0, 0, 0, 0)

    override fun evaluate(board: Board): Double {
        // Quick win detection
        if (board.hasWon(COLOR_WHITE)) return FastNeoBoardEvaluation.MATE_EVAL
        if (board.hasWon(COLOR_BLACK)) return -FastNeoBoardEvaluation.MATE_EVAL

        var evaluation = 0
        var whiteMaterial = 0
        var blackMaterial = 0
        var whiteKingPos = -1
        var blackKingPos = -1
        var whiteMobility = 0
        var blackMobility = 0

        // Single pass through all squares
        for (index in 0 until Board.ARRAY_SIZE) {
            val piece = board.getAt(index)
            val type = piece.getType()

            if (type != TYPE_NONE) {
                val color = piece.getColor()
                val value = pieceValues[type.toInt()]
                val x = index.x
                val y = index.y

                if (color == COLOR_WHITE) {
                    whiteMaterial += value

                    // King position tracking
                    if (type == TYPE_KING) {
                        whiteKingPos = index
                        evaluation += whiteKingAdvancement[y]
                    }

                    // Simple mobility bonus (frozen pieces get penalty)
                    if (!piece.getFrozen()) {
                        whiteMobility += getMobilityBonus(type, x, y)
                    }

                } else { // FAST_BLACK
                    blackMaterial += value

                    // King position tracking
                    if (type == TYPE_KING) {
                        blackKingPos = index
                        evaluation -= blackKingAdvancement[y]
                    }

                    // Simple mobility bonus (frozen pieces get penalty)
                    if (!piece.getFrozen()) {
                        blackMobility += getMobilityBonus(type, x, y)
                    }
                }
            }
        }

        // Material balance (most important)
        evaluation += (whiteMaterial - blackMaterial)

        // King advancement bonus (already calculated above)

        // Simple mobility evaluation
        evaluation += (whiteMobility - blackMobility) / 10

        // Endgame king proximity (simple heuristic)
        if (whiteMaterial + blackMaterial < 2000) { // Endgame threshold
            if (whiteKingPos != -1 && blackKingPos != -1) {
                val kingDistance = abs(whiteKingPos.x - blackKingPos.x) + abs(whiteKingPos.y - blackKingPos.y)
                evaluation += (8 - kingDistance) * 5 // Closer kings in endgame
            }
        }

        return evaluation.toDouble()
    }

    /**
     * Get mobility bonus for a piece based on type and position
     */
    private fun getMobilityBonus(type: FastType, x: Int, y: Int): Int {
        return when (type) {
            TYPE_PAWN -> 10
            TYPE_HORSE -> 15 + getCenterBonus(x, y)
            TYPE_ROOK -> 12 + getCenterBonus(x, y)
            TYPE_FLANGER -> 14 + getCenterBonus(x, y)
            TYPE_UNI -> 20 + getCenterBonus(x, y)
            TYPE_KING -> 8
            else -> 0
        }
    }

    /**
     * Get center control bonus
     */
    private fun getCenterBonus(x: Int, y: Int): Int {
        val centerDistance = maxOf(abs(x - 3), abs(x - 4)) + maxOf(abs(y - 3), abs(y - 4))
        return if (centerDistance < centerBonus.size) centerBonus[centerDistance] else 0
    }
}