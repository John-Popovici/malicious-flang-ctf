package de.tadris.flang.network_api.script

import de.tadris.flang.network_api.FlangAPI
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.analysis.FullEvaluationData
import de.tadris.flang_lib.bot.BotResult
import de.tadris.flang_lib.bot.CFlangEngine
import de.tadris.flang_lib.bot.evaluation.MoveEvaluation
import de.tadris.flang_lib.puzzle.PuzzleGenerator
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.roundToInt

fun main(){
    val gamesCount = 1000
    val searchDepth = 7
    val reviewDepth = 9

    val totalStart = System.currentTimeMillis()
    var totalPuzzles = 0

    val api = FlangAPI("www.tadris.de", 443, "api/flang", useSSL = true, loggingEnabled = true)
    val credentials = File("doc/auth.txt").readText().split(":")
    api.login(credentials[0], credentials[1])

    val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2)

    repeat(gamesCount){ index ->
        executor.submit {
            val start = System.currentTimeMillis()
            val evaluationData = generateRandomGame(searchDepth)
            val reviewStart = System.currentTimeMillis()
            val reviewEngine = CFlangEngine(maxDepth = reviewDepth, ttSizeMB = 1024, threads = 1)
            val puzzleGenerator = PuzzleGenerator(evaluationData, reviewEngine)
            val puzzles = puzzleGenerator.searchPuzzles()

            puzzles.forEach { puzzle ->
                api.injectPuzzle(puzzle.startFMN, puzzle.puzzleFMN)
                totalPuzzles++
            }

            val reviewTime = System.currentTimeMillis() - reviewStart
            val generateTime = reviewStart - start
            val totalTime = reviewTime + generateTime

            val moves = evaluationData.evaluations.size
            val timePerMove = totalTime / moves
            println("Processed game ${index + 1}/${gamesCount}. Generate: ${generateTime/1000}s | Review: ${reviewTime/1000}s | Moves: $moves | Time per move: $timePerMove ms")
        }
    }

    executor.shutdown()
    executor.awaitTermination(1, TimeUnit.DAYS)

    val totalTime = System.currentTimeMillis() - totalStart
    println("-------------------")
    println("TOTAL TIME: " + (totalTime.toDouble() / 1000 / 60).roundToInt() + " min")
    println("TOTAL PUZZLES: $totalPuzzles")
    println("FOUND PUZZLES per game: ${totalPuzzles.toDouble() / gamesCount}")
    println("-------------------")
}

private fun generateRandomGame(depth: Int): FullEvaluationData {
    val engine = CFlangEngine(min(5, depth), depth, ttSizeMB = 256, threads = 1)
    val game = Game()
    val results = mutableListOf<BotResult>()
    repeat(6){
        val randomMove = game.currentState.getMoves(game.currentState.atMove).random()
        game.execute(randomMove)
        results += BotResult(MoveEvaluation.DUMMY, listOf(MoveEvaluation.DUMMY), 0)
    }
    while (!game.currentState.gameIsComplete() || results.size > 100){
        val result = engine.findBestMove(game.currentState, printTime = false)
        results += result
        game.execute(result.bestMove.move)
    }
    val fmn = game.getFMNv2()
    println("Generated $fmn")
    return FullEvaluationData(fmn, results)
}