package de.tadris.flang_lib.script

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.analysis.FullEvaluationData
import de.tadris.flang_lib.bot.BotResult
import de.tadris.flang_lib.bot.CFlangEngine
import de.tadris.flang_lib.bot.evaluation.MoveEvaluation
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.random.Random

fun main() {
    println("=== Evaluation Dataset Generator ===\n")

    // Configuration
    val searchDepth = 4  // Depth to search for "ground truth" evaluation
    val outputFile = "doc/eval_dataset_self_$searchDepth.txt"
    val maxGames = 200000  // Maximum games to process
    val threads = 12 // Runtime.getRuntime().availableProcessors()

    println("Configuration:")
    println("  Output: $outputFile")
    println("  Search depth: $searchDepth")
    println("  Max games: $maxGames")
    println("  Threads: $threads")
    println()

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
    repeat(maxGames){
        executor.submit {
            try {
                val positions = mutableListOf<Pair<String, Double>>()

                val game = Game()
                repeat(6){
                    game.execute(game.currentState.getMoves().random())
                }
                while (!game.currentState.gameIsComplete()){
                    val engine = CFlangEngine(
                        minDepth = 1,
                        maxDepth = searchDepth,
                        threads = 1,
                        cflangPath = enginePath,
                        useLME = true,
                        lmeMaxExtension = 3,
                        onlyFindBest = true,
                    )

                    // Get deep evaluation
                    val result = engine.findBestMove(game.currentState, false)
                    val score = result.bestMove.evaluation

                    if(score.absoluteValue > 5000) break // we found a mate -> we dont need mate scores for our dataset
                    if(game.moveList.size > 100) break // probably endless loop

                    positions.add(game.currentState.getFBN2() to score)

                    game.execute(result.bestMove.move)
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

                    if(processed % 10 == 0){
                        println("Processed $processed/${maxGames} games, ${totalPositions.get()} positions...")
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