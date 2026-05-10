package de.tadris.flang.ui.view

import android.app.Activity
import android.view.ViewGroup
import de.tadris.flang.R
import de.tadris.flang.network_api.model.DailyGameRequest
import de.tadris.flang.network_api.model.GameConfiguration
import de.tadris.flang.network_api.model.UserInfo
import de.tadris.flang_lib.utils.TimeUtils

class DailyGameRequestView(
    context: Activity,
    parent: ViewGroup,
    request: DailyGameRequest,
    private val listener: DailyGameRequestListener?
) : AbstractGameRequestView<DailyGameRequest>(context, parent, request) {

    override fun getRequester(): UserInfo = request.requester

    override fun getConfiguration(): GameConfiguration = request.configuration

    override fun getTimeDisplayText(): String {
        return TimeUtils.getSmartTimeDisplay(request.configuration.time)
    }

    override fun onRequestClick() {
        listener?.onClick(request)
    }

    interface DailyGameRequestListener {
        fun onClick(dailyGameRequest: DailyGameRequest)
    }
}