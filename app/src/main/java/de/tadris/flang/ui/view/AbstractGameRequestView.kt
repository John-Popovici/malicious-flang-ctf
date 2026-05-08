package de.tadris.flang.ui.view

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.tadris.flang.R
import de.tadris.flang.network_api.model.GameConfiguration
import de.tadris.flang.network_api.model.UserInfo
import de.tadris.flang.util.applyTo
import de.tadris.flang_lib.utils.TimeUtils

abstract class AbstractGameRequestView<T>(
    protected val context: Activity,
    parent: ViewGroup,
    protected val request: T
) {
    protected val root = LayoutInflater.from(context).inflate(R.layout.view_game_request, parent, false)

    private val playerNameText = root.findViewById<TextView>(R.id.requestPlayerName)
    private val playerTitleText = root.findViewById<TextView>(R.id.requestPlayerTitle)
    private val playerRatingText = root.findViewById<TextView>(R.id.requestRating)
    private val timeText = root.findViewById<TextView>(R.id.requestTime)
    private val modeText = root.findViewById<TextView>(R.id.requestMode)

    init {
        setupPlayerInfo()
        setupTimeDisplay()
        setupModeDisplay()
        setupClickListener()
    }

    private fun setupPlayerInfo() {
        getRequester().applyTo(playerTitleText, playerNameText, playerRatingText)
    }

    private fun setupTimeDisplay() {
        timeText.text = getTimeDisplayText()
    }

    private fun setupModeDisplay() {
        val config = getConfiguration()
        modeText.setText(if (config.isRated) R.string.modeRated else R.string.modeCasual)
    }
    private fun setupClickListener() {
        root.setOnClickListener { onRequestClick() }
    }

    fun getView(): View = root

    // Abstract methods to be implemented by subclasses
    protected abstract fun getRequester(): UserInfo
    protected abstract fun getConfiguration(): GameConfiguration
    protected abstract fun getTimeDisplayText(): String
    protected abstract fun onRequestClick()
}