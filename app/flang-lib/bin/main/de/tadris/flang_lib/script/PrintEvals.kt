package de.tadris.flang_lib.script

import de.tadris.flang_lib.Board
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.bot.evaluation.FastNeoBoardEvaluation

fun main(){
    val eval = FastNeoBoardEvaluation()
    val board = Game(Board.fromFBN("+4r4P3PPK2Pf3P3P4p8p3pp2k2p6U-4"))

    println(board.currentState.getFBN2() + " -> ${eval.evaluate(board.currentState)}")

    board.replayAllGameStates { state, _ ->
        val eval = eval.evaluate(state)
        println(state.getFBN2() + " -> $eval")
    }
}