package de.tadris.flang_lib.analysis

import de.tadris.flang_lib.Color
import kotlin.math.absoluteValue
import kotlin.math.tanh

/**
 * Winrate approximation
 */
object WinRate {

    private const val A = 200
    private const val B = 0.47

    /**
     * Return range -1..1
     * -1 -> 100% black wins
     * 0 -> equal
     * 1 -> 100% white wins
     */
    fun getBidirectionalWinRate(eval: Double) = tanh(eval / (A + B * eval.absoluteValue))

    /**
     * Return range: 0..1
     * 0 -> 0% win rate
     * 1 -> 100% win rate
     */
    fun getUnidirectionalWinRate(eval: Double, color: Color): Double {
        val value = (1 + getBidirectionalWinRate(eval)) / 2
        return if(color) value else 1 - value
    }

}