package de.tadris.flang.network_api.model

data class Puzzle(
    val id: Long,
    val startFMN: String,
    val puzzleFMN: String,
    val elo: Double,
    val views: Int
)