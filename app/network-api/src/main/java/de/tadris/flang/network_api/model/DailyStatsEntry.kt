package de.tadris.flang.network_api.model

import java.util.*

data class DailyStatsEntry(
    val date: Date,
    val gamesLastDay: Int,
    val activePlayersLastDay: Int,
    val avgRating: Int,
    val solvedPuzzles: Int,
    val totalPuzzles: Int,
)