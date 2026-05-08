package de.tadris.flang.network_api.model

data class GameConfiguration(val isRated: Boolean, val infiniteTime: Boolean, val time: Long, val ratingDiff: Int = 300, val isBotRequest: Boolean = false, val timeIncrement: Long = 0){

    companion object {
        const val DEFAULT_RATING_DIFF = 300
        const val CUSTOM_GAME_TIME = -3L
    }

    fun isCustomGame() = time == CUSTOM_GAME_TIME
    
    fun isDailyGame() = infiniteTime && time >= 86400000

    fun equalsAllButDiff(other: GameConfiguration) =
        isRated == other.isRated && infiniteTime == other.infiniteTime && time == other.time &&
                isBotRequest == other.isBotRequest && timeIncrement == other.timeIncrement

}