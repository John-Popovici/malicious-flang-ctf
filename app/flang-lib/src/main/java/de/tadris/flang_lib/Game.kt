package de.tadris.flang_lib

class Game(
    val initialBoard: Board = Board.getDefault(),
    val currentState: Board = initialBoard.copy(),
    val moveList: MutableList<Move> = mutableListOf(),
) {

    constructor(variant: Variant): this(initialBoard = Board.getDefault().apply { this.variant = variant })

    companion object {

        @JvmStatic
        fun fromFMNAtIndex(fmn: String, index: Int, initialBoard: Board? = null) =
            fromFMN(fmn.split(" ").subList(0, index).joinToString(separator = " "), initialBoard)

        @JvmStatic
        fun fromFMN(fmn: String, initialBoard: Board? = null): Game {
            try {
                val game = Game(initialBoard = initialBoard ?: Board.getDefault())
                fmn.split(' ').filter { it.isNotEmpty() }.forEach { notation ->
                    game.execute(parseMove(game.currentState, notation))
                }
                return game
            }catch (e: Exception){
                throw IllegalArgumentException("Failed to parse FMN $fmn", e)
            }
        }

        @JvmStatic
        fun fromActionList(list: List<Move>) = Game().apply {
            list.forEach {
                execute(it)
            }
        }

    }

    fun getFMNv1() = moveList.joinToString(separator = " ") { it.getNotationV1() }

    fun getFMNv2() = buildString {
        replayAllMoves { state, nextMove ->
            append(nextMove.getNotationV2(state))
            append(" ")
        }
    }.trim()

    fun execute(move: Move){
        currentState.executeOnBoard(move)
        moveList += move
    }

    fun rewind(): Move? {
        val move = moveList.removeLastOrNull() ?: return null
        currentState.revertMove(move)
        return move
    }

    fun copy() = Game(
        initialBoard,
        currentState.copy(),
        moveList.toMutableList(),
    )

    inline fun replayAllMoves(action: (state: Board, nextMove: Move) -> Unit){
        val board = initialBoard.copy()
        moveList.forEach { move ->
            action(board.copy(), move)
            board.executeOnBoard(move)
        }
    }

    inline fun replayAllGameStates(action: (state: Board, nextMove: Move?) -> Unit){
        val board = initialBoard.copy()
        moveList.forEach { move ->
            action(board.copy(), move)
            board.executeOnBoard(move)
        }
        action(board.copy(), null)
    }

}