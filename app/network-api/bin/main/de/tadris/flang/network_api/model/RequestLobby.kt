package de.tadris.flang.network_api.model

data class RequestLobby(
    val requests: List<GameRequest>,
    val dailyRequests: List<DailyGameRequest>
)