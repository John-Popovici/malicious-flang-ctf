package de.tadris.flang_lib

typealias BoardIndex = Int

val BoardIndex.x get() = this % Board.BOARD_SIZE

val BoardIndex.y get() = this / Board.BOARD_SIZE

fun indexOf(x: Int, y: Int) = y * Board.BOARD_SIZE + x

fun BoardIndex.checkBounds() {
    if(!isValid()) throw Exception("Illegal board position: x: $x y: $y")
}

fun BoardIndex.isValid() = x >= 0 && y >= 0 && x < Board.BOARD_SIZE && y < Board.BOARD_SIZE

fun BoardIndex.getIndexNotation() = ('A'.toInt() + x).toChar() + (y+1).toString()