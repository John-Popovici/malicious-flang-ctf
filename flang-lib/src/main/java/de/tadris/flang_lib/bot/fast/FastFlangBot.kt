package de.tadris.flang_lib.bot.fast

import de.tadris.flang_lib.bot.BotResult
import de.tadris.flang_lib.bot.Engine
import de.tadris.flang_lib.bot.evaluation.FastBoardEvaluation
import de.tadris.flang_lib.bot.evaluation.FastNeoBoardEvaluation
import de.tadris.flang_lib.bot.evaluation.MoveEvaluation
import de.tadris.flang_lib.TYPE_KING
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.FastMoveGenerator
import de.tadris.flang_lib.MoveBuffer
import de.tadris.flang_lib.Variant
import de.tadris.flang_lib.evaluationNumber
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getFromIndex
import de.tadris.flang_lib.getFromPieceState
import de.tadris.flang_lib.getToIndex
import de.tadris.flang_lib.getType
import de.tadris.flang_lib.winningY
import de.tadris.flang_lib.y
import de.tadris.flang_lib.opening.DefaultOpeningDatabase
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Enhanced FlangBot based on ParallelFlangBot with transposition tables for improved performance.
 * Features:
 * - Zobrist hashing for position identification
 * - Transposition table for caching evaluations
 * - Parallel search with shared transposition table
 * - Enhanced move ordering
 */
