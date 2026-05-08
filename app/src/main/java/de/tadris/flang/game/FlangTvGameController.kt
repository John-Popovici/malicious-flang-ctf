package de.tadris.flang.game

import android.app.Activity
import de.tadris.flang.R
import de.tadris.flang.network.DataRepository
import java.lang.Exception
import kotlin.concurrent.thread

class FlangTvGameController(activity: Activity) : OnlineGameController(activity, -1) {

    override fun requestGame() {
        thread {
            try{
                gameId = DataRepository.getInstance().accessOpenAPI().tv().gameId
                if(gameId == -1L){
                    throw Exception(activity.getString(R.string.tvNoGamesAvailable))
                }
                activity.runOnUiThread {
                    super.requestGame()
                }
            }catch (e: Exception){
                e.printStackTrace()
                showError(e.message)
            } finally {
                startTvThread()
            }
        }
    }

    private fun startTvThread(){
        thread {
            while (!fragmentPaused){
                try{
                    val newGameId = DataRepository.getInstance().accessOpenAPI().tv().gameId
                    if(gameId != newGameId){
                        activity.runOnUiThread {
                            requestGame()
                        }
                        break // requestGame() will start a new tvThread
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                    showError(e.message)
                }
                Thread.sleep(15000)
            }
        }
    }

    override fun resume() {
        super.resume()
        startTvThread()
    }
}