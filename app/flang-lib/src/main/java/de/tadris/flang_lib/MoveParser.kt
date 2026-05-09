package de.tadris.flang_lib

import kotlin.text.plus

fun parseMove(board: Board, notation: String): Move {
    if(notation == "#+") return RESIGN_WHITE
    if(notation == "#-") return RESIGN_BLACK
    return when(notation.length){
        2 -> {
            val to = parseVector(notation)
            val availableMoves = board.getMoves()
            val filteredMoves = availableMoves.filter { it.getToIndex() == to }
            if(filteredMoves.size != 1)
                throw IllegalArgumentException("Cannot parse move '$notation' -> not explicit (moves: ${availableMoves.joinToString { it.getNotationV1() }})")
            filteredMoves.first()
        }
        3 -> {
            val to = parseVector(notation.substring(1, 3))
            val pieceType = parseType(notation[0])
            val availableMoves = board.getMoves()
            val filteredMoves = availableMoves.filter { it.getFromPieceState().getType() == pieceType && it.getToIndex() == to }
            if(filteredMoves.size != 1)
                throw IllegalArgumentException("Cannot parse move '$notation' -> not explicit (moves: ${availableMoves.joinToString { it.getNotationV1() }})")
            filteredMoves.first()
        }
        4 -> parseV1(board, notation)
        5 -> parseV1(board, notation.substring(1, 3) + notation.substring(3, 5))
        6 -> parseV1(board, notation.substring(1, 3) + notation.substring(4, 6))
        else -> throw IllegalArgumentException("Cannot parse move, illegal format: '$notation'")
    }
}

private fun parseV1(board: Board, str: String): Move {
    val from = parseVector(str.substring(0, 2))
    val to = parseVector(str.substring(2, 4))
    return packMove(board, from, to)
}

private fun parseVector(str: String): BoardIndex {
    val str = str.uppercase()
    if(str.length != 2) throw IllegalArgumentException("Cannot parse vector '$str'")
    val x = str[0].toInt() - 'A'.toInt()
    val y = str[1].toString().toInt() - 1
    return indexOf(x, y).also { it.checkBounds() }
}

fun Move.getNotationV1(): String {
    if(isResign()){
        return "#" + (if(getResignColor()) "+" else "-")
    }
    val piece = getFromPieceState()
    return piece.getType().getChar(piece.getColor()) + getFromIndex().getIndexNotation() + "-" + getToIndex().getIndexNotation()
}

fun Move.getNotationV2(board: Board): String {
    if(isResign()) return getNotationV1()
    val piece = getFromPieceState()
    val target = getToIndex()
    val movesToHere = board.getMoves().filter { it.getToIndex() == getToIndex() }
    if(movesToHere.size == 1) return getToIndex().getIndexNotation().lowercase()
    val movesToHereWithPiece = movesToHere.filter { it.getFromPieceState().getType() == piece.getType() }
    if(movesToHereWithPiece.size == 1) return piece.getType().getChar(piece.getColor()).toString() + getToIndex().getIndexNotation().lowercase()

    return (getFromIndex().getIndexNotation() + target.getIndexNotation()).lowercase()
}