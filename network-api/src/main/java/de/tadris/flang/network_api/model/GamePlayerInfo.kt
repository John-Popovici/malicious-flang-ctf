package de.tadris.flang.network_api.model

class GamePlayerInfo(username: String,
                     rating: Float,
                     val time: Long,
                     val ratingDiff: Float,
                     isBot: Boolean,
                     title: String)
    : UserInfo(username, rating, isBot, title)