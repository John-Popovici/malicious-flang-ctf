package de.tadris.flang.network_api.model

data class GameRequestResult(
    val gameId: Long,
    val requestId: Long? = null,
    val message: String? = null
)