package de.tadris.flang.ui.dialog

import android.app.Activity
import androidx.annotation.IdRes
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.exception.NotFoundException
import de.tadris.flang.network_api.model.GameConfiguration
import java.lang.Exception

class GameRequestAddDialog(context: Activity, @IdRes private val navigation: Int, private val configuration: GameConfiguration) : GameRequestDialog(context, navigation, true) {

    override fun tryRequest() = DataRepository.getInstance().requestGame(context, configuration)

    override fun onError(e: Exception) = e is NotFoundException

}