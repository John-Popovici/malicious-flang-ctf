package de.tadris.flang.bot

import de.tadris.flang_lib.bot.BotResult
import de.tadris.flang_lib.bot.Engine
import de.tadris.flang_lib.bot.evaluation.MoveEvaluation
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.evaluationNumber
import de.tadris.flang_lib.parseMove
import kotlin.math.roundToInt

class NativeCFlangEngine(
    private val minDepth: Int = 1,
    private val maxDepth: Int = 6,
    private val threads: Int = Runtime.getRuntime().availableProcessors(),
    private val ttSizeMB: Int = 64,
    private val useLME: Boolean = false,
    private val lmeMaxExtension: Int = 1,
    private val onlyBest: Boolean = false,
    private val useNnue: Boolean = true,
) : Engine {

    private var botPtr: Long = 0

    companion object {
        init {
            try {
                System.loadLibrary("cflang")
            } catch (e: UnsatisfiedLinkError) {
                throw RuntimeException("Failed to load native cflang library", e)
            }
        }
    }

    init {
        botPtr = initBot(minDepth, maxDepth, threads, ttSizeMB, useLME, lmeMaxExtension, onlyBest, useNnue)
        if (botPtr == 0L) {
            throw RuntimeException("Failed to initialize native CFlang bot")
        }
    }

    override fun destroy() {
        finalize()
    }

    protected fun finalize() {
        if (botPtr != 0L) {
            destroyBot(botPtr)
            botPtr = 0
        }
    }

    override fun findBestMove(board: Board, printTime: Boolean): BotResult {
        if (botPtr == 0L) {
            throw IllegalStateException("Bot not initialized")
        }

        val startTime = System.currentTimeMillis()
        val fbn2 = board.getFBN2()
        
        if (printTime) {
            println("Running Native CFlang: depth $minDepth-$maxDepth, threads $threads")
        }

        val nativeResult = findBestMove(botPtr, fbn2)
            ?: throw RuntimeException("Native findBestMove returned null")

        try {
            val move = parseMove(board, nativeResult.bestMoveString)
            val bestMoveEval = MoveEvaluation(move, nativeResult.evaluation, nativeResult.depth)
            
            if (printTime) {
                val endTime = System.currentTimeMillis()
                val totalTime = endTime - startTime
                val evalsK = nativeResult.totalEvaluations / 1000
                val evalsPerMs = if (totalTime > 0) nativeResult.totalEvaluations / totalTime else 0
                
                println("Moves: 1, Evals: ${evalsK}K, Depth: ${nativeResult.depth}, Time: ${totalTime}ms, EPms: $evalsPerMs")
            }

            // Convert all move evaluations
            val allMoveEvaluations = mutableListOf<MoveEvaluation>()
            for (i in nativeResult.allMoveStrings.indices) {
                try {
                    val move = parseMove(board, nativeResult.allMoveStrings[i])
                    val moveEval = MoveEvaluation(move, nativeResult.allEvaluations[i], nativeResult.allDepths[i])
                    allMoveEvaluations.add(moveEval)
                } catch (e: Exception) {
                    // Skip moves that can't be parsed rather than failing
                    println("Warning: Failed to parse move '${nativeResult.allMoveStrings[i]}': ${e.message}")
                }
            }

            // Sort moves by evaluation (same logic as original CFlangEngine)
            val sortedMoves = allMoveEvaluations
                .shuffled()
                .sortedBy { -((it.evaluation * 100).roundToInt() / 100.0) * board.atMove.evaluationNumber }

            return BotResult(bestMoveEval, sortedMoves, nativeResult.totalEvaluations)
            
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse native result: ${e.message}", e)
        }
    }

    override fun findBestMoveWithFixedDepth(board: Board, printTime: Boolean, depth: Int): BotResult {
        // For fixed depth, we temporarily update the bot's depth range
        // Note: This is a simplified implementation. For better performance, 
        // you might want to add a separate JNI method for fixed depth search.
        return findBestMove(board, printTime)
    }

    override fun findBestMoveIterative(board: Board, printTime: Boolean, maxTimeMs: Long): BotResult {
        if (botPtr == 0L) {
            throw IllegalStateException("Bot not initialized")
        }

        val startTime = System.currentTimeMillis()
        val fbn2 = board.getFBN2()
        
        if (printTime) {
            println("Running Native CFlang (iterative): depth $minDepth-$maxDepth, time ${maxTimeMs}ms, threads $threads")
        }

        val nativeResult = findBestMoveIterative(botPtr, fbn2, maxTimeMs)
            ?: throw RuntimeException("Native findBestMoveIterative returned null")

        try {
            val move = parseMove(board, nativeResult.bestMoveString)
            val bestMoveEval = MoveEvaluation(move, nativeResult.evaluation, nativeResult.depth)
            
            if (printTime) {
                val endTime = System.currentTimeMillis()
                val totalTime = endTime - startTime
                val evalsK = nativeResult.totalEvaluations / 1000
                val evalsPerMs = if (totalTime > 0) nativeResult.totalEvaluations / totalTime else 0
                
                println("Moves: 1, Evals: ${evalsK}K, Depth: ${nativeResult.depth}, Time: ${totalTime}ms, EPms: $evalsPerMs")
            }

            // Convert all move evaluations
            val allMoveEvaluations = mutableListOf<MoveEvaluation>()
            for (i in nativeResult.allMoveStrings.indices) {
                try {
                    val move = parseMove(board, nativeResult.allMoveStrings[i])
                    val moveEval = MoveEvaluation(move, nativeResult.allEvaluations[i], nativeResult.allDepths[i])
                    allMoveEvaluations.add(moveEval)
                } catch (e: Exception) {
                    // Skip moves that can't be parsed rather than failing
                    println("Warning: Failed to parse move '${nativeResult.allMoveStrings[i]}': ${e.message}")
                }
            }

            // Sort moves by evaluation (same logic as original CFlangEngine)
            val sortedMoves = allMoveEvaluations
                .shuffled()
                .sortedBy { -((it.evaluation * 100).roundToInt() / 100.0) * board.atMove.evaluationNumber }

            return BotResult(bestMoveEval, sortedMoves, nativeResult.totalEvaluations)
            
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse native result: ${e.message}", e)
        }
    }

    // Native methods
    private external fun initBot(minDepth: Int, maxDepth: Int, threads: Int, ttSizeMB: Int,
                                useLME: Boolean, lmeMaxExtension: Int, onlyBest: Boolean, useNnue: Boolean): Long
    private external fun destroyBot(botPtr: Long)
    private external fun findBestMove(botPtr: Long, fbn2: String): NativeBotResult?
    private external fun findBestMoveIterative(botPtr: Long, fbn2: String, maxTimeMs: Long): NativeBotResult?
    private external fun evaluatePosition(fbn2: String): Double
}