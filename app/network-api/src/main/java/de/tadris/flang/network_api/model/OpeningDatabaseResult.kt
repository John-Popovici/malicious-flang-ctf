package de.tadris.flang.network_api.model

data class OpeningDatabaseResult(
    val result: List<OpeningDatabaseEntry>,
    val games: List<GameInfo>
)