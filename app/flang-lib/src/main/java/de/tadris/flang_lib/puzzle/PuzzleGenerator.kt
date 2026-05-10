package de.tadris.flang_lib.puzzle

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.analysis.FullEvaluationData
import de.tadris.flang_lib.bot.BotResult
import de.tadris.flang_lib.bot.Engine
import de.tadris.flang_lib.bot.evaluation.MoveEvaluation
import de.tadris.flang_lib.TYPE_KING
import de.tadris.flang_lib.TYPE_UNI
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.FastType
import de.tadris.flang_lib.evaluationNumber
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getFromPieceState
import de.tadris.flang_lib.getNotationV2
import de.tadris.flang_lib.getToIndex
import de.tadris.flang_lib.getToPieceState
import de.tadris.flang_lib.getType
import de.tadris.flang_lib.parseMove
import de.tadris.flang_lib.winningY
import de.tadris.flang_lib.y
import de.tadris.flang_lib.puzzle.PuzzleGenerator.Companion.BEST_MOVE_THRESHOLD
import kotlin.math.absoluteValue

class PuzzleGenerator(private val data: FullEvaluationData, private val engine: Engine, private val skipRecalc: Boolean = false) {

    companion object {
        private const val BEST_MOVE_THRESHOLD = 70.0
        private const val BALANCED_GAME_THRESHOLD = 60.0
        private const val LIKELY_WON_GAME_THRESHOLD = 200.0
    }

    private val foundPuzzles = mutableListOf<PuzzleData>()

    fun searchPuzzles(): List<PuzzleData> {
        foundPuzzles.clear()

        searchForcedDefence()
        searchVeryGoodMoves()
        searchForcedMates()
        filterDuplicatePuzzles()
        combinePuzzlesToSequences()
        filterSingleForced()
        filterSimplePuzzles()
        filterEndlessPuzzles()
        println("Found ${foundPuzzles.size} puzzles")
        return foundPuzzles
    }

    /**
     * Searches single moves or sequences where all other moves would lose more than [BEST_MOVE_THRESHOLD] eval points
     */
    private fun searchVeryGoodMoves(){
        data.evaluations.forEachIndexed { moveIndex, botResult ->
            searchVeryGoodMove(moveIndex, botResult)
        }
    }

    private fun searchVeryGoodMove(moveIndex: Int, botResult: BotResult, recalculated: Boolean = false){
        val bestMove = botResult.bestMove
        val allMoves = botResult.evaluations

        // Check if there are multiple moves to analyze
        if (allMoves.size < 2) return

        // Skip if the best move leads to mate (game is decided)
        if (isMateEvaluation(bestMove)) return

        // Find moves that are within the threshold of the best move using normalized evaluation
        val goodMoves = allMoves.filter { move ->
            bestMove.normalizedEvaluation - move.normalizedEvaluation < BEST_MOVE_THRESHOLD
        }

        // Create puzzle if only one move is significantly better than others
        if (goodMoves.size == 1 && goodMoves.first().move == bestMove.move) {
            // Check if game is still balanced
            if(recalculated || skipRecalc){
                if(bestMove.evaluation.absoluteValue < BALANCED_GAME_THRESHOLD){
                    val puzzle = createPuzzle(PuzzleType.EXCELLENT_MOVE, moveIndex, bestMove.move)
                    foundPuzzles.add(puzzle)
                }
            }else{
                searchVeryGoodMove(moveIndex, engine.findBestMove(Game.fromFMN(getCurrentPositionFMN(moveIndex)).currentState, printTime = false), true)
            }
        }
    }

    /**
     * Searches single moves or sequences where only one move goes to mate FOR US
     */
    private fun searchForcedMates(){
        data.evaluations.forEachIndexed { moveIndex, botResult ->
            searchForcedMate(moveIndex, botResult)
        }
    }

    private fun searchForcedMate(moveIndex: Int, botResult: BotResult, recalculated: Boolean = false){
        val allMoves = botResult.evaluations

        // Check if there are multiple moves to analyze
        if (allMoves.size < 2) return

        // Find moves that lead to mate for us (positive normalized evaluation)
        val mateMoves = allMoves.filter { move ->
            isMateEvaluation(move) && move.normalizedEvaluation > 0
        }

        // Create puzzle if exactly one move leads to mate for us
        if (mateMoves.size == 1) {
            if(recalculated || skipRecalc){
                // Check if other moves are also likely won
                if((allMoves.filter { !isMateEvaluation(it) }.maxOfOrNull { it.normalizedEvaluation } ?: 0.0) > LIKELY_WON_GAME_THRESHOLD){
                    // Second best move isn't forced mate but also likely won -> not a puzzle
                    return
                }
                val mateMove = mateMoves.first()
                val puzzle = createPuzzle(PuzzleType.FORCED_MATE, moveIndex, mateMove.move)
                foundPuzzles.add(puzzle)
            }else{
                searchForcedMate(moveIndex, engine.findBestMove(Game.fromFMN(getCurrentPositionFMN(moveIndex)).currentState, printTime = false), true)
            }
        }
    }

