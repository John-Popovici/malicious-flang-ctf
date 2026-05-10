package de.tadris.flang.network_api.model

import de.tadris.flang_lib.Game
import de.tadris.flang_lib.Board

data class GameAttachment(val id: String, val fmn: String, val fbn: String){

    val isOnlineGame get() = id != "-1" && id.isNotEmpty()

    val game get() = if(fmn.isNotEmpty()){
        Game.fromFMN(fmn)
    } else {
        Game(initialBoard = Board.fromFBN(fbn))
    }

}