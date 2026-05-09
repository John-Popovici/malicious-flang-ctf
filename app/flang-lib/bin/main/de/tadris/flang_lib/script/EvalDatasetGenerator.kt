package de.tadris.flang_lib.script

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.analysis.FullEvaluationData
import de.tadris.flang_lib.bot.BotResult
import de.tadris.flang_lib.bot.CFlangEngine
import de.tadris.flang_lib.bot.evaluation.MoveEvaluation
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.random.Random

/**
 * Generates a dataset of positions with their deep search evaluations.
 * Output format: FMN|score (one per line)
 *
 * This dataset can be used to tune evaluation function parameters.
 */
fun main() {
    println("=== Evaluation Dataset Generator ===\n")

    // Configuration
    val inputFile = "doc/games2.txt"
    val searchDepth = 5  // Depth to search for "ground truth" evaluation
    val outputFile = "doc/eval_dataset_$searchDepth.txt"
    val maxGames = 10000  // Maximum games to process
    val threads = Runtime.getRuntime().availableProcessors()

    println("Configuration:")
    println("  Input: $inputFile")
    println("  Output: $outputFile")
    println("  Search depth: $searchDepth")
    println("  Max games: $maxGames")
    println("  Threads: $threads")
    println()

    // Load games
    println("Loading games from $inputFile...")
    val games = File(inputFile)
        .readLines()
        .shuffled()
        .filter { it.isNotBlank() }
        .take(maxGames)
        .map { Game.fromFMN(it) }

    println("Loaded ${games.size} games\n")

    // Create engine for evaluation
    val enginePath = "./cflang/cflang"

    // Output file
    val output = File(outputFile)
    output.writeText("")  // Clear file

    val lock = Object()
    val processedGames = AtomicInteger(0)
    val totalPositions = AtomicInteger(0)
    val executor = Executors.newFixedThreadPool(threads)

    // Process each game
    games.forEach { game ->
        executor.submit {
            try {
                val positions = mutableListOf<Pair<String, Double>>()


                game.replayAllGameStates { state, _ ->
                    if(state.gameIsComplete()) return@replayAllGameStates
                    // Create engine for this position
                    val engine = CFlangEngine(
                        minDepth = searchDepth,
                        maxDepth = searchDepth,
                        threads = 1,
                        cflangPath = enginePath,
                        useLME = true,
                        lmeMaxExtension = 3
                    )

                    // Get deep evaluation
                    val result = engine.findBestMove(state, false)
                    val score = result.bestMove.evaluation

                    positions.add(state.getFBN2() to score)
                }

                // Write positions to file
                synchronized(lock) {
                    output.appendText(positions.joinToString("\n") { (fmn, score) ->
                        "$fmn|$score"
                    })
                    if (positions.isNotEmpty()) {
                        output.appendText("\n")
                    }

                    totalPositions.addAndGet(positions.size)
                    val processed = processedGames.incrementAndGet()

                    if (processed % 10 == 0) {
                        println("Processed $processed/${games.size} games, ${totalPositions.get()} positions...")
                    }
                }

            } catch (e: Exception) {
                println("Error processing game: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Wait for completion
    executor.shutdown()
    while (!executor.isTerminated) {
        Thread.sleep(1000)
    }

    println("\n=== Complete ===")
    println("Total positions: ${totalPositions.get()}")
    println("Output saved to: $outputFile")
}