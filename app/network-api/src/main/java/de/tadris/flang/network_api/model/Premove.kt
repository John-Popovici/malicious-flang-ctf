package de.tadris.flang.network_api.model

import de.tadris.flang_lib.Move

data class Premove(
    val id: Long,
    val moveCount: Int,
    val move: Move,
    val fmnCondition: String? = null
)