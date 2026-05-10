package de.tadris.flang_lib.analysis

import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.Color
import kotlin.math.max

data class PlayerAccuracy(
    val accuracy: Double,
    val totalMoves: Int,
    val excellentMoves: Int,
    val goodMoves: Int,
    val inaccuracies: Int,
    val mistakes: Int,
    val blunders: Int,
    val averageCentipawnLoss: Double,
    val averageWinPercentLoss: Double
) {
    val errorRate: Double get() = (inaccuracies + mistakes + blunders).toDouble() / totalMoves
}

object AccuracyCalculator {
    
    fun calculatePlayerAccuracy(moves: List<MoveInfo>, color: Color): PlayerAccuracy {
        val playerMoves = moves.filter { moveInfo ->
            when (color) {
                true -> moveInfo.isWhiteMove
                false -> !moveInfo.isWhiteMove
            }
        }.filter { it.hasJudgment }
        
        if (playerMoves.isEmpty()) {
            return PlayerAccuracy(0.0, 0, 0, 0, 0, 0, 0, 0.0, 0.0)
        }
        
        val judgmentCounts = playerMoves.groupingBy { it.judgment!!.type }.eachCount()
        
        val excellentMoves = judgmentCounts[MoveJudgmentType.EXCELLENT] ?: 0
        val goodMoves = judgmentCounts[MoveJudgmentType.GOOD] ?: 0
        val inaccuracies = judgmentCounts[MoveJudgmentType.INACCURACY] ?: 0
        val mistakes = judgmentCounts[MoveJudgmentType.MISTAKE] ?: 0
        val blunders = judgmentCounts[MoveJudgmentType.BLUNDER] ?: 0
        
        val totalMoves = playerMoves.size
        
        val centipawnLosses = playerMoves.mapNotNull { it.judgment?.centipawnLoss?.coerceAtMost(200.0) }
        
        val avgCentipawnLoss = if (centipawnLosses.isNotEmpty()) {
            centipawnLosses.average()
        } else {
            0.0
        }
        
        val avgWinPercentLoss = playerMoves
            .mapNotNull { it.judgment?.winPercentLoss }
            .average()
            .let { if (it.isNaN()) 0.0 else it }
        
        val accuracy = calculateAccuracyFromWinRateLoss(avgWinPercentLoss)
        
        return PlayerAccuracy(
            accuracy = accuracy,
            totalMoves = totalMoves,
            excellentMoves = excellentMoves,
            goodMoves = goodMoves,
            inaccuracies = inaccuracies,
            mistakes = mistakes,
            blunders = blunders,
            averageCentipawnLoss = avgCentipawnLoss,
            averageWinPercentLoss = avgWinPercentLoss
        )
    }
    
    fun calculateGameAccuracy(moves: List<MoveInfo>): Pair<PlayerAccuracy, PlayerAccuracy> {
        val whiteAccuracy = calculatePlayerAccuracy(moves, COLOR_WHITE)
        val blackAccuracy = calculatePlayerAccuracy(moves, COLOR_BLACK)
        return Pair(whiteAccuracy, blackAccuracy)
    }
    
    private fun calculateAccuracyFromWinRateLoss(avgLoss: Double): Double {
        return max(0.0, 100.0 - (avgLoss * 1000))
    }

    fun getAccuracyGrade(accuracy: Double): String {
        return when {
            accuracy >= 95 -> "A+"
            accuracy >= 90 -> "A"
            accuracy >= 85 -> "A-"
            accuracy >= 80 -> "B+"
            accuracy >= 75 -> "B"
            accuracy >= 70 -> "B-"
            accuracy >= 65 -> "C+"
            accuracy >= 60 -> "C"
            accuracy >= 55 -> "C-"
            accuracy >= 50 -> "D"
            else -> "F"
        }
    }
}