package de.tadris.flang.network_api.model

class User(
    username: String,
    rating: Float,
    isBot: Boolean,
    val completedGames: Int,
    val online: Boolean,
    val registration: Long,
    val history: List<UserRatingEntry>,
    val ratings: List<Rating>,
    title: String,
    val puzzleRating: Float,
) : UserInfo(username, rating, isBot, title)