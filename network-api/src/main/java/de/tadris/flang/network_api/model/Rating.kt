package de.tadris.flang.network_api.model

data class Rating (
    val type: String,
    val rating: Float,
){

    companion object {

        const val TYPE_BULLET = "bullet"
        const val TYPE_BLITZ = "blitz"
        const val TYPE_CLASSICAL = "classical"
        const val TYPE_DAILY = "daily"
        const val TYPE_PUZZLE = "puzzle"

    }

}