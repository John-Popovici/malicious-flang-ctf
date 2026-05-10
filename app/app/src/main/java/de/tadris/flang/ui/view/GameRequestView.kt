package de.tadris.flang.ui.view

import android.app.Activity
import android.view.ViewGroup
import de.tadris.flang.R
import de.tadris.flang.network_api.model.GameConfiguration
import de.tadris.flang.network_api.model.GameRequest
import de.tadris.flang.network_api.model.UserInfo
import de.tadris.flang_lib.utils.TimeUtils

class GameRequestView(
    context: Activity,
    parent: ViewGroup,
    request: GameRequest,
    private val listener: GameRequestListener?
) : AbstractGameRequestView<GameRequest>(context, parent, request) {

    override fun getRequester(): UserInfo = request.requester

    override fun getConfiguration(): GameConfiguration = request.configuration

    override fun getTimeDisplayText(): String {
        return if (request.configuration.infiniteTime) {
            context.getString(R.string.infiniteTimeChar)
        } else {
            TimeUtils.getTimeAsString(request.configuration.time)
        }
    }

    override fun onRequestClick() {
        listener?.onClick(request)
    }

    interface GameRequestListener {
        fun onClick(gameRequest: GameRequest)
    }
}