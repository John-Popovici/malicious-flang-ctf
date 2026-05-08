package de.tadris.flang.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import de.tadris.flang.R
import de.tadris.flang.network_api.model.GameConfiguration
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang.network_api.model.GamePlayerInfo
import de.tadris.flang.network_api.util.DefaultConfigurations
import de.tadris.flang.ui.fragment.GameEndReason
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.COLOR_WHITE
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class GameEndDialog(
    private val context: Context,
    private val gameInfo: GameInfo,
    private val game: Game,
    private val endReason: GameEndReason,
    private val isWhitePlayer: Boolean,
    private val isBlackPlayer: Boolean,
    private val callback: GameEndDialogCallback
) {

    private val userWon = when {
        isWhitePlayer && isBlackPlayer -> false // Both players are same user, shouldn't happen
        isWhitePlayer -> game.currentState.hasWon(COLOR_WHITE)
        isBlackPlayer -> game.currentState.hasWon(COLOR_BLACK)
        else -> false
    }
    
    interface GameEndDialogCallback {

        fun onAnalysisRequested(fmn: String)

        fun onNewGameRequested(configuration: GameConfiguration)

    }
    
    private var alertDialog: AlertDialog? = null
    
    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_game_end, null)
        
        setupUI(dialogView)
        setupButtonListeners(dialogView)
        
        alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        
        alertDialog?.show()
    }
    
    fun dismiss() {
        alertDialog?.dismiss()
    }
    
    private fun setupUI(dialogView: View) {
        val resultIcon = dialogView.findViewById<TextView>(R.id.resultIcon)
        val resultTitle = dialogView.findViewById<TextView>(R.id.resultTitle)
        val resultSubtitle = dialogView.findViewById<TextView>(R.id.resultSubtitle)
        val btnRequestNewGame = dialogView.findViewById<Button>(R.id.btnRequestNewGame)

        // Player info views
        val whitePlayerName = dialogView.findViewById<TextView>(R.id.whitePlayerName)
        val whitePlayerRating = dialogView.findViewById<TextView>(R.id.whitePlayerRating)
        val whitePlayerRatingDiff = dialogView.findViewById<TextView>(R.id.whitePlayerRatingDiff)
        val blackPlayerName = dialogView.findViewById<TextView>(R.id.blackPlayerName)
        val blackPlayerRating = dialogView.findViewById<TextView>(R.id.blackPlayerRating)
        val blackPlayerRatingDiff = dialogView.findViewById<TextView>(R.id.blackPlayerRatingDiff)

        // Setup result display
        if (userWon) {
            resultIcon.text = "⭐"
            resultIcon.setTextColor(context.getColor(R.color.green_400))
            resultTitle.text = context.getString(R.string.gameEndWon)
            resultTitle.setTextColor(context.getColor(R.color.green_400))
        } else {
            resultIcon.text = "💔"
            resultIcon.setTextColor(context.getColor(R.color.red_700))
            resultTitle.text = context.getString(R.string.gameEndLost)
            resultTitle.setTextColor(context.getColor(R.color.red_700))
        }

        // Setup subtitle
        val subtitleText = when (endReason) {
            GameEndReason.FLANG -> context.getString(R.string.gameEndByFlang)
            GameEndReason.TIMEOUT -> context.getString(R.string.gameEndByTimeout)
            GameEndReason.RESIGN -> context.getString(R.string.gameEndByResign)
            GameEndReason.UNKNOWN -> context.getString(R.string.gameEndByUnknown)
        }
        resultSubtitle.text = subtitleText

        // Setup player information
        setupPlayerInfo(whitePlayerName, whitePlayerRating, whitePlayerRatingDiff, gameInfo.white)
        setupPlayerInfo(blackPlayerName, blackPlayerRating, blackPlayerRatingDiff, gameInfo.black)

        // Setup new game button
        val configuration = gameInfo.configuration
        val configurationName = DefaultConfigurations.getConfigurations().find {
            it.second.equalsAllButDiff(configuration)
        }?.first
        if(configurationName != null && !configuration.isDailyGame()){
            val gameTime = (configuration.time / 1000 / 60).toInt().toString() + " min"
            btnRequestNewGame.visibility = View.VISIBLE
            btnRequestNewGame.text = context.getString(R.string.requestNewGame, "$gameTime $configurationName")
        }else{
            btnRequestNewGame.visibility = View.GONE
        }
    }

    private fun setupPlayerInfo(nameView: TextView, ratingView: TextView, ratingDiffView: TextView, playerInfo: GamePlayerInfo) {
        nameView.text = playerInfo.username
        ratingView.text = playerInfo.getRatingText()
        
        val ratingDiff = playerInfo.ratingDiff.roundToInt()
        when {
            ratingDiff > 0 -> {
                ratingDiffView.visibility = View.VISIBLE
                ratingDiffView.setTextColor(context.getColor(R.color.ratingDiffPositive))
                ratingDiffView.text = "+$ratingDiff"
            }
            ratingDiff < 0 -> {
                ratingDiffView.visibility = View.VISIBLE
                ratingDiffView.setTextColor(context.getColor(R.color.ratingDiffNegative))
                ratingDiffView.text = "-${ratingDiff.absoluteValue}"
            }
            else -> {
                ratingDiffView.visibility = View.GONE
            }
        }
    }
    
    private fun setupButtonListeners(dialogView: View) {
        val btnAnalysis = dialogView.findViewById<Button>(R.id.btnAnalysis)
        val btnRequestNewGame = dialogView.findViewById<Button>(R.id.btnRequestNewGame)
        
        btnAnalysis.setOnClickListener {
            dismiss()
            callback.onAnalysisRequested(game.getFMNv2())
        }

        val opponent = if(isWhitePlayer) gameInfo.black else gameInfo.white
        
        btnRequestNewGame.setOnClickListener {
            dismiss()
            callback.onNewGameRequested(gameInfo.configuration.copy(
                ratingDiff = GameConfiguration.DEFAULT_RATING_DIFF,
                isBotRequest = opponent.isBot,
            ))
        }
    }

}