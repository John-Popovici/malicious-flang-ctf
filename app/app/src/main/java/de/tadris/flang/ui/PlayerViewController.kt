package de.tadris.flang.ui

import android.view.View
import android.widget.TextView
import de.tadris.flang.R
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang.util.applyTo
import de.tadris.flang_lib.GameClock
import de.tadris.flang_lib.Color
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class PlayerViewController(
    var color: Color,
    private val nameText: TextView,
    private val titleText: TextView,
    private val ratingText: TextView,
    private val ratingDiffText: TextView,
    private val clockParent: View,
    private val clockText: TextView
) {

    private val clock = GameClock(color)

    fun update(gameInfo: GameInfo){
        val playerInfo = if(color) gameInfo.white else gameInfo.black
        playerInfo.applyTo(titleText, nameText, ratingText)
        val player1RatingDiff = playerInfo.ratingDiff.roundToInt()
        ratingDiffText.visibility = View.VISIBLE
        when {
            player1RatingDiff > 0 -> {
                ratingDiffText.setTextColor(ratingDiffText.resources.getColor(R.color.ratingDiffPositive))
                ratingDiffText.text = "+$player1RatingDiff"
            }
            player1RatingDiff < 0 -> {
                ratingDiffText.setTextColor(ratingDiffText.resources.getColor(R.color.ratingDiffNegative))
                ratingDiffText.text = "-${player1RatingDiff.absoluteValue}"
            }
            else -> {
                ratingDiffText.visibility = View.GONE
            }
        }
        clock.setTimeLeft(playerInfo.time)
        updateClock(gameInfo.running && gameInfo.toGame().currentState.atMove == color, gameInfo)
    }

    fun updateClock(active: Boolean, info: GameInfo){
        clockParent.setBackgroundColor(
                clockParent.context.resources.getColor(if(active) R.color.clockBackgroundActive else R.color.clockBackgroundDefault)
        )
        if(info.configuration.infiniteTime && !info.configuration.isDailyGame()){
            clockText.setText(R.string.infiniteTimeChar)
        }else{
            clockText.text = clock.toString(active)
        }
    }

}