package de.tadris.flang.network_api.model

data class PuzzleResult(
    val me: UserInfo,
    val puzzles: List<Puzzle>
)