class FastFlangBot(
    val minDepth: Int,
    val maxDepth: Int,
    val useOpeningDatabase: Boolean = true,
    val evaluationFactory: () -> FastBoardEvaluation = { FastNeoBoardEvaluation() },
    private val threads: Int = Runtime.getRuntime().availableProcessors(),
    ttSizeMB: Int = 64,
    val useLME: Boolean = false,
    val lmeMaxExtension: Int = 1,
): Engine {

    val lock = Object()
    var totalEvaluations = 0L
    val moveEvaluations = mutableListOf<MoveEvaluation>()

    // Transposition table and Zobrist hashing
    private val transpositionTable = TranspositionTable(ttSizeMB)
    private val zobristHash = ZobristHash()
    private val moveOrderer = MoveOrderer()
    
    // Mate detection constants
    private companion object {
        const val MATE_TT_DEPTH = 99 // Special depth for mate positions in TT
    }
    
    /**
     * Check if a value represents a mate score
     */
    private fun isMateScore(value: Double): Boolean {
        return value.absoluteValue > 5000.0
    }

    /**
     * Find best move to exact depth (no iterative deepening)
     */
    override fun findBestMove(board: Board, printTime: Boolean) =
        findBestMoveIterative(board, printTime, )

    override fun findBestMoveWithFixedDepth(board: Board, printTime: Boolean, depth: Int): BotResult {
        if(useOpeningDatabase && board.variant == Variant.CLASSIC){
            DefaultOpeningDatabase.db.query(board)?.let { return it }
        }

        val start = System.currentTimeMillis()

        // Start new search in transposition table#
        totalEvaluations = 0
        transpositionTable.newSearch()
        moveOrderer.clearKillerMoves()

        val eval = findBestMove(board, depth)
        val end = System.currentTimeMillis()

        if (printTime) {
            val time = end - start
            println("Moves: ${eval?.evaluations?.size}, Evals: ${totalEvaluations/1000}k, Depth: $depth, Time: ${end - start}ms, EPms: ${totalEvaluations / max(1, time)}")
            println(transpositionTable.getUsageStats())
        }
        return eval ?: throw Exception("No moves available")
    }

    override fun findBestMoveIterative(board: Board, printTime: Boolean, maxTimeMs: Long): BotResult {
        if(useOpeningDatabase && board.variant == Variant.CLASSIC){
            DefaultOpeningDatabase.db.query(board)?.let { return it }
        }

        val start = System.currentTimeMillis()

        // Start new search in transposition table
        totalEvaluations = 0
        transpositionTable.newSearch()
        moveOrderer.clearKillerMoves()

        val eval = findBestMoveIterativeDeepening(board, if(maxTimeMs <= 0) Long.MAX_VALUE else maxTimeMs, start)
        val end = System.currentTimeMillis()

        if (printTime) {
            val time = end - start
            println("Moves: ${eval?.evaluations?.size}, Evals: ${totalEvaluations/1000}k, Depth: ${eval?.bestMove?.depth ?: maxDepth}, Time: ${end - start}ms, EPms: ${totalEvaluations / max(1, time)}")
            println(transpositionTable.getUsageStats())
        }
        return eval!!
    }

    /**
     * Iterative deepening search - searches progressively deeper until time limit or max depth
     */
    private fun findBestMoveIterativeDeepening(board: Board, maxTimeMs: Long, startTime: Long): BotResult? {
        var bestResult: BotResult? = null
        var depth = min(5, minDepth)
        
        while (depth <= maxDepth) {
            val iterationStart = System.currentTimeMillis()
            
            // Check if we have enough time for this iteration
            val elapsed = iterationStart - startTime
            if (depth > minDepth && bestResult != null && elapsed >= maxTimeMs) break
            
            // Estimate time needed for this depth (roughly 3x previous depth)
            if (depth > minDepth && bestResult != null) {
                val lastIterationTime = iterationStart - startTime
                val estimatedTime = lastIterationTime * 3
                if (elapsed + estimatedTime > maxTimeMs) break
            }
            
            val result = findBestMove(board, depth)
            if (result != null) {
                bestResult = result
                
                // Update the depth in the best move for reporting
                bestResult = BotResult(
                    MoveEvaluation(bestResult.bestMove.move, bestResult.bestMove.evaluation, depth),
                    bestResult.evaluations.toList(),
                    bestResult.count
                )
            }
            
            depth++
        }
        
        return bestResult
    }

    private fun findBestMove(board: Board, depth: Int): BotResult? {
        moveEvaluations.clear()

        val allMoves = board.getMoves(board.atMove).toMutableList()

        if(allMoves.isEmpty()) return null

        // Enhanced move ordering: check for hash move from previous search
        val boardHash = zobristHash.computeHash(board)

        // Sort moves: hash move first, then by quick evaluation
        val eval = FastNeoBoardEvaluation()
        allMoves.sortBy { eval.evaluate(board.executeOnNewBoard(it)) }

        // Create a new executor for this search
        val executors = Executors.newFixedThreadPool(threads)
        
        allMoves.forEach {
            executors.submit(FastFlangBotThread(board, it, depth - 1, evaluationFactory()))
        }

        executors.shutdown()
        executors.awaitTermination(1, TimeUnit.HOURS)

        moveEvaluations.shuffle()
        moveEvaluations.sortBy { -((it.evaluation * 100).roundToInt() / 100.0) * board.atMove.evaluationNumber }
        
        if (moveEvaluations.size == 0) {
            return null
        }
        
        val bestMove = moveEvaluations[0]

        // Store the best move in transposition table
        // Use special depth for mate positions to prevent eviction
        val storeDepth = if (isMateScore(bestMove.evaluation)) MATE_TT_DEPTH else depth
        transpositionTable.store(
            zobristHash = boardHash,
            depth = storeDepth,
            value = bestMove.evaluation,
            nodeType = TranspositionTable.NodeType.EXACT,
            bestMove = bestMove.move
        )

        return BotResult(MoveEvaluation(bestMove.move, bestMove.evaluation, depth), moveEvaluations.toList(), totalEvaluations)
    }

    inner class FastFlangBotThread(
        val board: Board,
        val move: Move,
        val depth: Int,
        val evaluation: FastBoardEvaluation
    ) : Runnable {

        private val maxDepth get() = if(useLME) depth + lmeMaxExtension else depth
        private val moveBuffers = Array(maxDepth) { MoveBuffer() }
        private var bufferStackPointer = 0
        private val moveGenerator = FastMoveGenerator(Board(), false, 1)
        private var evaluationCount = 0
        private var lmeCounter = 0

        /**
         * Check if a move should be extended by LME
         */
        private fun shouldExtendMove(move: Move, depth: Int): Boolean {
            if (!useLME || depth <= 1) {
                return false
            }
            if(lmeCounter >= lmeMaxExtension){
                return false
            }
            
            val fromPiece = move.getFromPieceState()

            if(fromPiece.getType() == TYPE_KING){
                val fromIndex = move.getFromIndex()
                val toIndex = move.getToIndex()
                val color = fromPiece.getColor()

                return (toIndex.y - fromIndex.y) * color.evaluationNumber > 0 // check if king moves forward
                        && (toIndex.y - color.winningY).absoluteValue <= 3 // and distance to win is low
            }
            
            return false
        }

        fun pushMoveBuffer(): MoveBuffer {
            if (bufferStackPointer >= moveBuffers.size) {
                throw IllegalStateException("Move buffer stack overflow - recursion depth $bufferStackPointer exceeds limit")
            }
            return moveBuffers[bufferStackPointer++]
        }
        
        fun popMoveBuffer() {
            if (bufferStackPointer <= 0) {
                throw IllegalStateException("Move buffer stack underflow")
            }
            bufferStackPointer--
        }

        override fun run() {
            try {
                val evaluation = MoveEvaluation(
                    move, 
                    evaluateMove(board.executeOnNewBoard(move), depth, -FastNeoBoardEvaluation.MATE_EVAL, FastNeoBoardEvaluation.MATE_EVAL),
                    depth + 1
                )
                synchronized(lock) {
                    moveEvaluations += evaluation
                    totalEvaluations += evaluationCount
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        private fun evaluateMove(board: Board, depth: Int, alphaParam: Double, betaParam: Double): Double {
            var alpha = alphaParam
            var beta = betaParam

            // Check transposition table first
            val boardHash = zobristHash.computeHash(board)
            val ttResult = transpositionTable.probe(boardHash, depth, alpha, beta)

            if (ttResult != null && ttResult.hit) {
                return ttResult.value
            }

            if (depth <= 0) {
                evaluationCount++
                val value = evaluation.evaluate(board)

                // Store leaf evaluation in transposition table
                // Use special depth for mate positions to prevent eviction
                val storeDepth = if (isMateScore(value)) MATE_TT_DEPTH else 0
                transpositionTable.store(
                    zobristHash = boardHash,
                    depth = storeDepth,
                    value = value,
                    nodeType = TranspositionTable.NodeType.EXACT
                )

                return value
            }

            var bestEvaluation = -FastNeoBoardEvaluation.MATE_EVAL * board.atMove.evaluationNumber
            var bestMove: Move? = null
            var nodeType = TranspositionTable.NodeType.UPPER_BOUND

            moveGenerator.board = board
            val moveBuffer = pushMoveBuffer()
            moveGenerator.loadMovesToBuffer(board.atMove, moveBuffer)

            // Enhanced move ordering using MoveOrderer
            val orderedMoves = moveOrderer.orderMoves(moveBuffer, board, ttResult?.bestMove, depth)

            for(moveIndex in orderedMoves.indices){
                val move = orderedMoves[moveIndex]

                board.executeOnBoard(move) // make move
                
                var rawMoveEvaluation: Double

                if(shouldExtendMove(move, depth)){
                    // Late Move Extensions (LME)
                    // Search at depth + 1
                    lmeCounter++
                    rawMoveEvaluation = evaluateMove(board, depth, alpha, beta)
                    lmeCounter--
                }else {
                    // Search at full depth (no reduction)
                    rawMoveEvaluation = evaluateMove(board, depth - 1, alpha, beta)
                }

                board.revertMove(move) // revert move

                val finalMoveEvaluation =
                    when {
                        rawMoveEvaluation > 1000 -> rawMoveEvaluation - FastNeoBoardEvaluation.MATE_STEP_LOSS
                        rawMoveEvaluation < -1000 -> rawMoveEvaluation + FastNeoBoardEvaluation.MATE_STEP_LOSS
                        else -> rawMoveEvaluation
                    }

                if (board.atMove) { // check if white
                    if (finalMoveEvaluation > bestEvaluation) {
                        bestEvaluation = finalMoveEvaluation
                        bestMove = move
                        nodeType = TranspositionTable.NodeType.EXACT
                    }
                    alpha = max(alpha, bestEvaluation)
                    if (alpha >= beta) {
                        nodeType = TranspositionTable.NodeType.LOWER_BOUND
                        bestMove = move
                        // Update move ordering heuristics for cutoff
                        moveOrderer.updateKillerMove(move, depth)
                        moveOrderer.updateHistory(move, depth)
                        break // Beta cutoff
                    }
                } else {
                    if (finalMoveEvaluation < bestEvaluation) {
                        bestEvaluation = finalMoveEvaluation
                        bestMove = move
                        nodeType = TranspositionTable.NodeType.EXACT
                    }
                    beta = min(beta, bestEvaluation)
                    if (beta <= alpha) {
                        nodeType = TranspositionTable.NodeType.UPPER_BOUND
                        bestMove = move
                        // Update move ordering heuristics for cutoff
                        moveOrderer.updateKillerMove(move, depth)
                        moveOrderer.updateHistory(move, depth)
                        break // Alpha cutoff
                    }
                }
            }

            // Store result in transposition table
            // Use special depth for mate positions to prevent eviction
            val storeDepth = if (isMateScore(bestEvaluation)) MATE_TT_DEPTH else depth
            transpositionTable.store(
                zobristHash = boardHash,
                depth = storeDepth,
                value = bestEvaluation,
                nodeType = nodeType,
                bestMove = bestMove
            )

            popMoveBuffer()
            return bestEvaluation
        }

    }

    /**
     * Get transposition table statistics
     */
    fun getTranspositionTableStats(): String {
        return transpositionTable.getUsageStats()
    }

    /**
     * Clear transposition table (useful between games)
     */
    fun clearTranspositionTable() {
        transpositionTable.clear()
    }
}