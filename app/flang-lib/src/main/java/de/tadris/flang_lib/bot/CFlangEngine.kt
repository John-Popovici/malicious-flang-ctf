package de.tadris.flang_lib.bot

import de.tadris.flang_lib.bot.evaluation.MoveEvaluation
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.evaluationNumber
import de.tadris.flang_lib.parseMove
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.math.roundToInt

class CFlangEngine(
    private val minDepth: Int = 1,
    private val maxDepth: Int = 6,
    private val cflangPath: String = "./cflang/cflang",
    private val threads: Int = Runtime.getRuntime().availableProcessors() - 1,
    private val ttSizeMB: Int = 64,
    private val maxTimeMs: Long = -1,
    private val useLME: Boolean = false,
    private val lmeMaxExtension: Int = 1,
    private val onlyFindBest: Boolean = false,
) : Engine {

    init {
        // Verify that the cflang executable exists and is executable
        val cflangFile = File(cflangPath)
        if (!cflangFile.exists()) {
            throw IllegalArgumentException("CFlang executable not found at: $cflangPath")
        }
        if (!cflangFile.canExecute()) {
            throw IllegalArgumentException("CFlang executable is not executable: $cflangPath")
        }
    }

    override fun findBestMove(board: Board, printTime: Boolean): BotResult {
        return runCFlangSearch(board, printTime, null)
    }

    override fun findBestMoveWithFixedDepth(board: Board, printTime: Boolean, depth: Int): BotResult {
        return runCFlangSearch(board, printTime, null, overrideMaxDepth = depth)
    }

    override fun findBestMoveIterative(board: Board, printTime: Boolean, maxTimeMs: Long): BotResult {
        return runCFlangSearch(board, printTime, maxTimeMs)
    }

    private fun runCFlangSearch(
        board: Board,
        printTime: Boolean,
        timeLimit: Long? = null,
        overrideMaxDepth: Int? = null
    ): BotResult {
        val startTime = System.currentTimeMillis()
        val fbn2 = board.getFBN2()
        val actualMaxDepth = overrideMaxDepth ?: maxDepth
        val actualTimeLimit = timeLimit ?: maxTimeMs
        
        // Build command arguments
        val command = mutableListOf(
            cflangPath,
            fbn2,
            "--min-depth", minDepth.coerceAtLeast(1).toString(),
            "--max-depth", actualMaxDepth.coerceAtLeast(1).toString(),
            "--threads", threads.toString(),
            "--ttsize", ttSizeMB.toString(),
            "--machine-readable"
        )

        if(maxDepth == 0){
            command.add("--evaluation-only")
        }
        
        if (useLME) {
            command.add("--use-lme")
            command.addAll(listOf("--lme-max-ext", lmeMaxExtension.toString()))
        }
        
        if (actualTimeLimit > 0) {
            command.addAll(listOf("--max-time", actualTimeLimit.toString()))
        }

        if(onlyFindBest){
            command.add("--only-best")
        }

        if (printTime) {
            println("Running CFlang: ${command.joinToString(" ")}")
        }

        try {
            val processBuilder = ProcessBuilder(command)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            // Read all output lines
            val outputLines = reader.readLines()
            val errorLines = errorReader.readLines()
            
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                val errorMsg = errorLines.joinToString("\n")
                throw RuntimeException("CFlang process failed with exit code $exitCode: $errorMsg. command: $command")
            }
            
            // Parse the machine-readable output
            val result = parseCFlangOutput(outputLines, board)
            
            // Print timing information if requested
            if (printTime) {
                val endTime = System.currentTimeMillis()
                val totalTime = endTime - startTime
                val evalsPerMs = if (totalTime > 0) result.count / totalTime else 0
                
                println("Moves: ${result.evaluations.size}, Evals: ${formatEvals(result.count)}, Depth: ${result.bestMove.depth}, Time: ${totalTime}ms, EPms: $evalsPerMs")
            }
            
            return result
            
        } catch (e: Exception) {
            throw RuntimeException("Failed to execute CFlang: ${e.message}", e)
        }
    }

    private fun formatEvals(evals: Long) = when {
        evals > 10_000_000L -> (evals / 1_000_000L).toString() + "M"
        evals > 10_000L -> (evals / 1000L).toString() + "K"
        else -> evals.toString()
    }

    private fun parseCFlangOutput(lines: List<String>, board: Board): BotResult {
        var bestMove: MoveEvaluation? = null
        val allMoves = mutableListOf<MoveEvaluation>()
        var totalEvaluations = 0L

        for (line in lines) {
            when {
                line.startsWith("EVAL ") -> {
                    val eval = line.substring(5).toDouble()
                    return BotResult(
                        MoveEvaluation(0L, eval, 0),
                        emptyList(),
                        1
                    )
                }
                line.startsWith("BEST ") -> {
                    val parts = line.substring(5).split(" ")
                    if (parts.size >= 3) {
                        val moveStr = parts[0]
                        val eval = parts[1].toDouble()
                        val depth = parts[2].toInt()
                        
                        try {
                            val move = parseMove(board, moveStr)
                            bestMove = MoveEvaluation(move, eval, depth)
                        } catch (e: Exception) {
                            throw RuntimeException("Failed to parse best move '$moveStr': ${e.message}", e)
                        }
                    }
                }
                line.startsWith("MOVE ") -> {
                    val parts = line.substring(5).split(" ")
                    if (parts.size >= 3) {
                        val moveStr = parts[0]
                        val eval = parts[1].toDouble()
                        val depth = parts[2].toInt()
                        
                        try {
                            val move = parseMove(board, moveStr)
                            allMoves.add(MoveEvaluation(move, eval, depth))
                        } catch (e: Exception) {
                            // Skip moves that can't be parsed rather than failing
                            println("Warning: Failed to parse move '$moveStr': ${e.message}")
                        }
                    }
                }
                line.startsWith("EVALUATIONS ") -> {
                    totalEvaluations = line.substring(12).toLong()
                }
            }
        }

        if (bestMove == null) {
            throw RuntimeException("No best move found in CFlang output")
        }

        val sortedMoves = allMoves
            .shuffled()
            .sortedBy { -((it.evaluation * 100).roundToInt() / 100.0) * board.atMove.evaluationNumber }
        return BotResult(bestMove, sortedMoves, totalEvaluations)
    }

}