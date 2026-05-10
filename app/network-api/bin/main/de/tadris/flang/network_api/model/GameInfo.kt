package de.tadris.flang.network_api.model

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.Color
import kotlin.math.absoluteValue

data class GameInfo(
    val gameId: Long,
    val white: GamePlayerInfo,
    val black: GamePlayerInfo,
    val fmn: String,
    val moves: Int,
    val running: Boolean,
    val configuration: GameConfiguration,
    val lastAction: Long,
    val won: Int,
    val spectatorCount: Int,
){

    fun toGame(): Game {
        return Game.fromFMN(fmn)
    }

    override fun toString(): String {
        return "GameInfo(white='$white', black='$black', fmn='$fmn', moves=$moves)"
    }

    fun getWinningColor(): Color? = WinReason.getWinningColor(won)

    fun getWinningReason(): WinReason = WinReason.getReason(won)

    enum class WinReason(val id: Int) {
        FLANG(1),
        BASE(2),
        RESIGN(4),
        TIMEOUT(8),
        UNDECIDED(0);

        companion object {

            fun getReason(id: Int) = entries.find { it.id == id.absoluteValue } ?: UNDECIDED

            fun getWinningColor(id: Int): Color? = when {
                id > 0 -> COLOR_WHITE
                id < 0 -> COLOR_BLACK
                else -> null
            }

        }

    }

}