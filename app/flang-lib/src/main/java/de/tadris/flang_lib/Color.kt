package de.tadris.flang_lib

typealias Color = Boolean

const val COLOR_WHITE: Boolean = true
const val COLOR_BLACK: Boolean = false

fun Color.getOpponent() = !this

val Color.winningY get() = if(this) 7 else 0

val Color.evaluationNumber get() = if(this) 1 else -1

fun parseColor(char: Char) = char.isUpperCase()