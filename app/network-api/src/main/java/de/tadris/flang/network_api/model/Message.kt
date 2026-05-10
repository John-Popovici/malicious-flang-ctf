package de.tadris.flang.network_api.model

data class Message(val sender: UserInfo, val date: Long, val text: String, val game: GameAttachment?){

    companion object {

        const val SYSTEM_SENDER = "system"

    }

    val isSystemMessage get() = sender.username == SYSTEM_SENDER

}