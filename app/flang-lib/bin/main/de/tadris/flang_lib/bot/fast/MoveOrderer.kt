package de.tadris.flang_lib.bot.fast

import de.tadris.flang_lib.TYPE_FLANGER
import de.tadris.flang_lib.TYPE_HORSE
import de.tadris.flang_lib.TYPE_KING
import de.tadris.flang_lib.TYPE_NONE
import de.tadris.flang_lib.TYPE_PAWN
import de.tadris.flang_lib.TYPE_ROOK
import de.tadris.flang_lib.TYPE_UNI
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.FastType
import de.tadris.flang_lib.MoveBuffer
import de.tadris.flang_lib.evaluationNumber
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getFromIndex
import de.tadris.flang_lib.getFromPieceState
import de.tadris.flang_lib.getToIndex
import de.tadris.flang_lib.getToPieceState
import de.tadris.flang_lib.getType
import de.tadris.flang_lib.y

/**
 * Enhanced move ordering for better alpha-beta pruning efficiency.
 * Orders moves by likelihood of being good, improving cutoff frequency.
 */
class MoveOrderer {
    
    // Killer moves: good moves that caused cutoffs at the same depth
    private val killerMoves = Array(32) { Array<Move?>(2) { null } } // [depth][slot]
    
    // History table: moves that frequently cause cutoffs
    private val historyTable = Array(64) { IntArray(64) } // [from][to]
    
    /**
     * Order moves for optimal alpha-beta search
     * Priority: Hash move > Captures > Killer moves > History > Quiet moves
     */
    fun orderMoves(moves: MoveBuffer, board: Board, hashMove: Move?, depth: Int): LongArray {
        if (moves.size() < 0) return LongArray(0)
        if (moves.size() == 1) return longArrayOf(moves.get(0))
        
        // Create arrays for faster operations
        val scoresArray = IntArray(moves.size())
        
        // Calculate scores
        for (i in 0..<moves.size()) {
            scoresArray[i] = scoreMove(moves.get(i), board, hashMove, depth)
        }

        // Sort using indices to avoid object creation
        val indices = IntArray(moves.size()) { it }

        // Quick sort by score (descending)
        quickSortByScore(indices, scoresArray, 0, indices.size - 1)

        // Reorder moves array based on sorted indices
        val result = LongArray(moves.size()) { moves.get(indices[it]) }
        
        return result
    }

    /**
     * Quick sort implementation that sorts indices by score (descending order)
     */
    private fun quickSortByScore(indices: IntArray, scores: IntArray, low: Int, high: Int) {
        if (low < high) {
            val pivotIndex = partitionByScore(indices, scores, low, high)
            quickSortByScore(indices, scores, low, pivotIndex - 1)
            quickSortByScore(indices, scores, pivotIndex + 1, high)
        }
    }
    
    /**
     * Partition function for quicksort (descending order by score)
     */
    private fun partitionByScore(indices: IntArray, scores: IntArray, low: Int, high: Int): Int {
        val pivot = scores[indices[high]]
        var i = low - 1
        
        for (j in low until high) {
            // Sort in descending order (higher scores first)
            if (scores[indices[j]] > pivot) {
                i++
                // Swap indices
                val temp = indices[i]
                indices[i] = indices[j]
                indices[j] = temp
            }
        }
        
        // Place pivot in correct position
        val temp = indices[i + 1]
        indices[i + 1] = indices[high]
        indices[high] = temp
        
        return i + 1
    }
    
