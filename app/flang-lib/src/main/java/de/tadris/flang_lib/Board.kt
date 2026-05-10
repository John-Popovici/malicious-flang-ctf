package de.tadris.flang_lib

import kotlin.math.absoluteValue

/**
 * @property pieces stores [PieceState]
 */
class Board(
    val pieces: ByteArray = ByteArray(ARRAY_SIZE),
    var atMove: Color = COLOR_WHITE,
    var frozenWhiteIndex: BoardIndex = -1,
    var frozenBlackIndex: BoardIndex = -1,
    var whiteKingIndex: BoardIndex = -1,
    var blackKingIndex: BoardIndex = -1,
    var moveNumber: Int = 0,
    var isInfiniteGame: Boolean = false,
    var resigned: Color? = null,
    var variant: Variant = Variant.CLASSIC,
) {

    companion object {

        const val BOARD_SIZE = 8
        const val ARRAY_SIZE = BOARD_SIZE * BOARD_SIZE
        val DEFAULT_FBN2 = "+2PRHFUK2PPPPPP32pppppp2kufhrp2"

        private val defaultBoard = fromFBNv2(DEFAULT_FBN2)

        @JvmStatic
        fun getDefault() = defaultBoard.copy()

        @JvmStatic
        fun fromFBN(fbn: String) =
            if(fbn.first() in listOf('+', '-')) fromFBNv2(fbn)
            else if(fbn.length == 64) fromFBNv1Short(fbn)
            else fromFBNv1(fbn)

        fun fromFBNv2(fbn: String): Board {
            val builder = StringBuilder()
            var atMove = COLOR_WHITE
            var digitBuilder = ""
            "$fbn?".forEachIndexed { index, c ->
                if (index == 0) {
                    atMove = when(c){
                        '+' -> COLOR_WHITE
                        '-' -> COLOR_BLACK
                        else -> throw IllegalArgumentException("Cannot parse FBNv2 '$fbn' -> must start with + or -")
                    }
                    return@forEachIndexed
                }
                if(c.isDigit()){
                    digitBuilder+= c
                    if(digitBuilder.length >= 3)
                        throw IllegalArgumentException("Cannot parse FBNv2 '$fbn' -> invalid number $digitBuilder")
                }else if(digitBuilder.isNotEmpty()){
                    repeat(digitBuilder.toInt()){
                        builder.append(" +")
                    }
                    digitBuilder = ""
                }
                if (c.isLetter()) {
                    builder.append(c)
                    builder.append('+')
                }
                if(c == '-'){
                    builder.deleteAt(builder.length - 1)
                    builder.append('-')
                }
            }
            val board = fromFBNv1(builder.toString())
            board.atMove = atMove
            return board
        }

        fun fromFBNv1Short(fbn: String): Board {
            if(fbn.length != 64) throw IllegalArgumentException("FBNv1 short must have a length of 64 characters but was ${fbn.length}")
            val board = Board()
            for(idx in 0 until 64){
                val pieceChar = fbn[idx]
                if(pieceChar == ' '){
                    board.clearAt(idx)
                }else{
                    board.setAt(idx, packPieceState(
                        parseTypeOrEmpty(pieceChar),
                        parseColor(pieceChar),
                        FROZEN_NORMAL
                    ))
                }
            }
            return board
        }

        fun fromFBNv1(fbn: String): Board {
            if(fbn.length != 128) throw IllegalArgumentException("FBNv1 must have a length of 128 characters but was ${fbn.length}")
            val board = Board()
            for(idx in 0 until 64){
                val pieceChar = fbn[idx * 2]
                val stateChar = fbn[idx * 2 + 1]
                val frozenState = when(stateChar){
                    '+' -> FROZEN_NORMAL
                    '-' -> FROZEN_FROZEN
                    else -> throw IllegalArgumentException("Unknown frozen state '$stateChar', must be '+' or '-'")
                }
                if(pieceChar == ' '){
                    board.clearAt(idx)
                }else{
                    board.setAt(idx, packPieceState(
                        parseTypeOrEmpty(pieceChar),
                        parseColor(pieceChar),
                        frozenState
                    ))
                }
            }
            return board
        }

    }

    init {
        if(pieces.size != ARRAY_SIZE) throw IllegalArgumentException(
            "Cannot create board with size ${pieces.size}. Must be ${ARRAY_SIZE}"
        )
    }

    fun copy() = Board(
        pieces.copyOf(),
        atMove,
        frozenWhiteIndex,
        frozenBlackIndex,
        whiteKingIndex,
        blackKingIndex,
        moveNumber,
        isInfiniteGame,
        resigned,
        variant,
    )

    fun executeOnBoard(move: Move) {
        if(move.isResign()){
            resigned = move.getResignColor()
            moveNumber++
            return
        }
        val piece = move.getFromPieceState()
        unfreezeOnBoard(piece.getColor())
        if(piece.getType() == TYPE_RIDER && (
                    (move.getToIndex().y - move.getFromIndex().y == piece.getColor().evaluationNumber && (move.getToIndex().x - move.getFromIndex().x).absoluteValue <= 1) ||
                            move.getToIndex().x == move.getFromIndex().x
                    )){
            // horse unwrap
            updateAt(move.getFromIndex(), TYPE_HORSE, piece.getColor(), autoFreeze = false)
            updateAt(move.getToIndex(), TYPE_PAWN, piece.getColor())
        }else if(piece.getColor() == move.getToPieceState().getColor() && piece.getType() == TYPE_PAWN && move.getToPieceState().getType() == TYPE_HORSE){
            // horse load
            clearAt(move.getFromIndex())
            updateAt(move.getToIndex(), TYPE_RIDER, piece.getColor())
        }else{
            clearAt(move.getFromIndex())
            updateAt(move.getToIndex(), piece.getType(), piece.getColor())
        }
        atMove = atMove.getOpponent()
        moveNumber++
    }

    fun executeOnNewBoard(move: Move): Board {
        val newBoard = copy()
        newBoard.executeOnBoard(move)
        return newBoard
    }

    fun revertMove(move: Move){
        if(move.isResign()){
            resigned = null
            moveNumber--
            return
        }
        val fromPieceState = move.getFromPieceState()
        val toPieceState = move.getToPieceState()
        setAt(move.getFromIndex(), fromPieceState)
        setAt(move.getToIndex(), toPieceState)

        val frozenPieceIndex = move.getPreviouslyFrozenPieceIndex()
        setFrozenPieceIndex(fromPieceState.getColor(), frozenPieceIndex)
        if(frozenPieceIndex != -1){
            val frozenPiece = getAt(frozenPieceIndex)
            setAt(frozenPieceIndex, packPieceState(frozenPiece.getType(), frozenPiece.getColor(), FROZEN_FROZEN))
        }

        atMove = atMove.getOpponent()
        moveNumber--
    }

    fun unfreezeOnBoard(color: Color){
        val index = getFrozenPieceIndex(color)
        if(index == -1) return
        val currentPiece = getAt(index)
        setAt(index, packPieceState(currentPiece.getType(), currentPiece.getColor(), FROZEN_NORMAL))
    }

    fun getFrozenPieceIndex(color: Color): BoardIndex = if(color) frozenWhiteIndex else frozenBlackIndex

    fun setFrozenPieceIndex(color: Color, index: BoardIndex) {
        if(color) {
            frozenWhiteIndex = index
        } else{
            frozenBlackIndex = index
        }
    }

    fun gameIsComplete() = !isInfiniteGame && (resigned != null || hasWon(COLOR_WHITE) || hasWon(COLOR_BLACK))

    fun getWinningColor() = when {
        hasWon(COLOR_WHITE) -> COLOR_WHITE
        hasWon(COLOR_BLACK) -> COLOR_BLACK
        else -> null
    }

    fun hasWon(color: Color): Boolean {
        if(resigned?.getOpponent() == color) return true // If opponent has resigned, it's a win
        val index = findKingIndex(color)
        if(index == -1) return false // has no king, so hasn't won
        return if(findKingIndex(color.getOpponent()) == -1){
            true // opponent has no king, so won
        }else index.y == color.winningY
    }

    fun findKingIndex(color: Color) = if(color) whiteKingIndex else blackKingIndex

    fun findIndex(type: FastType, color: Color): Int {
        for (index in pieces.indices) {
            val element = pieces[index]
            if (element.getType() == type && element.getColor() == color) {
                return index
            }
        }
        return -1
    }

    fun getAt(index: Int): PieceState = pieces[index]

    fun getAt(x: Int, y: Int): PieceState = pieces[indexOf(x, y)]

    fun updateAt(index: Int, type: FastType, color: Color, autoFreeze: Boolean = true){
        if(type != TYPE_NONE){
            val writtenType = if(type == TYPE_PAWN && index.y == color.winningY) TYPE_UNI else type // pawn promotion
            val isHorseCapture = variant == Variant.NEXT && (writtenType == TYPE_HORSE || writtenType == TYPE_RIDER) && !getAt(index).isEmpty() && getAt(index).getColor() != color // horse captures
            val frozen = autoFreeze && writtenType.hasFreeze && !isHorseCapture
            setAt(index, packPieceState(writtenType, color, frozen))
        }else{
            clearAt(index)
        }
    }

    fun clearAt(index: Int){
        setAt(index, 0)
    }

    fun setAt(index: Int, state: PieceState){
        pieces[index] = state
        if(index == frozenWhiteIndex){
            val stillFrozen = state.getColor() && state.getFrozen()
            if(!stillFrozen) frozenWhiteIndex = -1
        }else if(index == frozenBlackIndex){
            val stillFrozen = !state.getColor() && state.getFrozen()
            if(!stillFrozen) frozenBlackIndex = -1
        }
        if(index == whiteKingIndex) whiteKingIndex = -1
        if(index == blackKingIndex) blackKingIndex = -1

        if(state.getFrozen()){
            setFrozenPieceIndex(state.getColor(), index)
        }
        if(state.getType() == TYPE_KING){
            if(state.getColor()){
                whiteKingIndex = index
            }else{
                blackKingIndex = index
            }
        }
    }

    fun getFBN2(): String {
        val builder = StringBuilder()
        builder.append(if(atMove) '+' else '-')
        var spaceCount = 0
        (getFBN() + "?").forEach {
            if(it == ' '){
                spaceCount++
            }else if(spaceCount > 0 && it != '+'){
                builder.append(spaceCount.toString())
                spaceCount = 0
            }
            if(it.isLetter() || it == '-'){
                builder.append(it)
            }
        }
        return builder.toString()
    }

    fun getFBN() = buildString {
        eachLocation{ index ->
            val state = getAt(index)
            val char = state.getType().getChar(state.getColor())
            append(char)
            append(if(state.getFrozen()) '-' else '+')
        }
    }

    /**
     * High level method with low performance. For faster access, use a dedicated move generator
     * @param color Color to get the moves for, null will get for both colors
     */
    fun getMoves(color: Color? = atMove) =
        FastMoveGenerator(this, false).getAllMoves(color)

    override fun toString() = getFBN()

    inline fun eachLocation(action: (BoardIndex) -> Unit){
        for(index in 0..pieces.lastIndex){
            action(index)
        }
    }

    inline fun eachPiece(color: Color?, action: (BoardIndex, PieceState) -> Unit){
        for(index in 0..pieces.lastIndex){
            val piece = getAt(index)
            if(piece != 0.toByte() && (color == null || piece.getColor() == color)){
                action(index, piece)
            }
        }
    }

}