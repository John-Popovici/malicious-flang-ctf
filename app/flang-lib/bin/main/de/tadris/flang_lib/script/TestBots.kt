package de.tadris.flang_lib.script

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.Variant
import de.tadris.flang_lib.bot.CFlangEngine
import de.tadris.flang_lib.bot.fast.FastFlangBot
import de.tadris.flang_lib.getNotationV2

fun main(){
    val white = FastFlangBot(minDepth = 1, maxDepth = 20, threads = 23, useLME = true, lmeMaxExtension = 3, ttSizeMB = 512)
    val black = FastFlangBot(minDepth = 1, maxDepth = 20, threads = 23, useLME = true, lmeMaxExtension = 3, ttSizeMB = 512)

    val time = 2000L

    val game = Game(Variant.NEXT)
    while(!game.currentState.gameIsComplete()){
        val result = when(game.currentState.atMove){
            true -> white.findBestMoveIterative(game.currentState, true, time)
            false -> black.findBestMoveIterative(game.currentState, true, time)
        }
        println("Current estimated eval: " + result.bestMove.evaluation)
        val nextMove = result.bestMove.move
        println("Playing move: " + nextMove.getNotationV2(game.currentState))

        game.execute(nextMove)

        if(game.moveList.size > 150) {
            println("Too many moves -> draw")
            break
        }
    }

    println("Game completed.")
    println("Winner: ${game.currentState.getWinningColor()}")
    println("FMNv1: " + game.getFMNv1())
    println("FMNv2: " + game.getFMNv2())
}