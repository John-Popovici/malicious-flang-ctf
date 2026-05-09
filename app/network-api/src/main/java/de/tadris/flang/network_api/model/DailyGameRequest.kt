package de.tadris.flang.network_api.model

data class DailyGameRequest(
    val id: Long,
    val configuration: GameConfiguration,
    val requester: UserInfo,
    val dateCreated: Long
)