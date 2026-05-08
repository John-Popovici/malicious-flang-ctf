package de.tadris.flang.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Caches running games the user is part of
 */
class GameCache(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "game_cache", 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val CACHED_GAMES_KEY = "cached_games"
        private const val DELIMITER = ","
        
        @Volatile
        private var INSTANCE: GameCache? = null
        
        fun getInstance(context: Context): GameCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GameCache(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    fun addGame(gameId: Long) {
        val currentGames = getCachedGameIds().toMutableSet()
        currentGames.add(gameId)
        saveCachedGameIds(currentGames)
    }
    
    fun removeGame(gameId: Long) {
        val currentGames = getCachedGameIds().toMutableSet()
        currentGames.remove(gameId)
        saveCachedGameIds(currentGames)
    }
    
    fun getCachedGameIds(): Set<Long> {
        val gamesString = sharedPreferences.getString(CACHED_GAMES_KEY, "") ?: ""
        return if (gamesString.isBlank()) {
            emptySet()
        } else {
            gamesString.split(DELIMITER)
                .mapNotNull { it.toLongOrNull() }
                .toSet()
        }
    }
    
    fun isGameCached(gameId: Long): Boolean {
        return getCachedGameIds().contains(gameId)
    }
    
    fun clearCache() {
        sharedPreferences.edit { remove(CACHED_GAMES_KEY) }
    }
    
    private fun saveCachedGameIds(gameIds: Set<Long>) {
        val gamesString = gameIds.joinToString(DELIMITER)
        sharedPreferences.edit { putString(CACHED_GAMES_KEY, gamesString) }
    }
}