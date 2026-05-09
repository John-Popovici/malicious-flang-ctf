package de.tadris.flang_lib.script

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.analysis.FullEvaluationData
import de.tadris.flang_lib.bot.BotResult
import de.tadris.flang_lib.bot.CFlangEngine
import de.tadris.flang_lib.bot.evaluation.MoveEvaluation
import de.tadris.flang_lib.puzzle.PuzzleGenerator
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class Puzzle(
    val id: Int,
    val startFmn: String,
    val puzzleFmn: String
)

fun loadPuzzles(filePath: String): List<Puzzle> {
    val puzzles = mutableListOf<Puzzle>()
    val file = File(filePath)

    file.useLines { lines ->
        // Skip the header lines (first 3 lines: header, separator, content starts at line 4)
        lines.drop(3).forEach { line ->
            // Parse table format: | id | startfmn | puzzlefmn |
            if (line.startsWith("|") && line.trim() != "+------+") {
                val parts = line.split("|")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (parts.size >= 3) {
                    try {
                        val id = parts[0].toInt()
                        val startFmn = parts[1]
                        val puzzleFmn = parts[2]

                        puzzles.add(Puzzle(id, startFmn, puzzleFmn))
                    } catch (e: NumberFormatException) {
                        // Skip lines that don't have valid integer IDs
                    }
                }
            }
        }
    }

    return puzzles
}

fun main() {
    val reviewDepth = 7

    val fakeMove = MoveEvaluation.DUMMY
    val puzzles = loadPuzzles("doc/puzzles.txt")
    println("Loaded ${puzzles.size} puzzles")

    val results = mutableMapOf<Puzzle, Boolean>()

    val executor = Executors.newFixedThreadPool(12)
    val filtered = puzzles.filter { !it.puzzleFmn.contains(" ") }
    filtered.forEachIndexed { index, puzzle ->
        executor.submit {
            val engine = CFlangEngine(minDepth = reviewDepth, maxDepth = reviewDepth, ttSizeMB = 512, threads = 1)
            val gameFMN = puzzle.startFmn + " " + puzzle.puzzleFmn
            val moves = gameFMN.count { it == ' ' } + 1
            val evaluations = mutableListOf<BotResult>()
            repeat(moves - 1){
                evaluations += BotResult(fakeMove, listOf(fakeMove), 0)
            }
            val relevantEval = engine.findBestMove(Game.fromFMN(puzzle.startFmn, null).currentState)
            evaluations += relevantEval
            evaluations += BotResult(fakeMove, listOf(fakeMove), 0)

            val puzzleGenerator = PuzzleGenerator(FullEvaluationData(puzzle.startFmn, evaluations), engine, skipRecalc = true)
            val puzzles = puzzleGenerator.searchPuzzles()

            val puzzleData = puzzles.find { it.startFMN == puzzle.startFmn }
            val validated = puzzleData != null
            println("Puzzle $index")
            println("FMN: " + puzzle.startFmn + " / Puzzle: " + puzzle.puzzleFmn)
            println("Eval: $relevantEval")
            println("Validated: $validated")
            if(puzzleData != null){
                println("Info: type=" + puzzleData.type)
            }

            synchronized(results){
                results += puzzle to validated
            }

            println("=========================")
        }
    }
    executor.shutdown()
    executor.awaitTermination(1, TimeUnit.DAYS)

    println("Validated: " + results.values.count { it })
    println("Not Validated: " + results.values.count { !it })
    println("Total: " + results.size)

    println("Non validated IDs: ")
    println("(" + results.entries.filter { !it.value }.joinToString(", "){ it.key.id.toString() } + ")")

}