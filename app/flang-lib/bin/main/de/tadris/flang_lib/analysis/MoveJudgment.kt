package de.tadris.flang_lib.analysis

import de.tadris.flang_lib.COLOR_WHITE

data class MoveJudgment(
    val type: MoveJudgmentType,
    val comment: MoveJudgmentComment? = null,
    val centipawnLoss: Double = 0.0,
    val winPercentLoss: Double = 0.0
) {
    companion object {
        fun evaluate(
            ply: Int,
            actualEval: PositionEvaluation,
            bestEval: PositionEvaluation,
            previousEval: PositionEvaluation,
            isWhite: Boolean,
            forced: Boolean,
        ): MoveJudgment {
            
            val actualFromPlayer = actualEval.fromPlayerPerspective(isWhite)
            val bestFromPlayer = bestEval.fromPlayerPerspective(isWhite)
            val previousFromPlayer = previousEval.fromPlayerPerspective(isWhite)
            
            val centipawnLoss = maxOf(0.0, bestFromPlayer.centipawns - actualFromPlayer.centipawns)
            val winPercentLoss = WinRate.getUnidirectionalWinRate(bestFromPlayer.centipawns, COLOR_WHITE) - WinRate.getUnidirectionalWinRate(actualFromPlayer.centipawns, COLOR_WHITE)
            
            // Enhanced mate handling logic
            val (type, comment) = evaluateWithMateContext(
                ply, actualFromPlayer, bestFromPlayer, previousFromPlayer, centipawnLoss, winPercentLoss, forced
            )
            
            // Additional comment enhancement for material loss (non-mate situations)
            val enhancedComment = when {
                centipawnLoss > 200 && comment == MoveJudgmentComment.BLUNDER ->
                    MoveJudgmentComment.MAJOR_MATERIAL_LOSS
                else -> comment
            }
            
            // Cap centipawn loss for display purposes (mate evaluations can be extreme)
            val displayCentipawnLoss = if (centipawnLoss > 1000) {
                // For mate-level losses, show a reasonable number for display
                when (type) {
                    MoveJudgmentType.BLUNDER -> if (comment in setOf(MoveJudgmentComment.MISSED_FORCED_MATE, MoveJudgmentComment.ALLOWS_MATE)) 999.0 else centipawnLoss.coerceAtMost(500.0)
                    else -> centipawnLoss.coerceAtMost(300.0)
                }
            } else {
                centipawnLoss
            }
            
            return MoveJudgment(
                type = type,
                comment = enhancedComment,
                centipawnLoss = displayCentipawnLoss,
                winPercentLoss = winPercentLoss
            )
        }
        
        private fun evaluateWithMateContext(
            ply: Int,
            actualFromPlayer: PositionEvaluation,
            bestFromPlayer: PositionEvaluation,
            previousFromPlayer: PositionEvaluation,
            centipawnLoss: Double,
            winPercentLoss: Double,
            forced: Boolean,
        ): Pair<MoveJudgmentType, MoveJudgmentComment> {
            
            // Check if we're in a pre-mate situation (either player has forced mate)
            val isPreMateSituation = bestFromPlayer.isMate || actualFromPlayer.isMate || previousFromPlayer.isMate

            return when {
                // Case 1: Best move was mate, but actual move isn't
                bestFromPlayer.isMate && !actualFromPlayer.isMate ->
                    MoveJudgmentType.BLUNDER to MoveJudgmentComment.MISSED_FORCED_MATE

                // Case 2: Both moves lead to mate - compare mate distances (MOST SPECIFIC)
                bestFromPlayer.isMate && actualFromPlayer.isMate -> {
                    val bestMateForUs = bestFromPlayer.isMateForMe
                    val actualMateForUs = actualFromPlayer.isMateForMe

                    // If best move gives us mate but actual move gives opponent mate
                    if (bestMateForUs && !actualMateForUs) {
                        MoveJudgmentType.BLUNDER to MoveJudgmentComment.ALLOWS_MATE
                    }
                    // If both moves give us mate, compare distances
                    else if (bestMateForUs) {
                        val bestMateDistance = bestFromPlayer.mateInMoves ?: 0
                        val actualMateDistance = actualFromPlayer.mateInMoves ?: 0

                        when {
                            bestMateDistance == actualMateDistance ->
                                MoveJudgmentType.EXCELLENT to MoveJudgmentComment.BEST_MOVE
                            actualMateDistance <= bestMateDistance + 2 ->
                                MoveJudgmentType.INACCURACY to MoveJudgmentComment.FASTER_MATE_AVAILABLE
                            else ->
                                MoveJudgmentType.MISTAKE to MoveJudgmentComment.FASTER_MATE_AVAILABLE
                        }
                    }
                    // If both moves give opponent mate, pick the slower one (better defense)
                    else if (!actualMateForUs) {
                        val bestMateDistance = kotlin.math.abs(bestFromPlayer.mateInMoves ?: 0)
                        val actualMateDistance = kotlin.math.abs(actualFromPlayer.mateInMoves ?: 0)

                        when {
                            actualMateDistance >= bestMateDistance ->
                                MoveJudgmentType.GOOD to MoveJudgmentComment.DEFENDING_AGAINST_MATE
                            actualMateDistance == bestMateDistance - 1 ->
                                MoveJudgmentType.INACCURACY to MoveJudgmentComment.DEFENDING_AGAINST_MATE
                            else ->
                                MoveJudgmentType.MISTAKE to MoveJudgmentComment.ALLOWS_MATE
                        }
                    }
                    // Fallback
                    else {
                        MoveJudgmentType.GOOD to MoveJudgmentComment.GOOD_MOVE
                    }
                }

                // Case 3: Actual move allows opponent mate (but best move doesn't)
                !bestFromPlayer.isMate && actualFromPlayer.isMate && actualFromPlayer.mateInMoves != null && actualFromPlayer.isMateForOpponent ->
                    MoveJudgmentType.BLUNDER to MoveJudgmentComment.ALLOWS_MATE

                // Case 4: Previous position was mate for opponent, we're defending
                previousFromPlayer.isMate && previousFromPlayer.mateInMoves != null && previousFromPlayer.isMateForOpponent -> {
                    if (!actualFromPlayer.isMate || (actualFromPlayer.mateInMoves ?: 0) >= previousFromPlayer.mateInMoves) {
                        MoveJudgmentType.GOOD to MoveJudgmentComment.DEFENDING_AGAINST_MATE
                    } else {
                        // Making the mate faster for opponent
                        MoveJudgmentType.MISTAKE to MoveJudgmentComment.ALLOWS_MATE
                    }
                }

                // Case 5: We had mate, now we don't (or made it longer)
                previousFromPlayer.isMate && previousFromPlayer.isMateForMe -> {
                    MoveJudgmentType.BLUNDER to MoveJudgmentComment.MISSED_FORCED_MATE
                }
                
                // Case 6: Normal position evaluation (no mate involved)
                !isPreMateSituation -> {
                    when {
                        forced -> MoveJudgmentType.GOOD to MoveJudgmentComment.FORCED_MOVE
                        ply <= 3 && centipawnLoss < 25 -> MoveJudgmentType.BOOK to MoveJudgmentComment.BOOK_MOVE
                        centipawnLoss == 0.0 -> MoveJudgmentType.EXCELLENT to MoveJudgmentComment.BEST_MOVE
                        winPercentLoss < 0.04 -> MoveJudgmentType.GOOD to MoveJudgmentComment.GOOD_MOVE
                        winPercentLoss < 0.08 -> MoveJudgmentType.INACCURACY to MoveJudgmentComment.INACCURACY
                        winPercentLoss < 0.12 -> MoveJudgmentType.MISTAKE to MoveJudgmentComment.MISTAKE
                        winPercentLoss < 0.16 -> MoveJudgmentType.BLUNDER to MoveJudgmentComment.BLUNDER
                        else -> MoveJudgmentType.BLUNDER to MoveJudgmentComment.MAJOR_BLUNDER
                    }
                }
                
                // Default case for other mate situations
                else -> MoveJudgmentType.GOOD to MoveJudgmentComment.GOOD_MOVE
            }
        }
    }
    
    val isGoodMove: Boolean get() = type in setOf(MoveJudgmentType.EXCELLENT, MoveJudgmentType.GOOD)
    
    val isError: Boolean get() = type in setOf(MoveJudgmentType.INACCURACY, MoveJudgmentType.MISTAKE, MoveJudgmentType.BLUNDER)
}