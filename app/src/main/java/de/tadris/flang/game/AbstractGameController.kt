package de.tadris.flang.game

import android.app.Activity

abstract class AbstractGameController(protected val activity: Activity) : GameController {

    protected lateinit var callback: GameController.GameControllerCallback

    override fun registerCallback(callback: GameController.GameControllerCallback) {
        this.callback = callback
    }

    override fun stop() { }

    override fun resume() { }
}