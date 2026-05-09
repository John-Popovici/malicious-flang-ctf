package de.tadris.flang.network_api.util

import de.tadris.flang.network_api.model.GameConfiguration

object DefaultConfigurations {

    fun getConfigurations(): List<Pair<String, GameConfiguration>> {
        val map = mutableListOf<Pair<String, GameConfiguration>>()

        map+= Pair("Bullet", GameConfiguration(isRated = true, infiniteTime = false, time = 60000 * 1))
        map+= Pair("Bullet", GameConfiguration(isRated = true, infiniteTime = false, time = 60000 * 2, timeIncrement = 1000))
        map+= Pair("Blitz", GameConfiguration(isRated = true, infiniteTime = false, time = 60000 * 3))
        map+= Pair("Blitz", GameConfiguration(isRated = true, infiniteTime = false, time = 60000 * 5))
        map+= Pair("Rapid", GameConfiguration(isRated = true, infiniteTime = false, time = 60000 * 10))
        map+= Pair("Classical", GameConfiguration(isRated = true, infiniteTime = false, time = 60000 * 30))

        // Bot requests
        map+= Pair("Bot Bullet", GameConfiguration(isRated = true, infiniteTime = false, time = 60000 * 2, isBotRequest = true, timeIncrement = 1000))
        map+= Pair("Bot Blitz", GameConfiguration(isRated = true, infiniteTime = false, time = 60000 * 5, isBotRequest = true))
        map+= Pair("Bot Rapid", GameConfiguration(isRated = true, infiniteTime = false, time = 60000 * 15, isBotRequest = true))
        
        // Daily game configurations
        map+= Pair("Daily", GameConfiguration(isRated = true, infiniteTime = true, time = 86400000)) // 24 hours
        map+= Pair("Daily", GameConfiguration(isRated = true, infiniteTime = true, time = 172800000)) // 48 hours

        // Custom
        map+= Pair("Custom", GameConfiguration(isRated = true, infiniteTime = false, time = GameConfiguration.CUSTOM_GAME_TIME, isBotRequest = true))

        return map
    }

}