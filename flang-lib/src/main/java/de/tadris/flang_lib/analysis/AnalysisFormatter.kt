package de.tadris.flang_lib.analysis

object AnalysisFormatter {

    fun formatPlayerAccuracy(accuracy: PlayerAccuracy): String {
        val sb = StringBuilder()
        
        sb.appendLine("Accuracy: ${String.format("%.1f", accuracy.accuracy)}% (${AccuracyCalculator.getAccuracyGrade(accuracy.accuracy)})")
        sb.appendLine("Total moves: ${accuracy.totalMoves}")
        sb.appendLine("Excellent: ${accuracy.excellentMoves}")
        sb.appendLine("Good: ${accuracy.goodMoves}")
        sb.appendLine("Inaccuracies: ${accuracy.inaccuracies}")
        sb.appendLine("Mistakes: ${accuracy.mistakes}")
        sb.appendLine("Blunders: ${accuracy.blunders}")
        sb.appendLine("Avg centipawn loss: ${String.format("%.1f", accuracy.averageCentipawnLoss)}")
        sb.appendLine("Avg win percent loss: ${String.format("%.2f", accuracy.averageWinPercentLoss * 100)}%")
        sb.appendLine("Error rate: ${String.format("%.1f", accuracy.errorRate * 100)}%")
        
        return sb.toString()
    }
    
    fun formatMoveInfo(moveInfo: MoveInfo): String {
        val sb = StringBuilder()
        
        val playerSymbol = if (moveInfo.isWhiteMove) "♔" else "♚"
        val ply = String.format("%3d", moveInfo.ply)
        val move = String.format("%-8s", moveInfo.moveNotation)
        val eval = String.format("%8s", moveInfo.getEvaluationString())
        
        sb.append("$ply. $playerSymbol $move $eval")
        
        moveInfo.judgment?.let { judgment ->
            val judgmentStr = when (judgment.type) {
                MoveJudgmentType.EXCELLENT -> "⭐"
                MoveJudgmentType.GOOD -> "✓"
                MoveJudgmentType.INACCURACY -> "?!"
                MoveJudgmentType.MISTAKE, MoveJudgmentType.MISS -> "?"
                MoveJudgmentType.BLUNDER -> "??"
                MoveJudgmentType.BOOK -> "📖"
                MoveJudgmentType.FORCED -> "□"
                MoveJudgmentType.RESIGN -> "#"
            }
            
            sb.append(" $judgmentStr")
            
            if (judgment.centipawnLoss > 0) {
                sb.append(" (-${String.format("%.0f", judgment.centipawnLoss)})")
            }
            
            judgment.comment?.let { comment ->
                sb.append(" $comment")
            }
        }
        
        moveInfo.bestMoveNotation?.let { bestMove ->
            if (bestMove != moveInfo.moveNotation) {
                sb.append(" [Best: $bestMove]")
            }
        }
        
        return sb.toString()
    }
    
    fun formatGameSummary(result: AnalysisResult): String {
        val sb = StringBuilder()
        
        sb.appendLine("Game Analysis Summary")
        sb.appendLine("===================")
        sb.appendLine("White: ${String.format("%.1f", result.whiteAccuracy.accuracy)}% (${AccuracyCalculator.getAccuracyGrade(result.whiteAccuracy.accuracy)})")
        sb.appendLine("Black: ${String.format("%.1f", result.blackAccuracy.accuracy)}% (${AccuracyCalculator.getAccuracyGrade(result.blackAccuracy.accuracy)})")
        sb.appendLine("Total errors: ${result.errorMoves.size}")
        sb.appendLine("Analysis depth: ${result.analysisDepth}")
        sb.appendLine("Time: ${result.analysisTimeMs}ms")
        
        return sb.toString()
    }
}