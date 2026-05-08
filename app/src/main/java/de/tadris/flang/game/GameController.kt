package de.tadris.flang.game

import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang.network_api.model.Premove
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.Color
import de.tadris.flang_lib.Move

interface GameController {

    fun registerCallback(callback: GameControllerCallback)

    // If the move is played on a different board that the game controller knows of, newBoardRequest is not null
    // This happens in creative games when the user is replaying moves
    fun onMoveRequested(move: Move, newBoardRequest: Game?, onCancel: (() -> Unit)? = null)

    fun onPremoveRequested(move: Premove){ }

    fun onPremoveClearRequested(){ }

    fun requestGame()

    fun resignGame()

    fun isCreativeGame(): Boolean

    fun stop()

    fun resume()

    fun arePremovesAllowed(): Boolean {
        return false
    }

    interface GameControllerCallback {

        fun onGameRequestSuccess(info: GameInfo, isParticipant: Boolean, color: Color?, game: Game? = null)

        fun onGameRequestFailed(reason: String)

        fun onUpdate(gameInfo: GameInfo)

        fun onUpdate(move: Move)

        fun onVisiblePremoveChanged(premove: Premove?)

    }

}