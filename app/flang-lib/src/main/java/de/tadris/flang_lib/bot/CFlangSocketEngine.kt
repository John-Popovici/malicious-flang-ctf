package de.tadris.flang_lib.bot

import de.tadris.flang_lib.bot.evaluation.MoveEvaluation
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.evaluationNumber
import de.tadris.flang_lib.parseMove
import java.io.*
import java.net.Socket
import kotlin.math.roundToInt

class CFlangSocketEngine(
    private val cflangPath: String = "./cflang/cflang",
    private val host: String = "localhost", 
    private val port: Int = 8080,
    private val minDepth: Int = 1,
    private val maxDepth: Int = 6,
    private val threads: Int = Runtime.getRuntime().availableProcessors() - 1,
    private val ttSizeMB: Int = 64,
    private val useLME: Boolean = false,
    private val lmeMaxExtension: Int = 1,
    private val startServer: Boolean = true,
) : Engine, AutoCloseable {

    private var cflangProcess: Process? = null
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var isClosed = false

    init {
        if(startServer)  {
            startCFlangSocketServer()
            // Add shutdown hook as backup cleanup
            Runtime.getRuntime().addShutdownHook(Thread {
                if (!isClosed) {
                    cleanupResources()
                }
            })
        }
        connectToServer()
    }

    private fun startCFlangSocketServer() {
        // Build command to start cflang in socket mode
        val command = mutableListOf(
            cflangPath,
            "--socket",
            "--port", port.toString(),
            "--threads", threads.toString(),
            "--ttsize", ttSizeMB.toString()
        )
        
        if (useLME) {
            command.add("--use-lme")
            command.addAll(listOf("--lme-max-ext", lmeMaxExtension.toString()))
        }

        try {
            val processBuilder = ProcessBuilder(command)
            cflangProcess = processBuilder.start()
            
            // Give the server a moment to start up
            Thread.sleep(1000)
            
            // Check if process is still alive
            if (!cflangProcess!!.isAlive) {
                val errorReader = BufferedReader(InputStreamReader(cflangProcess!!.errorStream))
                val error = errorReader.readText()
                throw RuntimeException("CFlang socket server failed to start: $error")
            }
            
        } catch (e: Exception) {
            throw RuntimeException("Failed to start CFlang socket server: ${e.message}", e)
        }
    }

    private fun connectToServer() {
        try {
            socket = Socket(host, port)
            writer = PrintWriter(socket!!.getOutputStream(), true)
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
        } catch (e: Exception) {
            throw RuntimeException("Failed to connect to CFlang socket server: ${e.message}", e)
        }
    }

    override fun findBestMove(board: Board, printTime: Boolean): BotResult {
        return findBestMoveWithFixedDepth(board, printTime, maxDepth)
    }

    override fun findBestMoveWithFixedDepth(board: Board, printTime: Boolean, depth: Int): BotResult {
        return searchPosition(board, minDepth, depth, -1, printTime)
    }

    override fun findBestMoveIterative(board: Board, printTime: Boolean, maxTimeMs: Long): BotResult {
        return searchPosition(board, minDepth, maxDepth, maxTimeMs, printTime)
    }

    private fun searchPosition(board: Board, minDepth: Int, maxDepth: Int, maxTimeMs: Long, printTime: Boolean): BotResult {
        if (isClosed) {
            throw IllegalStateException("CFlangSocketEngine has been closed")
        }
        
        val startTime = System.currentTimeMillis()
        val fbn2 = board.getFBN2()
        
        // Send search command
        val command = "SEARCH $fbn2 $minDepth $maxDepth $maxTimeMs"
        writer!!.println(command)
        writer!!.flush()
        
        try {
            // Read response
            val lines = mutableListOf<String>()
            var line: String?
            
            while (reader!!.readLine().also { line = it } != null) {
                lines.add(line!!)
                
                // Check if we have all the data (ends with EVALUATIONS line)
                if (line.startsWith("EVALUATIONS ")) {
                    break
                }
            }
            
            // Parse response
            val result = parseSearchResponse(lines, board)
            
            if (printTime) {
                val totalTime = System.currentTimeMillis() - startTime
                val evalsK = (result.count / 1000.0).roundToInt()
                val evalsPerMs = if (totalTime > 0) result.count / totalTime else 0
                println("Moves: ${result.evaluations.size}, Evals: ${evalsK}K, Depth: ${result.bestMove.depth}, Time: ${totalTime}ms, EPms: $evalsPerMs")
            }
            
            return result
            
        } catch (e: Exception) {
            throw RuntimeException("Failed to get response from CFlang socket server: ${e.message}", e)
        }
    }

    private fun parseSearchResponse(lines: List<String>, board: Board): BotResult {
        var bestMove: MoveEvaluation? = null
        val moveEvaluations = mutableListOf<MoveEvaluation>()
        var totalEvaluations = 0L
        
        for (line in lines) {
            when {
                line.startsWith("BEST ") -> {
                    val parts = line.split(" ")
                    if (parts.size >= 4) {
                        val moveStr = parts[1]
                        val evaluation = parts[2].toDouble()
                        val depth = parts[3].toInt()
                        val move = parseMove(board, moveStr)
                        bestMove = MoveEvaluation(move, evaluation, depth)
                    }
                }
                line.startsWith("MOVE ") -> {
                    val parts = line.split(" ")
                    if (parts.size >= 4) {
                        val moveStr = parts[1]
                        val evaluation = parts[2].toDouble()
                        val depth = parts[3].toInt()
                        val move = parseMove(board, moveStr)
                        moveEvaluations.add(MoveEvaluation(move, evaluation, depth))
                    }
                }
                line.startsWith("EVALUATIONS ") -> {
                    totalEvaluations = line.substring(12).trim().toLong()
                }
            }
        }
        
        if (bestMove == null) {
            throw RuntimeException("No best move found in response")
        }

        val sortedEvaluations = moveEvaluations
            .shuffled()
            .sortedBy { -((it.evaluation * 100).roundToInt() / 100.0) * board.atMove.evaluationNumber }
        return BotResult(bestMove, sortedEvaluations, totalEvaluations)
    }

    override fun destroy() {
        close()
    }

    override fun close() {
        cleanupResources()
    }
    
    @Suppress("NewApi") // shouldnt be executed on Android
    private fun cleanupResources() {
        if (isClosed) return
        
        try {
            // Send disconnect command to server if connection is still active
            if (writer != null && socket?.isConnected == true) {
                writer?.println("DISCONNECT")
                writer?.flush()
                // Wait for disconnect acknowledgment
                try {
                    val response = reader?.readLine()
                    if (response == "DISCONNECTED") {
                        // Graceful disconnect confirmed
                    }
                } catch (e: Exception) {
                    // Ignore read errors during disconnect
                }
            }
            
            // Close socket connections
            writer?.close()
            reader?.close()
            socket?.close()
            
            // Terminate the CFlang process
            cflangProcess?.destroyForcibly()
            cflangProcess?.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            
        } catch (e: Exception) {
            // Best effort cleanup - don't throw exceptions during cleanup
            try {
                cflangProcess?.destroyForcibly()
            } catch (ignored: Exception) {}
        } finally {
            isClosed = true
            writer = null
            reader = null
            socket = null
            cflangProcess = null
        }
    }
}