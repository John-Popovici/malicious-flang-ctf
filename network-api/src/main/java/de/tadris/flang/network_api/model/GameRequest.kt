package de.tadris.flang.network_api.model

data class GameRequest(val id: Long, val configuration: GameConfiguration, val requester: UserInfo)