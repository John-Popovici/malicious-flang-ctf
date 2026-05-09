package de.tadris.flang.ui.adapter

import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.ui.board.BoardView
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang.network_api.model.GamePlayerInfo
import de.tadris.flang.util.AgeUtils
import de.tadris.flang.util.applyTo
import de.tadris.flang_lib.utils.TimeUtils
import de.tadris.flang_lib.Color
import de.tadris.flang_lib.getOpponent
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class GameAdapter(private val games: MutableList<GameInfo>, private val listener: GameAdapterListener?, private val currentUsername: String? = null) : RecyclerView.Adapter<GameAdapter.GameInfoViewHolder>() {

    private val handler = Handler()

    class GameInfoViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        val cardView = root.findViewById<CardView>(R.id.gameCard)!!
        val boardView = root.findViewById<ViewGroup>(R.id.gameInfoBoard)!!
        val headerText = root.findViewById<TextView>(R.id.gameInfoHeader)!!
        val ageText = root.findViewById<TextView>(R.id.gameInfoAge)!!
        val player1NameText = root.findViewById<TextView>(R.id.gameInfoPlayer1Name)!!
        val player1NameTitle = root.findViewById<TextView>(R.id.gameInfoPlayer1Title)!!
        val player1RatingText = root.findViewById<TextView>(R.id.gameInfoPlayer1Rating)!!
        val player1RatingDiffText = root.findViewById<TextView>(R.id.gameInfoPlayer1RatingDiff)!!
        val player2NameText = root.findViewById<TextView>(R.id.gameInfoPlayer2Name)!!
        val player2NameTitle = root.findViewById<TextView>(R.id.gameInfoPlayer2Title)!!
        val player2RatingText = root.findViewById<TextView>(R.id.gameInfoPlayer2Rating)!!
        val player2RatingDiffText = root.findViewById<TextView>(R.id.gameInfoPlayer2RatingDiff)!!
        val resultText = root.findViewById<TextView>(R.id.gameInfoResult)!!
    }

    fun appendGames(games: List<GameInfo>){
        val oldGameCount = this.games.size
        this.games.addAll(games)
        notifyItemRangeInserted(oldGameCount, this.games.size-1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameInfoViewHolder {
        return GameInfoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_game_info, parent, false))
    }

    override fun onBindViewHolder(holder: GameInfoViewHolder, position: Int) {
        val context = holder.root.context
        val game = games[position]
        val boardView = BoardView(holder.boardView, board = game.toGame().currentState, isClickable = false, animate = false)
        handler.postDelayed({
            boardView.refresh()
        }, 100)
        val mins = if(game.configuration.isDailyGame()) {
            TimeUtils.getSmartTimeDisplay(game.configuration.time)
        } else {
            if(game.configuration.infiniteTime){
                context.getString(R.string.infiniteTimeChar)
            } else {
                TimeUtils.getTimeControlDisplay(game.configuration.time, game.configuration.timeIncrement)
            }
        }
        val modeStr = context.getString(if(game.configuration.isRated) R.string.modeRated else R.string.modeCasual)
        holder.headerText.text = context.getString(
            R.string.gameInfoHeader,
            mins,
            TimeUtils.TimeControlZone.getZone(game.configuration.infiniteTime, game.configuration.time).displayName,
            modeStr)
        holder.ageText.text = AgeUtils.getAgeString(context, System.currentTimeMillis() - game.lastAction)
        bindPlayerInfo(game.white, holder.player1NameText, holder.player1NameTitle, holder.player1RatingText, holder.player1RatingDiffText)
        bindPlayerInfo(game.black, holder.player2NameText, holder.player2NameTitle, holder.player2RatingText, holder.player2RatingDiffText)
        val winningColor = game.getWinningColor()
        holder.resultText.visibility = View.VISIBLE
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.game_background_normal))
        when {
            winningColor != null -> {
                val firstPart = when(game.getWinningReason()) {
                    GameInfo.WinReason.BASE -> context.getString(R.string.flang)
                    GameInfo.WinReason.FLANG -> context.getString(R.string.flang)
                    GameInfo.WinReason.RESIGN -> context.getString(R.string.resultResigned, getStringFromColor(context, winningColor.getOpponent()))
                    GameInfo.WinReason.TIMEOUT -> context.getString(R.string.resultTimedOut, getStringFromColor(context, winningColor.getOpponent()))
                    GameInfo.WinReason.UNDECIDED -> context.getString(R.string.resultDraw)
                }
                val secondPart = context.getString(R.string.isVictorious, getStringFromColor(context, winningColor))
                holder.resultText.text = context.getString(R.string.gameResult, firstPart, secondPart)
            }
            game.running -> {
                val isUserAtMove = currentUsername != null && isCurrentUserAtMove(game, currentUsername)
                if (isUserAtMove) {
                    holder.resultText.text = context.getString(R.string.gameRunningYourMove)
                    holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.game_background_at_move))
                } else {
                    holder.resultText.text = context.getString(R.string.gameRunning)
                }
            }
            else -> {
                holder.resultText.visibility = View.GONE
            }
        }
        holder.root.setOnClickListener { listener?.onClick(game) }
    }

    private fun bindPlayerInfo(playerInfo: GamePlayerInfo, nameText: TextView, titleText: TextView, ratingText: TextView, diffText: TextView){
        playerInfo.applyTo(titleText, nameText, ratingText)
        val player1RatingDiff = playerInfo.ratingDiff.roundToInt()
        diffText.visibility = View.VISIBLE
        when {
            player1RatingDiff > 0 -> {
                diffText.setTextColor(diffText.resources.getColor(R.color.ratingDiffPositive))
                diffText.text = "+$player1RatingDiff"
            }
            player1RatingDiff < 0 -> {
                diffText.setTextColor(diffText.resources.getColor(R.color.ratingDiffNegative))
                diffText.text = "-${player1RatingDiff.absoluteValue}"
            }
            else -> {
                diffText.visibility = View.GONE
            }
        }
    }

    private fun getStringFromColor(context: Context, color: Color) = context.getString(if(color) R.string.white else R.string.black)

    private fun isCurrentUserAtMove(game: GameInfo, username: String): Boolean {
        val board = game.toGame().currentState
        val colorAtMove = board.atMove
        return when (colorAtMove) {
            true -> game.white.username == username
            false -> game.black.username == username
        }
    }

    override fun getItemCount() = games.size

    interface GameAdapterListener {

        fun onClick(gameInfo: GameInfo)

    }

}