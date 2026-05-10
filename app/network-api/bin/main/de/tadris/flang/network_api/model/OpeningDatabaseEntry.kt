package de.tadris.flang.network_api.model

data class OpeningDatabaseEntry(val move: String,
                                val gameCount: Int,
                                val winCount: Int,
                                val looseCount: Int){

    fun getWhitePercent() = if(isMoveByWhite()) getWinPercent() else getLoosePercent()

    fun getBlackPercent() = if(isMoveByWhite()) getLoosePercent() else getWinPercent()

    private fun isMoveByWhite() = move[0].isUpperCase()

    fun getWinPercent() = winCount * 100 / gameCount

    fun getLoosePercent() = looseCount * 100 / gameCount

}