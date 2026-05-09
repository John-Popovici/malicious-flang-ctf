package de.tadris.flang_lib.analysis

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.bot.BotResult
import de.tadris.flang_lib.bot.Engine
import de.tadris.flang_lib.bot.evaluation.FastNeoBoardEvaluation
import de.tadris.flang_lib.bot.fast.FastFlangBot
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.getNotationV1
import de.tadris.flang_lib.getNotationV2
import de.tadris.flang_lib.getResignColor
import de.tadris.flang_lib.isResign
import kotlin.math.absoluteValue
import kotlin.math.min

class ComputerAnalysis(
    private val fmn: String,
    private val engine: Engine,
    private val listener: AnalysisListener? = null,
) {

    private val evaluations = mutableListOf<BotResult>()

    constructor(
        fmn: String,
        depth: Int,
        listener: AnalysisListener? = null,
        threads: Int = Runtime.getRuntime().availableProcessors(),
        cacheMB: Int = 128
    ): this(fmn, FastFlangBot(min(5, depth), depth, true, { FastNeoBoardEvaluation() }, threads, cacheMB), listener)
    
    fun analyze(): AnalysisResult {
        evaluations.clear()
        val startTime = System.currentTimeMillis()
        val fullGame = Game.fromFMN(fmn)
        val fullHistory = fullGame.moveList
        val replayGame = Game()
        
        val moveInfoList = mutableListOf<MoveInfo>()
        var totalNodes = 0L
        
        listener?.onAnalysisStarted(fullHistory.size)
        
        fullHistory.forEachIndexed { index, action ->
            val positionBeforeMove = replayGame.currentState.copy()
            replayGame.execute(action)
            
            val moveInfo = analyzeAction(index + 1, positionBeforeMove, action)
            if (moveInfo != null) {
                moveInfoList.add(moveInfo)
                totalNodes += moveInfo.nodesSearched
            }
            
            listener?.onMoveAnalyzed(index + 1, fullHistory.size, moveInfo)
        }
        
        val movesWithJudgments = addJudgments(moveInfoList)
        val (whiteAccuracy, blackAccuracy) = AccuracyCalculator.calculateGameAccuracy(movesWithJudgments)
        
        val endTime = System.currentTimeMillis()
        val analysisResult = AnalysisResult(
            fmn = fmn,
            moves = movesWithJudgments,
            whiteAccuracy = whiteAccuracy,
            blackAccuracy = blackAccuracy,
            analysisDepth = moveInfoList.minOf { it.depth },
            nodesAnalyzed = totalNodes,
            analysisTimeMs = endTime - startTime,
        )
        
        listener?.onAnalysisCompleted(analysisResult)
        return analysisResult
    }

    fun getFullEvaluationDataAfterAnalysis() = FullEvaluationData(fmn, evaluations)
    
    private fun analyzeAction(ply: Int, board: Board, action: Move): MoveInfo? {
        return when {
            action.isResign() -> analyzeResign(ply, action)
            else -> analyzeMove(ply, board, action)
        }
    }
    
    private fun analyzeMove(ply: Int, board: Board, move: Move): MoveInfo? {
        try {
            val result = engine.findBestMove(board, printTime = false)
            evaluations += result
            
            val targetMoveEval = result.evaluations.find { 
                move.getNotationV1() == it.move.getNotationV1()
            }?.evaluation
            
            if (targetMoveEval == null) {
                println("Warning: Move ${move.getNotationV1()} not found in engine analysis")
                return null
            }
            
            val bestMove = result.evaluations.first().move
            val bestEval = PositionEvaluation.fromScore(result.evaluations.first().evaluation)
            val actualEvalRaw = result.evaluations.find { it.move == move }!!.evaluation
            val actualEval = PositionEvaluation.fromScore(actualEvalRaw)

            // Best is only move that prevents mate
            val bestIsForced = result.evaluations.first().evaluation.absoluteValue < PositionEvaluation.MATE_THRESHOLD &&
                    result.evaluations.none { it.move != bestMove && it.evaluation.absoluteValue < PositionEvaluation.MATE_THRESHOLD }
            
            return MoveInfo(
                ply = ply,
                moveNotation = move.getNotationV2(board),
                evaluation = actualEval,
                bestMoveNotation = bestMove.getNotationV2(board),
                bestEvaluation = bestEval,
                bestIsForced = bestIsForced,
                depth = result.bestMove.depth,
                nodesSearched = result.count
            )
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error analyzing move ${move.getNotationV1()}: ${e.message}")
            return null
        }
    }
    
    private fun analyzeResign(ply: Int, resign: Move): MoveInfo {
        val currentEval = PositionEvaluation.fromScore(
            if (resign.getResignColor()) -FastNeoBoardEvaluation.MATE_EVAL
            else FastNeoBoardEvaluation.MATE_EVAL
        )
        
        return MoveInfo(
            ply = ply,
            moveNotation = resign.toString(),
            evaluation = currentEval,
            depth = 99,
            nodesSearched = 0,
        )
    }
    
    private fun addJudgments(moves: List<MoveInfo>): List<MoveInfo> {
        val movesWithJudgments = mutableListOf<MoveInfo>()
        
        moves.forEachIndexed { index, moveInfo ->
            val previousEval = if (index > 0) {
                moves[index - 1].evaluation
            } else {
                PositionEvaluation.fromScore(0.0)
            }

            val action = moveInfo.getAction(fmn)

            val judgment = if (moveInfo.bestMoveNotation != null && moveInfo.bestEvaluation != null && moveInfo.bestIsForced != null) {
                MoveJudgment.evaluate(
                    ply = moveInfo.ply,
                    actualEval = moveInfo.evaluation,
                    bestEval = moveInfo.bestEvaluation,
                    previousEval = previousEval,
                    isWhite = moveInfo.isWhiteMove,
                    forced = moveInfo.bestIsForced
                )
            } else if(action.isResign()){
                MoveJudgment(MoveJudgmentType.RESIGN, MoveJudgmentComment.RESIGN)
            } else null
            
            movesWithJudgments.add(moveInfo.withJudgment(judgment ?: createDefaultJudgment()))
        }
        
        return movesWithJudgments
    }
    
    private fun createDefaultJudgment(): MoveJudgment {
        return MoveJudgment(MoveJudgmentType.BOOK, MoveJudgmentComment.BOOK_MOVE)
    }
}