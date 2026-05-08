package de.tadris.flang_lib.script

import de.tadris.flang_lib.analysis.AnalysisFormatter
import de.tadris.flang_lib.analysis.AnalysisListener
import de.tadris.flang_lib.analysis.AnalysisResult
import de.tadris.flang_lib.analysis.ComputerAnalysis
import de.tadris.flang_lib.analysis.MoveInfo
import de.tadris.flang_lib.bot.Engine
import de.tadris.flang_lib.bot.fast.FastFlangBot
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.bot.CFlangEngine

fun main(){
    val fmn = "Uf3 b7a6 Hd3 uc6 c2c3 kb7 Pb2 kb6 a1 e7f6 Ud4 he6 e2e3 d4 e6 re6 e3d4 d7d6 f4 g5 h2h3 re8 h5 fb5 h2 e2 Hg3 f4 d1 f2 e2 a2 e1 e2 Pg3 g3 e2 e2 g3 g7 Ph4 ka5 f4 pb5 h5 a4 g5 g2 Ph6 f7f6 e3 f5 h5 c4 h4 h6 f4 c3 e5 kb5 f5 c4 g4 d3 f5f6 e2 f5 d1"

    val listener = object : AnalysisListener {
        override fun onAnalysisStarted(totalMoves: Int) {
            println("Starting analysis of $totalMoves moves...")
        }

        override fun onMoveAnalyzed(currentMove: Int, totalMoves: Int, moveInfo: MoveInfo?) {
            val progress = (currentMove * 100) / totalMoves
            println("Analyzing move $currentMove/$totalMoves ($progress%): ${moveInfo?.moveNotation ?: "unknown"}")
        }

        override fun onAnalysisCompleted(result: AnalysisResult) {
            println("Analysis completed in ${result.analysisTimeMs}ms")
        }
    }

    val bot = CFlangEngine(cflangPath = "./cflang/cflang_nnue", minDepth = 5, maxDepth = 8, ttSizeMB = 512, useLME = true)
    val analysis = ComputerAnalysis(fmn, object : Engine {
        override fun findBestMove(board: Board, printTime: Boolean) =
            bot.findBestMove(board, true)
        //bot.findBestMoveIterative(board, true, 2000)
    }, listener)
    val result = analysis.analyze()

    println("=".repeat(50))
    println("ANALYSIS RESULTS")
    println("=".repeat(50))

    // Print formatted summary
    println(AnalysisFormatter.formatGameSummary(result))
    println()

    // Print detailed move analysis
    println("DETAILED MOVE ANALYSIS:")
    println("-".repeat(50))
    result.moves.forEach { moveInfo ->
        println(AnalysisFormatter.formatMoveInfo(moveInfo))
    }

    println()
    println("WHITE PLAYER ANALYSIS:")
    println("-".repeat(30))
    println(AnalysisFormatter.formatPlayerAccuracy(result.whiteAccuracy))

    println()
    println("BLACK PLAYER ANALYSIS:")
    println("-".repeat(30))
    println(AnalysisFormatter.formatPlayerAccuracy(result.blackAccuracy))
}