    /**
     * Score a move for ordering (higher = better)
     */
    private fun scoreMove(move: Move, board: Board, hashMove: Move?, depth: Int): Int {
        val fromPiece = move.getFromPieceState()
        val fromPieceType = fromPiece.getType()
        val fromIndex = move.getFromIndex()
        val toIndex = move.getToIndex()
        var score = 0
        
        // 1. Hash move (from transposition table) - highest priority
        if (move == hashMove) {
            score += 10000
        }
        
        // 2. Captures - order by Most Valuable Victim - Least Valuable Attacker (MVV-LVA)
        val targetPieceState = move.getToPieceState()
        if (targetPieceState.getType() != TYPE_NONE) {
            val attackerValue = getPieceValue(fromPieceType)
            val victimValue = getPieceValue(targetPieceState.getType())
            score += 1000 + (victimValue - attackerValue)
        }
        
        // 3. Killer moves - good moves at this depth
        score += getKillerMoveScore(move, depth)

        // 4. History heuristic - moves that often cause cutoffs
        score += getHistoryScore(fromIndex, toIndex)

        if(fromPieceType == TYPE_KING && (fromIndex.y - toIndex.y) * fromPiece.getColor().evaluationNumber > 0){
            score += 10
        }

        return score
    }
    
    /**
     * Update killer moves when a move causes a cutoff
     */
    fun updateKillerMove(move: Move, depth: Int) {
        if (depth >= killerMoves.size) return
        
        // Don't store captures as killer moves (they're already prioritized)
        if (isCapture(move)) return
        
        // Use synchronized block only for the critical section
        synchronized(killerMoves[depth]) {
            // Shift killer moves: new move becomes first, first becomes second
            if (killerMoves[depth][0] != move) {
                killerMoves[depth][1] = killerMoves[depth][0]
                killerMoves[depth][0] = move
            }
        }
    }
    
    /**
     * Update history table when a move causes a cutoff
     */
    fun updateHistory(move: Move, depth: Int) {
        val fromIndex = move.getFromIndex()
        val toIndex = move.getToIndex()

        // Bonus based on depth (deeper = more valuable)
        val bonus = depth * depth

        // Use atomic operation for the increment
        synchronized(historyTable) {
            historyTable[fromIndex][toIndex] += bonus

            // Prevent overflow
            if (historyTable[fromIndex][toIndex] > 10000) {
                // Age the history table
                for (i in historyTable.indices) {
                    for (j in historyTable[i].indices) {
                        historyTable[i][j] /= 2
                    }
                }
            }
        }
    }
    
    /**
     * Get killer move score (no synchronization needed for reads)
     */
    private fun getKillerMoveScore(move: Move, depth: Int): Int {
        if (depth >= killerMoves.size) return 0
        
        // Volatile reads are atomic, no sync needed
        val killer1 = killerMoves[depth][0]
        val killer2 = killerMoves[depth][1]
        
        return when (move) {
            killer1 -> 900
            killer2 -> 890
            else -> 0
        }
    }
    
    /**
     * Check if a move is a killer move at the given depth
     */
    fun isKillerMove(move: Move, depth: Int): Boolean {
        if (depth >= killerMoves.size) return false
        
        val killer1 = killerMoves[depth][0]
        val killer2 = killerMoves[depth][1]
        
        return move == killer1 || move == killer2
    }
    
    /**
     * Get history score (no synchronization needed for reads)
     */
    private fun getHistoryScore(from: Int, to: Int): Int {
        return historyTable[from][to] / 10
    }
    
    /**
     * Clear killer moves for a new search
     */
    fun clearKillerMoves() {
        for (depth in killerMoves.indices) {
            killerMoves[depth][0] = null
            killerMoves[depth][1] = null
        }
    }
    
    /**
     * Get piece value for MVV-LVA ordering
     */
    private fun getPieceValue(piece: FastType): Int {
        return when (piece) {
            TYPE_PAWN -> 100    // Pawn
            TYPE_HORSE -> 300    // Horse (Knight)
            TYPE_ROOK -> 500    // Rook
            TYPE_FLANGER -> 400    // Flanger
            TYPE_UNI -> 900    // Uni (Queen-like)
            TYPE_KING -> 10000  // King
            else -> 0
        }
    }
    
    /**
     * Check if move is a capture
     */
    private fun isCapture(move: Move): Boolean {
        return move.getToPieceState().getType() != TYPE_NONE
    }

}