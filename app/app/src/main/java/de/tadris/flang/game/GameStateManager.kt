package de.tadris.flang.game

import android.content.Context
import androidx.core.content.edit
import de.tadris.flang_lib.Game

class GameStateManager(private val context: Context) {

    companion object {
        private const val PREF_NAME = "play_over_board_game"
        private const val KEY_BOARD_STATE = "board_state_fmn2"
    }

    fun saveGameState(game: Game) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val fmn2 = game.getFMNv2()
        prefs.edit { putString(KEY_BOARD_STATE, fmn2) }
    }

    fun loadGameState(): Game {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedFMN2 = prefs.getString(KEY_BOARD_STATE, null)
        
        return if (savedFMN2 != null) {
            try {
                Game.fromFMN(savedFMN2)
            } catch (e: Exception) {
                // If loading fails, return default board
                e.printStackTrace()
                Game()
            }
        } else {
            Game()
        }
    }

    fun clearGameState() {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_BOARD_STATE) }
    }
}