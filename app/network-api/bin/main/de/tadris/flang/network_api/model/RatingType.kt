package de.tadris.flang.network_api.model

enum class RatingType(val value: String) {
    BULLET("bullet"),
    BLITZ("blitz"),
    CLASSICAL("classical");

    companion object {
        fun fromValue(value: String): RatingType {
            return entries.find { it.value == value } ?: BLITZ
        }
    }
}