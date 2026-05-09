package de.tadris.flang_lib.puzzle

/**
 * @property type Puzzle type
 * @property startFMN Notation for the start state of the puzzle
 * @property puzzleFMN Expected puzzle solution. For sequences: first move is puzzle, second is opponents move, third puzzle again
 */
data class PuzzleData(
    val type: PuzzleType,
    val startFMN: String,
    val puzzleFMN: String,
){

    override fun toString() = "Puzzle($type) Game: $startFMN Expect: $puzzleFMN"

}