    /**
     * Searches single moves or sequences where only one move evades mate AGAINST US.
     * Only sequences if afterwards the evaluation is somewhat equal or good for the own party.
     */
    private fun searchForcedDefence(){
        data.evaluations.forEachIndexed { moveIndex, botResult ->
            val allMoves = botResult.evaluations

            // Check if there are multiple moves to analyze
            if (allMoves.size <= 1) return@forEachIndexed
            
            // Check if we are under mate threat (some moves lead to mate against us)
            val mateThreatMoves = allMoves.filter { move ->
                isMateEvaluation(move) && move.normalizedEvaluation < 0
            }
            
            if (mateThreatMoves.isEmpty()) return@forEachIndexed
            
            // Find moves that don't lead to mate for the opponent
            val defensiveMoves = allMoves.filter { move ->
                !isMateEvaluation(move) || move.normalizedEvaluation > 0
            }
            

            // Create puzzle if exactly one move provides good defence
            if (defensiveMoves.size == 1) {
                val defenceMove = defensiveMoves.first()

                // Include if position is balanced OR good for the player who made the move
                if(defenceMove.evaluation.absoluteValue <= BALANCED_GAME_THRESHOLD || defenceMove.normalizedEvaluation > 0) {
                    val puzzle = createPuzzle(PuzzleType.FORCED_MOVES, moveIndex, defenceMove.move)
                    foundPuzzles.add(puzzle)
                }
            }
        }
    }

    private fun combinePuzzlesToSequences() {
        val initialPuzzleCount = foundPuzzles.size
        
        val puzzlesWithIndex = foundPuzzles
            .map { it to it.startFMN.count { c -> c == ' ' } }
            .sortedBy { it.second }

        val puzzleGroups = mutableListOf<List<PuzzleData>>()
        
        var i = 0
        while (i < puzzlesWithIndex.size) {
            val currentGroup = mutableListOf<PuzzleData>()
            val (currentPuzzle, currentIndex) = puzzlesWithIndex[i]
            currentGroup.add(currentPuzzle)
            
            // Look for consecutive puzzles (2 moves apart to account for opponent response)
            var j = i + 1
            var expectedIndex = currentIndex + 2
            
            while (j < puzzlesWithIndex.size && puzzlesWithIndex[j].second == expectedIndex) {
                val (nextPuzzle, nextIndex) = puzzlesWithIndex[j]
                currentGroup.add(nextPuzzle)
                expectedIndex = nextIndex + 2
                j++
            }
            
            puzzleGroups.add(currentGroup)
            i = j
        }
        
        // Replace single puzzles with sequence puzzles where applicable
        val updatedPuzzles = mutableListOf<PuzzleData>()
        val gameMoves = data.gameFMN.split(" ")
        
        for (group in puzzleGroups) {
            if (group.size > 1) {
                // Check if all puzzle moves in the group match actual game moves
                val allMovesMatch = group.all { puzzle ->
                    val puzzleIndex = puzzle.startFMN.count { c -> c == ' ' }
                    val matches = puzzleIndex < gameMoves.size && puzzle.puzzleFMN == gameMoves[puzzleIndex + 1]
                    if (!matches) {
                        println("  Puzzle move mismatch at index $puzzleIndex: puzzle='${puzzle.puzzleFMN}' vs game='${gameMoves.getOrNull(puzzleIndex + 1)}'")
                    }
                    matches
                }
                
                println("  Group of ${group.size} puzzles - all moves match: $allMovesMatch")
                
                if (allMovesMatch) {
                    println("  Group: \n  - " + group.joinToString(separator = "\n  - "))
                    // Create sequence puzzle
                    val firstPuzzle = group.first()
                    val moves = mutableListOf<String>()
                    
                    for (k in group.indices) {
                        val puzzle = group[k]
                        moves.add(puzzle.puzzleFMN)
                        
                        // Add opponent's response if not the last puzzle
                        if (k < group.lastIndex) {
                            val puzzleIndex = puzzle.startFMN.count { c -> c == ' ' }
                            val opponentsMoveIndex = puzzleIndex + 2
                            // Add opponent's actual response (next move in game)
                            if (opponentsMoveIndex < gameMoves.size) {
                                moves.add(gameMoves[opponentsMoveIndex])
                            }
                        }
                    }
                    
                    val sequenceNotation = moves.joinToString(" ")
                    val sequenceType = group.maxByOrNull { it.type.ordinal }?.type ?: firstPuzzle.type
                    
                    val sequencePuzzle = PuzzleData(sequenceType, firstPuzzle.startFMN, sequenceNotation)
                    updatedPuzzles.add(sequencePuzzle)
                    println("  ✓ Created sequence puzzle: $sequencePuzzle")
                } else {
                    // Don't create sequence - add puzzles individually
                    updatedPuzzles.addAll(group)
                    println("  ✗ Kept ${group.size} puzzles as individuals (moves don't match)")
                }
            } else {
                // Keep single puzzle as is
                updatedPuzzles.add(group.first())
            }
        }
        
        foundPuzzles.clear()
        foundPuzzles.addAll(updatedPuzzles)
        
        val sequencesCreated = puzzleGroups.count { group -> 
            group.size > 1 && group.all { puzzle ->
                val puzzleIndex = puzzle.startFMN.count { c -> c == ' ' }
                puzzleIndex < gameMoves.size && puzzle.puzzleFMN == gameMoves[puzzleIndex]
            }
        }
        if(initialPuzzleCount > 0){
            println("--- Combined $initialPuzzleCount individual puzzles into ${foundPuzzles.size} puzzles ($sequencesCreated sequences created) ---")
        }
    }

