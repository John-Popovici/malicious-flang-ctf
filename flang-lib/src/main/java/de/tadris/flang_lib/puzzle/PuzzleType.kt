package de.tadris.flang_lib.puzzle

enum class PuzzleType {
    FORCED_MATE, // only moves that make mate
    EXCELLENT_MOVE, // only move/moves that makes sense
    FORCED_MOVES, // only moves that don't lose
}