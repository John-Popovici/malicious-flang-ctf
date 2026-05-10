package de.tadris.flang_lib.script

import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.bot.evaluation.FastNeoBoardEvaluation
import java.io.File

fun main(){
    val bucketCount = 31
    val min = -1000
    val max = 1000
    val bucketValues = IntArray(bucketCount)
    val bucketCounts = IntArray(bucketCount)
    val eval = FastNeoBoardEvaluation()

    var count = 0
    File("doc/games2.txt").useLines { seq ->
        seq.forEach { fmn ->
            val game = Game.fromFMN(fmn)
            val winner = when {
                game.currentState.hasWon(COLOR_WHITE) -> 1
                game.currentState.hasWon(COLOR_BLACK) -> -1
                else -> 0
            }
            game.replayAllGameStates { state, _ ->
                val score = eval.evaluate(state)
                val bucket = ((score - min) / (max - min) * bucketCount)
                    .toInt()
                    .coerceIn(0, bucketCount - 1)

                bucketValues[bucket] += winner
                bucketCounts[bucket] += 1
            }

            count++
            if(count % 100 == 0) println("Analyzed $count game...")
        }
    }

    val bucketSize = (max - min).toDouble() / bucketCount
    for(bucket in 0 ..< bucketCount){
        val bucketValue = min + bucket * bucketSize + bucketSize / 2
        val avg = bucketValues[bucket].toDouble() / bucketCounts[bucket]
        println("$bucketValue,$avg,${bucketCounts[bucket]}")
    }
}