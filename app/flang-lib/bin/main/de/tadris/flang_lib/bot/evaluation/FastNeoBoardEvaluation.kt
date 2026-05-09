package de.tadris.flang_lib.bot.evaluation

import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.TYPE_NONE
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.BoardIndex
import de.tadris.flang_lib.Color
import de.tadris.flang_lib.FastMoveGenerator
import de.tadris.flang_lib.evaluationNumber
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getType
import de.tadris.flang_lib.indexOf
import de.tadris.flang_lib.pieceValue
import de.tadris.flang_lib.x
import de.tadris.flang_lib.y
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

/**
 * This is equivalent to [NeoBoardEvaluation] but uses [Board].
 */
open class FastNeoBoardEvaluation : FastBoardEvaluation {

    companion object {
        const val MATE_EVAL = 10000.0 // Highest evaluation possible
        const val MATE_STEP_LOSS = 1.0

        const val FACTOR_MATRIX = 2
        const val FACTOR_MOVEMENT = 10
        const val FACTOR_PIECE_VALUE = 140
        const val FACTOR_KINGS_EVAL = 2
        const val FACTOR_KINGS_SAFETY = 1
    }

    var board = Board.getDefault()

    private val evaluationMatrix = arrayOfNulls<LocationEvaluation>(Board.ARRAY_SIZE)

    init {
        for(i in evaluationMatrix.indices){
            evaluationMatrix[i] = LocationEvaluation()
        }
    }

    private val blackStats = OverallStats()
    private val whiteStats = OverallStats()

    private val moveGenerator = FastMoveGenerator(board, includeOwnPieces = true, kingRange = 2, ignoreFreeze = true)

    override fun evaluate(board: Board): Double {
        this.board = board
        this.moveGenerator.board = board
        return evaluate()
    }

    open fun evaluate(): Double {
        if(board.hasWon(COLOR_WHITE)){
            return MATE_EVAL
        }
        if(board.hasWon(COLOR_BLACK)){
            return -MATE_EVAL
        }
        prepare()
        for(index in 0..<Board.ARRAY_SIZE){
            evaluateLocation(index)
        }
        var evaluation = 0.0

        evaluation += getMatrixEval()
        evaluation += getMovementEval()
        evaluation += getPieceValueEval()
        evaluation += getKingsEval()
        evaluation += getKingSafety()

        return (evaluation * 1000).toInt() / 1000.0
    }

    private fun getMovementEval() = FACTOR_MOVEMENT * ((whiteStats.movements.toDouble() / blackStats.movements) - (blackStats.movements.toDouble() / whiteStats.movements))

    protected fun getPieceValueEval() = FACTOR_PIECE_VALUE * ((whiteStats.pieceValue / blackStats.pieceValue) - (blackStats.pieceValue / whiteStats.pieceValue))

    private fun getMatrixEval() = FACTOR_MATRIX * evaluationMatrix.sumOf { it!!.evaluateField() }

    private fun prepare(){
        evaluationMatrix.forEach {
            it?.reset()
        }
        blackStats.reset()
        whiteStats.reset()
    }

    private fun evaluateLocation(index: BoardIndex){
        val eval = getAt(index)
        val piece = board.getAt(index)
        val type = piece.getType()
        if(type != TYPE_NONE){
            val color = piece.getColor()
            val stats = getStats(color)
            val value = type.pieceValue
            stats.pieceValue += value
            eval.occupiedBy = value * color.evaluationNumber
            moveGenerator.forEachTargetLocation(index, piece){ targetIndex ->
                getAt(targetIndex).addThreat(color, 1000000 / value)
                stats.movements++
            }
        }
    }

    protected fun getKingsEval(): Double {
        val whiteKing = board.findKingIndex(COLOR_WHITE)
        val blackKing = board.findKingIndex(COLOR_BLACK)
        if(whiteKing == -1 || blackKing == -1){
            throw IllegalStateException("Cannot find kings in board $board")
        }
        val whiteKingX = whiteKing.x
        val whiteKingY = whiteKing.y
        val blackKingX = blackKing.x
        val blackKingY = blackKing.y

        val whiteEval = 1 shl whiteKingY
        val blackEval = 1 shl (7 - blackKingY)

        for(y in whiteKingY until (Board.BOARD_SIZE - 1)){
            val field = getAt(whiteKingX, y)
            field.weight += whiteEval
        }
        for(y in blackKingY downTo 1){
            val field = getAt(blackKingX, y)
            field.weight += blackEval
        }

        return FACTOR_KINGS_EVAL * ((whiteEval.toDouble() / blackEval) - (blackEval.toDouble() / whiteEval))
    }

    private fun getKingSafety(): Double {


        return FACTOR_KINGS_SAFETY * 0.0
    }

    private fun getAt(x: Int, y: Int): LocationEvaluation {
        return getAt(indexOf(x, y));
    }

    private fun getAt(index: Int): LocationEvaluation {
        return evaluationMatrix[index]!!
    }

    private fun getStats(color: Color) = if(color) whiteStats else blackStats

    fun evaluateBreakdown() = BoardEvaluationBreakdown(
        mapOf(
            "matrix" to getMatrixEval(),
            "piece value" to getPieceValueEval(),
            "king rush" to getKingsEval(),
            "king safety" to getKingSafety(),
            "movement" to getMovementEval(),
            "total" to evaluate()
        ),
        evaluationMatrix
    )

    class OverallStats(var movements: Int = 1, var pieceValue: Double = 1.0){

        override fun toString(): String {
            return "pieces=$pieceValue, moves=$movements"
        }

        fun reset(){
            movements = 1
            pieceValue = 1.0
        }

    }

    class LocationEvaluation(
        var occupiedBy: Int = 0,
        var whiteControl: Int = 0,
        var blackControl: Int = 0,
        var weight: Double = 1.0, // multiplier
    ){

        fun addThreat(color: Color, threat: Int){
            if(color){ // test if white
                whiteControl+= threat
            }else{
                blackControl+= threat
            }
        }

        fun evaluateField(): Double {
            val whiteControl = this.whiteControl + 10000.0
            val blackControl = this.blackControl + 10000.0
            val controlRate = (whiteControl / blackControl) - (blackControl / whiteControl)
            val w = weight * 0.6f + 1

            val result = when {
                occupiedBy > 0 -> {
                    val factor = if(blackControl > whiteControl) occupiedBy else 100

                    (1 + controlRate) * factor / 100.0
                }
                occupiedBy < 0 -> {
                    val factor = if(whiteControl > blackControl) abs(occupiedBy) else 100
                    (-1 + controlRate) * factor / 100.0
                }
                else -> {
                    controlRate
                }
            }
            return result * w
        }

        fun reset(){
            occupiedBy = 0
            whiteControl = 0
            blackControl = 0
            weight = 0.0
        }

    }

    fun printMatrix(){
        val d = evaluate()
        println("White: $whiteStats")
        println("Black: $blackStats")
        println("+-----------------+")
        for(y in 0 until Board.BOARD_SIZE){
            print("| ")
            for(x in 0 until Board.BOARD_SIZE){
                val s = (evaluationMatrix[y*8 + x]!!.evaluateField() * 2).toInt().toString()
                print(" ".repeat(max(0, 2-s.length)) + s)
            }
            println("|")
        }
        println("+-----------------+")
        println("Rating: $d")
    }

    class BoardEvaluationBreakdown(val factors: Map<String, Double>, val evaluationMatrix: Array<LocationEvaluation?>)

}