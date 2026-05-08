package de.tadris.flang.network_api.model

import kotlin.math.absoluteValue
import kotlin.math.roundToInt

const val BOT_TITLE = "BOT"

open class UserInfo(val username: String, val rating: Float, val isBot: Boolean, val title: String){

    fun hasTitle() = isBot || title.isNotEmpty()

    fun getDisplayedTitle() = if(isBot) BOT_TITLE else title

    fun getRatingText(): String {
        return if(rating == 0f){
            "?"
        }else{
            rating.absoluteValue.roundToInt().toString() + if(rating < 0) "?" else ""
        }
    }

}