    private fun filterDuplicatePuzzles(){
        val filtered = foundPuzzles.distinctBy { it.startFMN }
        foundPuzzles.clear()
        foundPuzzles.addAll(filtered)
    }

    private fun filterSimplePuzzles() {
        // Filter single simple captures, king runs, king dodges
        foundPuzzles.removeAll { isSingleMove(it) && (
                isSimpleCapture(it, TYPE_KING) || isSimpleCapture(it, TYPE_UNI) || isSimpleKingRun(it) || isSimpleKingDodge(it)
        ) }
    }

    private fun filterSingleForced(){
        foundPuzzles.removeAll { it.type == PuzzleType.FORCED_MOVES && isSingleMove(it) }
    }

    private fun filterEndlessPuzzles(){
        foundPuzzles.removeAll { it.startFMN.count { c -> c == ' ' } > 100 }
    }
    
    private fun isSingleMove(puzzle: PuzzleData) = !puzzle.puzzleFMN.contains(' ')
    
    private fun isSimpleCapture(puzzle: PuzzleData, type: FastType): Boolean {
        val (_, move) = puzzle.getSingleMove()
        return move.getToPieceState().getType() == type
    }

    private fun isSimpleKingRun(puzzle: PuzzleData): Boolean {
        val (_, move) = puzzle.getSingleMove()
        return move.getFromPieceState().getType() == TYPE_KING && (move.getFromPieceState().getColor().winningY - move.getToIndex().y).absoluteValue <= 3
    }

    private fun isSimpleKingDodge(puzzle: PuzzleData): Boolean {
        val (_, move) = puzzle.getSingleMove()
        return puzzle.type == PuzzleType.FORCED_MOVES && move.getFromPieceState().getType() == TYPE_KING
    }

    fun PuzzleData.getSingleMove(): Pair<Board, Move> {
        val board = Game.fromFMN(startFMN).currentState
        return board to parseMove(board, puzzleFMN)
    }

    private fun isMateEvaluation(evaluation: MoveEvaluation) =
        isMateEvaluation(evaluation.evaluation)

    private fun isMateEvaluation(evaluation: Double) = evaluation.absoluteValue > 1000.0

    private fun getCurrentPositionFMN(moveIndex: Int): String {
        val movesToInclude = data.gameFMN.split(" ").take(moveIndex)
        return movesToInclude.joinToString(" ")
    }

    private fun createPuzzle(type: PuzzleType, positionIndex: Int, bestMove: Move): PuzzleData {
        val startFMN = getCurrentPositionFMN(positionIndex)
        val board = Game.fromFMN(startFMN).currentState
        return PuzzleData(type, startFMN, bestMove.getNotationV2(board))
    }

    private val MoveEvaluation.normalizedEvaluation get() = evaluation * move.getFromPieceState().getColor().evaluationNumber

}