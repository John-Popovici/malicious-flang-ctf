package de.tadris.flang_lib.bot.fast

import de.tadris.flang_lib.TYPE_NONE
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.TYPE_COUNT
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getType
import kotlin.random.Random

/**
 * Zobrist hashing implementation for Flang board positions.
 * Provides fast hash computation and incremental updates for transposition tables.
 */
class ZobristHash {

    companion object {
        // Initialize random keys for hashing
        private val random = Random(12345) // Fixed seed for reproducibility

        // Hash keys for pieces on squares
        // [piece_type][square][color] where piece_type is char index, color is 0=WHITE, 1=BLACK
        private val pieceSquareKeys = Array(TYPE_COUNT + 1) { Array(Board.ARRAY_SIZE) { LongArray(2) } }

        // Hash key for side to move (WHITE vs BLACK)
        private val sideToMoveKey = random.nextLong()
        
        // Hash keys for move number (to reduce collisions)
        private val moveNumberKeys = LongArray(200) // Support up to 200 moves

        // Hash keys for frozen pieces [color] where color is 0=WHITE, 1=BLACK
        private val frozenWhiteKeys = LongArray(64) // Keys for white frozen piece at each square
        private val frozenBlackKeys = LongArray(64) // Keys for black frozen piece at each square

        init {
            // Initialize all piece-square-color combinations
            for (piece in 0..TYPE_COUNT) {
                for (square in 0..<Board.ARRAY_SIZE) {
                    for (color in 0..1) {
                        pieceSquareKeys[piece][square][color] = random.nextLong()
                    }
                }
            }

            // Initialize frozen piece keys
            for (square in 0..<Board.ARRAY_SIZE) {
                frozenWhiteKeys[square] = random.nextLong()
                frozenBlackKeys[square] = random.nextLong()
            }
            
            // Initialize move number keys
            for (move in 0..<moveNumberKeys.size) {
                moveNumberKeys[move] = random.nextLong()
            }
        }
    }

    /**
     * Compute hash for the current board position
     */
    fun computeHash(board: Board): Long {
        var hash = 0L

        // Hash all pieces on the board
        for (square in 0 ..<Board.ARRAY_SIZE) {
            val piece = board.pieces[square]
            if (piece != TYPE_NONE) {
                val color = if(piece.getColor()) 1 else 0
                hash = hash xor pieceSquareKeys[piece.getType().toInt()][square][color]
            }
        }

        // Hash side to move
        if (board.atMove) {
            hash = hash xor sideToMoveKey
        }

        // Hash frozen pieces
        val frozenWhite = board.frozenWhiteIndex
        val frozenBlack = board.frozenBlackIndex

        if (frozenWhite != -1) {
            hash = hash xor frozenWhiteKeys[frozenWhite]
        }
        if (frozenBlack != -1) {
            hash = hash xor frozenBlackKeys[frozenBlack]
        }
        
        // Hash move number (with bounds checking)
        val moveNum = board.moveNumber % moveNumberKeys.size
        hash = hash xor moveNumberKeys[moveNum]

        return hash
    }
}