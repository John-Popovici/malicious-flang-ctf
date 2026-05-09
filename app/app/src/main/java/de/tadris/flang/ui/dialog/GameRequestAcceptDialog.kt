package de.tadris.flang.ui.dialog

import android.app.Activity
import android.widget.Toast
import androidx.annotation.IdRes
import de.tadris.flang.R
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.exception.NotFoundException
import de.tadris.flang.network_api.model.GameRequest
import java.lang.Exception

class GameRequestAcceptDialog(context: Activity, @IdRes private val navigation: Int, private val gameRequest: GameRequest) : GameRequestDialog(context, navigation, false) {

    override fun tryRequest() = DataRepository.getInstance().acceptGame(context, gameRequest)

    override fun onError(e: Exception): Boolean {
        if(e is NotFoundException){
            context.runOnUiThread {
                Toast.makeText(context, R.string.gameRequestNotFound, Toast.LENGTH_SHORT).show()
            }
            return true
        }
        return false
    }
}