package de.tadris.flang_lib

typealias FastType = Byte

const val TYPE_COUNT = 7 // Number of available types

const val TYPE_NONE: Byte = 0
const val TYPE_PAWN: Byte = 1
const val TYPE_HORSE: Byte = 2
const val TYPE_ROOK: Byte = 3
const val TYPE_FLANGER: Byte = 4
const val TYPE_UNI: Byte = 5
const val TYPE_KING: Byte = 6
const val TYPE_RIDER: Byte = 7

val FastType.hasDoubleMoves get() = this == TYPE_FLANGER

val FastType.hasFreeze get() = this != TYPE_NONE && this != TYPE_KING

val FastType.moves get() = when(this){
    TYPE_PAWN -> MOVES_PAWN
    TYPE_HORSE -> MOVES_HORSE
    TYPE_ROOK -> MOVES_ROOK
    TYPE_FLANGER -> MOVES_FLANGER
    TYPE_UNI -> MOVES_UNI
    TYPE_KING -> MOVES_KING
    TYPE_RIDER -> MOVES_RIDER
    else -> throw Exception("Unknown type $this")
}

val FastType.pieceValue get() = when(this){
    TYPE_PAWN -> 110
    TYPE_HORSE -> 200
    TYPE_RIDER -> 500
    TYPE_ROOK -> 400
    TYPE_FLANGER -> 400
    TYPE_UNI -> 900
    TYPE_KING -> 400
    else -> throw Exception("Unknown type $this")
}

fun FastType.getChar(color: Color): Char {
    return if(color) getChar().uppercaseChar() else getChar()
}

fun parseType(char: Char) = parseTypeOrNull(char) ?: throw IllegalArgumentException("Type '$char' is not known.")

fun parseTypeOrEmpty(char: Char) = parseTypeOrNull(char) ?: TYPE_NONE

fun parseTypeOrNull(char: Char) = when(char.lowercaseChar()){
    'p' -> TYPE_PAWN
    'h' -> TYPE_HORSE
    'r' -> TYPE_ROOK
    'f' -> TYPE_FLANGER
    'u' -> TYPE_UNI
    'k' -> TYPE_KING
    'm' -> TYPE_RIDER
    else -> null
}

private fun FastType.getChar(): Char = when(this){
    TYPE_PAWN -> 'p'
    TYPE_HORSE -> 'h'
    TYPE_ROOK -> 'r'
    TYPE_FLANGER -> 'f'
    TYPE_UNI -> 'u'
    TYPE_KING -> 'k'
    TYPE_RIDER -> 'm'
    TYPE_NONE -> ' '
    else -> throw Exception("Unknown type $this")
}