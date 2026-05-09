package de.tadris.flang_lib.script

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.bot.CFlangEngine
import de.tadris.flang_lib.bot.Engine
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.bot.evaluation.FastNeoBoardEvaluation
import de.tadris.flang_lib.bot.fast.FastFlangBot

fun main(){
    val forEveryMove = false
    val moveStep = 5
    val game = Game()
    val depth = 8

    val engine: Engine =
        FastFlangBot(depth, depth, false, { FastNeoBoardEvaluation() }, ttSizeMB = 1024, threads = 1)
    //val engine: Engine = CFlangEngine(minDepth = 1, maxDepth = depth, ttSizeMB = 2048, threads = Runtime.getRuntime().availableProcessors())

    fun showForBoard(board: Board){
        val result = engine.findBestMoveWithFixedDepth(board, true, depth)
        println(result)
    }

    val start = System.currentTimeMillis()
    if(forEveryMove){
        var counter = 0
        game.replayAllGameStates { step, _ ->
            if(counter % moveStep == 0){
                showForBoard(step)
            }
            counter++
        }
    }else{
        showForBoard(game.currentState)
    }
    val time = System.currentTimeMillis() - start
    println("\nTotal time: $time ms")
}